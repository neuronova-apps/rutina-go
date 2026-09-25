package com.neuronovaapps.rutinago

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.neuronovaapps.rutinago.data.catalog.importer.AndroidCatalogAssetSource
import com.neuronovaapps.rutinago.data.catalog.importer.MasterCatalogImporter
import com.neuronovaapps.rutinago.data.catalog.local.RutinaGoDatabase
import com.neuronovaapps.rutinago.data.execution.repository.RoomRoutineExecutionRepository
import com.neuronovaapps.rutinago.data.user.repository.RoomUserRoutineRepository
import com.neuronovaapps.rutinago.domain.execution.ExecutionStatus
import com.neuronovaapps.rutinago.domain.execution.ExecutionStepStatus
import com.neuronovaapps.rutinago.domain.execution.RoutineExecutionValidationException
import com.neuronovaapps.rutinago.domain.user.UserRoutineDraft
import com.neuronovaapps.rutinago.domain.user.UserRoutineDraftStep
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineExecutionRoomTest {
    private lateinit var context: Context
    private lateinit var database: RutinaGoDatabase
    private lateinit var routines: RoomUserRoutineRepository
    private lateinit var executions: RoomRoutineExecutionRepository
    private val clock = AtomicLong(100_000L)
    private val ids = AtomicInteger()

    @Before
    fun setUp() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RutinaGoDatabase::class.java).build()
        MasterCatalogImporter(database, AndroidCatalogAssetSource(context)).importIfNeeded()
        routines = RoomUserRoutineRepository(database) { clock.incrementAndGet() }
        executions = RoomRoutineExecutionRepository(
            database = database,
            clock = { clock.incrementAndGet() },
            idFactory = { "execution-${ids.incrementAndGet()}" },
        )
        routines.save(
            PROFILE_A,
            UserRoutineDraft(
                id = ROUTINE_ID,
                sourceTemplateId = null,
                name = "Rutina original",
                steps = listOf(
                    UserRoutineDraftStep("ACT-001", "Primera instrucción personal"),
                    UserRoutineDraftStep("ACT-002"),
                    UserRoutineDraftStep("ACT-003"),
                ),
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun startsOwnedRoutineWithOrderedSnapshotAndDoesNotDuplicatePendingExecution() = runBlocking {
        val first = executions.start(PROFILE_A, ROUTINE_ID)
        val second = executions.start(PROFILE_A, ROUTINE_ID)

        assertEquals(first.id, second.id)
        assertEquals(PROFILE_A, first.profileId)
        assertEquals("Rutina original", first.routineName)
        assertEquals(listOf(1, 2, 3), first.steps.map { it.position })
        assertEquals("Primera instrucción personal", first.steps.first().instruction)
        assertEquals(1, first.currentStepPosition)
        assertEquals(ExecutionStatus.IN_PROGRESS, first.status)

        captureExecutionFailure { executions.start(PROFILE_B, ROUTINE_ID) }
    }

    @Test
    fun completesSkipsAndPreventsDoubleCounting() = runBlocking {
        val started = executions.start(PROFILE_A, ROUTINE_ID)
        val afterComplete = executions.completeStep(PROFILE_A, started.id, 1)
        assertEquals(1, afterComplete.completedSteps)
        assertEquals(2, afterComplete.currentStepPosition)
        assertEquals(ExecutionStepStatus.COMPLETED, afterComplete.steps.first().status)

        captureExecutionFailure { executions.completeStep(PROFILE_A, started.id, 1) }
        val unchanged = executions.execution(started.id).first()
        assertEquals(1, unchanged?.completedSteps)

        executions.completeStep(PROFILE_A, started.id, 2)
        val result = executions.skipStep(PROFILE_A, started.id, 3)
        assertEquals(ExecutionStatus.PARTIAL, result.status)
        assertEquals(2, result.completedSteps)
        assertEquals(1, result.skippedSteps)
        assertNotNull(result.finishedAtEpochMillis)
        assertNull(executions.activeExecution(PROFILE_A).first())
    }

    @Test
    fun allCompletedProducesSingleCompletedResultAndRealProgress() = runBlocking {
        val started = executions.start(PROFILE_A, ROUTINE_ID)
        executions.completeStep(PROFILE_A, started.id, 1)
        executions.completeStep(PROFILE_A, started.id, 2)
        val result = executions.completeStep(PROFILE_A, started.id, 3)
        val repeated = executions.completeStep(PROFILE_A, started.id, 3)

        assertEquals(ExecutionStatus.COMPLETED, result.status)
        assertEquals(result, repeated)
        val progress = executions.progress(PROFILE_A).first()
        assertEquals(1, progress.completedRoutines)
        assertEquals(0, progress.partialRoutines)
        assertEquals(3, progress.completedActivities)
        assertEquals(1, progress.history.size)
    }

    @Test
    fun abandonKeepsActualStepsButIsNotCountedAsCompleted() = runBlocking {
        val started = executions.start(PROFILE_A, ROUTINE_ID)
        executions.completeStep(PROFILE_A, started.id, 1)
        val abandoned = executions.abandon(PROFILE_A, started.id)

        assertEquals(ExecutionStatus.ABANDONED, abandoned.status)
        assertEquals(1, abandoned.completedSteps)
        assertEquals(0, abandoned.skippedSteps)
        val progress = executions.progress(PROFILE_A).first()
        assertEquals(0, progress.completedRoutines)
        assertEquals(0, progress.partialRoutines)
        assertEquals(1, progress.completedActivities)
        assertEquals(ExecutionStatus.ABANDONED, progress.history.single().status)
    }

    @Test
    fun explicitPartialAndProfileOwnershipAreEnforced() = runBlocking {
        val started = executions.start(PROFILE_A, ROUTINE_ID)
        executions.completeStep(PROFILE_A, started.id, 1)
        val partial = executions.finishPartial(PROFILE_A, started.id)

        assertEquals(ExecutionStatus.PARTIAL, partial.status)
        assertEquals(1, partial.completedSteps)
        assertEquals(2, partial.steps.count { it.status == ExecutionStepStatus.PENDING })
        captureExecutionFailure { executions.abandon(PROFILE_B, started.id) }
        assertTrue(executions.progress(PROFILE_B).first().history.isEmpty())
    }

    @Test
    fun editingAndArchivingRoutineDoNotRewriteExecutionSnapshot() = runBlocking {
        val started = executions.start(PROFILE_A, ROUTINE_ID)
        routines.save(
            PROFILE_A,
            UserRoutineDraft(
                id = ROUTINE_ID,
                sourceTemplateId = null,
                name = "Rutina editada",
                steps = listOf(UserRoutineDraftStep("ACT-004", "Contenido nuevo")),
            ),
        )
        routines.archive(PROFILE_A, ROUTINE_ID)

        val restored = executions.execution(started.id).first()
        assertEquals("Rutina original", restored?.routineName)
        assertEquals(listOf("ACT-001", "ACT-002", "ACT-003"), restored?.steps?.map { it.activityId })
        assertEquals("Primera instrucción personal", restored?.steps?.first()?.instruction)
    }

    @Test
    fun pendingExecutionIsRecoveredByANewRepositoryInstance() = runBlocking {
        val started = executions.start(PROFILE_A, ROUTINE_ID)
        executions.completeStep(PROFILE_A, started.id, 1)

        val restored = RoomRoutineExecutionRepository(database).activeExecution(PROFILE_A).first()
        assertEquals(started.id, restored?.id)
        assertEquals(2, restored?.currentStepPosition)
        assertEquals(1, restored?.completedSteps)
        assertNotEquals(PROFILE_B, restored?.profileId)
    }

    @Test
    fun executionIsRecoveredAfterDatabaseIsClosedAndReopened() = runBlocking {
        val databaseName = "phase-2-6-recovery.db"
        context.deleteDatabase(databaseName)
        var reopened: RutinaGoDatabase? = null
        try {
            val firstDatabase = Room.databaseBuilder(context, RutinaGoDatabase::class.java, databaseName)
                .addMigrations(RutinaGoDatabase.MIGRATION_1_2, RutinaGoDatabase.MIGRATION_2_3)
                .build()
            MasterCatalogImporter(firstDatabase, AndroidCatalogAssetSource(context)).importIfNeeded()
            RoomUserRoutineRepository(firstDatabase).save(
                PROFILE_A,
                UserRoutineDraft(
                    id = "disk-routine",
                    sourceTemplateId = null,
                    name = "Rutina persistente",
                    steps = listOf(
                        UserRoutineDraftStep("ACT-001"),
                        UserRoutineDraftStep("ACT-002"),
                    ),
                ),
            )
            val firstRepository = RoomRoutineExecutionRepository(firstDatabase)
            val started = firstRepository.start(PROFILE_A, "disk-routine")
            firstRepository.completeStep(PROFILE_A, started.id, 1)
            firstDatabase.close()

            reopened = Room.databaseBuilder(context, RutinaGoDatabase::class.java, databaseName)
                .addMigrations(RutinaGoDatabase.MIGRATION_1_2, RutinaGoDatabase.MIGRATION_2_3)
                .build()
            val restored = RoomRoutineExecutionRepository(reopened).activeExecution(PROFILE_A).first()
            assertEquals(started.id, restored?.id)
            assertEquals(1, restored?.completedSteps)
            assertEquals(2, restored?.currentStepPosition)
        } finally {
            reopened?.close()
            context.deleteDatabase(databaseName)
        }
    }

    private suspend fun captureExecutionFailure(block: suspend () -> Unit) {
        try {
            block()
            throw AssertionError("Expected RoutineExecutionValidationException")
        } catch (error: RoutineExecutionValidationException) {
            assertNotNull(error.message)
        }
    }

    private companion object {
        const val PROFILE_A = "profile-a"
        const val PROFILE_B = "profile-b"
        const val ROUTINE_ID = "personal-routine"
    }
}

@RunWith(AndroidJUnit4::class)
class RutinaGoExecutionMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RutinaGoDatabase::class.java,
    )

    @Test
    fun migration2To3PreservesCatalogAndPersonalRoutines() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            execSQL(
                "INSERT INTO catalog_categories (name, sort_order, catalog_version) " +
                    "VALUES ('Categoría conservada', 1, 'test')",
            )
            execSQL(
                "INSERT INTO user_routines " +
                    "(user_routine_id, profile_id, source_template_id, name, description, " +
                    "created_at_epoch_millis, modified_at_epoch_millis, archived) " +
                    "VALUES ('personal', 'profile', NULL, 'Conservada', NULL, 1, 1, 0)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            3,
            true,
            RutinaGoDatabase.MIGRATION_2_3,
        ).use { migrated ->
            migrated.query("SELECT name FROM catalog_categories").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Categoría conservada", cursor.getString(0))
            }
            migrated.query("SELECT name FROM user_routines").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Conservada", cursor.getString(0))
            }
            migrated.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'routine_executions'",
            ).use { cursor -> assertTrue(cursor.moveToFirst()) }
            migrated.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'execution_steps'",
            ).use { cursor -> assertTrue(cursor.moveToFirst()) }
        }
    }

    private companion object {
        const val TEST_DATABASE = "phase-2-6-migration.db"
    }
}

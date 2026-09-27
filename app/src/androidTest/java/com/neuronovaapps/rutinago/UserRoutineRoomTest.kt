package com.neuronovaapps.rutinago

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.neuronovaapps.rutinago.data.catalog.importer.AndroidCatalogAssetSource
import com.neuronovaapps.rutinago.data.catalog.importer.MasterCatalogImporter
import com.neuronovaapps.rutinago.data.catalog.local.RutinaGoDatabase
import com.neuronovaapps.rutinago.data.preferences.UserPreferencesRepository
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.data.execution.repository.RoomRoutineExecutionRepository
import com.neuronovaapps.rutinago.data.user.repository.RoomUserRoutineRepository
import com.neuronovaapps.rutinago.domain.execution.ExecutionStatus
import com.neuronovaapps.rutinago.domain.user.UserRoutineDraft
import com.neuronovaapps.rutinago.domain.user.UserRoutineDraftStep
import com.neuronovaapps.rutinago.domain.user.UserRoutineDeletionBlockedException
import com.neuronovaapps.rutinago.domain.user.UserRoutineValidationException
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserRoutineRoomTest {
    private lateinit var context: Context
    private lateinit var database: RutinaGoDatabase
    private lateinit var repository: RoomUserRoutineRepository
    private val clock = AtomicLong(10_000L)

    @Before
    fun setUp() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RutinaGoDatabase::class.java).build()
        MasterCatalogImporter(database, AndroidCatalogAssetSource(context)).importIfNeeded()
        repository = RoomUserRoutineRepository(database) { clock.incrementAndGet() }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createsTemplateAndScratchRoutinesWithoutChangingMasterCatalog() = runBlocking {
        val templateSteps = database.catalogReadDao()
            .getRoutineSteps("RUT-001", com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant.SIMPLE)
            .map { UserRoutineDraftStep(it.activityId) }
        repository.save(
            PROFILE_A,
            UserRoutineDraft("user-template", "RUT-001", "Mi mañana", steps = templateSteps),
        )
        repository.save(
            PROFILE_A,
            UserRoutineDraft(
                "user-scratch",
                null,
                "Rutina escolar",
                steps = listOf(UserRoutineDraftStep("ACT-010"), UserRoutineDraftStep("ACT-011")),
            ),
        )

        val saved = repository.activeRoutines(PROFILE_A).first { it.size == 2 }
        assertEquals("RUT-001", saved.first { it.id == "user-template" }.sourceTemplateId)
        assertEquals(listOf("ACT-010", "ACT-011"), saved.first { it.id == "user-scratch" }.steps.map { it.activityId })
        assertEquals(17, database.catalogWriteDao().categoryCount())
        assertEquals(203, database.catalogWriteDao().activityCount())
        assertEquals(52, database.catalogWriteDao().routineCount())
        assertEquals(490, database.catalogWriteDao().routineStepCount())
    }

    @Test
    fun editsReordersRemovesRetriesAndArchivesWithoutDuplicates() = runBlocking {
        val original = UserRoutineDraft(
            id = "stable-retry-id",
            sourceTemplateId = null,
            name = "Primera versión",
            steps = listOf(
                UserRoutineDraftStep("ACT-001"),
                UserRoutineDraftStep("ACT-002"),
                UserRoutineDraftStep("ACT-003"),
            ),
        )
        val firstSave = repository.save(PROFILE_A, original)
        repository.save(PROFILE_A, original)
        assertEquals(1, database.userRoutineDao().routineCount(PROFILE_A))

        val edited = repository.save(
            PROFILE_A,
            original.copy(
                name = "Versión editada",
                steps = listOf(
                    UserRoutineDraftStep("ACT-003", customInstruction = "Instrucción personal"),
                    UserRoutineDraftStep("ACT-001"),
                ),
            ),
        )
        assertEquals(firstSave.createdAtEpochMillis, edited.createdAtEpochMillis)
        assertTrue(edited.modifiedAtEpochMillis > firstSave.modifiedAtEpochMillis)
        assertEquals(listOf("ACT-003", "ACT-001"), edited.steps.map { it.activityId })
        assertEquals(listOf(1, 2), edited.steps.map { it.position })
        assertEquals("Instrucción personal", edited.steps.first().customInstruction)

        repository.archive(PROFILE_A, original.id)
        assertTrue(repository.activeRoutines(PROFILE_A).first().isEmpty())
        assertEquals(1, database.userRoutineDao().routineCount(PROFILE_A))
    }

    @Test
    fun profilesRemainSeparatedAndProfileRenameKeepsStableOwnership() = runBlocking {
        repository.save(PROFILE_A, draft("routine-a", "ACT-001"))
        repository.save(PROFILE_B, draft("routine-b", "ACT-002"))

        assertEquals(listOf("routine-a"), repository.activeRoutines(PROFILE_A).first().map { it.id })
        assertEquals(listOf("routine-b"), repository.activeRoutines(PROFILE_B).first().map { it.id })

        val preferences = UserPreferencesRepository(context)
        preferences.createProfile("Antes", PROFILE_A)
        preferences.renameProfile(PROFILE_A, "Después")
        preferences.setAppMode(AppMode.ADVANCED)
        preferences.setAppMode(AppMode.SIMPLE)
        assertEquals("Después", preferences.preferences.first().profiles.first { it.id == PROFILE_A }.name)
        assertEquals(AppMode.SIMPLE, preferences.preferences.first().appMode)
        assertEquals("routine-a", repository.activeRoutines(PROFILE_A).first().single().id)
    }

    @Test
    fun deletingOwnedRoutineCascadesItsStepsAndFlowUpdatesWithoutTouchingOthers() = runBlocking {
        repository.save(PROFILE_A, UserRoutineDraft(
            "delete-me", null, "Rutina temporal",
            steps = listOf(UserRoutineDraftStep("ACT-001"), UserRoutineDraftStep("ACT-002")),
        ))
        repository.save(PROFILE_A, draft("keep-a", "ACT-003"))
        repository.save(PROFILE_B, draft("keep-b", "ACT-004"))
        val counts = mutableListOf<Int>()
        val collector = launch {
            repository.activeRoutines(PROFILE_A).map { it.size }
                .distinctUntilChanged().take(2).collect { counts += it }
        }
        withTimeout(5_000) { while (counts.isEmpty()) yield() }
        assertEquals(2, database.userRoutineDao().stepCount("delete-me"))

        repository.delete(PROFILE_A, "delete-me")
        withTimeout(5_000) { collector.join() }

        assertEquals(listOf(2, 1), counts)
        assertEquals(0, database.userRoutineDao().stepCount("delete-me"))
        assertEquals(listOf("keep-a"), repository.activeRoutines(PROFILE_A).first().map { it.id })
        assertEquals(listOf("keep-b"), repository.activeRoutines(PROFILE_B).first().map { it.id })
        assertEquals(52, database.catalogWriteDao().routineCount())
        assertEquals(203, database.catalogWriteDao().activityCount())
    }

    @Test
    fun completedSnapshotSurvivesDeletingItsOriginalRoutine() = runBlocking {
        repository.save(PROFILE_A, draft("historical", "ACT-001"))
        val executions = RoomRoutineExecutionRepository(database)
        val started = executions.start(PROFILE_A, "historical")
        executions.completeStep(PROFILE_A, started.id, 1)

        repository.delete(PROFILE_A, "historical")

        assertEquals(0, database.userRoutineDao().routineCount(PROFILE_A))
        assertEquals(0, database.userRoutineDao().stepCount("historical"))
        val snapshot = executions.execution(started.id).first()
        assertEquals(ExecutionStatus.COMPLETED, snapshot?.status)
        assertEquals("ACT-001", snapshot?.steps?.single()?.activityId)
        assertEquals(1, executions.progress(PROFILE_A).first().history.size)
    }

    @Test
    fun activeExecutionBlocksDeletionUntilItFinishes() = runBlocking {
        repository.save(PROFILE_A, draft("active", "ACT-001"))
        repository.save(PROFILE_A, draft("unrelated", "ACT-002"))
        val executions = RoomRoutineExecutionRepository(database)
        val started = executions.start(PROFILE_A, "active")

        try {
            repository.delete(PROFILE_A, "active")
            throw AssertionError("Expected UserRoutineDeletionBlockedException")
        } catch (error: UserRoutineDeletionBlockedException) {
            assertTrue(error.message.orEmpty().contains("ejecución en curso"))
        }
        assertEquals(1, database.userRoutineDao().stepCount("active"))
        repository.delete(PROFILE_A, "unrelated")
        assertEquals(listOf("active"), repository.activeRoutines(PROFILE_A).first().map { it.id })

        executions.completeStep(PROFILE_A, started.id, 1)
        repository.delete(PROFILE_A, "active")
        assertTrue(repository.activeRoutines(PROFILE_A).first().isEmpty())
        assertEquals(1, executions.progress(PROFILE_A).first().history.size)
    }

    @Test
    fun wrongProfileAndMasterTemplateCannotBeDeleted() = runBlocking {
        repository.save(PROFILE_A, draft("private-a", "ACT-001"))
        repository.save(PROFILE_B, draft("private-b", "ACT-002"))

        captureValidationFailure { repository.delete(PROFILE_B, "private-a") }
        captureValidationFailure { repository.delete(PROFILE_A, "RUT-001") }

        assertEquals(1, database.userRoutineDao().stepCount("private-a"))
        assertEquals(listOf("private-a"), repository.activeRoutines(PROFILE_A).first().map { it.id })
        assertEquals(listOf("private-b"), repository.activeRoutines(PROFILE_B).first().map { it.id })
        assertEquals(52, database.catalogWriteDao().routineCount())
    }

    @Test
    fun rejectsMissingActivitiesAndCrossProfileRoutineTakeover() = runBlocking {
        captureValidationFailure {
            repository.save(PROFILE_A, draft("blank-name", "ACT-001").copy(name = "  "))
        }
        captureValidationFailure {
            repository.save(PROFILE_A, draft("empty-steps", "ACT-001").copy(steps = emptyList()))
        }
        captureValidationFailure {
            repository.save(PROFILE_A, draft("invalid", "ACT-999"))
        }
        assertEquals(0, database.userRoutineDao().routineCount(PROFILE_A))

        repository.save(PROFILE_A, draft("owned", "ACT-001"))
        captureValidationFailure {
            repository.save(PROFILE_B, draft("owned", "ACT-002"))
        }
        assertEquals("ACT-001", repository.activeRoutines(PROFILE_A).first().single().steps.single().activityId)
        assertTrue(repository.activeRoutines(PROFILE_B).first().isEmpty())
        captureValidationFailure { repository.archive(PROFILE_B, "owned") }
        assertEquals("owned", repository.activeRoutines(PROFILE_A).first().single().id)
    }

    @Test
    fun routineIsRecoveredAfterDatabaseIsClosedAndReopened() = runBlocking {
        val databaseName = "phase-2-5-recovery.db"
        context.deleteDatabase(databaseName)
        try {
            val firstDatabase = Room.databaseBuilder(context, RutinaGoDatabase::class.java, databaseName)
                .addMigrations(RutinaGoDatabase.MIGRATION_1_2)
                .build()
            MasterCatalogImporter(firstDatabase, AndroidCatalogAssetSource(context)).importIfNeeded()
            RoomUserRoutineRepository(firstDatabase).save(
                PROFILE_A,
                draft("persistent-routine", "ACT-001"),
            )
            firstDatabase.close()

            val reopened = Room.databaseBuilder(context, RutinaGoDatabase::class.java, databaseName)
                .addMigrations(RutinaGoDatabase.MIGRATION_1_2)
                .build()
            try {
                val restored = RoomUserRoutineRepository(reopened)
                    .activeRoutines(PROFILE_A).first().single()
                assertEquals("persistent-routine", restored.id)
                assertEquals("ACT-001", restored.steps.single().activityId)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    private fun draft(id: String, activityId: String) = UserRoutineDraft(
        id = id,
        sourceTemplateId = null,
        name = "Rutina $id",
        steps = listOf(UserRoutineDraftStep(activityId)),
    )

    private suspend fun captureValidationFailure(block: suspend () -> Unit) {
        try {
            block()
            throw AssertionError("Expected UserRoutineValidationException")
        } catch (error: UserRoutineValidationException) {
            assertNotNull(error.message)
        }
    }

    private companion object {
        const val PROFILE_A = "profile-a-stable-id"
        const val PROFILE_B = "profile-b-stable-id"
    }
}

@RunWith(AndroidJUnit4::class)
class RutinaGoDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RutinaGoDatabase::class.java,
    )

    @Test
    fun migration1To2PreservesCatalogAndCreatesUserRoutineTables() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                "INSERT INTO catalog_categories (name, sort_order, catalog_version) " +
                    "VALUES ('Categoría conservada', 1, 'test')",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            RutinaGoDatabase.MIGRATION_1_2,
        ).use { migrated ->
            migrated.query(
                "SELECT name FROM catalog_categories WHERE name = 'Categoría conservada'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Categoría conservada", cursor.getString(0))
            }
            migrated.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'user_routines'",
            ).use { cursor -> assertTrue(cursor.moveToFirst()) }
            migrated.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'user_routine_steps'",
            ).use { cursor -> assertTrue(cursor.moveToFirst()) }
        }
    }

    private companion object {
        const val TEST_DATABASE = "phase-2-5-migration.db"
    }
}

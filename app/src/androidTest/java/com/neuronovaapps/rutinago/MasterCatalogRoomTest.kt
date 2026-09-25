package com.neuronovaapps.rutinago

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.neuronovaapps.rutinago.data.catalog.importer.CatalogAssetSource
import com.neuronovaapps.rutinago.data.catalog.importer.CatalogImportErrorCode
import com.neuronovaapps.rutinago.data.catalog.importer.CatalogImportException
import com.neuronovaapps.rutinago.data.catalog.importer.CatalogImportOutcome
import com.neuronovaapps.rutinago.data.catalog.importer.CatalogJsonParser
import com.neuronovaapps.rutinago.data.catalog.importer.MasterCatalogImporter
import com.neuronovaapps.rutinago.data.catalog.local.RutinaGoDatabase
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogImportStatus
import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant
import com.neuronovaapps.rutinago.data.catalog.repository.RoomCatalogRepository
import com.neuronovaapps.rutinago.data.preferences.UserPreferencesRepository
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MasterCatalogRoomTest {
    private lateinit var context: Context
    private lateinit var database: RutinaGoDatabase
    private lateinit var source: MutableCatalogAssetSource
    private val clock = AtomicLong(1_000L)

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RutinaGoDatabase::class.java).build()
        source = MutableCatalogAssetSource.fromPackagedAssets(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun databaseOpensImportsCompleteCatalogAndSupportsQueries() = runBlocking {
        assertTrue(database.isOpen)
        val result = importer().importIfNeeded()
        assertEquals(CatalogImportOutcome.IMPORTED, result.outcome)
        assertEquals(369, result.counts.masterRecords)

        val writeDao = database.catalogWriteDao()
        assertEquals(17, writeDao.categoryCount())
        assertEquals(203, writeDao.activityCount())
        assertEquals(52, writeDao.routineCount())
        assertEquals(490, writeDao.routineStepCount())
        assertEquals(40, writeDao.achievementCount())
        assertEquals(20, writeDao.challengeCount())
        assertEquals(54, writeDao.messageCount())
        assertEquals(203, writeDao.visualReferenceCount())

        val readDao = database.catalogReadDao()
        assertEquals("Despertarse", readDao.getActivityById("ACT-001")?.name)
        assertNotNull(readDao.getRoutineById("RUT-001"))
        val simpleSteps = readDao.getRoutineSteps("RUT-001", RoutineVariant.SIMPLE)
        assertEquals(listOf(1, 2, 3), simpleSteps.map { it.position })
        assertEquals(listOf("ACT-001", "ACT-004", "ACT-008"), simpleSteps.map { it.activityId })

        val repository = RoomCatalogRepository(readDao)
        assertEquals(17, repository.categories().first().size)
        assertEquals(203, repository.activities().first().size)
        assertEquals(52, repository.routines().first().size)
        assertEquals(490, repository.routineSteps().first().size)
        assertEquals(
            10,
            repository.activitiesByCategory("Inicio del día").first().size,
        )
        assertEquals(
            3,
            repository.routinesByCategory("Inicio del día").first().size,
        )
        assertEquals(40, repository.achievements().first().size)
        assertEquals(20, repository.challenges().first().size)
        assertEquals(54, repository.reinforcementMessages().first().size)
    }

    @Test
    fun secondImportIsIdempotentAndDoesNotAddMetadataAttempt() = runBlocking {
        val catalogImporter = importer()
        catalogImporter.importIfNeeded()
        val attemptsAfterFirst = database.catalogImportMetadataDao().attemptCount()
        val second = catalogImporter.importIfNeeded()

        assertEquals(CatalogImportOutcome.ALREADY_CURRENT, second.outcome)
        assertEquals(attemptsAfterFirst, database.catalogImportMetadataDao().attemptCount())
        assertEquals(203, database.catalogWriteDao().activityCount())
        assertEquals(490, database.catalogWriteDao().routineStepCount())
    }

    @Test
    fun validVersionUpdateUsesStableIdsAndKeepsCounts() = runBlocking {
        importer().importIfNeeded()
        val updated = source.versioned("1.0.1") { filename, json ->
            if (filename == "activities.json") {
                json.getJSONArray("items").getJSONObject(0)
                    .put("name", "Despertarse actualizado")
            }
        }
        val result = importer(updated).importIfNeeded()

        assertEquals(CatalogImportOutcome.UPDATED, result.outcome)
        assertEquals("1.0.1", result.catalogVersion)
        assertEquals(
            "Despertarse actualizado",
            database.catalogReadDao().getActivityById("ACT-001")?.name,
        )
        assertEquals(203, database.catalogWriteDao().activityCount())
        assertEquals(490, database.catalogWriteDao().routineStepCount())
        assertEquals(
            "1.0.1",
            database.catalogImportMetadataDao().latestWithStatus()?.catalogVersion,
        )
    }

    @Test
    fun checksumFailureLeavesCatalogEmptyAndRecordsFailure() = runBlocking {
        source.files["activities.json"] =
            source.files.getValue("activities.json") + byteArrayOf(0x20)
        val error = captureImportFailure { importer().importIfNeeded() }

        assertEquals(CatalogImportErrorCode.CHECKSUM_MISMATCH, error.code)
        assertEquals(0, database.catalogWriteDao().activityCount())
        assertEquals(0, database.catalogWriteDao().routineStepCount())
        assertEquals(
            CatalogImportStatus.FAILED,
            database.catalogImportMetadataDao().latestAttempt()?.status,
        )
    }

    @Test
    fun missingActivityReferenceIsRejectedWithoutReplacingCurrentCatalog() = runBlocking {
        importer().importIfNeeded()
        val invalid = source.versioned("1.0.1") { filename, json ->
            if (filename == "routine_steps.json") {
                json.getJSONArray("items").getJSONObject(0).put("activityId", "ACT-999")
            }
        }
        val error = captureImportFailure { importer(invalid).importIfNeeded() }

        assertEquals(CatalogImportErrorCode.INVALID_DATA, error.code)
        assertEquals("Despertarse", database.catalogReadDao().getActivityById("ACT-001")?.name)
        assertEquals(203, database.catalogWriteDao().activityCount())
        assertEquals(490, database.catalogWriteDao().routineStepCount())
        assertEquals(
            CatalogImportStatus.FAILED,
            database.catalogImportMetadataDao().latestAttempt()?.status,
        )
    }

    @Test
    fun roomTransactionRollsBackWhenDatabaseRejectsVersionUpdate() = runBlocking {
        importer().importIfNeeded()
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_catalog_update BEFORE UPDATE ON catalog_categories " +
                "WHEN NEW.catalog_version = '1.0.1' " +
                "BEGIN SELECT RAISE(ABORT, 'forced rollback'); END",
        )
        val updated = source.versioned("1.0.1") { filename, json ->
            if (filename == "activities.json") {
                json.getJSONArray("items").getJSONObject(0)
                    .put("name", "No debe persistir")
            }
        }
        captureImportFailure { importer(updated).importIfNeeded() }

        assertEquals("Despertarse", database.catalogReadDao().getActivityById("ACT-001")?.name)
        assertEquals(203, database.catalogWriteDao().activityCount())
        assertEquals(
            "1.0.0",
            database.catalogImportMetadataDao().latestWithStatus()?.catalogVersion,
        )
    }

    @Test
    fun roomImportDoesNotChangeDataStorePreferences() = runBlocking {
        val preferences = UserPreferencesRepository(context)
        try {
            preferences.setHighContrast(true)
            assertTrue(preferences.preferences.first { it.highContrast }.highContrast)
            importer().importIfNeeded()
            assertTrue(preferences.preferences.first().highContrast)
        } finally {
            preferences.setHighContrast(false)
        }
    }

    private fun importer(assetSource: CatalogAssetSource = source) =
        MasterCatalogImporter(
            database = database,
            assetSource = assetSource,
            clock = { clock.incrementAndGet() },
            attemptId = { "attempt-" + clock.get() },
        )

    private suspend fun captureImportFailure(
        block: suspend () -> Unit,
    ): CatalogImportException {
        return try {
            block()
            throw AssertionError("Expected catalog import to fail.")
        } catch (error: CatalogImportException) {
            error
        }
    }

    private class MutableCatalogAssetSource(
        val files: MutableMap<String, ByteArray>,
    ) : CatalogAssetSource {
        override fun readBytes(filename: String): ByteArray =
            files[filename]?.copyOf()
                ?: throw IllegalArgumentException("Missing test asset $filename")

        fun versioned(
            version: String,
            mutate: (String, JSONObject) -> Unit,
        ): MutableCatalogAssetSource {
            val changed = mutableMapOf<String, ByteArray>()
            CatalogJsonParser.ARTIFACT_NAMES.forEach { filename ->
                val json = JSONObject(files.getValue(filename).toString(Charsets.UTF_8))
                json.put("catalogVersion", version)
                mutate(filename, json)
                changed[filename] = (json.toString(2) + "\n").toByteArray()
            }
            val manifest = JSONObject(files.getValue("manifest.json").toString(Charsets.UTF_8))
            manifest.put("catalogVersion", version)
            val artifactMetadata = manifest.getJSONObject("artifacts")
            changed.forEach { (filename, bytes) ->
                artifactMetadata.getJSONObject(filename)
                    .put("sha256", CatalogJsonParser.sha256(bytes))
            }
            changed["manifest.json"] = (manifest.toString(2) + "\n").toByteArray()
            return MutableCatalogAssetSource(changed)
        }

        companion object {
            fun fromPackagedAssets(context: Context): MutableCatalogAssetSource {
                val names = CatalogJsonParser.ARTIFACT_NAMES + CatalogJsonParser.MANIFEST
                val files = names.associateWith { filename ->
                    context.assets.open("master_catalog/v1/$filename").use { it.readBytes() }
                }.toMutableMap()
                return MutableCatalogAssetSource(files)
            }
        }
    }
}

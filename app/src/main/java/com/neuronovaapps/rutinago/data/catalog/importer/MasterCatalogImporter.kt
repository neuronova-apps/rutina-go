package com.neuronovaapps.rutinago.data.catalog.importer

import androidx.room.withTransaction
import com.neuronovaapps.rutinago.data.catalog.local.RutinaGoDatabase
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogImportMetadataEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogImportStatus
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class MasterCatalogImporter(
    private val database: RutinaGoDatabase,
    private val assetSource: CatalogAssetSource,
    private val clock: () -> Long = System::currentTimeMillis,
    private val attemptId: () -> String = { UUID.randomUUID().toString() },
) {
    private val mutex = Mutex()
    private val parser = CatalogJsonParser(assetSource)
    private val validator = CatalogBundleValidator()

    suspend fun importIfNeeded(): CatalogImportResult = mutex.withLock {
        var bundle: CatalogBundle? = null
        try {
            bundle = parser.parse()
            validator.validate(bundle)
            val previousSuccess = database.catalogImportMetadataDao().latestWithStatus()
            val currentCounts = databaseCounts()
            if (
                previousSuccess?.catalogVersion == bundle.catalogVersion &&
                previousSuccess.manifestChecksum == bundle.manifestChecksum &&
                currentCounts == bundle.expectedCounts &&
                databaseRelationsMatch(bundle)
            ) {
                return@withLock CatalogImportResult(
                    CatalogImportOutcome.ALREADY_CURRENT,
                    bundle.catalogVersion,
                    currentCounts,
                )
            }
            val outcome = if (previousSuccess == null) {
                CatalogImportOutcome.IMPORTED
            } else {
                CatalogImportOutcome.UPDATED
            }
            database.withTransaction {
                val writeDao = database.catalogWriteDao()
                writeDao.upsertCategories(bundle.categories)
                writeDao.upsertActivities(bundle.activities)
                writeDao.upsertRoutines(bundle.routines)
                writeDao.upsertAchievements(bundle.achievements)
                writeDao.upsertChallenges(bundle.challenges)
                writeDao.upsertMessages(bundle.messages)
                writeDao.upsertActivityContexts(bundle.activityContexts)
                writeDao.upsertRoutineSteps(bundle.routineSteps)
                writeDao.upsertGamificationReferences(bundle.gamificationReferences)
                writeDao.upsertVisualReferences(bundle.visualReferences)

                writeDao.deleteStaleVisualReferences(bundle.catalogVersion)
                writeDao.deleteStaleGamificationReferences(bundle.catalogVersion)
                writeDao.deleteStaleRoutineSteps(bundle.catalogVersion)
                writeDao.deleteStaleActivityContexts(bundle.catalogVersion)
                writeDao.deleteStaleMessages(bundle.catalogVersion)
                writeDao.deleteStaleAchievements(bundle.catalogVersion)
                writeDao.deleteStaleChallenges(bundle.catalogVersion)
                writeDao.deleteStaleRoutines(bundle.catalogVersion)
                writeDao.deleteStaleActivities(bundle.catalogVersion)
                writeDao.deleteStaleCategories(bundle.catalogVersion)

                val verifiedCounts = databaseCounts()
                if (verifiedCounts != bundle.expectedCounts) {
                    throw CatalogImportException(
                        CatalogImportErrorCode.DATABASE_VERIFICATION,
                        "Database counts differ after import: $verifiedCounts",
                    )
                }
                if (!databaseRelationsMatch(bundle)) {
                    throw CatalogImportException(
                        CatalogImportErrorCode.DATABASE_VERIFICATION,
                        "Database relation counts differ after import.",
                    )
                }
                database.catalogImportMetadataDao().insert(
                    bundle.toMetadata(CatalogImportStatus.SUCCESS),
                )
            }
            CatalogImportResult(outcome, bundle.catalogVersion, bundle.expectedCounts)
        } catch (error: Throwable) {
            val importError = error as? CatalogImportException
                ?: CatalogImportException(
                    CatalogImportErrorCode.INVALID_DATA,
                    "Unexpected catalog import failure.",
                    error,
                )
            recordFailure(bundle, importError)
            throw importError
        }
    }

    private suspend fun databaseCounts(): CatalogCounts {
        val dao = database.catalogWriteDao()
        val activities = dao.activityCount()
        val routines = dao.routineCount()
        val achievements = dao.achievementCount()
        val challenges = dao.challengeCount()
        val messages = dao.messageCount()
        return CatalogCounts(
            categories = dao.categoryCount(),
            activities = activities,
            routines = routines,
            routineSteps = dao.routineStepCount(),
            achievements = achievements,
            challenges = challenges,
            reinforcementMessages = messages,
            visualReferences = dao.visualReferenceCount(),
            masterRecords = activities + routines + achievements + challenges + messages,
        )
    }

    private suspend fun databaseRelationsMatch(bundle: CatalogBundle): Boolean {
        val dao = database.catalogWriteDao()
        return dao.activityContextCount() == bundle.activityContexts.size &&
            dao.gamificationReferenceCount() == bundle.gamificationReferences.size
    }

    private suspend fun recordFailure(
        bundle: CatalogBundle?,
        error: CatalogImportException,
    ) {
        val fallback = readManifestIdentity()
        val counts = bundle?.expectedCounts ?: CatalogCounts(0, 0, 0, 0, 0, 0, 0, 0, 0)
        runCatching {
            database.catalogImportMetadataDao().insert(
                CatalogImportMetadataEntity(
                    attemptId = attemptId(),
                    catalogVersion = bundle?.catalogVersion ?: fallback.catalogVersion,
                    contractVersion = bundle?.contractVersion ?: fallback.contractVersion,
                    manifestChecksum = bundle?.manifestChecksum ?: fallback.checksum,
                    attemptedAtEpochMillis = clock(),
                    status = if (
                        error.code == CatalogImportErrorCode.INCOMPATIBLE_CONTRACT
                    ) {
                        CatalogImportStatus.INCOMPATIBLE
                    } else {
                        CatalogImportStatus.FAILED
                    },
                    categoryCount = counts.categories,
                    activityCount = counts.activities,
                    routineCount = counts.routines,
                    routineStepCount = counts.routineSteps,
                    achievementCount = counts.achievements,
                    challengeCount = counts.challenges,
                    messageCount = counts.reinforcementMessages,
                    visualReferenceCount = counts.visualReferences,
                    masterRecordCount = counts.masterRecords,
                    errorMessage = error.message,
                ),
            )
        }
    }

    private fun CatalogBundle.toMetadata(status: CatalogImportStatus) =
        CatalogImportMetadataEntity(
            attemptId = attemptId(),
            catalogVersion = catalogVersion,
            contractVersion = contractVersion,
            manifestChecksum = manifestChecksum,
            attemptedAtEpochMillis = clock(),
            status = status,
            categoryCount = expectedCounts.categories,
            activityCount = expectedCounts.activities,
            routineCount = expectedCounts.routines,
            routineStepCount = expectedCounts.routineSteps,
            achievementCount = expectedCounts.achievements,
            challengeCount = expectedCounts.challenges,
            messageCount = expectedCounts.reinforcementMessages,
            visualReferenceCount = expectedCounts.visualReferences,
            masterRecordCount = expectedCounts.masterRecords,
            errorMessage = null,
        )

    private fun readManifestIdentity(): ManifestIdentity = runCatching {
        val bytes = assetSource.readBytes(CatalogJsonParser.MANIFEST)
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        ManifestIdentity(
            catalogVersion = json.optString("catalogVersion", "unverified"),
            contractVersion = json.optString("contractVersion", "unverified"),
            checksum = CatalogJsonParser.sha256(bytes),
        )
    }.getOrDefault(ManifestIdentity("unverified", "unverified", "unavailable"))

    private data class ManifestIdentity(
        val catalogVersion: String,
        val contractVersion: String,
        val checksum: String,
    )
}

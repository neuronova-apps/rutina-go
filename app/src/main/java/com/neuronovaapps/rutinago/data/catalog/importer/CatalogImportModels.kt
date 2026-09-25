package com.neuronovaapps.rutinago.data.catalog.importer

import com.neuronovaapps.rutinago.data.catalog.local.entity.ActivityContextCrossRef
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogAchievementEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogActivityEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogCategoryEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogChallengeEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogGamificationReferenceEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogReinforcementMessageEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogRoutineEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogRoutineStepEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogVisualReferenceEntity

data class CatalogCounts(
    val categories: Int,
    val activities: Int,
    val routines: Int,
    val routineSteps: Int,
    val achievements: Int,
    val challenges: Int,
    val reinforcementMessages: Int,
    val visualReferences: Int,
    val masterRecords: Int,
)

data class CatalogBundle(
    val catalogVersion: String,
    val contractVersion: String,
    val manifestChecksum: String,
    val expectedCounts: CatalogCounts,
    val categories: List<CatalogCategoryEntity>,
    val activities: List<CatalogActivityEntity>,
    val activityContexts: List<ActivityContextCrossRef>,
    val routines: List<CatalogRoutineEntity>,
    val routineSteps: List<CatalogRoutineStepEntity>,
    val achievements: List<CatalogAchievementEntity>,
    val challenges: List<CatalogChallengeEntity>,
    val gamificationReferences: List<CatalogGamificationReferenceEntity>,
    val messages: List<CatalogReinforcementMessageEntity>,
    val visualReferences: List<CatalogVisualReferenceEntity>,
)

enum class CatalogImportOutcome {
    IMPORTED,
    UPDATED,
    ALREADY_CURRENT,
}

data class CatalogImportResult(
    val outcome: CatalogImportOutcome,
    val catalogVersion: String,
    val counts: CatalogCounts,
)

enum class CatalogImportErrorCode {
    ASSET_MISSING,
    INVALID_MANIFEST,
    INCOMPATIBLE_CONTRACT,
    CHECKSUM_MISMATCH,
    INVALID_DATA,
    DATABASE_VERIFICATION,
}

class CatalogImportException(
    val code: CatalogImportErrorCode,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

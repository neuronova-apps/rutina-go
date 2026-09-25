package com.neuronovaapps.rutinago.data.catalog.importer

import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogReferenceType
import com.neuronovaapps.rutinago.data.catalog.local.entity.GamificationOwnerType
import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant

class CatalogBundleValidator {
    fun validate(bundle: CatalogBundle) {
        val expected = bundle.expectedCounts
        requireCount("categories", bundle.categories.size, expected.categories, 17)
        requireCount("activities", bundle.activities.size, expected.activities, 203)
        requireCount("routines", bundle.routines.size, expected.routines, 52)
        requireCount("routineSteps", bundle.routineSteps.size, expected.routineSteps, 490)
        requireCount("achievements", bundle.achievements.size, expected.achievements, 40)
        requireCount("challenges", bundle.challenges.size, expected.challenges, 20)
        requireCount(
            "reinforcementMessages",
            bundle.messages.size,
            expected.reinforcementMessages,
            54,
        )
        requireCount(
            "visualReferences",
            bundle.visualReferences.size,
            expected.visualReferences,
            203,
        )
        val masterRecords = bundle.activities.size + bundle.routines.size +
            bundle.achievements.size + bundle.challenges.size + bundle.messages.size
        requireCount("masterRecords", masterRecords, expected.masterRecords, 369)

        val categoryNames = unique("category", bundle.categories.map { it.name }, Regex("^.+$"))
        if (bundle.categories.map { it.sortOrder } != (1..17).toList()) {
            invalid("Category order must be contiguous from 1 to 17.")
        }
        val activityIds = unique(
            "activity",
            bundle.activities.map { it.activityId },
            Regex("^ACT-\\d{3}$"),
        )
        val routineIds = unique(
            "routine",
            bundle.routines.map { it.routineId },
            Regex("^RUT-\\d{3}$"),
        )
        val achievementIds = unique(
            "achievement",
            bundle.achievements.map { it.achievementId },
            Regex("^LOG-\\d{3}$"),
        )
        val challengeIds = unique(
            "challenge",
            bundle.challenges.map { it.challengeId },
            Regex("^RET-\\d{3}$"),
        )
        unique("message", bundle.messages.map { it.messageId }, Regex("^MSG-\\d{3}$"))

        bundle.activities.forEach { activity ->
            if (activity.categoryName !in categoryNames) {
                invalid(activity.activityId + " references an unknown category.")
            }
        }
        val contextKeys = bundle.activityContexts.map { it.activityId to it.context }
        if (contextKeys.size != contextKeys.toSet().size) {
            invalid("Activity contexts contain duplicate pairs.")
        }
        if (bundle.activityContexts.any {
                it.activityId !in activityIds || it.context.isBlank()
            }
        ) {
            invalid("Activity contexts contain an invalid activity or blank context.")
        }
        if (bundle.activityContexts.map { it.activityId }.toSet() != activityIds) {
            invalid("Every activity must have at least one context.")
        }

        bundle.routines.forEach { routine ->
            if (routine.primaryArea !in categoryNames) {
                invalid(routine.routineId + " references an unknown primary area.")
            }
        }
        val stepKeys = bundle.routineSteps.map {
            Triple(it.routineId, it.variant, it.position)
        }
        if (stepKeys.size != stepKeys.toSet().size) {
            invalid("Routine steps contain duplicate keys.")
        }
        if (bundle.routineSteps.any {
                it.routineId !in routineIds || it.activityId !in activityIds || it.position < 1
            }
        ) {
            invalid("Routine steps contain an invalid routine, activity or position.")
        }
        val groupedSteps = bundle.routineSteps.groupBy { it.routineId to it.variant }
        bundle.routines.forEach { routine ->
            RoutineVariant.entries.forEach { variant ->
                val steps = groupedSteps[routine.routineId to variant].orEmpty()
                val positions = steps.map { it.position }
                if (positions != (1..positions.size).toList()) {
                    invalid(routine.routineId + "/" + variant + " positions are not contiguous.")
                }
                val expectedCount = when (variant) {
                    RoutineVariant.SIMPLE -> routine.declaredSimpleStepCount
                    RoutineVariant.COMPLETE -> routine.declaredCompleteStepCount
                    RoutineVariant.OPTIONAL -> null
                }
                if (expectedCount != null && steps.size != expectedCount) {
                    invalid(routine.routineId + "/" + variant + " count differs.")
                }
            }
        }

        val ownerIds = achievementIds + challengeIds
        val referenceKeys = bundle.gamificationReferences.map { it.ownerId to it.position }
        if (referenceKeys.size != referenceKeys.toSet().size) {
            invalid("Gamification references contain duplicate owner positions.")
        }
        bundle.gamificationReferences.forEach { reference ->
            val expectedOwnerType = when {
                reference.ownerId in achievementIds -> GamificationOwnerType.ACHIEVEMENT
                reference.ownerId in challengeIds -> GamificationOwnerType.CHALLENGE
                else -> null
            }
            if (reference.ownerId !in ownerIds || reference.ownerType != expectedOwnerType) {
                invalid(reference.ownerId + " has an invalid gamification owner.")
            }
            val validTarget = when (reference.referenceType) {
                CatalogReferenceType.ACT -> reference.referenceId in activityIds
                CatalogReferenceType.RUT -> reference.referenceId in routineIds
            }
            if (!validTarget) {
                invalid(reference.ownerId + " references missing " + reference.referenceId + ".")
            }
        }
        if (bundle.gamificationReferences.map { it.ownerId }.toSet() != ownerIds) {
            invalid("Every achievement and challenge must keep at least one reference.")
        }

        val visualIds = bundle.visualReferences.map { it.activityId }
        if (visualIds.toSet() != activityIds || visualIds.size != activityIds.size) {
            invalid("There must be exactly one visual reference for every activity.")
        }
        bundle.visualReferences.forEach { visual ->
            if (visual.visualSource == "PENDING" && listOf(
                    visual.arasaacId,
                    visual.variant,
                    visual.localFile,
                    visual.attribution,
                    visual.license,
                ).any { it != null }
            ) {
                invalid(visual.activityId + " contains invented pending visual metadata.")
            }
            if (visual.visualSource == "ARASAAC" && (
                    visual.arasaacId == null || visual.arasaacId <= 0 ||
                        visual.localFile.isNullOrBlank() ||
                        visual.attribution.isNullOrBlank() ||
                        visual.license.isNullOrBlank()
                    )
            ) {
                invalid(visual.activityId + " has incomplete ARASAAC metadata.")
            }
        }
    }

    private fun unique(label: String, values: List<String>, pattern: Regex): Set<String> {
        if (values.size != values.toSet().size) invalid("$label IDs are not unique.")
        values.firstOrNull { !pattern.matches(it) }?.let {
            invalid("$label value has invalid format: $it")
        }
        return values.toSet()
    }

    private fun requireCount(name: String, actual: Int, manifest: Int, approved: Int) {
        if (actual != manifest || actual != approved) {
            invalid("$name count is $actual; manifest=$manifest; approved=$approved.")
        }
    }

    private fun invalid(message: String): Nothing =
        throw CatalogImportException(CatalogImportErrorCode.INVALID_DATA, message)
}

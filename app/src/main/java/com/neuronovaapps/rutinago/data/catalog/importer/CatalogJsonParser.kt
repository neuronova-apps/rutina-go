package com.neuronovaapps.rutinago.data.catalog.importer

import com.neuronovaapps.rutinago.data.catalog.local.entity.ActivityContextCrossRef
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogAchievementEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogActivityEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogCategoryEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogChallengeEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogGamificationReferenceEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogReferenceType
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogReinforcementMessageEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogRoutineEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogRoutineStepEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogVisualReferenceEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.GamificationOwnerType
import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class CatalogJsonParser(private val assetSource: CatalogAssetSource) {
    fun parse(): CatalogBundle {
        val manifestBytes = assetSource.readBytes(MANIFEST)
        val manifest = parseObject(MANIFEST, manifestBytes)
        val catalogVersion = manifest.requiredString("catalogVersion", MANIFEST)
        val contractVersion = manifest.requiredString("contractVersion", MANIFEST)
        manifest.requiredString("generatedAt", MANIFEST)
        val source = manifest.requiredObject("source", MANIFEST)
        source.requiredString("spreadsheetId", "$MANIFEST/source")
        if (source.requiredInt("sheetCount", "$MANIFEST/source") != 17) {
            invalid("manifest source must declare 17 sheets.")
        }
        if (contractVersion != SUPPORTED_CONTRACT_VERSION) {
            throw CatalogImportException(
                CatalogImportErrorCode.INCOMPATIBLE_CONTRACT,
                "Unsupported contract version: $contractVersion",
            )
        }
        val expectedCounts = manifest.requiredObject("counts", MANIFEST).toCounts()
        val artifacts = manifest.requiredObject("artifacts", MANIFEST)
        val artifactNames = artifacts.keySet()
        if (artifactNames != ARTIFACT_NAMES) {
            invalid("Unexpected manifest artifact set: $artifactNames")
        }
        val envelopes = ARTIFACT_NAMES.associateWith { filename ->
            val bytes = assetSource.readBytes(filename)
            val metadata = artifacts.requiredObject(filename, MANIFEST)
            val expectedChecksum = metadata.requiredString("sha256", "$MANIFEST/$filename")
            if (sha256(bytes) != expectedChecksum) {
                throw CatalogImportException(
                    CatalogImportErrorCode.CHECKSUM_MISMATCH,
                    "$filename checksum mismatch.",
                )
            }
            val envelope = parseObject(filename, bytes)
            if (envelope.requiredString("catalogVersion", filename) != catalogVersion) {
                invalid("$filename catalogVersion differs from manifest.")
            }
            if (envelope.requiredString("contractVersion", filename) != contractVersion) {
                invalid("$filename contractVersion differs from manifest.")
            }
            val items = envelope.requiredArray("items", filename)
            val declared = metadata.requiredInt("itemCount", "$MANIFEST/$filename")
            if (items.length() != declared) {
                invalid("$filename item count differs from manifest.")
            }
            items
        }
        val activityData = parseActivities(envelopes.getValue("activities.json"), catalogVersion)
        val achievementData = parseAchievements(
            envelopes.getValue("achievements.json"),
            catalogVersion,
        )
        val challengeData = parseChallenges(
            envelopes.getValue("challenges.json"),
            catalogVersion,
        )
        return CatalogBundle(
            catalogVersion = catalogVersion,
            contractVersion = contractVersion,
            manifestChecksum = sha256(manifestBytes),
            expectedCounts = expectedCounts,
            categories = parseCategories(envelopes.getValue("categories.json"), catalogVersion),
            activities = activityData.first,
            activityContexts = activityData.second,
            routines = parseRoutines(envelopes.getValue("routines.json"), catalogVersion),
            routineSteps = parseSteps(envelopes.getValue("routine_steps.json"), catalogVersion),
            achievements = achievementData.first,
            challenges = challengeData.first,
            gamificationReferences = achievementData.second + challengeData.second,
            messages = parseMessages(
                envelopes.getValue("reinforcement_messages.json"),
                catalogVersion,
            ),
            visualReferences = parseVisuals(
                envelopes.getValue("visual_references.json"),
                catalogVersion,
            ),
        )
    }

    private fun parseCategories(items: JSONArray, version: String) =
        items.objects("categories.json").map { item ->
            CatalogCategoryEntity(
                item.requiredString("name", "categories.json"),
                item.requiredInt("order", "categories.json"),
                version,
            )
        }

    private fun parseActivities(
        items: JSONArray,
        version: String,
    ): Pair<List<CatalogActivityEntity>, List<ActivityContextCrossRef>> {
        val activities = mutableListOf<CatalogActivityEntity>()
        val contexts = mutableListOf<ActivityContextCrossRef>()
        items.objects("activities.json").forEach { item ->
            val id = item.requiredString("id", "activities.json")
            activities += CatalogActivityEntity(
                activityId = id,
                categoryName = item.requiredString("category", id),
                name = item.requiredString("name", id),
                simpleInstruction = item.requiredString("simpleInstruction", id),
                arasaacSearchTerm = item.requiredString("arasaacSearchTerm", id),
                ageGroup = item.requiredString("ageGroup", id),
                autonomyLevel = item.requiredString("autonomyLevel", id),
                supervision = item.requiredString("supervision", id),
                simpleMode = item.requiredBoolean("simpleMode", id),
                advancedMode = item.requiredBoolean("advancedMode", id),
                visualResource = item.requiredString("visualResource", id),
                status = item.requiredString("status", id),
                observations = item.nullableString("observations"),
                catalogVersion = version,
            )
            item.requiredArray("usageContexts", id).strings(id).forEach { context ->
                contexts += ActivityContextCrossRef(id, context, version)
            }
        }
        return activities to contexts
    }

    private fun parseRoutines(items: JSONArray, version: String) =
        items.objects("routines.json").map { item ->
            val id = item.requiredString("id", "routines.json")
            CatalogRoutineEntity(
                routineId = id,
                name = item.requiredString("name", id),
                primaryArea = item.requiredString("primaryArea", id),
                context = item.requiredString("context", id),
                ageGroup = item.requiredString("ageGroup", id),
                functionalObjective = item.requiredString("functionalObjective", id),
                suggestedMoment = item.requiredString("suggestedMoment", id),
                declaredSimpleStepCount = item.requiredInt("declaredSimpleStepCount", id),
                declaredCompleteStepCount = item.requiredInt("declaredCompleteStepCount", id),
                personalizable = item.requiredBoolean("personalizable", id),
                keySupervision = item.requiredString("keySupervision", id),
                status = item.requiredString("status", id),
                observations = item.nullableString("observations"),
                catalogVersion = version,
            )
        }

    private fun parseSteps(items: JSONArray, version: String) =
        items.objects("routine_steps.json").map { item ->
            val routineId = item.requiredString("routineId", "routine_steps.json")
            CatalogRoutineStepEntity(
                routineId = routineId,
                activityId = item.requiredString("activityId", routineId),
                variant = enumValue(item.requiredString("sequenceType", routineId), routineId),
                position = item.requiredInt("position", routineId),
                catalogVersion = version,
            )
        }

    private fun parseAchievements(
        items: JSONArray,
        version: String,
    ): Pair<List<CatalogAchievementEntity>, List<CatalogGamificationReferenceEntity>> {
        val entities = mutableListOf<CatalogAchievementEntity>()
        val references = mutableListOf<CatalogGamificationReferenceEntity>()
        items.objects("achievements.json").forEach { item ->
            val id = item.requiredString("id", "achievements.json")
            val refType = enumValue<CatalogReferenceType>(
                item.requiredString("referenceType", id),
                id,
            )
            entities += CatalogAchievementEntity(
                id,
                item.requiredString("type", id),
                item.requiredString("name", id),
                item.requiredString("category", id),
                item.requiredString("gamificationLevel", id),
                item.requiredString("ageGroup", id),
                refType,
                item.requiredString("unlockCondition", id),
                item.requiredInt("targetQuantity", id),
                item.requiredInt("windowDays", id),
                item.requiredBoolean("repeatable", id),
                item.requiredString("visualReward", id),
                item.requiredBoolean("penalizesIfIncomplete", id),
                item.requiredString("defaultActivation", id),
                item.requiredString("status", id),
                item.requiredString("observations", id),
                version,
            )
            references += item.toReferences(
                id,
                GamificationOwnerType.ACHIEVEMENT,
                refType,
                version,
            )
        }
        return entities to references
    }

    private fun parseChallenges(
        items: JSONArray,
        version: String,
    ): Pair<List<CatalogChallengeEntity>, List<CatalogGamificationReferenceEntity>> {
        val entities = mutableListOf<CatalogChallengeEntity>()
        val references = mutableListOf<CatalogGamificationReferenceEntity>()
        items.objects("challenges.json").forEach { item ->
            val id = item.requiredString("id", "challenges.json")
            val refType = enumValue<CatalogReferenceType>(
                item.requiredString("referenceType", id),
                id,
            )
            entities += CatalogChallengeEntity(
                id,
                item.requiredString("type", id),
                item.requiredString("name", id),
                item.requiredString("category", id),
                item.requiredString("gamificationLevel", id),
                item.requiredString("ageGroup", id),
                refType,
                item.requiredString("unlockCondition", id),
                item.requiredInt("targetQuantity", id),
                item.requiredInt("windowDays", id),
                item.requiredBoolean("repeatable", id),
                item.requiredString("visualReward", id),
                item.requiredBoolean("penalizesIfIncomplete", id),
                item.requiredString("defaultActivation", id),
                item.requiredString("status", id),
                item.requiredString("observations", id),
                version,
            )
            references += item.toReferences(
                id,
                GamificationOwnerType.CHALLENGE,
                refType,
                version,
            )
        }
        return entities to references
    }

    private fun JSONObject.toReferences(
        ownerId: String,
        ownerType: GamificationOwnerType,
        referenceType: CatalogReferenceType,
        version: String,
    ) = requiredArray("references", ownerId).strings(ownerId).mapIndexed { index, ref ->
        CatalogGamificationReferenceEntity(
            ownerId,
            ownerType,
            referenceType,
            ref,
            index + 1,
            version,
        )
    }

    private fun parseMessages(items: JSONArray, version: String) =
        items.objects("reinforcement_messages.json").map { item ->
            val id = item.requiredString("id", "reinforcement_messages.json")
            CatalogReinforcementMessageEntity(
                id,
                item.requiredString("eventType", id),
                item.requiredString("category", id),
                item.requiredString("tone", id),
                item.requiredString("ageGroup", id),
                item.requiredBoolean("gamificationRequired", id),
                item.requiredString("gamificationLevel", id),
                item.requiredString("message", id),
                item.requiredString("usageChannel", id),
                item.requiredString("length", id),
                item.requiredString("allowsRepetition", id),
                item.requiredString("eventOrigin", id),
                item.requiredString("safetyRule", id),
                item.requiredString("status", id),
                item.requiredString("observations", id),
                item.requiredString("recommendedFrequency", id),
                item.requiredString("ttsActivation", id),
                version,
            )
        }

    private fun parseVisuals(items: JSONArray, version: String) =
        items.objects("visual_references.json").map { item ->
            val id = item.requiredString("activityId", "visual_references.json")
            CatalogVisualReferenceEntity(
                id,
                item.requiredString("visualSource", id),
                item.nullableInt("arasaacId"),
                item.nullableString("variant"),
                item.nullableString("localFile"),
                item.requiredString("alternativeText", id),
                item.requiredString("reviewStatus", id),
                item.nullableString("attribution"),
                item.nullableString("license"),
                version,
            )
        }

    private fun JSONObject.toCounts() = CatalogCounts(
        requiredInt("categories", "manifest/counts"),
        requiredInt("activities", "manifest/counts"),
        requiredInt("routines", "manifest/counts"),
        requiredInt("routineSteps", "manifest/counts"),
        requiredInt("achievements", "manifest/counts"),
        requiredInt("challenges", "manifest/counts"),
        requiredInt("reinforcementMessages", "manifest/counts"),
        requiredInt("visualReferences", "manifest/counts"),
        requiredInt("masterRecords", "manifest/counts"),
    )

    private fun parseObject(name: String, bytes: ByteArray): JSONObject = try {
        JSONObject(bytes.toString(Charsets.UTF_8))
    } catch (error: JSONException) {
        throw CatalogImportException(
            CatalogImportErrorCode.INVALID_MANIFEST,
            "$name is not valid JSON.",
            error,
        )
    }

    private fun JSONObject.requiredString(key: String, owner: String): String = try {
        getString(key).takeIf { it.isNotBlank() }
            ?: invalid("$owner/$key must not be blank.")
    } catch (error: JSONException) {
        invalid("$owner/$key must be a string.", error)
    }

    private fun JSONObject.requiredInt(key: String, owner: String): Int = try {
        getInt(key)
    } catch (error: JSONException) {
        invalid("$owner/$key must be an integer.", error)
    }

    private fun JSONObject.requiredBoolean(key: String, owner: String): Boolean = try {
        getBoolean(key)
    } catch (error: JSONException) {
        invalid("$owner/$key must be a boolean.", error)
    }

    private fun JSONObject.requiredObject(key: String, owner: String): JSONObject = try {
        getJSONObject(key)
    } catch (error: JSONException) {
        invalid("$owner/$key must be an object.", error)
    }

    private fun JSONObject.requiredArray(key: String, owner: String): JSONArray = try {
        getJSONArray(key)
    } catch (error: JSONException) {
        invalid("$owner/$key must be an array.", error)
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else getString(key)

    private fun JSONObject.nullableInt(key: String): Int? =
        if (isNull(key)) null else getInt(key)

    private fun JSONArray.objects(owner: String): List<JSONObject> =
        (0 until length()).map { index ->
            try {
                getJSONObject(index)
            } catch (error: JSONException) {
                invalid("$owner[$index] must be an object.", error)
            }
        }

    private fun JSONArray.strings(owner: String): List<String> =
        (0 until length()).map { index ->
            try {
                getString(index).takeIf { it.isNotBlank() }
                    ?: invalid("$owner[$index] must not be blank.")
            } catch (error: JSONException) {
                invalid("$owner[$index] must be a string.", error)
            }
        }

    private fun JSONObject.keySet(): Set<String> = buildSet {
        val iterator = keys()
        while (iterator.hasNext()) add(iterator.next())
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String, owner: String): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: invalid("$owner contains unsupported value $value.")

    private fun invalid(message: String, cause: Throwable? = null): Nothing =
        throw CatalogImportException(CatalogImportErrorCode.INVALID_DATA, message, cause)

    companion object {
        const val MANIFEST = "manifest.json"
        const val SUPPORTED_CONTRACT_VERSION = "1.0.0"
        val ARTIFACT_NAMES = setOf(
            "categories.json",
            "activities.json",
            "routines.json",
            "routine_steps.json",
            "achievements.json",
            "challenges.json",
            "reinforcement_messages.json",
            "visual_references.json",
        )

        fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { byte -> "%02x".format(byte) }
    }
}

package com.neuronovaapps.rutinago.data.preferences

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.neuronovaapps.rutinago.data.communication.CommunicationProfileRegistry
import com.neuronovaapps.rutinago.data.communication.CommunicationProfileSetupManager
import com.neuronovaapps.rutinago.data.communication.CommunicationSetupStorage
import com.neuronovaapps.rutinago.domain.communication.CommunicationProfileSetup
import com.neuronovaapps.rutinago.domain.communication.CommunicationProfileSetupRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationPresentationMode
import com.neuronovaapps.rutinago.domain.communication.CommunicationPresentationRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationPersonalVocabularyRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationHomeVocabularyRepository
import com.neuronovaapps.rutinago.data.communication.LocalCommunicationCatalogRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationSpeechPreferencesRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationSpeechRate

enum class ThemeMode(val storageValue: String) {
    DAY("day"), NIGHT("night"), SYSTEM("system");

    fun usesDarkTheme(systemInDarkTheme: Boolean): Boolean = when (this) {
        DAY -> false
        NIGHT -> true
        SYSTEM -> systemInDarkTheme
    }

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: DAY
    }
}

enum class TextScaleMode(val storageValue: String, val scaleFactor: Float) {
    NORMAL("normal", 1f), LARGE("large", 1.12f), VERY_LARGE("very_large", 1.24f);

    companion object {
        fun fromStorage(value: String?): TextScaleMode =
            entries.firstOrNull { it.storageValue == value } ?: NORMAL
    }
}

enum class AppMode(val storageValue: String) {
    SIMPLE("simple"), ADVANCED("advanced");

    companion object {
        fun fromStorage(value: String?): AppMode =
            entries.firstOrNull { it.storageValue == value } ?: SIMPLE
    }
}

data class UserProfile(val id: String, val name: String)

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DAY,
    val textScaleMode: TextScaleMode = TextScaleMode.NORMAL,
    val highContrast: Boolean = false,
    val appMode: AppMode = AppMode.SIMPLE,
    val profiles: List<UserProfile> = emptyList(),
    val activeProfileId: String? = null,
) {
    val activeProfile: UserProfile?
        get() = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.firstOrNull()
}

// Keep the existing store name so preferences written in Phase 1.1 remain valid.
// Profiles are small app preferences here; routines and activities deliberately stay out of DataStore.
private val Context.userPreferencesDataStore by preferencesDataStore(name = "appearance_preferences")

class UserPreferencesRepository(context: Context) : CommunicationProfileSetupRepository, CommunicationPresentationRepository, CommunicationPersonalVocabularyRepository, CommunicationHomeVocabularyRepository, CommunicationSpeechPreferencesRepository {
    private val appContext = context.applicationContext
    private val communicationCatalog by lazy {
        LocalCommunicationCatalogRepository {
            appContext.assets.open(LocalCommunicationCatalogRepository.ASSET_PATH)
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }
    private val communicationSetups by lazy {
        CommunicationProfileSetupManager(CommunicationSetupStorage.get(appContext),
            object : CommunicationProfileRegistry {
                override suspend fun exists(profileId: String): Boolean =
                    decodeProfiles(appContext.userPreferencesDataStore.data.first()[PROFILES_KEY].orEmpty())
                        .any { it.id == profileId }

                override suspend fun delete(profileId: String) {
                    edit { stored ->
                        val remaining = decodeProfiles(stored[PROFILES_KEY].orEmpty()).filterNot { it.id == profileId }
                        stored[PROFILES_KEY] = encodeProfiles(remaining)
                        if (stored[ACTIVE_PROFILE_KEY] == profileId) {
                            remaining.firstOrNull()?.let { stored[ACTIVE_PROFILE_KEY] = it.id }
                                ?: stored.remove(ACTIVE_PROFILE_KEY)
                        }
                    }
                }
            },
            catalog = { communicationCatalog },
        )
    }

    override fun setupForProfile(profileId: String) = communicationSetups.setupForProfile(profileId)
    override fun speechRateForProfile(profileId: String) = communicationSetups.speechRateForProfile(profileId)
    override suspend fun setSpeechRate(profileId: String, rate: CommunicationSpeechRate) = communicationSetups.setSpeechRate(profileId, rate)
    override fun vocabularyForProfile(profileId: String) = communicationSetups.vocabularyForProfile(profileId)
    override fun homeVocabularyForProfile(profileId: String) = communicationSetups.homeVocabularyForProfile(profileId)
    override suspend fun setHomeWord(profileId: String, vocabularyId: String, inHome: Boolean) =
        communicationSetups.setHomeWord(profileId, vocabularyId, inHome)
    override suspend fun saveVocabulary(profileId: String, visibleIds: Set<String>) =
        communicationSetups.saveVocabulary(profileId, visibleIds)
    override suspend fun saveSetup(setup: CommunicationProfileSetup) = communicationSetups.saveSetup(setup)
    override fun presentationForProfile(profileId: String) = communicationSetups.presentationForProfile(profileId)
    override suspend fun setPresentationMode(profileId: String, mode: CommunicationPresentationMode) =
        communicationSetups.setPresentationMode(profileId, mode)
    override suspend fun markPresentationIntroductionSeen(profileId: String) =
        communicationSetups.markPresentationIntroductionSeen(profileId)

    /** No deletion UI is introduced; every caller must use this cleanup-aware entry point. */
    suspend fun deleteProfile(profileId: String) = communicationSetups.deleteProfile(profileId)

    val preferences: Flow<UserPreferences> = appContext.userPreferencesDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { stored ->
            UserPreferences(
                themeMode = ThemeMode.fromStorage(stored[THEME_MODE_KEY]),
                textScaleMode = TextScaleMode.fromStorage(stored[TEXT_SCALE_KEY]),
                highContrast = stored[HIGH_CONTRAST_KEY] ?: false,
                appMode = AppMode.fromStorage(stored[APP_MODE_KEY]),
                profiles = decodeProfiles(stored[PROFILES_KEY].orEmpty()),
                activeProfileId = stored[ACTIVE_PROFILE_KEY],
            )
        }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[THEME_MODE_KEY] = mode.storageValue }

    suspend fun setTextScaleMode(mode: TextScaleMode) = edit { it[TEXT_SCALE_KEY] = mode.storageValue }

    suspend fun setHighContrast(enabled: Boolean) = edit { it[HIGH_CONTRAST_KEY] = enabled }

    suspend fun setAppMode(mode: AppMode) = edit { it[APP_MODE_KEY] = mode.storageValue }

    suspend fun createProfile(name: String, id: String = UUID.randomUUID().toString()): UserProfile {
        val profile = UserProfile(id = id, name = name.trim())
        require(profile.name.isNotEmpty()) { "Profile name cannot be blank" }
        edit { stored ->
            val profiles = decodeProfiles(stored[PROFILES_KEY].orEmpty())
                .filterNot { it.id == profile.id } + profile
            stored[PROFILES_KEY] = encodeProfiles(profiles)
            if (stored[ACTIVE_PROFILE_KEY].isNullOrBlank()) stored[ACTIVE_PROFILE_KEY] = profile.id
        }
        return profile
    }

    suspend fun renameProfile(profileId: String, name: String) {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "Profile name cannot be blank" }
        edit { stored ->
            val profiles = decodeProfiles(stored[PROFILES_KEY].orEmpty())
            require(profiles.any { it.id == profileId }) { "Unknown profile: $profileId" }
            stored[PROFILES_KEY] = encodeProfiles(
                profiles.map { if (it.id == profileId) it.copy(name = normalized) else it },
            )
        }
    }

    suspend fun selectProfile(profileId: String) {
        edit { stored ->
            require(decodeProfiles(stored[PROFILES_KEY].orEmpty()).any { it.id == profileId }) {
                "Unknown profile: $profileId"
            }
            stored[ACTIVE_PROFILE_KEY] = profileId
        }
    }

    private suspend fun edit(change: (MutablePreferences) -> Unit) {
        appContext.userPreferencesDataStore.edit(change)
    }

    private companion object {
        val THEME_MODE_KEY: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val TEXT_SCALE_KEY: Preferences.Key<String> = stringPreferencesKey("text_scale")
        val HIGH_CONTRAST_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("high_contrast")
        val APP_MODE_KEY: Preferences.Key<String> = stringPreferencesKey("app_mode")
        val PROFILES_KEY: Preferences.Key<Set<String>> = stringSetPreferencesKey("profiles_v1")
        val ACTIVE_PROFILE_KEY: Preferences.Key<String> = stringPreferencesKey("active_profile_id")

        fun encodeProfiles(profiles: List<UserProfile>): Set<String> = profiles.map { profile ->
            "${profile.id}|${encodeName(profile.name)}"
        }.toSet()

        fun decodeProfiles(values: Set<String>): List<UserProfile> = values.mapNotNull { value ->
            val separator = value.indexOf('|')
            if (separator <= 0 || separator == value.lastIndex) return@mapNotNull null
            runCatching {
                UserProfile(value.substring(0, separator), decodeName(value.substring(separator + 1)))
            }.getOrNull()?.takeIf { it.name.isNotBlank() }
        }.sortedBy { it.name.lowercase() }

        fun encodeName(name: String): String = Base64.encodeToString(
            name.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP,
        )

        fun decodeName(value: String): String = String(
            Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP),
            Charsets.UTF_8,
        )
    }
}

package com.neuronovaapps.rutinago.domain.communication

import kotlinx.coroutines.flow.Flow

enum class CommunicationPresentationMode { GUIDED, FULL }

/** An absent selection is a suggestion, not a choice inferred from personal characteristics. */
data class CommunicationPresentationPreferences(
    val profileId: String,
    val selectedMode: CommunicationPresentationMode? = null,
    val introductionSeen: Boolean = false,
) {
    init { require(profileId.isNotBlank()) }
}

enum class CommunicationSuggestionReason { QUICK_ACCESS_START, GENERAL_START }

data class CommunicationPresentationSuggestion(
    val mode: CommunicationPresentationMode,
    val reason: CommunicationSuggestionReason,
)

object CommunicationPresentationPolicy {
    fun suggest(setup: CommunicationProfileSetup?): CommunicationPresentationSuggestion =
        CommunicationPresentationSuggestion(
            CommunicationPresentationMode.GUIDED,
            when (setup?.oralCommunication) {
                OralCommunication.EMERGING_SPEECH, OralCommunication.NO_CURRENT_SPEECH ->
                    CommunicationSuggestionReason.QUICK_ACCESS_START
                else -> CommunicationSuggestionReason.GENERAL_START
            },
        )

    fun resolve(setup: CommunicationProfileSetup?, preferences: CommunicationPresentationPreferences) =
        preferences.selectedMode ?: suggest(setup).mode
}

interface CommunicationPresentationRepository {
    fun presentationForProfile(profileId: String): Flow<CommunicationPresentationPreferences>
    suspend fun setPresentationMode(profileId: String, mode: CommunicationPresentationMode)
    suspend fun markPresentationIntroductionSeen(profileId: String)
}

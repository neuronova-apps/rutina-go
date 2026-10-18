package com.neuronovaapps.rutinago.domain.communication

import kotlinx.coroutines.flow.Flow

enum class CommunicationAgeRange {
    YEARS_3_TO_5, YEARS_6_TO_8, YEARS_9_TO_12, YEARS_13_TO_17, ADULT, NOT_SPECIFIED,
}

enum class OralCommunication {
    WORDS_AND_PHRASES, EMERGING_SPEECH, NO_CURRENT_SPEECH, VARIABLE_SPEECH, NOT_SPECIFIED,
}

/** Context chosen by the user; never an input to catalog filtering or message rules. */
data class CommunicationProfileSetup(
    val profileId: String,
    val ageRange: CommunicationAgeRange,
    val oralCommunication: OralCommunication,
    val setupCompleted: Boolean = true,
    val schemaVersion: Int = 1,
) {
    init {
        require(profileId.isNotBlank())
        require(schemaVersion == 1)
    }
}

interface CommunicationProfileSetupRepository {
    fun setupForProfile(profileId: String): Flow<CommunicationProfileSetup?>
    suspend fun saveSetup(setup: CommunicationProfileSetup)
}

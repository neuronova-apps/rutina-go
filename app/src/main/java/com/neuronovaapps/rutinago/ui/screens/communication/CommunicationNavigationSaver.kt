package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.compose.runtime.saveable.listSaver
import com.neuronovaapps.rutinago.domain.communication.CommunicationDestination
import com.neuronovaapps.rutinago.domain.communication.CommunicationNavigationState

internal val CommunicationNavigationSaver = listSaver<CommunicationNavigationState, String>(
    save = { listOf(it.destination.name, it.personalizing.toString()) },
    restore = { CommunicationNavigationState(CommunicationDestination.valueOf(it[0]), it[1].toBooleanStrict()) },
)

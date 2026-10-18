package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.compose.runtime.saveable.listSaver
import com.neuronovaapps.rutinago.domain.communication.CommunicationDestination
import com.neuronovaapps.rutinago.domain.communication.CommunicationNavigationState

internal val CommunicationNavigationSaver = listSaver<CommunicationNavigationState, String>(
    save = { listOf(it.destination.name, it.personalizing.toString(), it.homeOrganizationOrigin?.name.orEmpty(), it.exploring.toString()) },
    restore = { saved ->
        // Restore a transient snapshot from the former Categories tab as the Home explorer.
        fun destination(name: String) = if (name == "CATEGORIES") CommunicationDestination.HOME
            else CommunicationDestination.valueOf(name)
        CommunicationNavigationState(destination(saved[0]), saved[1].toBooleanStrict(),
            saved.getOrNull(2)?.takeIf(String::isNotEmpty)?.let(::destination),
            saved[0] == "CATEGORIES" || saved.getOrNull(3)?.toBooleanStrictOrNull() == true)
    },
)

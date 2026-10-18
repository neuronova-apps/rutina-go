package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import com.neuronovaapps.rutinago.domain.communication.CommunicationDestination
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast

@Composable
internal fun CommunicationBottomBar(selected: CommunicationDestination, onSelect: (CommunicationDestination) -> Unit) {
    val highContrast = LocalHighContrast.current
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = MaterialTheme.typography.labelMedium
    BoxWithConstraints {
        // Keep natural text size. At narrow widths/large text, two rows give every label space.
        val longestWord = CommunicationDestination.entries.flatMap { it.label.split(' ') }
            .maxOf { measurer.measure(it, style).size.width }
        val twoRows = with(density) { longestWord.toDp() } + 24.dp > maxWidth / CommunicationDestination.entries.size
        val columns = if (twoRows) 2 else CommunicationDestination.entries.size
        val labelWidth = with(density) { (maxWidth / columns - 24.dp).roundToPx().coerceAtLeast(1) }
        val labelHeight = CommunicationDestination.entries.maxOf {
            measurer.measure(it.label, style, constraints = Constraints(maxWidth = labelWidth)).size.height
        }
        val itemHeight = with(density) { labelHeight.toDp() } + 56.dp
        NavigationBar(
            modifier = Modifier.testTag("communication-bottom-bar")
                .then(if (highContrast) Modifier.border(2.dp, MaterialTheme.colorScheme.outline) else Modifier),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column {
                CommunicationDestination.entries.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth().height(itemHeight)) {
                        row.forEach { destination ->
                            NavigationBarItem(
                                selected = selected == destination,
                                onClick = { onSelect(destination) },
                                modifier = Modifier.heightIn(min = itemHeight)
                                    .testTag("communication-destination-${destination.name}"),
                                icon = { Icon(communicationIcons.getValue(destination), null, Modifier.size(24.dp)) },
                                label = { Text(destination.label, modifier = Modifier.fillMaxWidth(),
                                    style = style, textAlign = TextAlign.Center) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = if (highContrast) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = if (highContrast) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun communicationIcon(name: String, draw: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black), pathBuilder = draw)
    }.build()

private val communicationIcons = mapOf(
    CommunicationDestination.HOME to communicationIcon("Casa") {
        moveTo(3f, 11f); lineTo(12f, 3f); lineTo(21f, 11f); lineTo(19f, 11f)
        lineTo(19f, 21f); lineTo(14f, 21f); lineTo(14f, 14f); lineTo(10f, 14f)
        lineTo(10f, 21f); lineTo(5f, 21f); lineTo(5f, 11f); close()
    },
    CommunicationDestination.MY_VOCABULARY to communicationIcon("Libro") {
        moveTo(12f, 5f); curveTo(8f, 2f, 4f, 3f, 2f, 4f); lineTo(2f, 20f)
        curveTo(6f, 18f, 9f, 19f, 12f, 21f); curveTo(15f, 19f, 18f, 18f, 22f, 20f)
        lineTo(22f, 4f); curveTo(19f, 3f, 16f, 2f, 12f, 5f); close()
    },
    CommunicationDestination.SETTINGS to communicationIcon("Engranaje") {
        moveTo(9f, 2f); lineTo(15f, 2f); lineTo(16f, 5f); lineTo(19f, 5f)
        lineTo(22f, 10f); lineTo(20f, 12f); lineTo(22f, 14f); lineTo(19f, 19f)
        lineTo(16f, 19f); lineTo(15f, 22f); lineTo(9f, 22f); lineTo(8f, 19f)
        lineTo(5f, 19f); lineTo(2f, 14f); lineTo(4f, 12f); lineTo(2f, 10f)
        lineTo(5f, 5f); lineTo(8f, 5f); close()
        moveTo(12f, 8f); curveTo(10f, 8f, 8f, 10f, 8f, 12f)
        curveTo(8f, 14f, 10f, 16f, 12f, 16f); curveTo(14f, 16f, 16f, 14f, 16f, 12f)
        curveTo(16f, 10f, 14f, 8f, 12f, 8f); close()
    },
)

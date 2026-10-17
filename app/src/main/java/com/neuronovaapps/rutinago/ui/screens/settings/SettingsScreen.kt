package com.neuronovaapps.rutinago.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.data.preferences.ThemeMode
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.ui.components.InfoCallout
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.components.VisualPlaceholder
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    textScaleMode: TextScaleMode,
    onTextScaleModeChange: (TextScaleMode) -> Unit,
    highContrast: Boolean,
    onHighContrastChange: (Boolean) -> Unit,
    appMode: AppMode,
    onAppModeChange: (AppMode) -> Unit,
    activeProfileName: String,
    onManageProfiles: () -> Unit,
    onOpenAbout: () -> Unit,
    onChangeModule: () -> Unit,
) {
    ScreenColumn {
        SectionHeading("Apariencia")
        Text("Tema de la aplicación", style = MaterialTheme.typography.titleMedium)
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                ThemeModeOption(
                    mode = mode,
                    selected = themeMode == mode,
                    onSelect = { onThemeModeChange(mode) },
                )
            }
        }

        SectionHeading("Modo de uso")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = appMode == AppMode.SIMPLE,
                    onClick = { onAppModeChange(AppMode.SIMPLE) },
                    label = { Text("Modo simple") },
                )
            }
            item {
                FilterChip(
                    selected = appMode == AppMode.ADVANCED,
                    onClick = { onAppModeChange(AppMode.ADVANCED) },
                    label = { Text("Modo avanzado") },
                )
            }
        }
        Text(
            if (appMode == AppMode.ADVANCED) {
                "Añade búsqueda, organización de pasos, programación y opciones complementarias."
            } else {
                "Prioriza acciones cotidianas, instrucciones breves y apoyos visuales."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionHeading("Perfiles")
        ProfileRow(activeProfileName, "Perfil activo", active = true)
        RutinaGoOutlinedButton(onClick = onManageProfiles, modifier = Modifier.fillMaxWidth()) {
            Text("Gestionar perfiles")
        }

        SectionHeading("Accesibilidad y visual")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                VisualPlaceholder("MANOS")
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vista previa", style = MaterialTheme.typography.labelMedium)
                    Text("Lavarse las manos", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Este ejemplo cambia con tus preferencias.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text("Tamaño del texto", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                TextScaleMode.entries,
                key = TextScaleMode::storageValue,
            ) { mode ->
                FilterChip(
                    selected = textScaleMode == mode,
                    onClick = { onTextScaleModeChange(mode) },
                    label = {
                        Text(
                            when (mode) {
                                TextScaleMode.NORMAL -> "Normal"
                                TextScaleMode.LARGE -> "Grande"
                                TextScaleMode.VERY_LARGE -> "Muy grande"
                            },
                        )
                    },
                )
            }
        }
        SettingToggle(
            title = "Alto contraste",
            description = "Refuerza textos, bordes, tarjetas y acciones importantes.",
            checked = highContrast,
            onCheckedChange = onHighContrastChange,
        )
        InfoCallout(
            title = "Apoyos visuales siempre disponibles",
            body = "El modo simple mantiene visibles los apoyos esenciales y no oculta la accesibilidad.",
        )

        if (appMode == AppMode.ADVANCED) {
            InfoCallout(
                title = "Próximas opciones avanzadas",
                body = "Texto a voz, gamificación y protección del configurador no se muestran como controles hasta contar con una implementación real.",
            )
        }

        SectionHeading("Información")
        InformationLinkCard(onClick = onOpenAbout)
        RutinaGoOutlinedButton(onClick = onChangeModule, modifier = Modifier.fillMaxWidth()) {
            Text("Cambiar módulo")
        }
    }
}

@Composable
private fun InformationLinkCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "Acerca de RutinaGo. Información, privacidad, créditos y desarrollador. Abrir."
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "i",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Acerca de RutinaGo", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Información, privacidad, créditos y desarrollador.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun ThemeModeOption(
    mode: ThemeMode,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val highContrast = LocalHighContrast.current
    val (label, description) = when (mode) {
        ThemeMode.DAY -> "Día" to "Usa siempre la paleta clara."
        ThemeMode.NIGHT -> "Noche" to "Usa siempre la paleta oscura."
        ThemeMode.SYSTEM -> "Sistema" to "Sigue la apariencia del dispositivo."
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected && highContrast) {
                MaterialTheme.colorScheme.primary
            } else if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (selected && highContrast) {
                MaterialTheme.colorScheme.onPrimary
            } else if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = onSelect,
                    role = Role.RadioButton,
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = if (highContrast) {
                    RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedColor = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    RadioButtonDefaults.colors()
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        if (highContrast) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (selected) {
                Text(
                    "Seleccionado",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (highContrast) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(name: String, detail: String, active: Boolean) {
    val highContrast = LocalHighContrast.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (active && highContrast) {
                MaterialTheme.colorScheme.primary
            } else if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (active && highContrast) {
                MaterialTheme.colorScheme.onPrimary
            } else if (active) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
        border = BorderStroke(
            if (active) 2.dp else 1.dp,
            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(if (active) "✓ $name" else name, style = MaterialTheme.typography.titleMedium)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (active) {
                        if (highContrast) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text("Activo", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val highContrast = LocalHighContrast.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = if (highContrast) {
                    SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    SwitchDefaults.colors()
                },
            )
        }
    }
}

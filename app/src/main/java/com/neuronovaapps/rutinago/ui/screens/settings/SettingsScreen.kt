package com.neuronovaapps.rutinago.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.ui.components.InfoCallout
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.components.StatusPill
import com.neuronovaapps.rutinago.ui.components.VisualPlaceholder

@Composable
fun SettingsScreen(
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    textScale: Float,
    onTextScaleChange: (Float) -> Unit,
) {
    var advancedMode by rememberSaveable { mutableStateOf(false) }
    var textToSpeech by rememberSaveable { mutableStateOf(false) }
    var visualSupports by rememberSaveable { mutableStateOf(true) }
    var gamification by rememberSaveable { mutableStateOf(false) }
    var configuratorProtection by rememberSaveable { mutableStateOf(false) }

    ScreenColumn {
        SectionHeading("Modo de uso")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = !advancedMode,
                    onClick = { advancedMode = false },
                    label = { Text("Modo simple") },
                )
            }
            item {
                FilterChip(
                    selected = advancedMode,
                    onClick = { advancedMode = true },
                    label = { Text("Modo avanzado") },
                )
            }
        }
        Text(
            if (advancedMode) {
                "Muestra opciones de organización y apoyo adicionales."
            } else {
                "Mantiene visibles solo las opciones cotidianas principales."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionHeading("Perfiles")
        ProfileRow("Alex", "Activo · General", active = true)
        ProfileRow("Sam", "Escolar+", active = false)
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
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
                listOf(1f to "Normal", 1.12f to "Grande", 1.24f to "Muy grande"),
                key = { it.second },
            ) { option ->
                FilterChip(
                    selected = textScale == option.first,
                    onClick = { onTextScaleChange(option.first) },
                    label = { Text(option.second) },
                )
            }
        }
        SettingToggle(
            title = "Tema oscuro",
            description = "Mantiene la misma jerarquía y contraste.",
            checked = darkTheme,
            onCheckedChange = onDarkThemeChange,
        )
        SettingToggle(
            title = "Texto a voz",
            description = "Apoyo opcional para instrucciones breves.",
            checked = textToSpeech,
            onCheckedChange = { textToSpeech = it },
        )
        SettingToggle(
            title = "Apoyos visuales",
            description = "Prepara pictogramas y fotografías personalizadas.",
            checked = visualSupports,
            onCheckedChange = { visualSupports = it },
        )

        SectionHeading("Gamificación opcional")
        SettingToggle(
            title = if (gamification) "Logros discretos" else "Desactivada",
            description = "Sin rankings, presión ni penalizaciones.",
            checked = gamification,
            onCheckedChange = { gamification = it },
        )

        SectionHeading("Protección del configurador")
        SettingToggle(
            title = if (configuratorProtection) "Representada como activa" else "Desactivada",
            description = "Limita visualmente los cambios administrativos, no el uso cotidiano.",
            checked = configuratorProtection,
            onCheckedChange = { configuratorProtection = it },
        )
        InfoCallout(
            title = "Sin seguridad simulada",
            body = "Esta fase no usa PIN ni contraseña de demostración como protección real.",
        )

        SectionHeading("Acerca de RutinaGo")
        InfoCallout(
            title = "RutinaGo · NeuroNova Apps",
            body = "Base Android local para organizar y anticipar actividades mediante secuencias visuales personalizables.",
        )
        Text(
            "Privacidad: en esta fase no hay cuenta, nube ni persistencia definitiva. Los créditos de ARASAAC se incorporarán cuando se integren sus recursos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusPill("Fase 1 · datos demo")
    }
}

@Composable
private fun ProfileRow(name: String, detail: String, active: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("Editar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
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
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

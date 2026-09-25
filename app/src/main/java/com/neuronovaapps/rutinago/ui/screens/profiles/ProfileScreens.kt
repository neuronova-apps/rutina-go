package com.neuronovaapps.rutinago.ui.screens.profiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.preferences.UserProfile
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.InfoCallout
import com.neuronovaapps.rutinago.ui.components.ScreenColumn

@Composable
fun FirstProfileScreen(onCreateProfile: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val invalid = attempted && name.isBlank()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Bienvenido a RutinaGo",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Comencemos creando tu perfil.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del perfil") },
                singleLine = true,
                isError = invalid,
                supportingText = if (invalid) ({ Text("Introduce un nombre para continuar.") }) else null,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    attempted = true
                    if (name.isNotBlank()) onCreateProfile(name.trim())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) { Text("Continuar") }
            Text(
                "Solo guardaremos el nombre que elijas en este dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
        }
    }
}

@Composable
fun ProfilesScreen(
    profiles: List<UserProfile>,
    activeProfileId: String?,
    onCreateProfile: (String) -> Unit,
    onRenameProfile: (String, String) -> Unit,
    onSelectProfile: (String) -> Unit,
    profileSwitchingEnabled: Boolean = true,
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val editingProfile = profiles.firstOrNull { it.id == editingId }

    ScreenColumn {
        Text(
            "Elige quién usará sus rutinas y preferencias.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!profileSwitchingEnabled) {
            InfoCallout(
                title = "Hay una rutina en curso",
                body = "Reanúdala y finalízala o sal de ella antes de cambiar de perfil.",
            )
        }
        profiles.forEach { profile ->
            val active = profile.id == activeProfileId
            Card(
                onClick = { onSelectProfile(profile.id) },
                enabled = active || profileSwitchingEnabled,
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
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (active) "Perfil activo" else "Seleccionar perfil",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { editingId = profile.id }) { Text("Editar nombre") }
                }
            }
        }
        RutinaGoOutlinedButton(
            onClick = { showCreate = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Crear otro perfil") }
    }

    if (showCreate) {
        ProfileNameDialog(
            title = "Crear perfil",
            initialName = "",
            confirmLabel = "Crear",
            onDismiss = { showCreate = false },
            onConfirm = {
                onCreateProfile(it)
                showCreate = false
            },
        )
    }
    if (editingProfile != null) {
        ProfileNameDialog(
            title = "Editar nombre",
            initialName = editingProfile.name,
            confirmLabel = "Guardar",
            onDismiss = { editingId = null },
            onConfirm = {
                onRenameProfile(editingProfile.id, it)
                editingId = null
            },
        )
    }
}

@Composable
private fun ProfileNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Nombre del perfil") },
                singleLine = true,
                isError = attempted && value.isBlank(),
                supportingText = if (attempted && value.isBlank()) {
                    ({ Text("El nombre no puede estar vacío.") })
                } else {
                    null
                },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                attempted = true
                if (value.isNotBlank()) onConfirm(value.trim())
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

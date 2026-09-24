package com.neuronovaapps.rutinago.ui.screens.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import com.neuronovaapps.rutinago.data.institutional.InstitutionalLinks
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appVersion = remember(context) { loadAppVersion(context) }

    ScreenColumn(modifier = modifier) {
        AppIdentityCard()

        AboutSection("Sobre RutinaGo") {
            AboutBody(
                "RutinaGo es una aplicación de rutinas visuales desarrollada por " +
                    "NeuroNova Apps para facilitar la organización de actividades cotidianas " +
                    "mediante secuencias claras y apoyos visuales.",
            )
            AboutBody(
                "Está orientada a personas con TEA y otras necesidades de apoyo, así como a " +
                    "familias, docentes y profesionales que acompañan el desarrollo de la " +
                    "autonomía personal.",
            )
        }

        AboutSection("¿Qué ofrece RutinaGo?") {
            StatusLabel("Disponible en esta base")
            BulletItem("Organización y consulta de rutinas diarias de demostración.")
            BulletItem("Secuencias de actividades paso a paso con apoyos visuales provisionales.")
            BulletItem("Opciones de accesibilidad y personalización visual.")
            StatusLabel("En desarrollo")
            BulletItem("Creación y personalización definitiva de rutinas.")
            BulletItem("Persistencia de perfiles, rutinas y seguimiento de actividades.")
            BulletItem("Integración de recursos visuales definitivos.")
        }

        AboutSection("Accesibilidad") {
            AboutBody(
                "RutinaGo incorpora opciones de accesibilidad para adaptar la presentación de " +
                    "la información a diferentes preferencias y necesidades de apoyo.",
            )
            BulletItem("Tema Día, Noche o Sistema.")
            BulletItem("Tamaño de texto Normal, Grande o Muy grande.")
            BulletItem("Alto contraste para reforzar fondo, texto, bordes y acciones.")
            BulletItem("Compatibilidad inicial con TalkBack mediante etiquetas y estructura semántica.")
        }

        AboutSection("Privacidad") {
            AboutBody(
                "En esta fase, las preferencias de apariencia se guardan localmente en el " +
                    "dispositivo. Las rutinas, perfiles y el progreso visibles son contenido de " +
                    "demostración y todavía no cuentan con persistencia definitiva.",
            )
            AboutBody(
                "RutinaGo no incluye actualmente una cuenta ni sincronización en la nube. La " +
                    "política oficial explica el tratamiento de datos aplicable y los cambios " +
                    "que puedan acompañar futuras funciones.",
            )
            ExternalLink(
                label = "Política de privacidad",
                uri = InstitutionalLinks.RUTINAGO_PRIVACY_POLICY,
                accessibilityDescription =
                    "Política de privacidad. Abre la política de privacidad en el navegador.",
            )
        }

        AboutSection("Desarrollador") {
            Text(
                text = "NeuroNova Apps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            AboutBody(
                "NeuroNova Apps desarrolla aplicaciones digitales orientadas al aprendizaje, " +
                    "la accesibilidad y el desarrollo de habilidades mediante experiencias " +
                    "interactivas.",
            )
            ExternalLink(
                label = "Sitio oficial de NeuroNova Apps",
                uri = InstitutionalLinks.NEURONOVA_APPS_WEBSITE,
            )
            ExternalLink(
                label = "Contacto: ${InstitutionalLinks.CONTACT_EMAIL}",
                uri = InstitutionalLinks.CONTACT_EMAIL_URI,
            )
        }

        AboutSection("Información de la versión") {
            AppInfoRow("Nombre", "RutinaGo")
            AppInfoRow("Versión instalada", appVersion.name)
            AppInfoRow("Código de versión", appVersion.code)
            AppInfoRow("Desarrollador", "NeuroNova Apps")
            StatusLabel("Versión en desarrollo")
        }

        AboutSection("Créditos y licencias") {
            AboutBody("Desarrollado por NeuroNova Apps.")
            AboutBody(
                "Construido con Kotlin, Jetpack Compose, Material 3, Android Jetpack y " +
                    "DataStore Preferences.",
            )
            Text(
                text = "Pictogramas ARASAAC",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            AboutBody(
                "Atribución pendiente: se incorporará junto con las condiciones de licencia " +
                    "correspondientes si se integran pictogramas de ARASAAC. Esta base no incluye " +
                    "un banco completo de pictogramas.",
            )
        }
    }
}

@Composable
private fun AppIdentityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = aboutBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RutinaGoProvisionalMark()
            Text(
                text = "RUTINAGO",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "Organiza tu día con rutinas visuales",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Desarrollado por NeuroNova Apps.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RutinaGoProvisionalMark() {
    Surface(
        modifier = Modifier
            .size(112.dp)
            .clearAndSetSemantics {
                contentDescription = "Identidad visual provisional de RutinaGo"
            },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "1 · 2 · 3",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = aboutBorder(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            content()
        }
    }
}

@Composable
private fun AboutBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BulletItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusLabel(text: String) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ExternalLink(
    label: String,
    uri: String,
    accessibilityDescription: String = "$label. Abre un enlace externo",
) {
    val context = LocalContext.current
    Surface(
        onClick = { openExternalUri(context, uri) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .semantics {
                contentDescription = accessibilityDescription
                role = Role.Button
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = aboutBorder(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "↗",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

private fun openExternalUri(context: Context, uri: String) {
    val intent = Intent(Intent.ACTION_VIEW, uri.toUri()).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        showExternalLinkError(context)
    } catch (_: SecurityException) {
        showExternalLinkError(context)
    }
}

private fun showExternalLinkError(context: Context) {
    Toast.makeText(
        context,
        "No se encontró una aplicación para abrir este enlace.",
        Toast.LENGTH_SHORT,
    ).show()
}

@Composable
private fun AppInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "$label:",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun aboutBorder(): BorderStroke = BorderStroke(
    width = if (LocalHighContrast.current) 2.dp else 1.dp,
    color = MaterialTheme.colorScheme.outline,
)

private fun loadAppVersion(context: Context): AppVersion = try {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    AppVersion(
        name = packageInfo.versionName.orEmpty().ifBlank { "No disponible" },
        code = PackageInfoCompat.getLongVersionCode(packageInfo).toString(),
    )
} catch (_: PackageManager.NameNotFoundException) {
    AppVersion(name = "No disponible", code = "No disponible")
}

private data class AppVersion(
    val name: String,
    val code: String,
)

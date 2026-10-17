package com.neuronovaapps.rutinago

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.data.preferences.ThemeMode
import com.neuronovaapps.rutinago.ui.screens.communication.CommunicationHomeScreen
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CommunicationFoundationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private data class Appearance(
        val name: String,
        val mode: ThemeMode,
        val systemDark: Boolean = false,
        val highContrast: Boolean = false,
    )

    private data class Scenario(
        val appearance: Appearance,
        val scale: TextScaleMode,
        val smallScreen: Boolean,
    )

    @Test
    fun emptyFoundationRemainsReadableAndAccessibleAcrossAppearancesAndPortraitSizes() {
        val appearances = listOf(
            Appearance("day", ThemeMode.DAY),
            Appearance("night", ThemeMode.NIGHT),
            Appearance("system-light", ThemeMode.SYSTEM),
            Appearance("system-dark", ThemeMode.SYSTEM, systemDark = true),
            Appearance("contrast", ThemeMode.DAY, highContrast = true),
        )
        var scenario by mutableStateOf(Scenario(appearances.first(), TextScaleMode.NORMAL, false))
        var returns = 0
        composeRule.setContent {
            val density = LocalDensity.current
            val current = scenario
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (current.smallScreen) 1.6f else 1f),
            ) {
                RutinaGoTheme(
                    darkTheme = current.appearance.mode.usesDarkTheme(current.appearance.systemDark),
                    textScale = current.scale.scaleFactor,
                    highContrast = current.appearance.highContrast,
                ) {
                    Box(Modifier.requiredSize(
                        if (current.smallScreen) 320.dp else 400.dp,
                        if (current.smallScreen) 480.dp else 760.dp,
                    )) {
                        key(current) {
                            CommunicationHomeScreen("Familia de prueba") { returns++ }
                        }
                    }
                }
            }
        }

        appearances.forEach { appearance ->
            TextScaleMode.entries.forEach { scale ->
                listOf(false, true).forEach { smallScreen ->
                    composeRule.runOnIdle { scenario = Scenario(appearance, scale, smallScreen) }
                    val captureName = "${appearance.name}-${scale.name}-$smallScreen"
                    saveCapture("$captureName-top")
                    listOf(
                        "ComunicaGo",
                        "Perfil activo: Familia de prueba",
                        "Mensaje",
                        "Tu mensaje aparecerá aquí",
                        "Palabras y apoyos visuales",
                        "Las palabras y apoyos visuales aparecerán aquí.",
                        "En la siguiente etapa podrás seleccionar palabras para construir tu mensaje.",
                    ).forEach { text ->
                        val node = composeRule.onNodeWithText(text)
                        node.performScrollTo().assertIsDisplayed()
                        val layouts = mutableListOf<TextLayoutResult>()
                        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                        assertTrue("Missing text layout: $scenario / $text", layouts.isNotEmpty())
                        layouts.forEach { layout ->
                            // Semantics may retain the paragraph's maximum width; inspect actual lines.
                            val widestLine = (0 until layout.lineCount).maxOfOrNull {
                                layout.getLineRight(it) - layout.getLineLeft(it)
                            } ?: 0f
                            assertTrue(
                                "Clipped text: $scenario / $text",
                                !layout.multiParagraph.didExceedMaxLines &&
                                    layout.multiParagraph.height <= layout.size.height + 1f &&
                                    widestLine <= layout.size.width + 1f,
                            )
                        }
                    }
                    saveCapture("$captureName-bottom")
                    listOf("ComunicaGo", "Mensaje", "Palabras y apoyos visuales").forEach {
                        composeRule.onNodeWithText(it)
                            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
                    }
                    listOf("communication-message-area", "communication-vocabulary-area").forEach {
                        composeRule.onNodeWithTag(it).assert(
                            SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true),
                        )
                    }
                    // The only available action is returning; no pretend speech or vocabulary controls.
                    composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
                    listOf("Hablar", "Borrar último", "Limpiar", "Repetir", "Favoritos").forEach {
                        composeRule.onNodeWithText(it).assertDoesNotExist()
                    }
                    val expectedReturns = returns + 1
                    composeRule.onNodeWithTag("communication-back-to-modules")
                        .performScrollTo().assertIsDisplayed().assertIsEnabled()
                        .assertHeightIsAtLeast(48.dp)
                        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                        .performClick()
                    composeRule.runOnIdle { assertEquals(expectedReturns, returns) }
                }
            }
        }
    }

    private fun saveCapture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "phase32-communication-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            composeRule.onNodeWithTag("communication-home").captureToImage()
                .asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

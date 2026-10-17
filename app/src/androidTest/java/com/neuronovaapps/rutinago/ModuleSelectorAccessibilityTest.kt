package com.neuronovaapps.rutinago

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Surface
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
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
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
import com.neuronovaapps.rutinago.ui.screens.modules.ModuleSelectorScreen
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ModuleSelectorAccessibilityTest {
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
    fun moduleOptionsRemainReadableScrollableAndAccessibleAcrossAppearances() {
        val appearances = listOf(
            Appearance("day", ThemeMode.DAY),
            Appearance("night", ThemeMode.NIGHT),
            Appearance("system-light", ThemeMode.SYSTEM),
            Appearance("system-dark", ThemeMode.SYSTEM, systemDark = true),
            Appearance("contrast", ThemeMode.DAY, highContrast = true),
        )
        var scenario by mutableStateOf(Scenario(appearances.first(), TextScaleMode.NORMAL, false))
        var communication by mutableStateOf(false)
        var openedModule = ""
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
                    Surface {
                        Box(Modifier.requiredSize(
                            if (current.smallScreen) 320.dp else 400.dp,
                            if (current.smallScreen) 480.dp else 760.dp,
                        )) {
                            key(current, communication) {
                                if (communication) {
                                    CommunicationHomeScreen("Perfil de prueba") {
                                        communication = false
                                    }
                                } else {
                                    ModuleSelectorScreen(
                                        activeProfileName = "Perfil de prueba",
                                        onOpenRutinaGo = { openedModule = "rutinago" },
                                        onOpenCommunication = { communication = true },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        appearances.forEach { appearance ->
            TextScaleMode.entries.forEach { scale ->
                listOf(false, true).forEach { smallScreen ->
                    composeRule.runOnIdle { scenario = Scenario(appearance, scale, smallScreen) }
                    saveCapture("${appearance.name}-${scale.name}-$smallScreen")
                    listOf(
                        "Elige un módulo",
                        "Perfil activo: Perfil de prueba",
                        "Selecciona el apoyo que deseas utilizar.",
                        "Organiza tus rutinas y consulta tu progreso.",
                        "Un espacio de comunicación en preparación.",
                    ).forEach { text ->
                        val node = composeRule.onNodeWithText(text)
                        node.performScrollTo().assertIsDisplayed()
                        val layouts = mutableListOf<TextLayoutResult>()
                        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                        assertTrue("Missing text: $scenario / $text", layouts.isNotEmpty())
                        assertTextFits(layouts, "$scenario / $text")
                    }
                    listOf("module-rutinago" to "RutinaGo", "module-communication" to "ComunicaGo")
                        .forEach { (tag, title) ->
                            val button = composeRule.onNodeWithTag(tag)
                            button.performScrollTo().assertIsDisplayed().assertHasClickAction()
                                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
                            val layouts = mutableListOf<TextLayoutResult>()
                            composeRule.onNodeWithText(title, useUnmergedTree = true)
                                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                            assertTrue("Missing text layout: $scenario / $title", layouts.isNotEmpty())
                            assertTextFits(layouts, "$scenario / $title")
                            val bounds = button.fetchSemanticsNode().boundsInRoot
                            val viewport = composeRule.onNodeWithTag("module-selector")
                                .fetchSemanticsNode().boundsInRoot
                            assertTrue("Button outside viewport: $scenario", bounds.top >= viewport.top - 1f)
                            assertTrue("Button outside viewport: $scenario", bounds.bottom <= viewport.bottom + 1f)
                        }
                    composeRule.onNodeWithTag("module-rutinago").performScrollTo().performClick()
                    composeRule.runOnIdle { assertEquals("rutinago", openedModule) }
                    composeRule.onNodeWithTag("module-communication").performScrollTo().performClick()
                    composeRule.onNodeWithText("Tu mensaje aparecerá aquí").performScrollTo().assertIsDisplayed()
                    composeRule.onNodeWithTag("communication-back-to-modules")
                        .performScrollTo().assertIsDisplayed().assertHasClickAction().performClick()
                    composeRule.onNodeWithTag("module-selector").assertIsDisplayed()

                }
            }
        }
    }

    private fun saveCapture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "module-shell-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            composeRule.onNodeWithTag("module-selector").captureToImage()
                .asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun assertTextFits(layouts: List<TextLayoutResult>, label: String) {
        layouts.forEach { layout ->
            // Semantics can retain the paragraph's maximum width after intrinsic measurement.
            // Compare the drawn lines, with one pixel for fractional measurement rounding.
            val widestLine = (0 until layout.lineCount).maxOfOrNull {
                layout.getLineRight(it) - layout.getLineLeft(it)
            } ?: 0f
            val fits = !layout.multiParagraph.didExceedMaxLines &&
                layout.multiParagraph.height <= layout.size.height + 1f &&
                widestLine <= layout.size.width + 1f
            assertTrue(
                "Clipped text: $label / size=${layout.size}, " +
                    "lineWidth=$widestLine, textHeight=${layout.multiParagraph.height}",
                fits,
            )
        }
    }
}

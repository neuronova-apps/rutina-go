package com.neuronovaapps.rutinago

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.domain.communication.CommunicationAgeRange
import com.neuronovaapps.rutinago.domain.communication.OralCommunication
import com.neuronovaapps.rutinago.ui.screens.communication.CommunicationProfileSetupScreen
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CommunicationProfileSetupAccessibilityTest {
    @get:Rule val composeRule = createComposeRule()
    private data class Scenario(val appearance: Int, val scale: TextScaleMode, val small: Boolean)

    @Test fun optionsAndActionsRemainReadableAndAccessibleAcrossAppearancesAndTextSizes() {
        var scenario by mutableStateOf(Scenario(0, TextScaleMode.NORMAL, false))
        var saved: Pair<CommunicationAgeRange, OralCommunication>? = null
        composeRule.setContent {
            val current = scenario
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (current.small) 1.6f else 1f)) {
                // Day, Night, System light, System dark, High contrast.
                RutinaGoTheme(darkTheme = current.appearance in listOf(1, 3),
                    highContrast = current.appearance == 4, textScale = current.scale.scaleFactor) {
                    Box(Modifier.requiredSize(if (current.small) 320.dp else 400.dp, if (current.small) 480.dp else 760.dp)) {
                        key(current) {
                            CommunicationProfileSetupScreen(
                                "accessibility-profile", "Perfil de prueba", null, false, false, null,
                                onSave = { age, oral -> saved = age to oral }, onBack = { },
                            )
                        }
                    }
                }
            }
        }
        (0..4).forEach { appearance ->
            TextScaleMode.entries.forEach { scale ->
                listOf(false, true).forEach { small ->
                    composeRule.runOnIdle { scenario = Scenario(appearance, scale, small); saved = null }
                    listOf("Personalizar ComunicaGo", "Perfil activo: Perfil de prueba",
                        "¿Cuál es su rango de edad?", "¿Cómo se comunica oralmente actualmente?").forEach { text ->
                        assertFits(composeRule.onNodeWithText(text))
                    }
                    composeRule.onNodeWithTag("communication-setup-save").performScrollTo().assertIsNotEnabled()
                    CommunicationAgeRange.entries.forEach { age -> selectAndCheck("communication-age-${age.name}") }
                    OralCommunication.entries.forEach { oral -> selectAndCheck("communication-oral-${oral.name}") }
                    composeRule.onNodeWithTag("communication-setup-save").performScrollTo()
                        .assertHeightIsAtLeast(48.dp).assertIsEnabled().performClick()
                    composeRule.runOnIdle {
                        assertEquals(CommunicationAgeRange.NOT_SPECIFIED to OralCommunication.NOT_SPECIFIED, saved)
                    }
                }
            }
        }
    }
    private fun selectAndCheck(tag: String) {
        val node = composeRule.onNodeWithTag(tag).performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        node.assertIsSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        assertFits(node)
    }
    private fun assertFits(node: SemanticsNodeInteraction) {
        node.performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { layout ->
            val widest = (0 until layout.lineCount).maxOfOrNull { layout.getLineRight(it) - layout.getLineLeft(it) } ?: 0f
            assertTrue(!layout.multiParagraph.didExceedMaxLines && layout.multiParagraph.height <= layout.size.height + 1f &&
                widest <= layout.size.width + 1f)
        }
    }
}

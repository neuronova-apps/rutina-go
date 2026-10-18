package com.neuronovaapps.rutinago

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.communication.LocalCommunicationCatalogRepository
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.ui.screens.communication.CommunicationVocabularyContent
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CommunicationVocabularyAccessibilityTest {
    @get:Rule val rule = createComposeRule()
    private data class Scenario(val appearance: Int, val scale: TextScaleMode, val small: Boolean)
    @Test fun administrationSupportsAppearancesTextScalesHiddenStateAndLargeTextScrolling() {
        var scenario by mutableStateOf(Scenario(0, TextScaleMode.NORMAL, false))
        rule.setContent {
            val context = LocalContext.current
            val catalog = remember { LocalCommunicationCatalogRepository {
                context.assets.open(LocalCommunicationCatalogRepository.ASSET_PATH).bufferedReader().use { it.readText() }
            } }
            val current = scenario
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (current.small) 1.6f else 1f)) {
                RutinaGoTheme(darkTheme = current.appearance in listOf(1, 3), highContrast = current.appearance == 4,
                    textScale = current.scale.scaleFactor) {
                    Box(Modifier.requiredSize(if (current.small) 320.dp else 400.dp, if (current.small) 480.dp else 760.dp)) {
                        key(current) {
                            var ids by remember { mutableStateOf(setOf("VOC_083")) }
                            CommunicationVocabularyContent("Perfil de accesibilidad", catalog, ids, false, null,
                                onSave = { ids = it }, onBack = {})
                        }
                    }
                }
            }
        }
        (0..4).forEach { appearance -> TextScaleMode.entries.forEach { scale -> listOf(false, true).forEach { small ->
            rule.runOnIdle { scenario = Scenario(appearance, scale, small) }
            val list = rule.onNodeWithTag("personal-vocabulary")
            list.performScrollToKey("title")
            assertFits(rule.onNodeWithText("Mi vocabulario"))
            list.performScrollToKey("search")
            rule.onNodeWithTag("vocabulary-search").performTextReplacement("VOC_083")
            list.performScrollToKey("VOC_083")
            assertFits(rule.onNodeWithText("Cepillarse los dientes"))
            val toggle = rule.onNodeWithTag("vocabulary-toggle-VOC_083").performScrollTo().assertHeightIsAtLeast(48.dp)
            assertFits(rule.onNodeWithText("Ocultar Cepillarse los dientes"))
            toggle.performClick()
            rule.onNodeWithText("Oculta").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Oculta"))
            assertFits(rule.onNodeWithText("Agregar Cepillarse los dientes"))
            rule.onNodeWithTag("vocabulary-toggle-VOC_083").performClick()
            rule.onNodeWithText("Visible").assertIsDisplayed()
            list.performScrollToKey("filters")
            rule.onNodeWithTag("vocabulary-filter-HIDDEN").performScrollTo().assertHeightIsAtLeast(48.dp).performClick().assertIsSelected()
            assertFits(rule.onNodeWithText("Palabras ocultas"))
            list.performScrollToKey("reset")
            rule.onNodeWithTag("vocabulary-reset").performClick()
            rule.onNodeWithTag("vocabulary-reset-suggested").performScrollTo().assertHeightIsAtLeast(48.dp).performClick().assertIsSelected()
            rule.onNodeWithText("Cancelar").performClick()
        } } }
    }
    private fun assertFits(node: SemanticsNodeInteraction) {
        node.performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { layout ->
            val width = (0 until layout.lineCount).maxOfOrNull { layout.getLineRight(it) - layout.getLineLeft(it) } ?: 0f
            assertTrue(!layout.multiParagraph.didExceedMaxLines && layout.multiParagraph.height <= layout.size.height + 1f && width <= layout.size.width + 1f)
        }
    }
}

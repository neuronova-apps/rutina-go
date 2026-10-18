package com.neuronovaapps.rutinago

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.communication.LocalCommunicationCatalogRepository
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.domain.communication.*
import com.neuronovaapps.rutinago.ui.screens.communication.CommunicationHomeScreen
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CommunicationPersonalHomeAccessibilityTest {
    @get:Rule val rule = createComposeRule()
    private data class Scenario(val appearance: Int, val scale: TextScaleMode, val small: Boolean, val mode: CommunicationPresentationMode)
    @Test fun useAndAssignmentRemainDistinctReadableAndReachableAcrossTheAppearanceMatrix() {
        var scenario by mutableStateOf(Scenario(0, TextScaleMode.NORMAL, false, CommunicationPresentationMode.GUIDED))
        rule.setContent {
            val context = LocalContext.current
            val catalog = remember { LocalCommunicationCatalogRepository {
                context.assets.open(LocalCommunicationCatalogRepository.ASSET_PATH).bufferedReader().use { it.readText() }
            } }
            val current = scenario
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (current.small) 1.6f else 1f)) {
                // Day, Night, System light/dark, and high contrast use the existing theme values.
                RutinaGoTheme(darkTheme = current.appearance in listOf(1, 3), highContrast = current.appearance == 4,
                    textScale = current.scale.scaleFactor) {
                    Box(Modifier.requiredSize(if (current.small) 320.dp else 400.dp, if (current.small) 480.dp else 760.dp)) {
                        key(current) {
                            var home by remember { mutableStateOf(CommunicationMessageBuilder.CORE_VOCABULARY_IDS.toSet() + "VOC_083") }
                            CommunicationHomeScreen("Perfil accesibilidad", presentationMode = current.mode,
                                visibleVocabularyIds = catalog.vocabulary.map { it.vocabId }.toSet(), homeVocabularyIds = home,
                                onSetHomeWord = { id, assigned -> home = if (assigned) home + id else home - id },
                                onBackToModules = {})
                        }
                    }
                }
            }
        }
        (0..4).forEach { appearance -> TextScaleMode.entries.forEach { scale -> listOf(false, true).forEach { small ->
            CommunicationPresentationMode.entries.forEach { mode ->
                rule.runOnIdle { scenario = Scenario(appearance, scale, small, mode) }
                rule.awaitCommunicationCatalog()
                rule.onNodeWithTag("communication-home").performScrollToKey("core-VOC_083")
                assertFits(rule.onNodeWithTag("communication-core-VOC_083"))
                rule.onNodeWithTag("communication-core-VOC_083").assertHeightIsAtLeast(48.dp)
                rule.onNodeWithTag("communication-home").performScrollToKey("organize-home")
                assertFits(rule.onNodeWithTag("communication-organize-home"))
                rule.selectCommunicationCategory("CAT_003")
                rule.onNodeWithTag("communication-category-grid").performScrollToKey("VOC_015")
                val use = rule.onNodeWithTag("communication-word-VOC_015").performScrollTo().assertHeightIsAtLeast(48.dp)
                use.assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Usar palabra: Agua")))
                val add = rule.onNodeWithTag("home-assignment-VOC_015").performScrollTo().assertHeightIsAtLeast(48.dp)
                assertFits(add)
                add.assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Añadir a Inicio: Agua")))
                // These are sibling controls with independent touch surfaces and callbacks.
                val useBounds = use.fetchSemanticsNode().boundsInRoot
                val addBounds = add.fetchSemanticsNode().boundsInRoot
                assertTrue(useBounds.bottom <= addBounds.top + 1)
                add.performClick()
                rule.onNodeWithTag("home-assigned-VOC_015").assertTextEquals("En Inicio")
                rule.onNodeWithTag("communication-compact-message").assertTextEquals("Mensaje: Tu mensaje aparecerá aquí")
                rule.onNodeWithTag("communication-word-VOC_015").performScrollTo().performClick()
                rule.onNodeWithTag("communication-compact-message").assertTextEquals("Mensaje: Agua")
                rule.onNodeWithTag("communication-destination-HOME").performClick()
                rule.onNodeWithTag("communication-home").performScrollToKey("organize-home")
                rule.onNodeWithTag("communication-organize-home").performClick()
                val organization = rule.onNodeWithTag("home-vocabulary-organization")
                organization.performScrollToKey("VOC_015")
                val remove = rule.onNodeWithTag("home-assignment-VOC_015").performScrollTo().assertHeightIsAtLeast(48.dp)
                assertFits(remove)
                remove.assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Quitar de Inicio: Agua")))
                remove.performClick()
                organization.performScrollToKey("back")
                rule.onNodeWithTag("home-organization-back").performClick()
                rule.onNodeWithTag("communication-destination-HOME").assertIsSelected()
                rule.scrollCommunicationTo("message")
                rule.onNodeWithTag("communication-message-item-1").assertTextEquals("Agua")
            }
        } } }
    }
    private fun assertFits(node: SemanticsNodeInteraction) {
        node.performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { assertFalse(it.hasVisualOverflow) }
    }
}

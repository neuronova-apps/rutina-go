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
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.neuronovaapps.rutinago.data.communication.LocalCommunicationCatalogRepository
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.data.preferences.ThemeMode
import com.neuronovaapps.rutinago.domain.communication.CommunicationPresentationMode
import com.neuronovaapps.rutinago.domain.communication.CommunicationSuggestionReason
import com.neuronovaapps.rutinago.ui.screens.communication.CommunicationHomeScreen
import com.neuronovaapps.rutinago.ui.screens.communication.components.CommunicationPresentationSelector
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CommunicationFoundationTest {
    @get:Rule val composeRule = createComposeRule()
    private val catalog by lazy {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        LocalCommunicationCatalogRepository {
            assets.open(LocalCommunicationCatalogRepository.ASSET_PATH).bufferedReader().use { it.readText() }
        }
    }
    private val grid get() = composeRule.onNodeWithTag("communication-home")

    @Test fun all132WordsAndEveryCategoryRemainBrowsableAndSelectableWithoutSpeech() {
        composeRule.setContent { RutinaGoTheme { CommunicationHomeScreen("Perfil catálogo") { } } }
        awaitCatalog()
        assertCount(132)
        catalog.vocabulary.forEach { word ->
            grid.performScrollToKey(word.vocabId)
            composeRule.onNodeWithTag("communication-word-${word.vocabId}")
                .assertIsDisplayed().assertTextEquals(word.text).assertHasClickAction()
        }
        catalog.categories.forEach { category ->
            selectCategory(category.id)
            val expected = catalog.vocabularyForCategory(category.id)
            composeRule.onNodeWithTag("communication-category-${category.id}").assertIsSelected()
            assertCount(expected.size)
            expected.forEach { word ->
                grid.performScrollToKey(word.vocabId)
                composeRule.onNodeWithTag("communication-word-${word.vocabId}").assertTextEquals(word.text)
            }
            val wrong = catalog.vocabulary.first { it.categoryId != category.id }
            composeRule.onNodeWithTag("communication-word-${wrong.vocabId}").assertDoesNotExist()
        }
        selectCategory(null)
        assertCount(132)
        grid.performScrollToKey("profile")
        composeRule.onNodeWithText("Perfil activo: Perfil catálogo").assertIsDisplayed()
        grid.performScrollToKey("message")
        composeRule.onNodeWithText("Tu mensaje aparecerá aquí").assertIsDisplayed()
        composeRule.onNodeWithTag("communication-remove-last").assertIsNotEnabled()
        composeRule.onNodeWithTag("communication-clear").assertIsNotEnabled()
        listOf("Hablar", "Repetir", "Favoritos").forEach {
            composeRule.onNodeWithText(it).assertDoesNotExist()
        }
    }

    private data class Appearance(val name: String, val mode: ThemeMode,
        val systemDark: Boolean = false, val highContrast: Boolean = false)
    private data class Scenario(val appearance: Appearance, val scale: TextScaleMode, val small: Boolean,
        val mode: CommunicationPresentationMode = CommunicationPresentationMode.GUIDED)

    @Test fun catalogIsReadableAndSelectionAccessibleAcrossAllAppearancesAndTextSizes() {
        val appearances = listOf(Appearance("day", ThemeMode.DAY), Appearance("night", ThemeMode.NIGHT),
            Appearance("system-light", ThemeMode.SYSTEM), Appearance("system-dark", ThemeMode.SYSTEM, true),
            Appearance("contrast", ThemeMode.DAY, highContrast = true))
        var scenario by mutableStateOf(Scenario(appearances.first(), TextScaleMode.NORMAL, false))
        var returns = 0
        composeRule.setContent {
            val density = LocalDensity.current
            val current = scenario
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (current.small) 1.6f else 1f)) {
                RutinaGoTheme(
                    darkTheme = current.appearance.mode.usesDarkTheme(current.appearance.systemDark),
                    textScale = current.scale.scaleFactor, highContrast = current.appearance.highContrast,
                ) {
                    Box(Modifier.requiredSize(if (current.small) 320.dp else 400.dp, if (current.small) 480.dp else 760.dp)) {
                        key(current) {
                            CommunicationHomeScreen("Familia de prueba", presentationMode = current.mode,
                                presentationControls = {
                                    CommunicationPresentationSelector(current.mode, false, null, false,
                                        CommunicationSuggestionReason.GENERAL_START, {}, {}, {})
                                }) { returns++ }
                        }
                    }
                }
            }
        }
        var expectedReturns = 0
        CommunicationPresentationMode.entries.forEach { mode ->
        appearances.forEach { appearance ->
            TextScaleMode.entries.forEach { scale ->
                listOf(false, true).forEach { small ->
                    composeRule.runOnIdle { scenario = Scenario(appearance, scale, small, mode) }
                    awaitCatalog()
                    grid.performScrollToKey("presentation")
                    composeRule.onNodeWithTag("communication-presentation-${mode.name}").performScrollTo()
                        .assertIsSelected().assertHeightIsAtLeast(48.dp)
                        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                    assertTextFits(composeRule.onNodeWithText(if (mode == CommunicationPresentationMode.GUIDED)
                        "Vista guiada" else "Vista completa"), scenario.toString())
                    listOf("title" to "ComunicaGo", "profile" to "Perfil activo: Familia de prueba",
                        "message" to "Tu mensaje aparecerá aquí", "categories-title" to "Categorías",
                        "vocabulary-title" to "Vocabulario").forEach { (key, text) ->
                        grid.performScrollToKey(key)
                        assertTextFits(composeRule.onNodeWithText(text), scenario.toString())
                    }
                    catalog.categories.forEach { category ->
                        selectCategory(category.id)
                        val node = composeRule.onNodeWithTag("communication-category-${category.id}")
                        node.assertIsSelected().assertHeightIsAtLeast(48.dp)
                            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Seleccionada"))
                        assertTextFits(composeRule.onNodeWithText(category.name), scenario.toString())
                    }
                    selectCategory(null)
                    val words = listOf(catalog.vocabulary.maxBy { it.text.length },
                        catalog.vocabulary.first { it.visualStrategy == "SOLO_TEXTO" },
                        catalog.vocabulary.first { it.visualStrategy == "PICTOGRAMA_PENDIENTE" },
                        catalog.vocabulary.last())
                    words.forEach { word ->
                        grid.performScrollToKey(word.vocabId)
                        assertTextFits(composeRule.onNodeWithText(word.text), scenario.toString())
                    }
                    grid.performScrollToKey("core-VOC_006")
                    composeRule.onNodeWithTag("communication-core-VOC_006").assertHeightIsAtLeast(48.dp).performClick()
                    selectCategory("CAT_003")
                    grid.performScrollToKey("VOC_015")
                    composeRule.onNodeWithTag("communication-word-VOC_015").performClick()
                    grid.performScrollToKey("message")
                    assertTextFits(composeRule.onNodeWithTag("communication-message-item-1"), scenario.toString())
                    assertTextFits(composeRule.onNodeWithTag("communication-message-item-2"), scenario.toString())
                    composeRule.onNodeWithTag("communication-clear").performScrollTo().performClick()
                    composeRule.onNodeWithTag("communication-undo-clear").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
                    composeRule.onNodeWithTag("communication-remove-last").performScrollTo().performClick()
                    composeRule.onNodeWithTag("communication-message-item-2").assertDoesNotExist()
                    saveCapture("${mode.name}-${appearance.name}-${scale.name}-$small")
                    grid.performScrollToKey("back")
                    composeRule.onNodeWithTag("communication-back-to-modules")
                        .assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
                    expectedReturns++
                    composeRule.runOnIdle { assertEquals(expectedReturns, returns) }
                }
            }
        }
        }
    }

    private fun awaitCatalog() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { grid.performScrollToKey("categories"); true }.getOrDefault(false)
        }
    }
    private fun selectCategory(id: String?) {
        composeRule.selectCommunicationCategory(id)
        composeRule.openCommunicationCategories()
        composeRule.onNodeWithTag("communication-category-${id ?: "all"}").performScrollTo()
    }
    private fun assertCount(expected: Int) {
        grid.performScrollToKey("count")
        composeRule.onNodeWithTag("communication-vocabulary-count").assertTextEquals("$expected palabras")
    }
    private fun assertTextFits(node: SemanticsNodeInteraction, scenario: String) {
        node.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { layout ->
            val widest = (0 until layout.lineCount).maxOfOrNull { layout.getLineRight(it) - layout.getLineLeft(it) } ?: 0f
            assertTrue("Clipped: $scenario", !layout.multiParagraph.didExceedMaxLines &&
                layout.multiParagraph.height <= layout.size.height + 1f && widest <= layout.size.width + 1f)
        }
    }
    private fun saveCapture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "phase34-communication-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            grid.captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

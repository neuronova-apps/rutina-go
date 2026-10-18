package com.neuronovaapps.rutinago

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.data.preferences.ThemeMode
import com.neuronovaapps.rutinago.domain.communication.CommunicationDestination
import com.neuronovaapps.rutinago.ui.screens.communication.CommunicationHomeScreen
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CommunicationBottomNavigationAccessibilityTest {
    @get:Rule val rule = createComposeRule()
    private data class Scenario(val theme: ThemeMode, val systemDark: Boolean,
        val contrast: Boolean, val scale: TextScaleMode, val small: Boolean)

    @Test fun labelsTouchTargetsSelectionAndContentFitAcrossThemesSizesAndAllThreeTabs() {
        val appearances = listOf(Triple(ThemeMode.DAY, false, false), Triple(ThemeMode.NIGHT, false, false),
            Triple(ThemeMode.SYSTEM, false, false), Triple(ThemeMode.SYSTEM, true, false), Triple(ThemeMode.DAY, false, true))
        var scenario by mutableStateOf(Scenario(ThemeMode.DAY, false, false, TextScaleMode.NORMAL, false))
        rule.setContent {
            val current = scenario
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (current.small) 1.6f else 1f)) {
                RutinaGoTheme(darkTheme = current.theme.usesDarkTheme(current.systemDark),
                    textScale = current.scale.scaleFactor, highContrast = current.contrast) {
                    Box(Modifier.requiredSize(if (current.small) 320.dp else 400.dp, if (current.small) 480.dp else 760.dp)) {
                        key(current) { CommunicationHomeScreen("Perfil accesibilidad") {} }
                    }
                }
            }
        }
        appearances.forEach { (theme, systemDark, contrast) ->
            TextScaleMode.entries.forEach { scale ->
                listOf(false, true).forEach { small ->
                    rule.runOnIdle { scenario = Scenario(theme, systemDark, contrast, scale, small) }
                    rule.awaitCommunicationCatalog()
                    CommunicationDestination.entries.forEach { destination ->
                        val tag = "communication-destination-${destination.name}"
                        val tab = rule.onNodeWithTag(tag).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
                            .assertWidthIsAtLeast(48.dp).assertHasClickAction().performClick().assertIsSelected()
                        tab.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
                        val label = rule.onNode(hasText(destination.label) and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)
                        val layouts = mutableListOf<TextLayoutResult>()
                        label.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                        assertTrue(layouts.isNotEmpty())
                        layouts.forEach {
                            assertFalse("Clipped ${it.layoutInput.text} in $scenario: size=${it.size}, " +
                                "paragraph=${it.multiParagraph.width}x${it.multiParagraph.height}, " +
                                "width=${it.didOverflowWidth}, height=${it.didOverflowHeight}", it.hasVisualOverflow)
                        }
                        rule.onNodeWithTag("communication-bottom-bar").assertIsDisplayed()
                    }
                    rule.scrollCommunicationTo("message")
                    val content = rule.onNodeWithTag("communication-home").fetchSemanticsNode().boundsInRoot
                    val bar = rule.onNodeWithTag("communication-bottom-bar").fetchSemanticsNode().boundsInRoot
                    assertTrue("Bar covers content in $scenario", content.bottom <= bar.top + 1)
                    // Long compact messages retain their full spoken text and stay above category results.
                    rule.scrollCommunicationTo("core-VOC_006")
                    repeat(12) { rule.onNodeWithTag("communication-core-VOC_006").performClick() }
                    rule.scrollCommunicationTo("categories")
                    rule.onNodeWithTag("communication-compact-message").assertIsDisplayed()
                        .assertTextEquals("Mensaje: " + List(12) { "Quiero" }.joinToString(" · "))
                        .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
                    val message = rule.onNodeWithTag("communication-compact-message").fetchSemanticsNode().boundsInRoot
                    val results = rule.onNodeWithTag("communication-category-grid").fetchSemanticsNode().boundsInRoot
                    assertTrue(message.bottom <= results.top)
                    assertTrue("No room for categories in $scenario", results.height > 48)
                    rule.onNodeWithTag("communication-view-full-message").performClick()
                    rule.onNodeWithTag("communication-destination-HOME").assertIsSelected()
                }
            }
        }
    }
}

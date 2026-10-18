package com.neuronovaapps.rutinago

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.domain.communication.*
import com.neuronovaapps.rutinago.ui.screens.communication.*
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import org.junit.*
import org.junit.Assert.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class CommunicationSpeechAccessibilityTest {
    // Async catalog/state changes must be scheduled by the UI test clock, never applied
    // directly on an IO continuation through the default UnconfinedTestDispatcher.
    @get:Rule val rule = createComposeRule(effectContext = StandardTestDispatcher())
    private data class Scenario(val appearance: Int, val scale: TextScaleMode, val small: Boolean, val mode: CommunicationPresentationMode)
    @Test fun speechControlsRemainReadableDistinctAndReachableAcrossSixtyScenarios() {
        var scenario by mutableStateOf(Scenario(0, TextScaleMode.NORMAL, false, CommunicationPresentationMode.GUIDED))
        lateinit var engine: FakeSpeechEngine
        rule.setContent {
            val current = scenario
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (current.small) 1.6f else 1f)) {
                RutinaGoTheme(darkTheme = current.appearance in listOf(1, 3), highContrast = current.appearance == 4,
                    textScale = current.scale.scaleFactor) {
                    Box(Modifier.requiredSize(if (current.small) 320.dp else 400.dp, if (current.small) 480.dp else 760.dp)) {
                        key(current) {
                            var rate by remember { mutableStateOf(CommunicationSpeechRate.NORMAL) }
                            val speech = rememberCommunicationSpeechController("speech-a11y") {
                                FakeSpeechEngine().also { engine = it }
                            }
                            CommunicationHomeScreen("Voz accesible", presentationMode = current.mode,
                                speechContent = { CommunicationSpeechControls(speech, it, rate) },
                                speechSettings = { CommunicationSpeechSettings(speech, rate, false, null) { rate = it } },
                                onBackToModules = {})
                        }
                    }
                }
            }
        }
        var verified = 0
        for (appearance in 0..4) for (scale in TextScaleMode.entries) for (small in listOf(false, true))
            for (mode in CommunicationPresentationMode.entries) {
                rule.runOnIdle { scenario = Scenario(appearance, scale, small, mode) }
                rule.awaitCommunicationCatalog()
                rule.scrollCommunicationTo("message")
                rule.onNodeWithTag("communication-speak").performScrollTo().assertIsNotEnabled()
                rule.scrollCommunicationTo("core-VOC_006")
                rule.onNodeWithTag("communication-core-VOC_006").performClick()
                rule.scrollCommunicationTo("message")
                val speak = rule.onNodeWithTag("communication-speak").performScrollTo().assertIsDisplayed()
                    .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).assertTextContains("Hablar")
                speak.performClick()
                val stop = rule.onNodeWithTag("communication-speech-stop").performScrollTo().assertIsDisplayed()
                    .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).assertIsEnabled()
                val a = speak.fetchSemanticsNode().boundsInRoot
                val b = stop.fetchSemanticsNode().boundsInRoot
                assertFalse("Overlapping speech controls: $scenario", a.overlaps(b))
                rule.runOnIdle { engine.finish() }
                rule.onNodeWithTag("communication-speech-repeat").performScrollTo().assertIsDisplayed().assertIsEnabled()
                    .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
                rule.onNodeWithTag("communication-speech-status").performScrollTo()
                    .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
                rule.onNodeWithTag("communication-bottom-bar").assertIsDisplayed()
                if (appearance == 4 && scale == TextScaleMode.VERY_LARGE && small && mode == CommunicationPresentationMode.FULL)
                    rule.captureSpeechEvidence("speech-large-contrast")
                rule.onNodeWithTag("communication-destination-SETTINGS").performClick()
                rule.onNodeWithTag("communication-voice-rate-FAST").performScrollTo().assertIsDisplayed()
                    .assertHeightIsAtLeast(48.dp).performClick().assertIsSelected()
                if (appearance == 4 && scale == TextScaleMode.VERY_LARGE && small && mode == CommunicationPresentationMode.FULL)
                    rule.captureSpeechEvidence("speed-large-contrast")
                android.util.Log.i("SpeechMatrix", "Verified ${++verified}/60: $scenario")
            }
        assertEquals(60, verified)
    }
    @Test fun backgroundStopsAndForegroundDoesNotReplayAndDisposalShutsDown() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle get() = registry
        }
        rule.runOnUiThread { owner.registry.currentState = Lifecycle.State.RESUMED }
        val engine = FakeSpeechEngine()
        lateinit var controller: CommunicationSpeechController
        var shown by mutableStateOf(true)
        rule.setContent {
            RutinaGoTheme {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    if (shown) {
                        controller = rememberCommunicationSpeechController("lifecycle-test") { engine }
                        CommunicationSpeechControls(controller,
                            listOf(CommunicationMessageItem(1, "VOC_015", "Agua")), CommunicationSpeechRate.NORMAL)
                    }
                }
            }
        }
        rule.onNodeWithTag("communication-speak").performClick()
        val stops = engine.stops
        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        assertTrue(engine.stops > stops)
        assertFalse(controller.state.value.active)
        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        assertEquals(1, engine.requests.size)
        rule.runOnIdle { shown = false }
        rule.waitForIdle()
        assertEquals(1, engine.shutdowns)
        rule.runOnIdle { engine.finish() }
        assertNull(controller.state.value.lastConfirmed)
    }
}

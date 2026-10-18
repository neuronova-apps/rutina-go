package com.neuronovaapps.rutinago

import com.neuronovaapps.rutinago.data.communication.CommunicationStateCodec
import com.neuronovaapps.rutinago.data.communication.LocalCommunicationCatalogRepository
import com.neuronovaapps.rutinago.domain.communication.*
import com.neuronovaapps.rutinago.ui.screens.communication.components.presentationLayout
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class CommunicationPresentationTest {
    private val catalog by lazy {
        val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "app/src/main/assets/communication").isDirectory }
        LocalCommunicationCatalogRepository {
            File(root, "app/src/main/assets/${LocalCommunicationCatalogRepository.ASSET_PATH}").readText()
        }
    }

    @Test fun emergingAndNoCurrentSpeechSuggestGuidedWithoutClassifyingAbilities() {
        listOf(OralCommunication.EMERGING_SPEECH, OralCommunication.NO_CURRENT_SPEECH).forEach { oral ->
            val suggestion = CommunicationPresentationPolicy.suggest(
                CommunicationProfileSetup("profile", CommunicationAgeRange.YEARS_9_TO_12, oral))
            assertEquals(CommunicationPresentationMode.GUIDED, suggestion.mode)
            assertEquals(CommunicationSuggestionReason.QUICK_ACCESS_START, suggestion.reason)
        }
    }

    @Test fun ageNeverDeterminesTheSuggestionOrAutomaticallyAssignsFull() {
        OralCommunication.entries.forEach { oral ->
            val suggestions = CommunicationAgeRange.entries.map { age ->
                CommunicationPresentationPolicy.suggest(CommunicationProfileSetup("profile", age, oral))
            }
            assertEquals(1, suggestions.toSet().size)
            assertTrue(suggestions.all { it.mode == CommunicationPresentationMode.GUIDED })
        }
        assertEquals(CommunicationPresentationMode.GUIDED, CommunicationPresentationPolicy.suggest(null).mode)
    }

    @Test fun explicitPreferenceAlwaysWinsForAllThirtyProfileCombinations() {
        CommunicationAgeRange.entries.forEach { age ->
            OralCommunication.entries.forEach { oral ->
                val setup = CommunicationProfileSetup("profile", age, oral)
                CommunicationPresentationMode.entries.forEach { mode ->
                    assertEquals(mode, CommunicationPresentationPolicy.resolve(setup,
                        CommunicationPresentationPreferences("profile", mode, true)))
                }
            }
        }
    }

    @Test fun bothViewsKeepTheSame132Words18CategoriesAndSixRealCoreReferences() {
        val builder = CommunicationMessageBuilder(catalog)
        CommunicationPresentationMode.entries.forEach { mode ->
            val preference = CommunicationPresentationPreferences("profile", mode)
            assertEquals(mode, CommunicationPresentationPolicy.resolve(null, preference))
            assertEquals(132, builder.visibleVocabulary(builder.emptyState()).size)
            assertEquals(18, catalog.categories.size)
            assertEquals(listOf("VOC_001", "VOC_006", "VOC_007", "VOC_063", "VOC_053", "VOC_014"),
                builder.coreVocabulary.map { it.vocabId })
            catalog.categories.forEach { category ->
                assertEquals(catalog.vocabulary.filter { it.categoryId == category.id },
                    builder.visibleVocabulary(builder.selectCategory(builder.emptyState(), category.id)))
            }
        }
    }

    @Test fun resolvingViewsCannotRewriteMessageOccurrencesCategoryOrUndo() {
        val builder = CommunicationMessageBuilder(catalog)
        val message = builder.selectCategory(
            listOf("VOC_001", "VOC_006", "VOC_015", "VOC_015").fold(builder.emptyState(), builder::add), "CAT_003")
        val cleared = builder.clear(message)
        val originalMessage = CommunicationStateCodec.encode(message)
        val originalUndo = CommunicationStateCodec.encode(cleared)
        CommunicationPresentationMode.entries.forEach { mode ->
            CommunicationPresentationPolicy.resolve(null, CommunicationPresentationPreferences("profile", mode))
            assertEquals(originalMessage, CommunicationStateCodec.encode(message))
            assertEquals(originalUndo, CommunicationStateCodec.encode(cleared))
            assertEquals(message.messageItems, builder.undoClear(cleared).messageItems)
            assertEquals("CAT_003", message.selectedCategoryId)
        }
    }

    @Test fun guidedEmphasizesQuickAccessWhileFullIsMoreCompactWithAccessibleMinimums() {
        val guided = presentationLayout(CommunicationPresentationMode.GUIDED)
        val full = presentationLayout(CommunicationPresentationMode.FULL)
        assertTrue(guided.minimumCardWidth > full.minimumCardWidth)
        assertTrue(guided.quickAccessHeight > full.quickAccessHeight)
        assertTrue(guided.spacing > full.spacing)
        listOf(guided, full).forEach {
            assertTrue(it.wordHeight >= 48 && it.quickAccessHeight >= 48)
            assertTrue(it.cardPadding > 0)
        }
    }
}

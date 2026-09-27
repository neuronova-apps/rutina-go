package com.neuronovaapps.rutinago

import com.neuronovaapps.rutinago.domain.arasaac.ArasaacApiException
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacFailureKind
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacPictogramCandidate
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacRepository
import com.neuronovaapps.rutinago.ui.screens.arasaac.ARASAAC_ATTRIBUTION
import com.neuronovaapps.rutinago.ui.screens.arasaac.ArasaacSearchController
import com.neuronovaapps.rutinago.ui.screens.arasaac.ArasaacSearchState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArasaacSearchControllerTest {
    private val candidate = ArasaacPictogramCandidate(
        id = 10384, keywords = listOf("lavarse las manos"), description = null,
        categories = emptyList(), previewUrl = "https://static.arasaac.org/pictograms/10384/10384_300.png",
    )

    @Test fun initialTermPreloadsAndDebouncesSearch() = runTest {
        val fake = FakeRepository()
        val controller = ArasaacSearchController("lavarse manos", fake, backgroundScope)
        runCurrent()
        assertEquals("lavarse manos", controller.query)
        assertTrue(controller.state.value is ArasaacSearchState.Loading)
        advanceTimeBy(399)
        runCurrent()
        assertTrue(fake.queries.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("lavarse manos"), fake.queries)
        assertTrue(controller.state.value is ArasaacSearchState.Success)
    }

    @Test fun typingCancelsPreviousRequestAndOnlyLatestResultShows() = runTest {
        val fake = FakeRepository()
        fake.firstBlocked = CompletableDeferred()
        val controller = ArasaacSearchController("", fake, backgroundScope)
        runCurrent()
        controller.setQuery("lavarse")
        advanceTimeBy(400)
        runCurrent()
        assertEquals(listOf("lavarse"), fake.queries)
        controller.setQuery("comer")
        advanceTimeBy(400)
        runCurrent()
        assertEquals(listOf("lavarse", "comer"), fake.queries)
        assertEquals("comer", fake.queries.last())
        assertTrue(controller.state.value is ArasaacSearchState.Success)
    }

    @Test fun emptyQueryAndEmptyResponseAreDifferentStates() = runTest {
        val fake = FakeRepository(results = emptyList())
        val controller = ArasaacSearchController("", fake, backgroundScope)
        runCurrent()
        assertEquals(ArasaacSearchState.Idle, controller.state.value)
        controller.setQuery("desconocido")
        advanceTimeBy(400)
        runCurrent()
        assertEquals(ArasaacSearchState.Empty, controller.state.value)
        controller.setQuery("")
        runCurrent()
        assertEquals(ArasaacSearchState.Idle, controller.state.value)
    }

    @Test fun rateLimitRetryWaitsAndDoesNotLoop() = runTest {
        val fake = FakeRepository()
        fake.failure = ArasaacApiException(ArasaacFailureKind.RATE_LIMITED, 429, 2_000)
        val controller = ArasaacSearchController("comer", fake, backgroundScope)
        advanceTimeBy(400)
        runCurrent()
        assertEquals(1, fake.queries.size)
        assertTrue(controller.state.value is ArasaacSearchState.Error)
        fake.failure = null
        controller.retry()
        advanceTimeBy(1_999)
        runCurrent()
        assertEquals(1, fake.queries.size)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, fake.queries.size)
    }

    @Test fun attributionIsAvailableAndNoSelectionPersistenceApiExists() {
        assertTrue(ARASAAC_ATTRIBUTION.contains("Sergio Palao"))
        assertTrue(ARASAAC_ATTRIBUTION.contains("Gobierno de Aragón"))
        assertTrue(ARASAAC_ATTRIBUTION.contains("BY-NC-SA"))
        assertEquals(2, ArasaacRepository::class.java.methods.count {
            it.name == "searchPictograms" || it.name == "getPictogram"
        })
    }

    private inner class FakeRepository(
        val results: List<ArasaacPictogramCandidate> = listOf(candidate),
    ) : ArasaacRepository {
        val queries = mutableListOf<String>()
        var firstBlocked: CompletableDeferred<Unit>? = null
        var failure: ArasaacApiException? = null

        override suspend fun searchPictograms(query: String): List<ArasaacPictogramCandidate> {
            queries += query
            if (queries.size == 1) firstBlocked?.await()
            failure?.let { throw it }
            return results
        }

        override suspend fun getPictogram(id: Long): ArasaacPictogramCandidate = candidate
    }
}

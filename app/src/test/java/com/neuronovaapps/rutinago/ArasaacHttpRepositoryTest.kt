package com.neuronovaapps.rutinago

import com.neuronovaapps.rutinago.data.arasaac.ArasaacHttpRepository
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacApiException
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacFailureKind
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacImageResolution
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacImageUrls
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ArasaacHttpRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: ArasaacHttpRepository

    @Before fun setup() {
        server = MockWebServer()
        server.start()
        repository = ArasaacHttpRepository(baseUrl = server.url("/"))
    }

    @After fun tearDown() { server.close() }

    @Test fun imageUrlsUseRealIdAndConfiguredResolution() {
        assertEquals(
            "https://static.arasaac.org/pictograms/10384/10384_300.png",
            ArasaacImageUrls.forPictogram(10384, ArasaacImageResolution.THUMBNAIL),
        )
        assertEquals(
            "https://static.arasaac.org/pictograms/10384/10384_500.png",
            ArasaacImageUrls.forPictogram(10384, ArasaacImageResolution.PREVIEW),
        )
    }

    @Test fun realShapeParsesIdMetadataAndOnlySearchTextGoesInRequest() = runBlocking {
        server.enqueue(MockResponse.Builder().body(
            """[{"_id":10384,"keywords":[{"keyword":"lavarse las manos","meaning":"higiene"}],"desc":"Lavar las manos","categories":["Acciones"]}]""",
        ).build())
        val results = repository.searchPictograms("lavarse manos")
        assertEquals(10384L, results.single().id)
        assertEquals("lavarse las manos", results.single().label)
        assertEquals("Lavar las manos", results.single().description)
        assertEquals("ARASAAC", results.single().source)
        assertEquals("/v1/pictograms/es/search/lavarse%20manos", server.takeRequest().url.encodedPath)
        assertEquals(null, server.takeRequest(50, TimeUnit.MILLISECONDS))
    }

    @Test fun detailUsesActualIdReturnedByService() = runBlocking {
        server.enqueue(MockResponse.Builder().body("""{"_id":6456,"keywords":[{"keyword":"comer"}]}""").build())
        assertEquals(6456L, repository.getPictogram(6456).id)
        assertEquals("/v1/pictograms/es/6456", server.takeRequest().url.encodedPath)
    }

    @Test fun emptyAndSearch404AreEmptyStates() = runBlocking {
        server.enqueue(MockResponse.Builder().body("[]").build())
        server.enqueue(MockResponse.Builder().code(404).body("[]").build())
        assertTrue(repository.searchPictograms("sin resultados").isEmpty())
        assertTrue(repository.searchPictograms("sin resultados").isEmpty())
    }

    @Test fun http429IncludesBoundedRetryDelay() = runBlocking {
        server.enqueue(MockResponse.Builder().code(429).addHeader("Retry-After", "5").build())
        val error = expectFailure { repository.searchPictograms("comer") }
        assertEquals(ArasaacFailureKind.RATE_LIMITED, error.kind)
        assertEquals(5_000L, error.retryAfterMillis)
    }

    @Test fun serverFailureAndInvalidJsonAreTyped() = runBlocking {
        server.enqueue(MockResponse.Builder().code(503).build())
        server.enqueue(MockResponse.Builder().body("{wrong json").build())
        assertEquals(ArasaacFailureKind.TEMPORARY,
            expectFailure { repository.searchPictograms("comer") }.kind)
        assertEquals(ArasaacFailureKind.INVALID_RESPONSE,
            expectFailure { repository.searchPictograms("comer") }.kind)
    }

    @Test fun missingDetailIsNotFound() = runBlocking {
        server.enqueue(MockResponse.Builder().code(404).build())
        assertEquals(ArasaacFailureKind.NOT_FOUND, expectFailure { repository.getPictogram(999) }.kind)
    }

    @Test fun missingOrFractionalIdNeverBecomesAFakeCandidate() = runBlocking {
        server.enqueue(MockResponse.Builder().body("""[{"keywords":[{"keyword":"comer"}]},{"_id":1.5}]""").build())
        assertEquals(ArasaacFailureKind.INVALID_RESPONSE,
            expectFailure { repository.searchPictograms("comer") }.kind)
    }

    @Test fun timeoutIsTyped() = runBlocking {
        server.enqueue(MockResponse.Builder().body("[]").bodyDelay(250, TimeUnit.MILLISECONDS).build())
        val client = OkHttpClient.Builder().readTimeout(40, TimeUnit.MILLISECONDS)
            .callTimeout(100, TimeUnit.MILLISECONDS).build()
        val slow = ArasaacHttpRepository(client = client, baseUrl = server.url("/"))
        assertEquals(ArasaacFailureKind.TIMEOUT,
            expectFailure { slow.searchPictograms("comer") }.kind)
    }

    @Test fun refusedConnectionIsTyped() = runBlocking {
        val unavailable = ArasaacHttpRepository(baseUrl = "http://127.0.0.1:1/".toHttpUrl())
        assertEquals(ArasaacFailureKind.NO_CONNECTION,
            expectFailure { unavailable.searchPictograms("comer") }.kind)
    }

    private suspend fun expectFailure(block: suspend () -> Unit): ArasaacApiException = try {
        block()
        throw AssertionError("Expected a typed API failure")
    } catch (error: ArasaacApiException) {
        error
    }
}

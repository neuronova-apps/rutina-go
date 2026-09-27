package com.neuronovaapps.rutinago.ui.screens.arasaac

import com.neuronovaapps.rutinago.domain.arasaac.ArasaacApiException
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacFailureKind
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacPictogramCandidate
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface ArasaacSearchState {
    data object Idle : ArasaacSearchState
    data object Loading : ArasaacSearchState
    data class Success(val results: List<ArasaacPictogramCandidate>) : ArasaacSearchState
    data object Empty : ArasaacSearchState
    data class Error(val failure: ArasaacFailureKind) : ArasaacSearchState
}

class ArasaacSearchController(
    initialTerm: String,
    private val repository: ArasaacRepository,
    scope: CoroutineScope,
    private val debounceMillis: Long = 400,
) {
    private data class SearchInput(val query: String, val revision: Int = 0, val retryDelay: Long = 0)

    private val input = MutableStateFlow(SearchInput(initialTerm.trim()))
    private val _state = MutableStateFlow<ArasaacSearchState>(ArasaacSearchState.Idle)
    val state: StateFlow<ArasaacSearchState> = _state
    val query: String get() = input.value.query
    private var lastFailure: ArasaacApiException? = null
    private val job: Job = scope.launch {
        input.collectLatest { current ->
            if (current.query.isBlank()) {
                _state.value = ArasaacSearchState.Idle
                return@collectLatest
            }
            _state.value = ArasaacSearchState.Loading
            delay(if (current.revision == 0) debounceMillis else current.retryDelay)
            try {
                val results = repository.searchPictograms(current.query)
                lastFailure = null
                _state.value = if (results.isEmpty()) ArasaacSearchState.Empty
                    else ArasaacSearchState.Success(results)
            } catch (error: CancellationException) {
                throw error
            } catch (error: ArasaacApiException) {
                lastFailure = error
                _state.value = ArasaacSearchState.Error(error.kind)
            } catch (error: Exception) {
                lastFailure = null
                _state.value = ArasaacSearchState.Error(ArasaacFailureKind.TEMPORARY)
            }
        }
    }

    fun setQuery(value: String) {
        lastFailure = null
        val cleaned = value.trimStart()
        if (cleaned == input.value.query) return
        _state.value = if (cleaned.isBlank()) ArasaacSearchState.Idle else ArasaacSearchState.Loading
        input.value = SearchInput(cleaned)
    }

    fun retry() {
        if (query.isBlank()) return
        val delayMillis = when (lastFailure?.kind) {
            ArasaacFailureKind.RATE_LIMITED -> lastFailure?.retryAfterMillis ?: 3_000
            ArasaacFailureKind.TEMPORARY -> 1_500
            else -> 0
        }
        input.value = input.value.copy(revision = input.value.revision + 1, retryDelay = delayMillis)
    }

    fun close() = job.cancel()
}

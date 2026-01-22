package com.akvo.externalodk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akvo.externalodk.data.dao.FormMetadataDao
import com.akvo.externalodk.data.dao.SubmissionDao
import com.akvo.externalodk.data.entity.SubmissionEntity
import com.akvo.externalodk.data.network.AuthCredentials
import com.akvo.externalodk.ui.model.SubmissionUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val submissions: List<SubmissionUiModel> = emptyList(),
    val filteredSubmissions: List<SubmissionUiModel> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val submissionDao: SubmissionDao,
    private val formMetadataDao: FormMetadataDao,
    private val authCredentials: AuthCredentials
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadSubmissions()
    }

    private fun loadSubmissions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val assetUid = authCredentials.assetUid
            if (assetUid.isBlank()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            submissionDao.getSubmissions(assetUid).collect { entities ->
                val uiModels = entities.map { it.toUiModel() }
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        submissions = uiModels,
                        filteredSubmissions = if (state.searchQuery.isBlank()) {
                            uiModels
                        } else {
                            filterSubmissions(uiModels, state.searchQuery)
                        }
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredSubmissions = filterSubmissions(state.submissions, query)
            )
        }
    }

    fun onSearchActiveChange(active: Boolean) {
        _uiState.update { state ->
            if (!active) {
                state.copy(
                    isSearchActive = false,
                    searchQuery = "",
                    filteredSubmissions = state.submissions
                )
            } else {
                state.copy(isSearchActive = true)
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            submissionDao.deleteAll()
            formMetadataDao.deleteAll()
            authCredentials.clear()
            onComplete()
        }
    }

    private fun filterSubmissions(
        submissions: List<SubmissionUiModel>,
        query: String
    ): List<SubmissionUiModel> {
        if (query.isBlank()) {
            return submissions
        }
        val lowerQuery = query.lowercase()
        return submissions.filter { submission ->
            submission.submittedBy.lowercase().contains(lowerQuery) ||
                submission.uuid.lowercase().contains(lowerQuery) ||
                submission.submissionTime.lowercase().contains(lowerQuery)
        }
    }

    private fun SubmissionEntity.toUiModel(): SubmissionUiModel {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
        val formattedTime = formatter.format(Instant.ofEpochMilli(submissionTime))

        return SubmissionUiModel(
            uuid = _uuid,
            submittedBy = submittedBy ?: "Unknown",
            submissionTime = formattedTime,
            isSynced = true
        )
    }
}

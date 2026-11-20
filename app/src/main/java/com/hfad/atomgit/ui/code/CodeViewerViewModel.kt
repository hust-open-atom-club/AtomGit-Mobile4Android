package com.hfad.atomgit.ui.code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfad.atomgit.data.model.CodeViewerPreferences
import com.hfad.atomgit.data.remote.error.ApiError
import com.hfad.atomgit.data.remote.error.ApiResult
import com.hfad.atomgit.data.repository.CodeViewerPreferencesRepository
import com.hfad.atomgit.data.repository.RepoContentRepository
import com.hfad.atomgit.ui.components.CODE_VIEWER_SAMPLE_CONTENT
import com.hfad.atomgit.ui.components.CODE_VIEWER_SAMPLE_PATH
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CodeViewerUiState(
    val owner: String = "",
    val repo: String = "",
    val path: String = "",
    val ref: String = "",
    val sample: Boolean = false,
    val content: String = "",
    val isLoading: Boolean = false,
    val error: ApiError? = null,
) {
    val displayPath: String
        get() = when {
            sample -> CODE_VIEWER_SAMPLE_PATH
            owner.isNotBlank() && repo.isNotBlank() -> "$owner/$repo/$path"
            else -> path
        }
}

@HiltViewModel
class CodeViewerViewModel @Inject constructor(
    private val contentRepository: RepoContentRepository,
    preferencesRepository: CodeViewerPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CodeViewerUiState())
    val state: StateFlow<CodeViewerUiState> = _state.asStateFlow()

    val preferences: StateFlow<CodeViewerPreferences> = preferencesRepository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = preferencesRepository.preferences.value,
        )

    private var loadJob: Job? = null

    fun start(
        owner: String,
        repo: String,
        path: String,
        ref: String,
        sample: Boolean,
    ) {
        val current = _state.value
        if (
            current.owner == owner &&
            current.repo == repo &&
            current.path == path &&
            current.ref == ref &&
            current.sample == sample &&
            (current.content.isNotEmpty() || current.error != null || current.isLoading)
        ) {
            return
        }
        if (sample) {
            _state.value = CodeViewerUiState(
                owner = owner,
                repo = repo,
                path = path.ifBlank { CODE_VIEWER_SAMPLE_PATH },
                ref = ref,
                sample = true,
                content = CODE_VIEWER_SAMPLE_CONTENT,
            )
            return
        }
        _state.value = CodeViewerUiState(
            owner = owner,
            repo = repo,
            path = path,
            ref = ref,
            sample = false,
            isLoading = true,
        )
        load()
    }

    fun retry() {
        if (_state.value.sample) return
        load()
    }

    private fun load() {
        val snapshot = _state.value
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (
                val result = contentRepository.getFile(
                    owner = snapshot.owner,
                    repo = snapshot.repo,
                    path = snapshot.path,
                    ref = snapshot.ref.ifBlank { "HEAD" },
                )
            ) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            content = result.value.content,
                            path = result.value.path,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _state.update {
                        it.copy(isLoading = false, error = result.error)
                    }
                }
            }
        }
    }
}

package com.hfad.atomgit.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfad.atomgit.data.model.AuthSession
import com.hfad.atomgit.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MinRefreshMillis = 800L

/**
 * 我的页 ViewModel
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: SessionRepository,
) : ViewModel() {

    val session: StateFlow<AuthSession?> = repository.session

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            try {
                coroutineScope {
                    val refresh = async { repository.refreshProfile() }
                    delay(MinRefreshMillis)
                    refresh.await()
                }
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }
}

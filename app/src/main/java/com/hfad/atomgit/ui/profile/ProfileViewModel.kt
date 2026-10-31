package com.hfad.atomgit.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfad.atomgit.data.model.AuthSession
import com.hfad.atomgit.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 我的页 ViewModel
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: SessionRepository,
) : ViewModel() {

    val session: StateFlow<AuthSession?> = repository.session

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }
}

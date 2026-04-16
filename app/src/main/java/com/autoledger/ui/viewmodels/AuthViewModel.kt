package com.autoledger.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoledger.data.repository.UserPreferencesRepository
import com.autoledger.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading       : Boolean = true,
    val isAuthenticated : Boolean = false,
    val biometricError  : String? = null,
    val nameError       : String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userPrefsRepo: UserPreferencesRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> =
        userPrefsRepo.userPreferences
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = UserPreferences()
            )

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPrefsRepo.userPreferences.collect {
                _authUiState.update { state ->
                    state.copy(isLoading = false)
                }
            }
        }
    }

    fun saveNameAndSetup(name: String) {
        if (name.isBlank()) {
            _authUiState.update {
                it.copy(nameError = "Please enter your name")
            }
            return
        }
        if (name.trim().length < 2) {
            _authUiState.update {
                it.copy(nameError = "Name must be at least 2 characters")
            }
            return
        }
        viewModelScope.launch {
            userPrefsRepo.saveUserName(name.trim())
            _authUiState.update { it.copy(nameError = null) }
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            userPrefsRepo.setLoggedIn(true)
            _authUiState.update {
                it.copy(isAuthenticated = true, biometricError = null)
            }
        }
    }

    fun onBiometricError(message: String) {
        _authUiState.update { it.copy(biometricError = message) }
    }

    fun clearBiometricError() {
        _authUiState.update { it.copy(biometricError = null) }
    }

    fun savePhotoUri(uri: String) {
        viewModelScope.launch {
            userPrefsRepo.savePhotoUri(uri)
        }
    }

    // ← Saves theme name to DataStore so it persists across relaunches
    fun saveAppTheme(themeName: String) {
        viewModelScope.launch {
            userPrefsRepo.saveAppTheme(themeName)
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPrefsRepo.clearSession()
            _authUiState.update { it.copy(isAuthenticated = false) }
        }
    }
}
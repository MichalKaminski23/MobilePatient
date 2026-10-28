package pl.polsl.mobilepatient.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.repository.AuthRepository


class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value =
                LoginUiState.Error("Wprowadź adres e-mail i hasło.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            val result = authRepository.signIn(
                email = email.trim(),
                password = password
            )

            _uiState.value = result.fold(
                onSuccess = { profile ->
                    LoginUiState.Success(
                        role = profile.role,
                        userName = "${profile.name} ${profile.surname}"
                    )
                },
                onFailure = { error ->
                    LoginUiState.Error(
                        error.localizedMessage
                            ?: "Błąd logowania."
                    )
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
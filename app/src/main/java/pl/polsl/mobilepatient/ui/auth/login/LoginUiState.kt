package pl.polsl.mobilepatient.ui.auth.login

sealed interface LoginUiState {
    data object Idle : LoginUiState

    data object Loading : LoginUiState

    data class Success(
        val role: String,
        val userName: String
    ) : LoginUiState

    data class Error(
        val message: String
    ) : LoginUiState
}
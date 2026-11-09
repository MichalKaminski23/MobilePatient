package pl.polsl.mobilepatient.ui.admin.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.repository.AdminRepository

class AdminStaffViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminStaffUiState())
    val uiState: StateFlow<AdminStaffUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentUserId = repository.getCurrentUserId()
            repository.getUsers().fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            users = list,
                            currentUserId = currentUserId,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage
                                ?: "Nie udało się załadować listy personelu"
                        )
                    }
                }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectFilter(filter: StaffRoleFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun changeUserRole(
        userId: String,
        newRole: String,
        onSuccess: () -> Unit
    ) {
        if (userId == _uiState.value.currentUserId) {
            _uiState.update {
                it.copy(errorMessage = "Nie możesz zmienić uprawnień własnego konta administratora.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingRole = true, errorMessage = null) }
            repository.updateUserRole(userId, newRole).fold(
                onSuccess = {
                    val roleLabel = when (newRole) {
                        "doctor" -> "LEKARZ"
                        "patient" -> "PACJENT"
                        "admin" -> "ADMIN"
                        else -> newRole.uppercase()
                    }
                    _uiState.update {
                        it.copy(
                            isUpdatingRole = false,
                            successMessage = "Rola użytkownika została zaktualizowana na: $roleLabel"
                        )
                    }
                    loadUsers()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUpdatingRole = false,
                            errorMessage = error.localizedMessage
                                ?: "Nie udało się zmienić roli użytkownika"
                        )
                    }
                }
            )
        }
    }

    fun createDoctor(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        pesel: String?,
        phone: String?,
        address: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingDoctor = true, errorMessage = null) }
            repository.createDoctor(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                pesel = pesel,
                phone = phone,
                address = address
            ).fold(
                onSuccess = { doctor ->
                    _uiState.update {
                        it.copy(
                            isCreatingDoctor = false,
                            successMessage = "Pomyślnie utworzono konto lekarza: Dr ${doctor.first_name} ${doctor.last_name}"
                        )
                    }
                    loadUsers()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCreatingDoctor = false,
                            errorMessage = error.localizedMessage
                                ?: "Błąd podczas rejestracji lekarza"
                        )
                    }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

package pl.polsl.mobilepatient.ui.admin.clinics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.dto.ClinicInsertDto
import pl.polsl.mobilepatient.data.repository.AdminRepository

class AdminClinicsViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminClinicsUiState())
    val uiState: StateFlow<AdminClinicsUiState> = _uiState.asStateFlow()

    init {
        loadClinics()
    }

    fun loadClinics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getClinics().fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            clinics = list,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Błąd pobierania placówek"
                        )
                    }
                }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun addClinic(
        name: String,
        address: String,
        phone: String?,
        latitude: Double,
        longitude: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val insertDto = ClinicInsertDto(
                name = name.trim(),
                address = address.trim(),
                latitude = latitude,
                longitude = longitude,
                phone = phone?.trim()?.ifBlank { null }
            )
            repository.addClinic(insertDto).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "Placówka została pomyślnie dodana"
                        )
                    }
                    loadClinics()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.localizedMessage ?: "Nie udało się zapisać placówki"
                        )
                    }
                }
            )
        }
    }

    fun deleteClinic(clinicId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.deleteClinic(clinicId).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Placówka została usunięta"
                        )
                    }
                    loadClinics()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage
                                ?: "Nie udało się usunąć placówki (może mieć powiązane wizyty)"
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

package pl.polsl.mobilepatient.ui.patient.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.dto.ClinicDto
import pl.polsl.mobilepatient.data.repository.PatientRepository

class ClinicsMapViewModel(
    private val repository: PatientRepository = PatientRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClinicsMapUiState())
    val uiState: StateFlow<ClinicsMapUiState> = _uiState.asStateFlow()

    init {
        loadClinics()
    }

    fun loadClinics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getAvailableClinics().fold(
                onSuccess = { list ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            clinics = list,
                            selectedClinic = current.selectedClinic ?: list.firstOrNull()
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Nie udało się pobrać listy placówek"
                        )
                    }
                }
            )
        }
    }

    fun selectClinic(clinic: ClinicDto) {
        _uiState.update { it.copy(selectedClinic = clinic) }
    }

    fun clearSelectedClinic() {
        _uiState.update { it.copy(selectedClinic = null) }
    }

    fun setViewMode(mode: MapViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }
}

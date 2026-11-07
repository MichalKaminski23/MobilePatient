package pl.polsl.mobilepatient.ui.patient.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.repository.PatientRepository

class PatientPrescriptionsViewModel(
    private val repository: PatientRepository = PatientRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientPrescriptionsUiState(isLoading = true))
    val uiState: StateFlow<PatientPrescriptionsUiState> = _uiState.asStateFlow()

    init {
        loadPrescriptions()
    }

    fun loadPrescriptions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getMyPrescriptions()
            result.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            prescriptions = list,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage
                                ?: "Nie udało się pobrać listy e-recept."
                        )
                    }
                }
            )
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun fulfillPrescription(prescriptionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFulfilling = true) }
            val result = repository.fulfillPrescription(prescriptionId)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        val updated = state.prescriptions.map {
                            if (it.id == prescriptionId) it.copy(status = "fulfilled") else it
                        }
                        state.copy(isFulfilling = false, prescriptions = updated)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isFulfilling = false,
                            errorMessage = error.localizedMessage
                                ?: "Nie udało się zrealizować e-recepty."
                        )
                    }
                }
            )
        }
    }
}

package pl.polsl.mobilepatient.ui.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.repository.PatientRepository

class PatientRecordsViewModel(
    private val repository: PatientRepository = PatientRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientRecordsUiState(isLoading = true))
    val uiState: StateFlow<PatientRecordsUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getMyMedicalRecords()
            result.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            records = list,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Nie udało się pobrać dokumentacji medycznej."
                        )
                    }
                }
            )
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }
}

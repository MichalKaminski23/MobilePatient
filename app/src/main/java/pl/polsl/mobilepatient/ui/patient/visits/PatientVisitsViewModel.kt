package pl.polsl.mobilepatient.ui.patient.visits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.dto.AppointmentInsertDto
import pl.polsl.mobilepatient.data.repository.PatientRepository

class PatientVisitsViewModel(
    private val repository: PatientRepository = PatientRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientVisitsUiState(isLoading = true))
    val uiState: StateFlow<PatientVisitsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val clinicsResult = repository.getAvailableClinics()
            val doctorsResult = repository.getAvailableDoctors()
            val appointmentsResult = repository.getMyPatientAppointments()

            val clinics = clinicsResult.getOrDefault(emptyList())
            val doctors = doctorsResult.getOrDefault(emptyList())

            appointmentsResult.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            appointments = list,
                            clinics = clinics,
                            doctors = doctors,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            clinics = clinics,
                            doctors = doctors,
                            errorMessage = error.localizedMessage
                                ?: "Nie udało się pobrać harmonogramu wizyt."
                        )
                    }
                }
            )
        }
    }

    fun bookAppointment(
        doctorId: String,
        clinicId: String,
        scheduledAt: String,
        notes: String?
    ) {
        viewModelScope.launch {
            val userId = repository.getCurrentUserId() ?: return@launch
            _uiState.update {
                it.copy(
                    isBooking = true,
                    errorMessage = null,
                    successMessage = null
                )
            }
            val insertDto = AppointmentInsertDto(
                patient_id = userId,
                doctor_id = doctorId,
                clinic_id = clinicId,
                scheduled_at = scheduledAt,
                status = "scheduled",
                notes = notes?.trim()?.ifEmpty { null }
            )

            val result = repository.bookAppointment(insertDto)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isBooking = false,
                            successMessage = "Pomyślnie zarejestrowano wizytę w przychodni!"
                        )
                    }
                    loadData()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isBooking = false,
                            errorMessage = error.localizedMessage
                                ?: "Błąd podczas rejestracji wizyty."
                        )
                    }
                }
            )
        }
    }

    fun cancelAppointment(appointmentId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCancelling = true,
                    errorMessage = null,
                    successMessage = null
                )
            }
            val result = repository.cancelAppointment(appointmentId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            successMessage = "Wizyta została odwołana."
                        )
                    }
                    loadData()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            errorMessage = error.localizedMessage ?: "Nie udało się odwołać wizyty."
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
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

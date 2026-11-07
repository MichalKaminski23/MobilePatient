package pl.polsl.mobilepatient.ui.patient.visits

import pl.polsl.mobilepatient.data.dto.ClinicDto
import pl.polsl.mobilepatient.data.dto.PatientAppointmentDetailsDto
import pl.polsl.mobilepatient.data.dto.ProfileDto

data class PatientVisitsUiState(
    val isLoading: Boolean = false,
    val isBooking: Boolean = false,
    val isCancelling: Boolean = false,
    val appointments: List<PatientAppointmentDetailsDto> = emptyList(),
    val clinics: List<ClinicDto> = emptyList(),
    val doctors: List<ProfileDto> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedFilter: String = "UPCOMING"
)

package pl.polsl.mobilepatient.ui.doctor

import pl.polsl.mobilepatient.data.dto.AppointmentWithPatientDto
import pl.polsl.mobilepatient.data.dto.ClinicDto
import pl.polsl.mobilepatient.data.dto.MedicalRecordDto
import pl.polsl.mobilepatient.data.dto.PrescriptionDto
import pl.polsl.mobilepatient.data.dto.ProfileDto

sealed interface DoctorUiState {
    data object Loading : DoctorUiState

    data class Success(
        val doctorId: String,
        val patients: List<ProfileDto>,
        val appointments: List<AppointmentWithPatientDto>,
        val selectedPatient: ProfileDto? = null,
        val medicalRecords: List<MedicalRecordDto> = emptyList(),
        val prescriptions: List<PrescriptionDto> = emptyList(),
        val clinics: List<ClinicDto> = emptyList(),
        val searchQuery: String = "",
        val isAddingRecordLoading: Boolean = false,
        val isIssuingPrescriptionLoading: Boolean = false,
        val actionSuccessMessage: String? = null,
        val actionErrorMessage: String? = null,
        val availablePatients: List<ProfileDto> = emptyList(),
        val showAddPatientDialog: Boolean = false,
        val showAddAppointmentDialog: Boolean = false,
        val recordToEdit: MedicalRecordDto? = null
    ) : DoctorUiState

    data class Error(val message: String) : DoctorUiState
}

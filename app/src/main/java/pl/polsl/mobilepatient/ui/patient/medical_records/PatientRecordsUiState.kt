package pl.polsl.mobilepatient.ui.patient.medical_records

import pl.polsl.mobilepatient.data.dto.MedicalRecordDto

data class PatientRecordsUiState(
    val isLoading: Boolean = false,
    val records: List<MedicalRecordDto> = emptyList(),
    val errorMessage: String? = null,
    val selectedFilter: String = "ALL"
)

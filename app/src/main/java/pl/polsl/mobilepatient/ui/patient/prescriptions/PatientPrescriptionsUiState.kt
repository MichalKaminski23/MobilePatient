package pl.polsl.mobilepatient.ui.patient.prescriptions

import pl.polsl.mobilepatient.data.dto.PrescriptionDto

data class PatientPrescriptionsUiState(
    val isLoading: Boolean = false,
    val isFulfilling: Boolean = false,
    val prescriptions: List<PrescriptionDto> = emptyList(),
    val errorMessage: String? = null,
    val selectedFilter: String = "ALL"
)

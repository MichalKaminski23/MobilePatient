package pl.polsl.mobilepatient.ui.admin.clinics

import pl.polsl.mobilepatient.data.dto.ClinicDto

data class AdminClinicsUiState(
    val isLoading: Boolean = false,
    val clinics: List<ClinicDto> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSaving: Boolean = false
) {
    val filteredClinics: List<ClinicDto>
        get() = if (searchQuery.isBlank()) {
            clinics
        } else {
            val query = searchQuery.trim().lowercase()
            clinics.filter {
                it.name.lowercase().contains(query) ||
                    it.address.lowercase().contains(query) ||
                    (it.phone?.contains(query) == true)
            }
        }
}

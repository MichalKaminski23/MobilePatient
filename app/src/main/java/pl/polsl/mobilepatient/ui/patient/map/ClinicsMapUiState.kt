package pl.polsl.mobilepatient.ui.patient.map

import pl.polsl.mobilepatient.data.dto.ClinicDto

enum class MapViewMode {
    MAP,
    LIST
}

data class ClinicsMapUiState(
    val isLoading: Boolean = false,
    val clinics: List<ClinicDto> = emptyList(),
    val selectedClinic: ClinicDto? = null,
    val errorMessage: String? = null,
    val viewMode: MapViewMode = MapViewMode.MAP
)

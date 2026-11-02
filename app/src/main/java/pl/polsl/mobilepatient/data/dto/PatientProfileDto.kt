package pl.polsl.mobilepatient.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class PatientProfileDto(
    val id: String,
    val first_name: String,
    val last_name: String,
    val pesel: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val role: String = "patient"
)

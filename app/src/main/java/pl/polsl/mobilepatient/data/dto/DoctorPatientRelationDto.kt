package pl.polsl.mobilepatient.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class DoctorPatientRelationDto(
    val id: String,
    val doctor_id: String,
    val patient_id: String,
    val created_at: String? = null
)

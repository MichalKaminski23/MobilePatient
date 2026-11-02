package pl.polsl.mobilepatient.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MedicalRecordDto(
    val id: String,
    val patient_id: String,
    val doctor_id: String,
    val appointment_id: String? = null,
    val record_type: String,
    val description: String,
    val created_at: String? = null
)

@Serializable
data class MedicalRecordInsertDto(
    val patient_id: String,
    val doctor_id: String,
    val appointment_id: String? = null,
    val record_type: String,
    val description: String
)

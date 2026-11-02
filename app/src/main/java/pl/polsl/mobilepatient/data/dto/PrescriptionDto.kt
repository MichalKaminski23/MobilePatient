package pl.polsl.mobilepatient.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class PrescriptionDto(
    val id: String,
    val patient_id: String,
    val doctor_id: String,
    val medical_record_id: String? = null,
    val code: String,
    val medications: String,
    val issued_at: String? = null,
    val valid_until: String,
    val status: String = "active"
)

@Serializable
data class PrescriptionInsertDto(
    val patient_id: String,
    val doctor_id: String,
    val medical_record_id: String? = null,
    val code: String,
    val medications: String,
    val valid_until: String,
    val status: String = "active"
)

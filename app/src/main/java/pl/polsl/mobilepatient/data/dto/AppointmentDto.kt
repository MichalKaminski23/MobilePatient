package pl.polsl.mobilepatient.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AppointmentDto(
    val id: String,
    val patient_id: String,
    val doctor_id: String,
    val clinic_id: String,
    val scheduled_at: String,
    val status: String,
    val notes: String? = null,
    val created_at: String? = null
)

@Serializable
data class AppointmentWithPatientDto(
    val id: String,
    val patient_id: String,
    val doctor_id: String,
    val clinic_id: String,
    val scheduled_at: String,
    val status: String,
    val notes: String? = null,
    val created_at: String? = null,
    val patient_name: String? = null
)

@Serializable
data class AppointmentInsertDto(
    val patient_id: String,
    val doctor_id: String,
    val clinic_id: String,
    val scheduled_at: String,
    val status: String = "scheduled",
    val notes: String? = null
)

@Serializable
data class PatientAppointmentDetailsDto(
    val id: String,
    val patient_id: String,
    val doctor_id: String,
    val clinic_id: String,
    val scheduled_at: String,
    val status: String,
    val notes: String? = null,
    val created_at: String? = null,
    val doctor_name: String? = null,
    val clinic_name: String? = null,
    val clinic_address: String? = null
)

package pl.polsl.mobilepatient.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MedicalFileDto(
    val id: String,
    val patient_id: String,
    val uploaded_by: String,
    val medical_record_id: String? = null,
    val storage_path: String,
    val file_name: String,
    val mime_type: String,
    val created_at: String? = null,
    val signed_url: String? = null,
    val local_bytes: ByteArray? = null
)

@Serializable
data class MedicalFileInsertDto(
    val patient_id: String,
    val uploaded_by: String,
    val medical_record_id: String? = null,
    val storage_path: String,
    val file_name: String,
    val mime_type: String
)

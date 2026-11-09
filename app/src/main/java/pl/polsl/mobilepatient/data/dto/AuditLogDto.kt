package pl.polsl.mobilepatient.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuditLogDto(
    val id: String,
    val actor_id: String? = null,
    val action: String,
    val entity_type: String,
    val entity_id: String? = null,
    val created_at: String
)

@Serializable
data class AuditLogInsertDto(
    val actor_id: String? = null,
    val action: String,
    val entity_type: String,
    val entity_id: String? = null
)

data class AuditLogWithActor(
    val id: String,
    val actor_id: String?,
    val actorName: String,
    val actorEmail: String?,
    val actorRole: String?,
    val action: String,
    val entity_type: String,
    val entity_id: String?,
    val created_at: String
)

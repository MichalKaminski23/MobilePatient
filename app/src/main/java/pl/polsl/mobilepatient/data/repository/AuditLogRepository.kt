package pl.polsl.mobilepatient.data.repository

import io.github.jan.supabase.postgrest.from
import pl.polsl.mobilepatient.data.dto.AuditLogInsertDto
import pl.polsl.mobilepatient.data.network.SupabaseProvider
import java.util.UUID

class AuditLogRepository(
    private val userRepository: UserRepository = UserRepository(),
) {
    private val client = SupabaseProvider.client

    suspend fun logEvent(
        action: String,
        entityType: String,
        entityId: String? = null,
        actorId: String? = null
    ): Result<Unit> {
        return try {
            val resolvedActorId = actorId ?: userRepository.getCurrentUserId()
            val validEntityId = if (isValidUuid(entityId)) entityId else null
            val insert = AuditLogInsertDto(
                actor_id = resolvedActorId,
                action = action,
                entity_type = entityType,
                entity_id = validEntityId
            )
            client.from("audit_logs").insert(insert)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isValidUuid(str: String?): Boolean {
        return !str.isNullOrBlank() && try {
            UUID.fromString(str)
            true
        } catch (_: Exception) {
            false
        }
    }
}

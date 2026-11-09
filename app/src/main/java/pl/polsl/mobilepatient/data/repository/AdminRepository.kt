package pl.polsl.mobilepatient.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import pl.polsl.mobilepatient.BuildConfig
import pl.polsl.mobilepatient.data.dto.AuditLogDto
import pl.polsl.mobilepatient.data.dto.AuditLogWithActor
import pl.polsl.mobilepatient.data.dto.ClinicDto
import pl.polsl.mobilepatient.data.dto.ClinicInsertDto
import pl.polsl.mobilepatient.data.dto.ProfileDto
import pl.polsl.mobilepatient.data.network.SupabaseProvider

class AdminRepository(
    private val client: SupabaseClient = SupabaseProvider.client
) {

    private val userRepository = UserRepository()
    suspend fun getCurrentUserId(): String? {
        return userRepository.getCurrentUserId()
    }

    private val auditLogRepository = AuditLogRepository()

    suspend fun getClinics(): Result<List<ClinicDto>> {
        return try {
            val clinics = client.from("clinics")
                .select()
                .decodeList<ClinicDto>()
                .sortedBy { it.name }
            Result.success(clinics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addClinic(clinic: ClinicInsertDto): Result<ClinicDto> {
        return try {
            val inserted = client.from("clinics")
                .insert(clinic) {
                    select()
                }
                .decodeSingle<ClinicDto>()

            try {
                auditLogRepository.logEvent(
                    action = "ADD_CLINIC",
                    entityType = "clinics",
                    entityId = inserted.id
                )
            } catch (_: Exception) {
            }

            Result.success(inserted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteClinic(clinicId: String): Result<Unit> {
        return try {
            client.from("clinics")
                .delete {
                    filter {
                        eq("id", clinicId)
                    }
                }

            try {
                auditLogRepository.logEvent(
                    action = "DELETE_CLINIC",
                    entityType = "clinics",
                    entityId = clinicId
                )
            } catch (_: Exception) {
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAuditLogs(): Result<List<AuditLogWithActor>> {
        return try {
            val rawLogs = client.from("audit_logs")
                .select {
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<AuditLogDto>()

            val profiles = try {
                client.from("profiles")
                    .select()
                    .decodeList<ProfileDto>()
                    .associateBy { it.id }
            } catch (_: Exception) {
                emptyMap()
            }

            val enrichedLogs = rawLogs.map { log ->
                val actor = log.actor_id?.let { profiles[it] }
                val actorName = when {
                    actor != null -> "${actor.first_name} ${actor.last_name}"
                    log.actor_id != null -> "Użytkownik (${log.actor_id.take(8)}...)"
                    else -> "System / Automat"
                }

                AuditLogWithActor(
                    id = log.id,
                    actor_id = log.actor_id,
                    actorName = actorName,
                    actorEmail = actor?.email,
                    actorRole = actor?.role,
                    action = log.action,
                    entity_type = log.entity_type,
                    entity_id = log.entity_id,
                    created_at = log.created_at
                )
            }

            Result.success(enrichedLogs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getUsers(): Result<List<ProfileDto>> {
        return try {
            val profiles = client.from("profiles")
                .select()
                .decodeList<ProfileDto>()
                .sortedWith(compareBy({ it.last_name }, { it.first_name }))
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(userId: String, newRole: String): Result<Unit> {
        return try {
            val currentAdminId = getCurrentUserId()
            if (userId == currentAdminId) {
                return Result.failure(IllegalStateException("Administrator nie może zmienić własnej roli."))
            }

            if (newRole !in listOf("patient", "doctor", "admin")) {
                return Result.failure(IllegalArgumentException("Nieprawidłowa rola: $newRole"))
            }

            client.from("profiles")
                .update({
                    set("role", newRole)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }

            try {
                auditLogRepository.logEvent(
                    action = "UPDATE_ROLE_${newRole.uppercase()}",
                    entityType = "profiles",
                    entityId = userId
                )
            } catch (_: Exception) {
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDoctor(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        pesel: String? = null,
        phone: String? = null,
        address: String? = null
    ): Result<ProfileDto> {
        val tempClient = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
        }

        return try {
            val userInfo = tempClient.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                this.data = buildJsonObject {
                    put("first_name", firstName.trim())
                    put("last_name", lastName.trim())
                    put("role", "doctor")
                }
            }

            val newUserId = userInfo?.id ?: tempClient.auth.currentUserOrNull()?.id
            ?: return Result.failure(IllegalStateException("Nie udało się zarejestrować konta lekarza."))

            try {
                client.from("profiles")
                    .update({
                        set("role", "doctor")
                        if (!pesel.isNullOrBlank()) set("pesel", pesel.trim())
                        if (!phone.isNullOrBlank()) set("phone", phone.trim())
                        if (!address.isNullOrBlank()) set("address", address.trim())
                    }) {
                        filter {
                            eq("id", newUserId)
                        }
                    }
            } catch (_: Exception) {
            }

            try {
                auditLogRepository.logEvent(
                    action = "CREATE_DOCTOR",
                    entityType = "profiles",
                    entityId = newUserId
                )
            } catch (_: Exception) {
            }

            val createdProfile = client.from("profiles")
                .select {
                    filter { eq("id", newUserId) }
                }
                .decodeSingleOrNull<ProfileDto>()
                ?: ProfileDto(
                    id = newUserId,
                    first_name = firstName.trim(),
                    last_name = lastName.trim(),
                    pesel = pesel?.trim(),
                    phone = phone?.trim(),
                    address = address?.trim(),
                    role = "doctor",
                    email = email.trim()
                )

            Result.success(createdProfile)
        } catch (e: Exception) {
            android.util.Log.e("AdminRepo", "createDoctor failed", e)
            val msg = when {
                e.message?.contains("User already registered", ignoreCase = true) == true ->
                    "Użytkownik o podanym adresie e-mail już istnieje w systemie."

                e.message?.contains("Password should be at least", ignoreCase = true) == true ->
                    "Hasło musi mieć co najmniej 6 znaków."

                else -> e.message ?: "Nie udało się utworzyć konta lekarza."
            }
            Result.failure(Exception(msg, e))
        } finally {
            try {
                tempClient.close()
            } catch (_: Exception) {
            }
        }
    }
}

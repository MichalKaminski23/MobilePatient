package pl.polsl.mobilepatient.data.repository

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.from
import pl.polsl.mobilepatient.BuildConfig
import pl.polsl.mobilepatient.data.dto.ProfileDto
import pl.polsl.mobilepatient.data.network.SupabaseProvider

class UserRepository(
    private val auditLogRepository: AuditLogRepository = AuditLogRepository(),
) {

    private val client = SupabaseProvider.client
    suspend fun getCurrentUserId(): String? {
        val direct = client.auth.currentUserOrNull()?.id
            ?: client.auth.currentSessionOrNull()?.user?.id
        if (!direct.isNullOrBlank()) {
            SupabaseProvider.cachedUserId = direct
            return direct
        }

        try {
            client.auth.awaitInitialization()
        } catch (_: Exception) {
        }

        val afterInit = client.auth.currentUserOrNull()?.id
            ?: client.auth.currentSessionOrNull()?.user?.id
        if (!afterInit.isNullOrBlank()) {
            SupabaseProvider.cachedUserId = afterInit
            return afterInit
        }

        return SupabaseProvider.cachedUserId
    }

    suspend fun getMyProfile(): Result<ProfileDto> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(IllegalStateException("Brak aktywnej sesji użytkownika."))
            val email = client.auth.currentUserOrNull()?.email
                ?: client.auth.currentSessionOrNull()?.user?.email

            val profile = client.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()
                ?: return Result.failure(IllegalStateException("Nie znaleziono profilu pacjenta."))

            Result.success(profile.copy(email = email))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(
        newPassword: String,
        currentPassword: String? = null
    ): Result<Unit> {
        return try {
            if (newPassword.length < 6) {
                return Result.failure(IllegalArgumentException("Nowe hasło musi mieć co najmniej 6 znaków."))
            }

            val userId = getCurrentUserId()
                ?: return Result.failure(IllegalStateException("Brak aktywnej sesji użytkownika."))

            if (!currentPassword.isNullOrBlank()) {
                val email = client.auth.currentUserOrNull()?.email
                    ?: client.auth.currentSessionOrNull()?.user?.email
                if (!email.isNullOrBlank()) {
                    val verifyClient = createSupabaseClient(
                        supabaseUrl = BuildConfig.SUPABASE_URL,
                        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
                    ) {
                        install(Auth)
                    }
                    try {
                        verifyClient.auth.signInWith(Email) {
                            this.email = email.trim()
                            this.password = currentPassword
                        }
                    } catch (e: Exception) {
                        return Result.failure(IllegalArgumentException("Aktualne hasło jest nieprawidłowe."))
                    } finally {
                        try {
                            verifyClient.close()
                        } catch (_: Exception) {
                        }
                    }
                }
            }

            client.auth.updateUser {
                this.password = newPassword
            }

            try {
                client.from("profiles")
                    .update({
                        set("must_change_password", false)
                    }) {
                        filter {
                            eq("id", userId)
                        }
                    }
            } catch (_: Exception) {
            }

            try {
                auditLogRepository.logEvent(
                    action = "CHANGE_PASSWORD",
                    entityType = "profiles",
                    entityId = userId,
                    actorId = userId
                )
            } catch (_: Exception) {
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
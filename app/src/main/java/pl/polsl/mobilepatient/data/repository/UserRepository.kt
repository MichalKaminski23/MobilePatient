package pl.polsl.mobilepatient.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import pl.polsl.mobilepatient.data.dto.ProfileDto
import pl.polsl.mobilepatient.data.network.SupabaseProvider

class UserRepository {

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
}
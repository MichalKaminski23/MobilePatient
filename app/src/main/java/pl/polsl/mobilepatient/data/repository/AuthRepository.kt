package pl.polsl.mobilepatient.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import pl.polsl.mobilepatient.data.dto.ShortProfileDto
import pl.polsl.mobilepatient.data.network.SupabaseProvider

class AuthRepository {

    private val client = SupabaseProvider.client

    suspend fun signIn(
        email: String,
        password: String
    ): Result<ShortProfileDto> {
        return try {
            client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }

            val userId = client.auth.currentUserOrNull()?.id
                ?: client.auth.currentSessionOrNull()?.user?.id
                ?: return Result.failure(
                    IllegalStateException(
                        "Nie udało się pobrać identyfikatora użytkownika."
                    )
                )

            SupabaseProvider.cachedUserId = userId

            val profile = client
                .from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<ShortProfileDto>()
                ?: return Result.failure(
                    IllegalStateException(
                        "Nie znaleziono profilu użytkownika."
                    )
                )

            Result.success(profile)
        } catch (exception: Exception) {
            val message = exception.message.orEmpty()
            val userMessage = if (
                message.contains("invalid login credentials", ignoreCase = true)
            ) {
                "Nieprawidłowy adres e-mail lub hasło."
            } else {
                "Nie udało się zalogować. Spróbuj ponownie."
            }

            Result.failure(IllegalStateException(userMessage, exception))
        }
    }

    suspend fun signOut() {
        SupabaseProvider.cachedUserId = null
        client.auth.signOut()
    }
}
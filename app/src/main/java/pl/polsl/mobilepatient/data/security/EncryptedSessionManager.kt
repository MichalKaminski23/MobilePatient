package pl.polsl.mobilepatient.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

@Suppress("DEPRECATION")
class EncryptedSessionManager(
    context: Context,
    private val sessionKey: String = "session",
) : SessionManager {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private val encryptedPrefs: SharedPreferences = try {
        createPrefs(context)
    } catch (_: Exception) {
        context.getSharedPreferences("encrypted_supabase_session", Context.MODE_PRIVATE).edit { clear() }
        createPrefs(context)
    }

    private fun createPrefs(ctx: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            ctx,
            "encrypted_supabase_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasSession(): Boolean {
        return try {
            encryptedPrefs.getString(sessionKey, null) != null
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun saveSession(session: UserSession) {
        val serialized = json.encodeToString(UserSession.serializer(), session)
        encryptedPrefs.edit { putString(sessionKey, serialized) }
    }

    override suspend fun loadSession(): UserSession {
        val raw = encryptedPrefs.getString(sessionKey, null) ?: throw NoSessionFoundException()
        return try {
            json.decodeFromString(UserSession.serializer(), raw)
        } catch (_: Exception) {
            deleteSession()
            throw NoSessionFoundException()
        }
    }

    override suspend fun deleteSession() {
        encryptedPrefs.edit { remove(sessionKey) }
    }
}

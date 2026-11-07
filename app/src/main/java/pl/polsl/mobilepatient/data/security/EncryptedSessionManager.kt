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

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "encrypted_supabase_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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

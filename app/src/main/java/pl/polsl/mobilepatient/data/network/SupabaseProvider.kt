package pl.polsl.mobilepatient.data.network

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import pl.polsl.mobilepatient.BuildConfig
import pl.polsl.mobilepatient.data.security.EncryptedSessionManager

object SupabaseProvider {
    @Volatile
    var cachedUserId: String? = null

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val client: SupabaseClient by lazy {
        val context = appContext
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                if (context != null) {
                    sessionManager = EncryptedSessionManager(context)
                }
            }
            install(Postgrest)
            install(Storage)
        }
    }
}

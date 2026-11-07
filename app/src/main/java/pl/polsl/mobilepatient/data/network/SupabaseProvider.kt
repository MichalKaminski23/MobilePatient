package pl.polsl.mobilepatient.data.network

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import pl.polsl.mobilepatient.BuildConfig

object SupabaseProvider {
    @Volatile
    var cachedUserId: String? = null

    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {

        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}
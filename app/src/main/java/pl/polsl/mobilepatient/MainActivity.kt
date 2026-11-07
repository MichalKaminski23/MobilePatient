package pl.polsl.mobilepatient

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import pl.polsl.mobilepatient.data.network.SupabaseProvider
import pl.polsl.mobilepatient.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseProvider.init(applicationContext)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        ThemeManager.init(applicationContext)
        val deepLinkPatientId = extractPatientIdFromIntent(intent)
        setContent {
            MobilePatientApp(deepLinkPatientId = deepLinkPatientId)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractPatientIdFromIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme == "medapp" && data.host == "patient") {
            val path = data.path?.trim('/')
            return if (!path.isNull_or_empty()) {
                path
            } else {
                data.pathSegments.lastOrNull()
            }
        }
        return null
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.isBlank()

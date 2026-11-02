package pl.polsl.mobilepatient

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

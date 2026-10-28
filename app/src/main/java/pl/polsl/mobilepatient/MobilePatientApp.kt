package pl.polsl.mobilepatient

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.repository.AuthRepository
import pl.polsl.mobilepatient.ui.auth.LoginScreen
import pl.polsl.mobilepatient.ui.patient.PatientDashboardScreen

@Composable
fun MobilePatientApp() {
    MaterialTheme {
        val authRepository = remember { AuthRepository() }
        var loggedInRole by remember { mutableStateOf<String?>(null) }
        var loggedInName by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        if (loggedInRole == null) {
            LoginScreen(
                onLoginSuccess = { role, name ->
                    loggedInRole = role
                    loggedInName = name
                }
            )
        } else {
            PatientDashboardScreen(
                role = loggedInRole!!,
                userName = loggedInName,
                onLogout = {
                    scope.launch {
                        authRepository.signOut()
                        loggedInRole = null
                        loggedInName = ""
                    }
                }
            )
        }
    }
}
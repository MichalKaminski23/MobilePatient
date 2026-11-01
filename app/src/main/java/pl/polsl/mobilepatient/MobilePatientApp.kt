package pl.polsl.mobilepatient

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.repository.AuthRepository
import pl.polsl.mobilepatient.ui.auth.LoginScreen
import pl.polsl.mobilepatient.ui.common.MainDashboardScreen
import pl.polsl.mobilepatient.ui.theme.MobilePatientTheme

@Composable
fun MobilePatientApp() {
    MobilePatientTheme {
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
            MainDashboardScreen(
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
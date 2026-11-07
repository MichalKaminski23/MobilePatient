package pl.polsl.mobilepatient

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.repository.AuthRepository
import pl.polsl.mobilepatient.ui.auth.login.LoginScreen
import pl.polsl.mobilepatient.ui.doctor.DoctorDashboardScreen
import pl.polsl.mobilepatient.ui.patient.PatientDashboardScreen
import pl.polsl.mobilepatient.ui.theme.MobilePatientTheme
import pl.polsl.mobilepatient.ui.theme.ThemeManager
import pl.polsl.mobilepatient.ui.theme.ThemeMode

@Composable
fun MobilePatientApp(
    deepLinkPatientId: String? = null
) {
    val themeMode by ThemeManager.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MobilePatientTheme(darkTheme = isDark) {
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
            val logoutAction = {
                scope.launch {
                    authRepository.signOut()
                    loggedInRole = null
                    loggedInName = ""
                }
                Unit
            }

            when (loggedInRole) {
                "doctor" -> {
                    DoctorDashboardScreen(
                        userName = loggedInName,
                        onLogout = logoutAction,
                        initialTargetPatientId = deepLinkPatientId
                    )
                }

                "patient" -> {
                    PatientDashboardScreen(
                        role = loggedInRole!!,
                        userName = loggedInName,
                        onLogout = logoutAction
                    )
                }

                else -> {
                    // admin
                }
            }
        }
    }
}

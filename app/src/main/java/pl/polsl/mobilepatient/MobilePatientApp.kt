package pl.polsl.mobilepatient

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.repository.AuthRepository
import pl.polsl.mobilepatient.data.security.SessionTimeoutManager
import pl.polsl.mobilepatient.ui.admin.AdminDashboardScreen
import pl.polsl.mobilepatient.ui.auth.login.LoginScreen
import pl.polsl.mobilepatient.ui.doctor.DoctorDashboardScreen
import pl.polsl.mobilepatient.ui.patient.PatientDashboardScreen
import pl.polsl.mobilepatient.ui.theme.MobilePatientTheme
import pl.polsl.mobilepatient.ui.theme.ThemeManager
import pl.polsl.mobilepatient.ui.theme.ThemeMode
import kotlin.time.Duration.Companion.milliseconds

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

    val lifecycleOwner = LocalLifecycleOwner.current

    MobilePatientTheme(darkTheme = isDark) {
        val authRepository = remember { AuthRepository() }
        var loggedInRole by remember { mutableStateOf<String?>(null) }
        var loggedInName by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (loggedInRole != null && SessionTimeoutManager.checkTimeout()) {
                        loggedInRole = null
                        loggedInName = ""
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        LaunchedEffect(loggedInRole) {
            if (loggedInRole != null) {
                SessionTimeoutManager.resetTimeout()
                while (true) {
                    delay(30_000L.milliseconds)
                    if (SessionTimeoutManager.checkTimeout()) {
                        loggedInRole = null
                        loggedInName = ""
                        break
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            SessionTimeoutManager.onUserInteraction()
                        }
                    }
                },
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = loggedInRole,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(
                        animationSpec = tween(
                            220
                        )
                    )
                },
                label = "RoleTransition"
            ) { role ->
                if (role == null) {
                    LoginScreen(
                        onLoginSuccess = { userRole, name ->
                            loggedInRole = userRole
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

                    when (role) {
                        "doctor" -> {
                            DoctorDashboardScreen(
                                userName = loggedInName,
                                onLogout = logoutAction,
                                initialTargetPatientId = deepLinkPatientId
                            )
                        }

                        "patient" -> {
                            PatientDashboardScreen(
                                role = role,
                                userName = loggedInName,
                                onLogout = logoutAction
                            )
                        }

                        "admin" -> {
                            AdminDashboardScreen(
                                userName = loggedInName,
                                onLogout = logoutAction
                            )
                        }
                    }
                }
            }
        }
    }
}

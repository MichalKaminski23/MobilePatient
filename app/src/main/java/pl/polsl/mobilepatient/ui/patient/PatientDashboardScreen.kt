package pl.polsl.mobilepatient.ui.patient

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.animation.core.tween
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.polsl.mobilepatient.ui.components.DashboardModuleCard
import pl.polsl.mobilepatient.ui.components.DashboardModuleItem
import pl.polsl.mobilepatient.ui.components.IconBadge
import pl.polsl.mobilepatient.ui.legal.TermsAndPrivacyScreen
import pl.polsl.mobilepatient.ui.map.ClinicsMapScreen
import pl.polsl.mobilepatient.ui.patient.components.PatientProfileDialog
import pl.polsl.mobilepatient.ui.patient.medical_records.PatientRecordsScreen
import pl.polsl.mobilepatient.ui.patient.prescriptions.PatientPrescriptionsScreen
import pl.polsl.mobilepatient.ui.patient.scans.PatientScansScreen
import pl.polsl.mobilepatient.ui.patient.visits.PatientVisitsScreen
import pl.polsl.mobilepatient.ui.settings.SettingsScreen

enum class PatientDestination {
    OVERVIEW,
    DOCUMENTS,
    PRESCRIPTIONS,
    SCANS,
    VISITS,
    MAP,
    TERMS,
    SETTINGS
}

@Composable
fun PatientDashboardScreen(
    role: String,
    userName: String,
    onLogout: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(PatientDestination.OVERVIEW) }

    BackHandler(enabled = true) {
        if (currentDestination != PatientDestination.OVERVIEW) {
            currentDestination = PatientDestination.OVERVIEW
        } else {
            onLogout()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                if (targetState != PatientDestination.OVERVIEW) {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut(animationSpec = tween(220)))
                } else {
                    (slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(220)))
                }
            },
            label = "DashboardTransition"
        ) { destination ->
            when (destination) {
                PatientDestination.OVERVIEW -> {
                DashboardOverview(
                    role = role,
                    userName = userName,
                    onOpenDocuments = { currentDestination = PatientDestination.DOCUMENTS },
                    onOpenPrescriptions = { currentDestination = PatientDestination.PRESCRIPTIONS },
                    onOpenScans = { currentDestination = PatientDestination.SCANS },
                    onOpenVisits = { currentDestination = PatientDestination.VISITS },
                    onOpenMap = { currentDestination = PatientDestination.MAP },
                    onOpenTerms = { currentDestination = PatientDestination.TERMS },
                    onOpenSettings = { currentDestination = PatientDestination.SETTINGS },
                    onLogout = onLogout
                )
            }

            PatientDestination.DOCUMENTS -> {
                PatientRecordsScreen(onBack = { currentDestination = PatientDestination.OVERVIEW })
            }

            PatientDestination.PRESCRIPTIONS -> {
                PatientPrescriptionsScreen(onBack = {
                    currentDestination = PatientDestination.OVERVIEW
                })
            }

            PatientDestination.SCANS -> {
                PatientScansScreen(onBack = { currentDestination = PatientDestination.OVERVIEW })
            }

            PatientDestination.VISITS -> {
                PatientVisitsScreen(onBack = { currentDestination = PatientDestination.OVERVIEW })
            }

            PatientDestination.MAP -> {
                ClinicsMapScreen(onBack = { currentDestination = PatientDestination.OVERVIEW })
            }

            PatientDestination.TERMS -> {
                TermsAndPrivacyScreen(onBack = { currentDestination = PatientDestination.OVERVIEW })
            }

            PatientDestination.SETTINGS -> {
                SettingsScreen(onBack = { currentDestination = PatientDestination.OVERVIEW })
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardOverview(
    role: String,
    userName: String,
    onOpenDocuments: () -> Unit,
    onOpenPrescriptions: () -> Unit,
    onOpenScans: () -> Unit,
    onOpenVisits: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val modules = listOf(
        DashboardModuleItem(
            id = "documents",
            title = "Dokumentacja",
            subtitle = "Historia leczenia, diagnozy i skierowania",
            icon = Icons.Default.Description,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenDocuments
        ),
        DashboardModuleItem(
            id = "prescriptions",
            title = "E-recepty",
            subtitle = "Kody odbioru, wykaz leków i dawkowanie",
            icon = Icons.Default.Medication,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenPrescriptions
        ),
        DashboardModuleItem(
            id = "scans",
            title = "Skany i badania",
            subtitle = "Wgrywanie oraz podgląd skanów i badań",
            icon = Icons.Default.CloudUpload,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenScans
        ),
        DashboardModuleItem(
            id = "visits",
            title = "Wizyty i konsultacje",
            subtitle = "Harmonogram wizyt",
            icon = Icons.Default.Event,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenVisits
        ),
        DashboardModuleItem(
            id = "map",
            title = "Placówki i mapa",
            subtitle = "Lokalizacje przychodni, kontakt i nawigacja",
            icon = Icons.Default.Map,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenMap
        ),
        DashboardModuleItem(
            id = "terms",
            title = "Regulamin i RODO",
            subtitle = "Polityka prywatności, prawa pacjenta i zasady przetwarzania",
            icon = Icons.Default.Policy,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenTerms
        ),
        DashboardModuleItem(
            id = "settings",
            title = "Ustawienia",
            subtitle = "Ustawienia aplikacji",
            icon = Icons.Default.Settings,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenSettings
        )
    )

    var showProfileDialog by remember { mutableStateOf(false) }

    if (showProfileDialog) {
        PatientProfileDialog(
            initialUserName = userName,
            initialRole = role,
            onDismiss = { showProfileDialog = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = Icons.Default.HealthAndSafety,
                            tint = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            badgeSize = 36.dp,
                            iconSize = 20.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Mobilny Pacjent",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProfileDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase().ifEmpty { "U" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Witaj, $userName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Pokaż dane",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Dostępne moduły",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            modules.forEach { moduleItem ->
                DashboardModuleCard(item = moduleItem)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Wyloguj się",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

package pl.polsl.mobilepatient.ui.admin

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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import pl.polsl.mobilepatient.ui.admin.audit.AdminAuditLogsScreen
import pl.polsl.mobilepatient.ui.admin.clinics.AdminClinicsScreen
import pl.polsl.mobilepatient.ui.admin.components.AdminProfileDialog
import pl.polsl.mobilepatient.ui.admin.staff.AdminStaffScreen
import pl.polsl.mobilepatient.ui.components.DashboardModuleCard
import pl.polsl.mobilepatient.ui.components.DashboardModuleItem
import pl.polsl.mobilepatient.ui.components.IconBadge
import pl.polsl.mobilepatient.ui.legal.TermsAndPrivacyScreen
import pl.polsl.mobilepatient.ui.settings.SettingsScreen

enum class AdminDestination {
    OVERVIEW,
    CLINICS,
    AUDIT_LOGS,
    STAFF,
    TERMS,
    SETTINGS
}

@Composable
fun AdminDashboardScreen(
    userName: String,
    onLogout: () -> Unit,
) {
    var currentDestination by remember { mutableStateOf(AdminDestination.OVERVIEW) }

    BackHandler(enabled = currentDestination != AdminDestination.OVERVIEW) {
        currentDestination = AdminDestination.OVERVIEW
    }

    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            if (targetState != AdminDestination.OVERVIEW) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "AdminNavigationAnimation"
    ) { destination ->
        when (destination) {
            AdminDestination.OVERVIEW -> {
                AdminOverviewView(
                    userName = userName,
                    onOpenClinics = { currentDestination = AdminDestination.CLINICS },
                    onOpenAuditLogs = { currentDestination = AdminDestination.AUDIT_LOGS },
                    onOpenStaff = { currentDestination = AdminDestination.STAFF },
                    onOpenTerms = { currentDestination = AdminDestination.TERMS },
                    onOpenSettings = { currentDestination = AdminDestination.SETTINGS },
                    onLogout = onLogout
                )
            }

            AdminDestination.CLINICS -> {
                AdminClinicsScreen(
                    onBack = { currentDestination = AdminDestination.OVERVIEW }
                )
            }

            AdminDestination.AUDIT_LOGS -> {
                AdminAuditLogsScreen(
                    onBack = { currentDestination = AdminDestination.OVERVIEW }
                )
            }

            AdminDestination.STAFF -> {
                AdminStaffScreen(
                    onBack = { currentDestination = AdminDestination.OVERVIEW }
                )
            }

            AdminDestination.TERMS -> {
                TermsAndPrivacyScreen(onBack = { currentDestination = AdminDestination.OVERVIEW })
            }

            AdminDestination.SETTINGS -> {
                SettingsScreen(onBack = { currentDestination = AdminDestination.OVERVIEW })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminOverviewView(
    userName: String,
    onOpenClinics: () -> Unit,
    onOpenAuditLogs: () -> Unit,
    onOpenStaff: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    var showProfileDialog by remember { mutableStateOf(false) }

    if (showProfileDialog) {
        AdminProfileDialog(
            initialUserName = userName,
            initialRole = "admin",
            onDismiss = { showProfileDialog = false }
        )
    }

    val modules = listOf(
        DashboardModuleItem(
            id = "clinics",
            title = "Placówki medyczne",
            subtitle = "Zarządzanie siecią przychodni, adresami i współrzędnymi",
            icon = Icons.Default.Business,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenClinics
        ),
        DashboardModuleItem(
            id = "audit_logs",
            title = "Dziennik audytu",
            subtitle = "Rejestr operacji w systemie (kto, jaka akcja, jaki zasób, data)",
            icon = Icons.Default.Security,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenAuditLogs
        ),
        DashboardModuleItem(
            id = "staff",
            title = "Zarządzanie personelem",
            subtitle = "Weryfikacja użytkowników i przypisywanie ról lekarzy",
            icon = Icons.Default.ManageAccounts,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenStaff
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
                            text = userName.take(1).uppercase().ifEmpty { "A" },
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



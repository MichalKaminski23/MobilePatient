package pl.polsl.mobilepatient.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import pl.polsl.mobilepatient.ui.settings.SettingsScreen

@Composable
fun MainDashboardScreen(
    role: String,
    userName: String,
    onLogout: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    AnimatedContent(
        targetState = showSettings,
        transitionSpec = {
            if (targetState) {
                slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                        slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
            } else {
                slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() togetherWith
                        slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            }
        },
        label = "DashboardTransition"
    ) { isSettings ->
        if (isSettings) {
            SettingsScreen(onBack = { showSettings = false })
        } else {
            DashboardOverview(
                role = role,
                userName = userName,
                onOpenSettings = { showSettings = true },
                onLogout = onLogout
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardOverview(
    role: String,
    userName: String,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val modules = listOf(
        DashboardModuleItem(
            id = "documents",
            title = "Moja dokumentacja",
            subtitle = "Wyniki badań, e-recepty i skany dokumentacji",
            icon = Icons.Default.Description,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        DashboardModuleItem(
            id = "visits",
            title = "Wizyty i konsultacje",
            subtitle = "Harmonogram wizyt i lista przychodni",
            icon = Icons.Default.Event,
            iconTint = MaterialTheme.colorScheme.secondary,
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        DashboardModuleItem(
            id = "settings",
            title = "Ustawienia i uprawnienia",
            subtitle = "Zarządzanie uprawnieniami (aparat, powiadomienia, Photo Picker)",
            icon = Icons.Default.Security,
            iconTint = MaterialTheme.colorScheme.tertiary,
            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
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
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Ustawienia i uprawnienia",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
            // Karta profilowa użytkownika
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(44.dp)
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "ROLA: ${role.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Dostępne moduły",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Renderowanie modułów z listy komponentów
            modules.forEach { moduleItem ->
                DashboardModuleCard(item = moduleItem)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Przycisk wylogowania
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

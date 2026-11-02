package pl.polsl.mobilepatient.ui.doctor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.polsl.mobilepatient.data.dto.PatientProfileDto
import pl.polsl.mobilepatient.ui.components.IconBadge
import pl.polsl.mobilepatient.ui.doctor.components.AddMedicalRecordDialog
import pl.polsl.mobilepatient.ui.doctor.components.IssuePrescriptionDialog
import pl.polsl.mobilepatient.ui.settings.SettingsScreen

@Composable
fun DoctorDashboardScreen(
    userName: String,
    onLogout: () -> Unit,
    initialTargetPatientId: String? = null,
    viewModel: DoctorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsScreen by remember { mutableStateOf(false) }

    var patientForAddRecord by remember { mutableStateOf<PatientProfileDto?>(null) }
    var patientForIssuePrescription by remember { mutableStateOf<PatientProfileDto?>(null) }

    LaunchedEffect(initialTargetPatientId) {
        if (initialTargetPatientId != null) {
            viewModel.loadDoctorData(targetPatientId = initialTargetPatientId)
        }
    }

    LaunchedEffect(uiState) {
        val state = uiState as? DoctorUiState.Success
        if (state?.actionSuccessMessage != null) {
            snackbarHostState.showSnackbar(state.actionSuccessMessage)
            viewModel.clearActionMessages()
        } else if (state?.actionErrorMessage != null) {
            snackbarHostState.showSnackbar(state.actionErrorMessage)
            viewModel.clearActionMessages()
        }
    }

    if (showSettingsScreen) {
        SettingsScreen(onBack = { showSettingsScreen = false })
        return
    }

    when (val state = uiState) {
        is DoctorUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is DoctorUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        is DoctorUiState.Success -> {
            if (state.selectedPatient != null) {
                DoctorPatientDetailScreen(
                    patient = state.selectedPatient,
                    records = state.medicalRecords,
                    onBack = { viewModel.clearSelectedPatient() },
                    onAddRecordClick = { patientForAddRecord = state.selectedPatient },
                    onIssuePrescriptionClick = { patientForIssuePrescription = state.selectedPatient }
                )
            } else {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        DoctorTopAppBar(
                            doctorName = userName,
                            onOpenSettings = { showSettingsScreen = true },
                            onLogout = onLogout
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.People, contentDescription = "Pacjenci") },
                                label = { Text("Moi Pacjenci") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Terminarz") },
                                label = { Text("Terminarz") }
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                                            slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
                                } else {
                                    slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() togetherWith
                                            slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                                }
                            },
                            label = "DoctorTabTransition"
                        ) { tab ->
                            when (tab) {
                                0 -> DoctorPatientListScreen(
                                    patients = state.patients,
                                    searchQuery = state.searchQuery,
                                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                    onSelectPatient = { viewModel.selectPatient(it) },
                                    onOpenAddRecordDialog = { patientForAddRecord = it },
                                    onOpenIssuePrescriptionDialog = { patientForIssuePrescription = it }
                                )
                                1 -> DoctorAppointmentsScreen(
                                    appointments = state.appointments,
                                    onUpdateStatus = { apptId, newStatus ->
                                        viewModel.updateAppointmentStatus(apptId, newStatus)
                                    },
                                    onSelectPatient = { viewModel.selectPatient(it) }
                                )
                            }
                        }
                    }
                }
            }

            patientForAddRecord?.let { patient ->
                AddMedicalRecordDialog(
                    patientName = "${patient.first_name} ${patient.last_name}",
                    isLoading = state.isAddingRecordLoading,
                    onDismiss = { patientForAddRecord = null },
                    onSubmit = { recordType, description ->
                        viewModel.addMedicalRecord(patient.id, recordType, description)
                        patientForAddRecord = null
                    }
                )
            }

            patientForIssuePrescription?.let { patient ->
                IssuePrescriptionDialog(
                    patientName = "${patient.first_name} ${patient.last_name}",
                    isLoading = state.isIssuingPrescriptionLoading,
                    onDismiss = { patientForIssuePrescription = null },
                    onSubmit = { medications, validDays ->
                        viewModel.issuePrescription(patient.id, medications, validDays)
                        patientForIssuePrescription = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoctorTopAppBar(
    doctorName: String,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
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
                Column {
                    Text(
                        text = "Gabinet Lekarza",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = doctorName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    contentDescription = "Ustawienia",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Wyloguj",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

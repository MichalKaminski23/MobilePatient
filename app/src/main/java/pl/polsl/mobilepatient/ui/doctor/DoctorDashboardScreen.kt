package pl.polsl.mobilepatient.ui.doctor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.polsl.mobilepatient.data.dto.ProfileDto
import pl.polsl.mobilepatient.ui.components.DashboardModuleCard
import pl.polsl.mobilepatient.ui.components.DashboardModuleItem
import pl.polsl.mobilepatient.ui.components.IconBadge
import pl.polsl.mobilepatient.ui.doctor.components.AddAppointmentDialog
import pl.polsl.mobilepatient.ui.doctor.components.AddMedicalRecordDialog
import pl.polsl.mobilepatient.ui.doctor.components.AddPatientDialog
import pl.polsl.mobilepatient.ui.doctor.components.EditMedicalRecordDialog
import pl.polsl.mobilepatient.ui.doctor.components.IssuePrescriptionDialog
import pl.polsl.mobilepatient.ui.doctor.scans.DoctorPatientScansScreen
import pl.polsl.mobilepatient.ui.map.ClinicsMapScreen
import pl.polsl.mobilepatient.ui.settings.SettingsScreen

enum class DoctorDestination {
    OVERVIEW,
    PATIENTS,
    APPOINTMENTS,
    MAP,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboardScreen(
    userName: String,
    onLogout: () -> Unit,
    initialTargetPatientId: String? = null,
    viewModel: DoctorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentDestination by remember { mutableStateOf(DoctorDestination.OVERVIEW) }
    var showPatientScansScreen by remember { mutableStateOf(false) }

    var patientForAddRecord by remember { mutableStateOf<ProfileDto?>(null) }
    var patientForIssuePrescription by remember { mutableStateOf<ProfileDto?>(null) }

    BackHandler(enabled = true) {
        val state = uiState as? DoctorUiState.Success
        if (showPatientScansScreen) {
            showPatientScansScreen = false
        } else if (state?.selectedPatient != null) {
            viewModel.clearSelectedPatient()
        } else if (currentDestination != DoctorDestination.OVERVIEW) {
            currentDestination = DoctorDestination.OVERVIEW
        } else {
            onLogout()
        }
    }

    LaunchedEffect(initialTargetPatientId) {
        if (initialTargetPatientId != null) {
            viewModel.loadDoctorData(targetPatientId = initialTargetPatientId)
            currentDestination = DoctorDestination.PATIENTS
        }
    }

    LaunchedEffect(uiState) {
        val state = uiState as? DoctorUiState.Success
        if (state?.selectedPatient == null) {
            showPatientScansScreen = false
        }
        if (state?.actionSuccessMessage != null) {
            snackbarHostState.showSnackbar(state.actionSuccessMessage)
            viewModel.clearActionMessages()
        } else if (state?.actionErrorMessage != null) {
            snackbarHostState.showSnackbar(state.actionErrorMessage)
            viewModel.clearActionMessages()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                    if (showPatientScansScreen) {
                        DoctorPatientScansScreen(
                            patientName = "${state.selectedPatient.first_name} ${state.selectedPatient.last_name}",
                            patientId = state.selectedPatient.id,
                            doctorId = state.doctorId,
                            onBack = { showPatientScansScreen = false }
                        )
                    } else {
                        DoctorPatientDetailScreen(
                            patient = state.selectedPatient,
                            records = state.medicalRecords,
                            prescriptions = state.prescriptions,
                            onBack = { viewModel.clearSelectedPatient() },
                            onAddRecordClick = { patientForAddRecord = state.selectedPatient },
                            onIssuePrescriptionClick = {
                                patientForIssuePrescription = state.selectedPatient
                            },
                            onAddAppointmentClick = { viewModel.openAddAppointmentDialog() },
                            onOpenScansClick = { showPatientScansScreen = true },
                            onEditRecordClick = { viewModel.setRecordToEdit(it) },
                            onDeleteRecordClick = { viewModel.deleteMedicalRecord(it) }
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = currentDestination,
                        transitionSpec = {
                            if (targetState != DoctorDestination.OVERVIEW) {
                                (slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(220)))
                                    .togetherWith(slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut(animationSpec = tween(220)))
                            } else {
                                (slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn(animationSpec = tween(220)))
                                    .togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(220)))
                            }
                        },
                        label = "DoctorDestinationTransition"
                    ) { destination ->
                    when (destination) {
                        DoctorDestination.OVERVIEW -> {
                            DashboardOverview(
                                doctorName = userName,
                                onOpenPatients = { currentDestination = DoctorDestination.PATIENTS },
                                onOpenAppointments = { currentDestination = DoctorDestination.APPOINTMENTS },
                                onOpenMap = { currentDestination = DoctorDestination.MAP },
                                onOpenSettings = { currentDestination = DoctorDestination.SETTINGS },
                                onLogout = onLogout
                            )
                        }

                        DoctorDestination.PATIENTS -> {
                            Scaffold(
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                topBar = {
                                    TopAppBar(
                                        title = { Text("Moi pacjenci", fontWeight = FontWeight.Bold) },
                                        navigationIcon = {
                                            IconButton(onClick = { currentDestination = DoctorDestination.OVERVIEW }) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Powrót"
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                                    )
                                },
                                containerColor = MaterialTheme.colorScheme.background
                            ) { inner ->
                                Box(modifier = Modifier.padding(inner)) {
                                    DoctorPatientListScreen(
                                        patients = state.patients,
                                        searchQuery = state.searchQuery,
                                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                        onSelectPatient = { viewModel.selectPatient(it) },
                                        onOpenAddRecordDialog = { patientForAddRecord = it },
                                        onOpenIssuePrescriptionDialog = { patientForIssuePrescription = it },
                                        onOpenAddPatientDialog = { viewModel.openAddPatientDialog() },
                                        onRemovePatient = { viewModel.removeAssignedPatient(it) }
                                    )
                                }
                            }
                        }

                        DoctorDestination.APPOINTMENTS -> {
                            Scaffold(
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                topBar = {
                                    TopAppBar(
                                        title = { Text("Terminarz wizyt", fontWeight = FontWeight.Bold) },
                                        navigationIcon = {
                                            IconButton(onClick = { currentDestination = DoctorDestination.OVERVIEW }) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Powrót"
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                                    )
                                },
                                containerColor = MaterialTheme.colorScheme.background
                            ) { inner ->
                                Box(modifier = Modifier.padding(inner)) {
                                    DoctorAppointmentsScreen(
                                        appointments = state.appointments,
                                        onUpdateStatus = { apptId, newStatus ->
                                            viewModel.updateAppointmentStatus(apptId, newStatus)
                                        },
                                        onSelectPatient = { viewModel.selectPatient(it) }
                                    )
                                }
                            }
                        }

                        DoctorDestination.MAP -> {
                            ClinicsMapScreen(onBack = { currentDestination = DoctorDestination.OVERVIEW })
                        }

                        DoctorDestination.SETTINGS -> {
                            SettingsScreen(onBack = { currentDestination = DoctorDestination.OVERVIEW })
                        }
                    }
                }
            }

            if (state.showAddPatientDialog) {
                AddPatientDialog(
                    availablePatients = state.availablePatients,
                    onDismiss = { viewModel.closeAddPatientDialog() },
                    onAssignPatient = { patientId -> viewModel.assignPatient(patientId) }
                )
            }

            if (state.showAddAppointmentDialog) {
                AddAppointmentDialog(
                    patientName = state.selectedPatient?.let { "${it.first_name} ${it.last_name}" } ?: "Pacjent",
                    clinics = state.clinics,
                    onDismiss = { viewModel.closeAddAppointmentDialog() },
                    onSubmit = { clinicId, scheduledAt, notes ->
                        viewModel.createAppointment(clinicId, scheduledAt, notes)
                    }
                )
            }

            state.recordToEdit?.let { record ->
                EditMedicalRecordDialog(
                    record = record,
                    onDismiss = { viewModel.setRecordToEdit(null) },
                    onSubmit = { newDesc ->
                        viewModel.updateMedicalRecord(record.id, newDesc)
                    }
                )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardOverview(
    doctorName: String,
    onOpenPatients: () -> Unit,
    onOpenAppointments: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val modules = listOf(
        DashboardModuleItem(
            id = "patients",
            title = "Moi pacjenci",
            subtitle = "Zarządzanie listą pacjentów, karta i dokumentacja",
            icon = Icons.Default.People,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenPatients
        ),
        DashboardModuleItem(
            id = "appointments",
            title = "Terminarz wizyt",
            subtitle = "Harmonogram i obsługa statusów konsultacji",
            icon = Icons.Default.CalendarMonth,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenAppointments
        ),
        DashboardModuleItem(
            id = "map",
            title = "Placówki i mapa",
            subtitle = "Lokalizacje przychodni medycznych",
            icon = Icons.Default.Map,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onOpenMap
        ),
        DashboardModuleItem(
            id = "settings",
            title = "Ustawienia",
            subtitle = "Zarządzanie uprawnieniami i preferencjami",
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
                            text = "Gabinet Lekarza",
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
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = doctorName.take(1).uppercase().ifEmpty { "L" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Witaj, dr $doctorName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "ROLA: LEKARZ",
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
                text = "Dostępne moduły lekarza",
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

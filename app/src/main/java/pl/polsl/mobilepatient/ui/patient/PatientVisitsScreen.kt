package pl.polsl.mobilepatient.ui.patient

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.polsl.mobilepatient.data.dto.PatientAppointmentDetailsDto
import pl.polsl.mobilepatient.ui.components.IconBadge
import pl.polsl.mobilepatient.ui.patient.components.BookAppointmentDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientVisitsScreen(
    onBack: () -> Unit,
    viewModel: PatientVisitsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showBookingDialog by remember { mutableStateOf(false) }
    var appointmentToCancel by remember { mutableStateOf<PatientAppointmentDetailsDto?>(null) }

    BackHandler(onBack = onBack)

    val filteredAppointments = when (uiState.selectedFilter) {
        "UPCOMING" -> uiState.appointments.filter { it.status.lowercase() == "scheduled" }
        "COMPLETED" -> uiState.appointments.filter { it.status.lowercase() == "completed" }
        "CANCELLED" -> uiState.appointments.filter { it.status.lowercase() == "cancelled" }
        else -> uiState.appointments
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = Icons.Default.Event,
                            tint = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            badgeSize = 36.dp,
                            iconSize = 20.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Wizyty i konsultacje",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "Harmonogram i rejestracja",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cofnij",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadData() },
                        enabled = !uiState.isLoading,
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Odśwież",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Przycisk rejestracji nowej wizyty
            Button(
                onClick = { showBookingDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Zarejestruj nową wizytę",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Komunikaty sukcesu i błędów
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.dismissMessages() }) {
                            Icon(Icons.Default.Close, contentDescription = "Zamknij", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            uiState.successMessage?.let { success ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = success,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.dismissMessages() }) {
                            Icon(Icons.Default.Close, contentDescription = "Zamknij", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filtry wizyt
            VisitsFilterRow(
                selectedFilter = uiState.selectedFilter,
                onSelectFilter = { viewModel.setFilter(it) },
                allCount = uiState.appointments.size,
                upcomingCount = uiState.appointments.count { it.status.lowercase() == "scheduled" },
                completedCount = uiState.appointments.count { it.status.lowercase() == "completed" },
                cancelledCount = uiState.appointments.count { it.status.lowercase() == "cancelled" },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    uiState.isLoading && uiState.appointments.isEmpty() -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }

                    filteredAppointments.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.selectedFilter == "UPCOMING") {
                                    "Brak nadchodzących wizyt"
                                } else {
                                    "Brak wizyt w tej kategorii"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Kliknij „Zarejestruj nową wizytę”, aby wybrać placówkę, lekarza i termin.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(filteredAppointments, key = { it.id }) { appointment ->
                                PatientAppointmentCard(
                                    appointment = appointment,
                                    isCancelling = uiState.isCancelling,
                                    onCancelClick = { appointmentToCancel = appointment },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog rejestracji nowej wizyty
    if (showBookingDialog) {
        BookAppointmentDialog(
            clinics = uiState.clinics,
            doctors = uiState.doctors,
            isLoading = uiState.isBooking,
            onDismiss = { showBookingDialog = false },
            onSubmit = { doctorId, clinicId, scheduledAt, notes ->
                showBookingDialog = false
                viewModel.bookAppointment(
                    doctorId = doctorId,
                    clinicId = clinicId,
                    scheduledAt = scheduledAt,
                    notes = notes,
                )
            },
        )
    }

    // Dialog potwierdzenia odwołania wizyty
    appointmentToCancel?.let { appointment ->
        AlertDialog(
            onDismissRequest = { appointmentToCancel = null },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp),
                )
            },
            title = {
                Text("Odwołać wizytę?")
            },
            text = {
                Text(
                    "Czy na pewno chcesz odwołać wizytę zaplanowaną na ${appointment.scheduled_at.replace("T", " ").take(16)} u ${appointment.doctor_name}?",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = appointment.id
                        appointmentToCancel = null
                        viewModel.cancelAppointment(id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Odwołaj wizytę")
                }
            },
            dismissButton = {
                TextButton(onClick = { appointmentToCancel = null }) {
                    Text("Wróć")
                }
            },
        )
    }
}

@Composable
private fun VisitsFilterRow(
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    allCount: Int,
    upcomingCount: Int,
    completedCount: Int,
    cancelledCount: Int,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            FilterChip(
                selected = selectedFilter == "UPCOMING",
                onClick = { onSelectFilter("UPCOMING") },
                label = { Text("Nadchodzące ($upcomingCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "COMPLETED",
                onClick = { onSelectFilter("COMPLETED") },
                label = { Text("Zrealizowane ($completedCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "CANCELLED",
                onClick = { onSelectFilter("CANCELLED") },
                label = { Text("Odwołane ($cancelledCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { onSelectFilter("ALL") },
                label = { Text("Wszystkie ($allCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
    }
}

@Composable
private fun PatientAppointmentCard(
    appointment: PatientAppointmentDetailsDto,
    isCancelling: Boolean,
    onCancelClick: () -> Unit,
) {
    val statusLower = appointment.status.lowercase()
    val (statusLabel, statusColor, statusBorder) = when (statusLower) {
        "scheduled" -> Triple(
            "ZAPLANOWANA",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        )
        "completed" -> Triple(
            "ZREALIZOWANA",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
        )
        else -> Triple(
            "ODWOŁANA",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Nagłówek: Data i godzina oraz status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = appointment.scheduled_at.replace("T", " ").take(16),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor,
                    border = BorderStroke(1.dp, statusBorder),
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lekarz
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = appointment.doctor_name ?: "Lekarz prowadzący",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Placówka
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = appointment.clinic_name ?: "Placówka medyczna",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    appointment.clinic_address?.let { address ->
                        Text(
                            text = address,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Uwagi pacjenta
            if (!appointment.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = appointment.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Przycisk odwołania wizyty dla zaplanowanych
            if (statusLower == "scheduled") {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onCancelClick,
                    enabled = !isCancelling,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                ) {
                    Text(
                        text = "Odwołaj wizytę",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

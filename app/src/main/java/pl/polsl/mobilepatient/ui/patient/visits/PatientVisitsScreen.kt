package pl.polsl.mobilepatient.ui.patient.visits

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import pl.polsl.mobilepatient.ui.patient.components.ConfirmActionDialog
import pl.polsl.mobilepatient.ui.patient.components.EmptyStateView
import pl.polsl.mobilepatient.ui.patient.components.ErrorMessageBanner
import pl.polsl.mobilepatient.ui.patient.components.SuccessMessageBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientVisitsScreen(
    onBack: () -> Unit,
    viewModel: PatientVisitsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler(onBack = onBack)

    var showBookingDialog by remember { mutableStateOf(false) }
    var appointmentToCancel by remember { mutableStateOf<PatientAppointmentDetailsDto?>(null) }

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
                            icon = Icons.Default.CalendarMonth,
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
            Button(
                onClick = { showBookingDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Zarejestruj nową wizytę",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(10.dp))
                ErrorMessageBanner(
                    message = error,
                    onDismiss = { viewModel.dismissMessages() },
                )
            }

            uiState.successMessage?.let { success ->
                Spacer(modifier = Modifier.height(10.dp))
                SuccessMessageBanner(
                    message = success,
                    onDismiss = { viewModel.dismissMessages() },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                        EmptyStateView(
                            icon = Icons.Default.CalendarMonth,
                            title = if (uiState.selectedFilter == "UPCOMING") {
                                "Brak nadchodzących wizyt"
                            } else {
                                "Brak wizyt w tej kategorii"
                            },
                            description = "Kliknij „Zarejestruj nową wizytę”, aby wybrać placówkę, lekarza i termin.",
                        )
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

    appointmentToCancel?.let { appointment ->
        ConfirmActionDialog(
            icon = Icons.Default.Warning,
            iconTint = MaterialTheme.colorScheme.error,
            iconContainerColor = MaterialTheme.colorScheme.errorContainer,
            title = "Odwołać wizytę lekarską?",
            message = "Czy na pewno chcesz odwołać wizytę zaplanowaną na ${
                appointment.scheduled_at.replace(
                    "T",
                    " "
                ).take(16)
            } u ${appointment.doctor_name}? Zwolniony termin powróci do puli wolnych terminów.",
            confirmText = "Odwołaj wizytę",
            confirmButtonColor = MaterialTheme.colorScheme.error,
            dismissText = "Wróć",
            onConfirm = {
                val id = appointment.id
                appointmentToCancel = null
                viewModel.cancelAppointment(id)
            },
            onDismiss = { appointmentToCancel = null },
        )
    }
}

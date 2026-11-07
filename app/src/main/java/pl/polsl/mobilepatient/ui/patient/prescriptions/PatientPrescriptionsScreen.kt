package pl.polsl.mobilepatient.ui.patient.prescriptions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Refresh
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
import pl.polsl.mobilepatient.data.dto.PrescriptionDto
import pl.polsl.mobilepatient.ui.components.IconBadge
import pl.polsl.mobilepatient.ui.patient.components.ConfirmActionDialog
import pl.polsl.mobilepatient.ui.patient.components.EmptyStateView
import pl.polsl.mobilepatient.ui.patient.components.ErrorMessageBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientPrescriptionsScreen(
    onBack: () -> Unit,
    viewModel: PatientPrescriptionsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var prescriptionToFulfill by remember { mutableStateOf<PrescriptionDto?>(null) }

    BackHandler(onBack = onBack)

    val filteredList = when (uiState.selectedFilter) {
        "active" -> uiState.prescriptions.filter { it.status.lowercase() == "active" }
        "fulfilled" -> uiState.prescriptions.filter { it.status.lowercase() == "fulfilled" }
        "expired" -> uiState.prescriptions.filter {
            it.status.lowercase() != "active" && it.status.lowercase() != "fulfilled"
        }

        else -> uiState.prescriptions
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = Icons.Default.Medication,
                            tint = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            badgeSize = 36.dp,
                            iconSize = 20.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Moje e-recepty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "Kody odbioru i wykaz leków",
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
                        onClick = { viewModel.loadPrescriptions() },
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
                .padding(innerPadding),
        ) {
            PrescriptionsFilterRow(
                selectedFilter = uiState.selectedFilter,
                onSelectFilter = { viewModel.setFilter(it) },
                allCount = uiState.prescriptions.size,
                activeCount = uiState.prescriptions.count { it.status.lowercase() == "active" },
                fulfilledCount = uiState.prescriptions.count { it.status.lowercase() == "fulfilled" },
                expiredCount = uiState.prescriptions.count {
                    it.status.lowercase() != "active" && it.status.lowercase() != "fulfilled"
                },
            )

            uiState.errorMessage?.let { error ->
                ErrorMessageBanner(
                    message = error,
                    onDismiss = { viewModel.dismissMessages() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    uiState.isLoading && uiState.prescriptions.isEmpty() -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }

                    filteredList.isEmpty() -> {
                        EmptyStateView(
                            icon = Icons.Default.Medication,
                            title = if (uiState.selectedFilter == "active") {
                                "Brak aktywnych recept"
                            } else {
                                "Brak recept w tej kategorii"
                            },
                            description = "Gdy lekarz wystawi e-receptę, pojawi się ona na tej liście z 4-cyfrowym kodem odbioru.",
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(filteredList, key = { it.id }) { prescription ->
                                PatientPrescriptionCard(
                                    prescription = prescription,
                                    isFulfilling = uiState.isFulfilling,
                                    onFulfillClick = { prescriptionToFulfill = prescription },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    prescriptionToFulfill?.let { prescription ->
        ConfirmActionDialog(
            icon = Icons.Default.CheckCircle,
            iconTint = MaterialTheme.colorScheme.primary,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            title = "Zrealizować e-receptę?",
            message = "Czy chcesz zasymulować odbiór leków w aptece dla e-recepty o kodzie ${prescription.code}? Po zatwierdzeniu status zmieni się na ZREALIZOWANA.",
            confirmText = "Potwierdź",
            confirmButtonColor = MaterialTheme.colorScheme.primary,
            dismissText = "Wróć",
            onConfirm = {
                val id = prescription.id
                prescriptionToFulfill = null
                viewModel.fulfillPrescription(id)
            },
            onDismiss = { prescriptionToFulfill = null },
        )
    }
}

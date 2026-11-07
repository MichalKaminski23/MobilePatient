package pl.polsl.mobilepatient.ui.patient.medical_records

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
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.polsl.mobilepatient.ui.components.IconBadge
import pl.polsl.mobilepatient.ui.patient.components.EmptyStateView
import pl.polsl.mobilepatient.ui.patient.components.ErrorMessageBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientRecordsScreen(
    onBack: () -> Unit,
    viewModel: PatientRecordsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(onBack = onBack)

    val filteredRecords = when (uiState.selectedFilter) {
        "visit" -> uiState.records.filter { it.record_type == "visit" }
        "test_result" -> uiState.records.filter { it.record_type == "test_result" }
        "referral" -> uiState.records.filter { it.record_type == "referral" }
        else -> uiState.records
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = Icons.Default.Description,
                            tint = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            badgeSize = 36.dp,
                            iconSize = 20.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Moja dokumentacja",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "Historia leczenia i diagnozy",
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
                        onClick = { viewModel.loadRecords() },
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
            RecordsFilterRow(
                selectedFilter = uiState.selectedFilter,
                onSelectFilter = { viewModel.setFilter(it) },
                allCount = uiState.records.size,
                visitsCount = uiState.records.count { it.record_type == "visit" },
                testsCount = uiState.records.count { it.record_type == "test_result" },
                referralsCount = uiState.records.count { it.record_type == "referral" },
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
                    uiState.isLoading && uiState.records.isEmpty() -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }

                    filteredRecords.isEmpty() -> {
                        EmptyStateView(
                            icon = Icons.Default.Description,
                            title = if (uiState.selectedFilter == "ALL") {
                                "Brak dokumentacji medycznej"
                            } else {
                                "Brak wpisów w tej kategorii"
                            },
                            description = "Wpisy dodane przez lekarza podczas wizyt lub badań pojawią się w tym miejscu.",
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(filteredRecords, key = { it.id }) { record ->
                                PatientMedicalRecordCard(record = record)
                            }
                        }
                    }
                }
            }
        }
    }
}

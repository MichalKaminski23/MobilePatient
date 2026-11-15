package pl.polsl.mobilepatient.ui.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.polsl.mobilepatient.data.dto.ProfileDto
import pl.polsl.mobilepatient.ui.components.ConfirmActionDialog
import pl.polsl.mobilepatient.ui.patient.components.EmptyStateView

@Composable
fun DoctorPatientListScreen(
    patients: List<ProfileDto>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectPatient: (patientId: String) -> Unit,
    onOpenAddRecordDialog: (patient: ProfileDto) -> Unit,
    onOpenIssuePrescriptionDialog: (patient: ProfileDto) -> Unit,
    onOpenAddPatientDialog: () -> Unit,
    onRemovePatient: (patientId: String) -> Unit
) {
    var patientToRemove by remember { mutableStateOf<ProfileDto?>(null) }

    patientToRemove?.let { patient ->
        ConfirmActionDialog(
            title = "Odpiąć pacjenta?",
            message = "Czy na pewno chcesz usunąć pacjenta ${patient.first_name} ${patient.last_name} z listy swoich pacjentów?",
            confirmText = "Odepnij",
            confirmButtonColor = MaterialTheme.colorScheme.error,
            dismissText = "Anuluj",
            onConfirm = {
                val id = patient.id
                patientToRemove = null
                onRemovePatient(id)
            },
            onDismiss = { patientToRemove = null }
        )
    }

    val filteredPatients = patients.filter { patient ->
        val fullName = "${patient.first_name} ${patient.last_name}"
        val pesel = patient.pesel.orEmpty()
        fullName.contains(searchQuery, ignoreCase = true) || pesel.contains(searchQuery)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Szukaj pacjenta") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Szukaj")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Liczba pacjentów: ${filteredPatients.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (filteredPatients.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.People,
                        title = if (searchQuery.isBlank()) "Brak przypisanych pacjentów" else "Brak wyników wyszukiwania",
                        description = if (searchQuery.isBlank()) {
                            "Nie masz jeszcze przypisanych pacjentów pod swoją opieką. Kliknij przycisk „Dodaj pacjenta”, aby przypisać pacjenta."
                        } else {
                            "Nie znaleziono pacjenta odpowiadającego kryteriom wyszukiwania."
                        }
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPatients, key = { it.id }) { patient ->
                        PatientCardItem(
                            patient = patient,
                            onCardClick = { onSelectPatient(patient.id) },
                            onAddRecordClick = { onOpenAddRecordDialog(patient) },
                            onIssuePrescriptionClick = { onOpenIssuePrescriptionDialog(patient) },
                            onRemoveClick = { patientToRemove = patient }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onOpenAddPatientDialog,
            icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
            text = { Text("Dodaj pacjenta", fontWeight = FontWeight.SemiBold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun PatientCardItem(
    patient: ProfileDto,
    onCardClick: () -> Unit,
    onAddRecordClick: () -> Unit,
    onIssuePrescriptionClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        text = patient.first_name.take(1).uppercase().ifEmpty { "P" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${patient.first_name} ${patient.last_name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PESEL: ${patient.pesel ?: "brak"} • Tel: ${patient.phone ?: "brak"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onRemoveClick) {
                    Icon(
                        imageVector = Icons.Default.PersonRemove,
                        contentDescription = "Odepnij pacjenta",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCardClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Karta",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = onAddRecordClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Wpis",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = onIssuePrescriptionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Medication,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Wystaw e-receptę",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

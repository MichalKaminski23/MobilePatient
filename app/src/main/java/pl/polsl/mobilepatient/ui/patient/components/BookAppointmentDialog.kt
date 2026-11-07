package pl.polsl.mobilepatient.ui.patient.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import pl.polsl.mobilepatient.data.dto.ClinicDto
import pl.polsl.mobilepatient.data.dto.PatientProfileDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentDialog(
    clinics: List<ClinicDto>,
    doctors: List<PatientProfileDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (doctorId: String, clinicId: String, scheduledAt: String, notes: String?) -> Unit,
) {
    var selectedClinic by remember { mutableStateOf(clinics.firstOrNull()) }
    var clinicExpanded by remember { mutableStateOf(false) }

    var selectedDoctor by remember { mutableStateOf(doctors.firstOrNull()) }
    var doctorExpanded by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    // Domyślnie jutro o 10:00
    var dateString by remember { mutableStateOf(today.plusDays(1).format(formatter)) }
    var selectedTime by remember { mutableStateOf("10:00") }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val times = listOf("08:30", "10:00", "11:30", "13:00", "15:00", "16:30")
    val quickDays = listOf(
        "Jutro" to today.plusDays(1).format(formatter),
        "Za 3 dni" to today.plusDays(3).format(formatter),
        "Za tydzień" to today.plusWeeks(1).format(formatter),
    )

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rejestracja na wizytę",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 1. Wybór przychodni
                Text(
                    text = "1. Wybierz placówkę medyczną:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                ExposedDropdownMenuBox(
                    expanded = clinicExpanded,
                    onExpandedChange = { clinicExpanded = !clinicExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = selectedClinic?.name ?: "Brak dostępnych placówek",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clinicExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp),
                    )

                    ExposedDropdownMenu(
                        expanded = clinicExpanded,
                        onDismissRequest = { clinicExpanded = false },
                    ) {
                        clinics.forEach { clinic ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(clinic.name, fontWeight = FontWeight.Bold)
                                        Text(clinic.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedClinic = clinic
                                    clinicExpanded = false
                                },
                            )
                        }
                    }
                }

                // 2. Wybór lekarza
                Text(
                    text = "2. Wybierz lekarza prowadzącego:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                ExposedDropdownMenuBox(
                    expanded = doctorExpanded,
                    onExpandedChange = { doctorExpanded = !doctorExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = if (selectedDoctor != null) "dr ${selectedDoctor!!.first_name} ${selectedDoctor!!.last_name}" else "Brak lekarzy w bazie",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp),
                    )

                    ExposedDropdownMenu(
                        expanded = doctorExpanded,
                        onDismissRequest = { doctorExpanded = false },
                    ) {
                        doctors.forEach { doctor ->
                            DropdownMenuItem(
                                text = {
                                    Text("dr ${doctor.first_name} ${doctor.last_name}", fontWeight = FontWeight.Medium)
                                },
                                onClick = {
                                    selectedDoctor = doctor
                                    doctorExpanded = false
                                },
                            )
                        }
                    }
                }

                // 3. Wybór terminu
                Text(
                    text = "3. Wybierz datę wizyty:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    quickDays.forEach { (label, date) ->
                        FilterChip(
                            selected = dateString == date,
                            onClick = { dateString = date },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Data (RRRR-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                Text(
                    text = "Godzina przyjęcia:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    times.take(3).forEach { time ->
                        FilterChip(
                            selected = selectedTime == time,
                            onClick = { selectedTime = time },
                            label = { Text(time, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    times.drop(3).forEach { time ->
                        FilterChip(
                            selected = selectedTime == time,
                            onClick = { selectedTime = time },
                            label = { Text(time, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }

                // 4. Uwagi pacjenta
                Text(
                    text = "4. Powód wizyty lub dolegliwości (opcjonalnie):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("np. Kontrola wyników badań, ból gardła...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                )

                errorText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedClinic == null) {
                        errorText = "Wybierz placówkę medyczną."
                        return@Button
                    }
                    if (selectedDoctor == null) {
                        errorText = "Wybierz lekarza prowadzącego."
                        return@Button
                    }
                    if (dateString.isBlank()) {
                        errorText = "Podaj poprawną datę wizyty."
                        return@Button
                    }

                    // Format daty ISO-8601 (np. 2026-11-10T10:00:00Z)
                    val scheduledAt = "${dateString.trim()}T${selectedTime.trim()}:00Z"
                    onSubmit(
                        selectedDoctor!!.id,
                        selectedClinic!!.id,
                        scheduledAt,
                        notes,
                    )
                },
                enabled = !isLoading && selectedClinic != null && selectedDoctor != null,
                shape = RoundedCornerShape(10.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rejestracja...")
                } else {
                    Text("Zarejestruj wizytę")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Anuluj")
            }
        },
    )
}

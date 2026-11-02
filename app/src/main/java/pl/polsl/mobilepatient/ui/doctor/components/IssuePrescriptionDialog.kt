package pl.polsl.mobilepatient.ui.doctor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun IssuePrescriptionDialog(
    patientName: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (medications: String, validDays: Int) -> Unit
) {
    var medications by remember { mutableStateOf("") }
    var validDays by remember { mutableIntStateOf(30) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val validOptions = listOf(
        30 to "30 dni",
        90 to "90 dni",
        365 to "365 dni"
    )

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Wystaw e-receptę",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Pacjent: $patientName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = medications,
                    onValueChange = {
                        medications = it
                        if (errorText != null) errorText = null
                    },
                    label = { Text("Wykaz leków i dawkowanie") },
                    placeholder = { Text("np. Paracetamol 500mg - 2x1 dziennie przez 5 dni") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )

                Text(
                    text = "Ważność recepty:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    validOptions.forEach { (days, label) ->
                        FilterChip(
                            selected = validDays == days,
                            onClick = { validDays = days },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text(
                    text = "Automatycznie zostanie wygenerowany 4-cyfrowy kod odbioru.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (medications.isBlank()) {
                        errorText = "Wykaz leków nie może być pusty."
                    } else {
                        onSubmit(medications.trim(), validDays)
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Wystaw receptę")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Anuluj")
            }
        }
    )
}

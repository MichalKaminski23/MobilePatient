package pl.polsl.mobilepatient.ui.doctor.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import pl.polsl.mobilepatient.ui.components.ProfileDataRow
import pl.polsl.mobilepatient.ui.components.UserProfileDialog

@Composable
fun DoctorProfileDialog(
    initialUserName: String,
    initialRole: String = "doctor",
    onDismiss: () -> Unit,
) {
    UserProfileDialog(
        initialUserName = initialUserName,
        initialRole = initialRole,
        dialogTitle = "Profil lekarza",
        onDismiss = onDismiss,
    ) { profile ->
        ProfileDataRow(
            icon = Icons.Default.Email,
            label = "Adres e-mail",
            value = profile.email ?: "Nie podano",
        )

        ProfileDataRow(
            icon = Icons.Default.Phone,
            label = "Telefon kontaktowy",
            value = profile.phone ?: "Nie podano",
        )

        ProfileDataRow(
            icon = Icons.Default.MedicalServices,
            label = "Rola w systemie",
            value = "Lekarz prowadzący / specjalista",
        )
    }
}

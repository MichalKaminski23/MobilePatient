package pl.polsl.mobilepatient.ui.patient.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import pl.polsl.mobilepatient.ui.components.ProfileDataRow
import pl.polsl.mobilepatient.ui.components.UserProfileDialog

@Composable
fun PatientProfileDialog(
    initialUserName: String,
    initialRole: String = "patient",
    onDismiss: () -> Unit,
) {
    UserProfileDialog(
        initialUserName = initialUserName,
        initialRole = initialRole,
        dialogTitle = "Moje dane osobowe",
        onDismiss = onDismiss,
    ) { profile ->
        ProfileDataRow(
            icon = Icons.Default.Badge,
            label = "Numer PESEL",
            value = profile.pesel ?: "Brak danych w bazie",
            isMonospace = true,
        )

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
            icon = Icons.Default.Home,
            label = "Adres zamieszkania",
            value = profile.address ?: "Nie podano",
        )
    }
}

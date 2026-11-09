package pl.polsl.mobilepatient.ui.admin.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.runtime.Composable
import pl.polsl.mobilepatient.ui.components.ProfileDataRow
import pl.polsl.mobilepatient.ui.components.UserProfileDialog

@Composable
fun AdminProfileDialog(
    initialUserName: String,
    initialRole: String = "admin",
    onDismiss: () -> Unit,
) {
    UserProfileDialog(
        initialUserName = initialUserName,
        initialRole = initialRole,
        dialogTitle = "Profil administratora",
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
            icon = Icons.Default.VerifiedUser,
            label = "Status konta",
            value = "Zweryfikowane konto administratora",
        )
    }
}

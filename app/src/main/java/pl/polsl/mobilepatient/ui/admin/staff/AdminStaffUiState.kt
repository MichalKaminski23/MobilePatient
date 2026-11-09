package pl.polsl.mobilepatient.ui.admin.staff

import pl.polsl.mobilepatient.data.dto.ProfileDto

enum class StaffRoleFilter(val label: String) {
    ALL("Wszyscy"),
    DOCTOR("Lekarze"),
    PATIENT("Pacjenci"),
    ADMIN("Administratorzy")
}

data class AdminStaffUiState(
    val isLoading: Boolean = false,
    val users: List<ProfileDto> = emptyList(),
    val currentUserId: String? = null,
    val searchQuery: String = "",
    val selectedFilter: StaffRoleFilter = StaffRoleFilter.ALL,
    val isUpdatingRole: Boolean = false,
    val isCreatingDoctor: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val doctorsCount: Int
        get() = users.count { it.role.equals("doctor", ignoreCase = true) }

    val patientsCount: Int
        get() = users.count { it.role.equals("patient", ignoreCase = true) }

    val adminsCount: Int
        get() = users.count { it.role.equals("admin", ignoreCase = true) }

    val filteredUsers: List<ProfileDto>
        get() = users.filter { profile ->
            val matchesFilter = when (selectedFilter) {
                StaffRoleFilter.ALL -> true
                StaffRoleFilter.DOCTOR -> profile.role.equals("doctor", ignoreCase = true)
                StaffRoleFilter.PATIENT -> profile.role.equals("patient", ignoreCase = true)
                StaffRoleFilter.ADMIN -> profile.role.equals("admin", ignoreCase = true)
            }

            if (!matchesFilter) return@filter false

            if (searchQuery.isBlank()) return@filter true

            val q = searchQuery.trim().lowercase()
            val fullName = "${profile.first_name} ${profile.last_name}".lowercase()
            fullName.contains(q) ||
                (profile.email?.lowercase()?.contains(q) == true) ||
                (profile.pesel?.contains(q) == true) ||
                (profile.phone?.contains(q) == true) ||
                (profile.role?.lowercase()?.contains(q) == true)
        }
}

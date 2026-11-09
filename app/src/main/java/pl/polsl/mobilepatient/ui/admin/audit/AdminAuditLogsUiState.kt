package pl.polsl.mobilepatient.ui.admin.audit

import pl.polsl.mobilepatient.data.dto.AuditLogWithActor

enum class AuditFilter(val label: String) {
    ALL("Wszystkie"),
    MEDICAL_RECORDS("Dokumentacja"),
    PRESCRIPTIONS("Recepty"),
    APPOINTMENTS("Wizyty"),
    CLINICS("Placówki"),
    PROFILES("Profile"),
    OTHER("Inne")
}

data class AdminAuditLogsUiState(
    val isLoading: Boolean = false,
    val logs: List<AuditLogWithActor> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: AuditFilter = AuditFilter.ALL,
    val isGeneratingTestEvent: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val filteredLogs: List<AuditLogWithActor>
        get() = logs.filter { log ->
            val matchesFilter = when (selectedFilter) {
                AuditFilter.ALL -> true
                AuditFilter.MEDICAL_RECORDS -> log.entity_type == "medical_records"
                AuditFilter.PRESCRIPTIONS -> log.entity_type == "prescriptions"
                AuditFilter.APPOINTMENTS -> log.entity_type == "appointments"
                AuditFilter.CLINICS -> log.entity_type == "clinics"
                AuditFilter.PROFILES -> log.entity_type == "profiles"
                AuditFilter.OTHER -> log.entity_type !in listOf(
                    "medical_records", "prescriptions", "appointments", "clinics", "profiles"
                )
            }

            if (!matchesFilter) return@filter false

            if (searchQuery.isBlank()) return@filter true

            val q = searchQuery.trim().lowercase()
            log.action.lowercase().contains(q) ||
                log.entity_type.lowercase().contains(q) ||
                log.actorName.lowercase().contains(q) ||
                (log.actorEmail?.lowercase()?.contains(q) == true) ||
                (log.entity_id?.lowercase()?.contains(q) == true) ||
                (log.actor_id?.lowercase()?.contains(q) == true)
        }
}

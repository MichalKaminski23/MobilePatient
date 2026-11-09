package pl.polsl.mobilepatient.ui.admin.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.repository.AdminRepository

class AdminAuditLogsViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminAuditLogsUiState())
    val uiState: StateFlow<AdminAuditLogsUiState> = _uiState.asStateFlow()

    init {
        loadAuditLogs()
    }

    fun loadAuditLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getAuditLogs().fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            logs = list,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage
                                ?: "Nie udało się załadować dziennika audytu"
                        )
                    }
                }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectFilter(filter: AuditFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

//    fun generateTestAuditEvent() {
//        viewModelScope.launch {
//            _uiState.update { it.copy(isGeneratingTestEvent = true, errorMessage = null) }
//            val testEntityId = UUID.randomUUID().toString()
//            repository.logAuditEvent(
//                action = "TEST_AUDIT_T11",
//                entityType = "medical_records",
//                entityId = testEntityId
//            ).fold(
//                onSuccess = {
//                    _uiState.update {
//                        it.copy(
//                            isGeneratingTestEvent = false,
//                            successMessage = "Zarejestrowano zdarzenie testowe T11 w dzienniku audytu!"
//                        )
//                    }
//                    loadAuditLogs()
//                },
//                onFailure = { error ->
//                    android.util.Log.e("AuditLogVM", "Failed to generate audit event", error)
//                    _uiState.update {
//                        it.copy(
//                            isGeneratingTestEvent = false,
//                            errorMessage = error.localizedMessage
//                                ?: "Błąd zapisu zdarzenia testowego do dziennika"
//                        )
//                    }
//                }
//            )
//        }
//    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

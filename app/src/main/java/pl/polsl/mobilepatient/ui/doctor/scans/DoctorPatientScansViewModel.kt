package pl.polsl.mobilepatient.ui.doctor.scans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.dto.MedicalFileDto
import pl.polsl.mobilepatient.data.repository.DoctorRepository

data class DoctorPatientScansUiState(
    val isLoading: Boolean = false,
    val files: List<MedicalFileDto> = emptyList(),
    val isUploading: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val uploadSuccessMessage: String? = null,
    val selectedPreviewFile: MedicalFileDto? = null,
    val previewBytes: ByteArray? = null,
    val isPreviewLoading: Boolean = false
)

class DoctorPatientScansViewModel(
    private val patientId: String,
    private val doctorId: String,
    private val repository: DoctorRepository = DoctorRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorPatientScansUiState(isLoading = true))
    val uiState: StateFlow<DoctorPatientScansUiState> = _uiState.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getMedicalFilesForPatient(patientId)
            result.fold(
                onSuccess = { list ->
                    _uiState.update { it.copy(isLoading = false, files = list) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Błąd pobierania plików.") }
                }
            )
        }
    }

    fun uploadScan(fileName: String, mimeType: String, bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, errorMessage = null, uploadSuccessMessage = null) }
            val result = repository.uploadMedicalFileForPatient(patientId, doctorId, fileName, mimeType, bytes)
            result.fold(
                onSuccess = { newFile ->
                    _uiState.update { state ->
                        state.copy(
                            isUploading = false,
                            files = listOf(newFile) + state.files,
                            uploadSuccessMessage = "Pomyślnie dodano plik medyczny dla pacjenta."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isUploading = false, errorMessage = error.localizedMessage ?: "Błąd wgrywania pliku.") }
                }
            )
        }
    }

    fun selectPreviewFile(file: MedicalFileDto?) {
        if (file == null) {
            _uiState.update { it.copy(selectedPreviewFile = null, previewBytes = null, isPreviewLoading = false) }
            return
        }

        _uiState.update { it.copy(selectedPreviewFile = file, previewBytes = null, isPreviewLoading = true) }

        viewModelScope.launch {
            val result = repository.downloadFileBytes(file.storage_path)
            result.fold(
                onSuccess = { bytes ->
                    _uiState.update { it.copy(previewBytes = bytes, isPreviewLoading = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(isPreviewLoading = false) }
                }
            )
        }
    }

    fun deleteScan(file: MedicalFileDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            val result = repository.deleteMedicalFileForPatient(file.id, file.storage_path)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isDeleting = false,
                            files = state.files.filter { it.id != file.id },
                            uploadSuccessMessage = "Usunięto dokument: ${file.file_name}"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isDeleting = false, errorMessage = error.localizedMessage ?: "Nie udało się usunąć pliku.") }
                }
            )
        }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, uploadSuccessMessage = null) }
    }
}

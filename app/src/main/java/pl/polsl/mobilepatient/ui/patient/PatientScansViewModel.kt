package pl.polsl.mobilepatient.ui.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.dto.MedicalFileDto
import pl.polsl.mobilepatient.data.repository.PatientRepository

class PatientScansViewModel(
    private val repository: PatientRepository = PatientRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientScansUiState(isLoading = true))
    val uiState: StateFlow<PatientScansUiState> = _uiState.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getMyMedicalFiles()
            result.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            files = list,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Nie udało się pobrać listy skanów medycznych."
                        )
                    }
                }
            )
        }
    }

    fun uploadScan(fileName: String, mimeType: String, bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, errorMessage = null, uploadSuccessMessage = null) }
            val result = repository.uploadMedicalScan(fileName, mimeType, bytes)
            result.fold(
                onSuccess = { newFile ->
                    _uiState.update { state ->
                        state.copy(
                            isUploading = false,
                            files = listOf(newFile) + state.files,
                            uploadSuccessMessage = "Pomyślnie przesłano dokument medyczny do chmury."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = error.localizedMessage ?: "Błąd podczas wgrywania pliku do Supabase Storage."
                        )
                    }
                }
            )
        }
    }

    fun selectPreviewFile(file: MedicalFileDto?) {
        if (file == null) {
            _uiState.update { it.copy(selectedPreviewFile = null, previewBytes = null, isPreviewLoading = false) }
            return
        }

        _uiState.update {
            it.copy(
                selectedPreviewFile = file,
                previewBytes = null,
                isPreviewLoading = true
            )
        }

        viewModelScope.launch {
            val result = repository.downloadFileBytes(file.storage_path)
            result.fold(
                onSuccess = { bytes ->
                    _uiState.update {
                        it.copy(
                            previewBytes = bytes,
                            isPreviewLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("PatientScansViewModel", "Błąd pobierania bajtów skanu", error)
                    _uiState.update { it.copy(isPreviewLoading = false) }
                }
            )
        }
    }

    fun deleteScan(file: MedicalFileDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null, uploadSuccessMessage = null) }
            val result = repository.deleteMedicalFile(file.id, file.storage_path)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isDeleting = false,
                            files = state.files.filter { it.id != file.id },
                            uploadSuccessMessage = "Pomyślnie usunięto dokument: ${file.file_name}"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = error.localizedMessage ?: "Nie udało się usunąć dokumentu."
                        )
                    }
                }
            )
        }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, uploadSuccessMessage = null) }
    }
}

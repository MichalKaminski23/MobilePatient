package pl.polsl.mobilepatient.ui.patient

import pl.polsl.mobilepatient.data.dto.MedicalFileDto

data class PatientScansUiState(
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val isDeleting: Boolean = false,
    val isPreviewLoading: Boolean = false,
    val files: List<MedicalFileDto> = emptyList(),
    val errorMessage: String? = null,
    val uploadSuccessMessage: String? = null,
    val selectedPreviewFile: MedicalFileDto? = null,
    val previewBytes: ByteArray? = null
)

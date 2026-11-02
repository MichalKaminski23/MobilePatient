package pl.polsl.mobilepatient.ui.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.dto.MedicalRecordInsertDto
import pl.polsl.mobilepatient.data.dto.PrescriptionInsertDto
import pl.polsl.mobilepatient.data.repository.DoctorRepository
import java.time.LocalDate
import kotlin.random.Random

class DoctorViewModel(
    private val repository: DoctorRepository = DoctorRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DoctorUiState>(DoctorUiState.Loading)
    val uiState: StateFlow<DoctorUiState> = _uiState.asStateFlow()

    init {
        loadDoctorData()
    }

    fun loadDoctorData(targetPatientId: String? = null) {
        viewModelScope.launch {
            _uiState.value = DoctorUiState.Loading
            val currentDoctorId = repository.getCurrentUserId()
            if (currentDoctorId == null) {
                _uiState.value = DoctorUiState.Error("Brak aktywnej sesji lekarza.")
                return@launch
            }

            val patientsResult = repository.getAssignedPatients(currentDoctorId)
            val appointmentsResult = repository.getAppointmentsForDoctor(currentDoctorId)

            val patients = patientsResult.getOrDefault(emptyList())
            val appointments = appointmentsResult.getOrDefault(emptyList())

            val currentState = DoctorUiState.Success(
                doctorId = currentDoctorId,
                patients = patients,
                appointments = appointments
            )
            _uiState.value = currentState

            if (targetPatientId != null) {
                selectPatient(targetPatientId)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val current = _uiState.value as? DoctorUiState.Success ?: return
        _uiState.value = current.copy(searchQuery = query)
    }

    fun selectPatient(patientId: String) {
        viewModelScope.launch {
            val current = _uiState.value as? DoctorUiState.Success ?: return@launch
            val patientResult = repository.getPatientDetails(patientId)
            val recordsResult = repository.getMedicalRecords(patientId)

            val patient = patientResult.getOrNull()
            val records = recordsResult.getOrDefault(emptyList())

            if (patient != null) {
                _uiState.value = current.copy(
                    selectedPatient = patient,
                    medicalRecords = records
                )
            } else {
                _uiState.value = current.copy(
                    actionErrorMessage = "Nie udało się załadować danych pacjenta."
                )
            }
        }
    }

    fun clearSelectedPatient() {
        val current = _uiState.value as? DoctorUiState.Success ?: return
        _uiState.value = current.copy(
            selectedPatient = null,
            medicalRecords = emptyList()
        )
    }

    fun addMedicalRecord(
        patientId: String,
        recordType: String,
        description: String,
        appointmentId: String? = null
    ) {
        viewModelScope.launch {
            val current = _uiState.value as? DoctorUiState.Success ?: return@launch
            _uiState.value = current.copy(isAddingRecordLoading = true)

            val insertDto = MedicalRecordInsertDto(
                patient_id = patientId,
                doctor_id = current.doctorId,
                appointment_id = appointmentId,
                record_type = recordType,
                description = description
            )

            val result = repository.addMedicalRecord(insertDto)
            if (result.isSuccess) {
                val updatedRecordsResult = repository.getMedicalRecords(patientId)
                val newRecords = updatedRecordsResult.getOrDefault(current.medicalRecords)
                _uiState.value = current.copy(
                    isAddingRecordLoading = false,
                    medicalRecords = newRecords,
                    actionSuccessMessage = "Pomyślnie dodano wpis do dokumentacji medycznej."
                )
            } else {
                _uiState.value = current.copy(
                    isAddingRecordLoading = false,
                    actionErrorMessage = result.exceptionOrNull()?.message ?: "Błąd dodawania wpisu."
                )
            }
        }
    }

    fun issuePrescription(
        patientId: String,
        medications: String,
        validUntilDays: Int = 30,
        medicalRecordId: String? = null
    ) {
        viewModelScope.launch {
            val current = _uiState.value as? DoctorUiState.Success ?: return@launch
            _uiState.value = current.copy(isIssuingPrescriptionLoading = true)

            val randomCode = String.format(java.util.Locale.ROOT, "%04d", Random.nextInt(10000))
            val validUntilDate = LocalDate.now().plusDays(validUntilDays.toLong()).toString()

            val insertDto = PrescriptionInsertDto(
                patient_id = patientId,
                doctor_id = current.doctorId,
                medical_record_id = medicalRecordId,
                code = randomCode,
                medications = medications,
                valid_until = validUntilDate,
                status = "active"
            )

            val result = repository.createPrescription(insertDto)
            if (result.isSuccess) {
                _uiState.value = current.copy(
                    isIssuingPrescriptionLoading = false,
                    actionSuccessMessage = "E-recepta została pomyślnie wystawiona! Kod odbioru: $randomCode"
                )
            } else {
                _uiState.value = current.copy(
                    isIssuingPrescriptionLoading = false,
                    actionErrorMessage = result.exceptionOrNull()?.message ?: "Błąd wystawiania e-recepty."
                )
            }
        }
    }

    fun updateAppointmentStatus(appointmentId: String, newStatus: String) {
        viewModelScope.launch {
            val current = _uiState.value as? DoctorUiState.Success ?: return@launch

            val result = repository.updateAppointmentStatus(appointmentId, newStatus)
            if (result.isSuccess) {
                val updatedAppointmentsResult = repository.getAppointmentsForDoctor(current.doctorId)
                val newAppointments = updatedAppointmentsResult.getOrDefault(current.appointments)
                _uiState.value = current.copy(
                    appointments = newAppointments,
                    actionSuccessMessage = "Zaktualizowano status wizyty."
                )
            } else {
                _uiState.value = current.copy(
                    actionErrorMessage = "Nie udało się zaktualizować statusu wizyty."
                )
            }
        }
    }

    fun clearActionMessages() {
        val current = _uiState.value as? DoctorUiState.Success ?: return
        _uiState.value = current.copy(
            actionSuccessMessage = null,
            actionErrorMessage = null
        )
    }
}

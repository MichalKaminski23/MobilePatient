package pl.polsl.mobilepatient.ui.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.polsl.mobilepatient.data.dto.AppointmentInsertDto
import pl.polsl.mobilepatient.data.dto.MedicalRecordDto
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
            val prescriptionsResult = repository.getPrescriptionsForPatient(patientId)

            val patient = patientResult.getOrNull()
            val records = recordsResult.getOrDefault(emptyList())
            val prescriptions = prescriptionsResult.getOrDefault(emptyList())

            if (patient != null) {
                _uiState.value = current.copy(
                    selectedPatient = patient,
                    medicalRecords = records,
                    prescriptions = prescriptions
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
            medicalRecords = emptyList(),
            prescriptions = emptyList()
        )
    }

    fun openAddPatientDialog() {
        viewModelScope.launch {
            val current = _uiState.value as? DoctorUiState.Success ?: return@launch
            val allPatientsResult = repository.getAllPatients()
            val allPatients = allPatientsResult.getOrDefault(emptyList())

            val assignedIds = current.patients.map { it.id }.toSet()
            val available = allPatients.filter { it.id !in assignedIds }

            _uiState.value = current.copy(
                availablePatients = available,
                showAddPatientDialog = true
            )
        }
    }

    fun closeAddPatientDialog() {
        val current = _uiState.value as? DoctorUiState.Success ?: return
        _uiState.value = current.copy(showAddPatientDialog = false)
    }

    fun assignPatient(patientId: String) {
        viewModelScope.launch {
            val current = _uiState.value as? DoctorUiState.Success ?: return@launch
            val result = repository.assignPatient(current.doctorId, patientId)
            if (result.isSuccess) {
                val updatedPatients = repository.getAssignedPatients(current.doctorId).getOrDefault(current.patients)
                _uiState.value = current.copy(
                    patients = updatedPatients,
                    showAddPatientDialog = false,
                    actionSuccessMessage = "Pomyślnie przypisano pacjenta do opieki."
                )
            } else {
                _uiState.value = current.copy(
                    showAddPatientDialog = false,
                    actionErrorMessage = "Nie udało się przypisać pacjenta."
                )
            }
        }
    }

    fun removeAssignedPatient(patientId: String) {
        viewModelScope.launch {
            val current = _uiState.value as? DoctorUiState.Success ?: return@launch
            val result = repository.removeAssignedPatient(current.doctorId, patientId)
            if (result.isSuccess) {
                val updatedPatients = repository.getAssignedPatients(current.doctorId).getOrDefault(current.patients)
                _uiState.value = current.copy(
                    patients = updatedPatients,
                    actionSuccessMessage = "Usunięto pacjenta z listy opieki."
                )
            } else {
                _uiState.value = current.copy(
                    actionErrorMessage = "Nie udało się usunąć pacjenta."
                )
            }
        }
    }

    fun openAddAppointmentDialog() {
        viewModelScope.launch {
            val current = _uiState.value as? DoctorUiState.Success ?: return@launch
            val clinicsResult = repository.getClinics()
            val clinics = clinicsResult.getOrDefault(emptyList())

            _uiState.value = current.copy(
                clinics = clinics,
                showAddAppointmentDialog = true
            )
        }
    }

    fun closeAddAppointmentDialog() {
        val current = _uiState.value as? DoctorUiState.Success ?: return
        _uiState.value = current.copy(showAddAppointmentDialog = false)
    }

    fun createAppointment(clinicId: String, scheduledAt: String, notes: String?) {
        viewModelScope.launch {
            val current = _uiState.value as? DoctorUiState.Success ?: return@launch
            val patientId = current.selectedPatient?.id ?: return@launch

            val insertDto = AppointmentInsertDto(
                patient_id = patientId,
                doctor_id = current.doctorId,
                clinic_id = clinicId,
                scheduled_at = scheduledAt,
                status = "scheduled",
                notes = notes
            )

            val result = repository.createAppointment(insertDto)
            if (result.isSuccess) {
                val updatedAppointments = repository.getAppointmentsForDoctor(current.doctorId).getOrDefault(current.appointments)
                _uiState.value = current.copy(
                    appointments = updatedAppointments,
                    showAddAppointmentDialog = false,
                    actionSuccessMessage = "Pomyślnie zaplanowano wizytę."
                )
            } else {
                _uiState.value = current.copy(
                    showAddAppointmentDialog = false,
                    actionErrorMessage = "Nie udało się zaplanować wizyty."
                )
            }
        }
    }

    fun setRecordToEdit(record: MedicalRecordDto?) {
        val current = _uiState.value as? DoctorUiState.Success ?: return
        _uiState.value = current.copy(recordToEdit = record)
    }

    fun updateMedicalRecord(recordId: String, newDescription: String) {
        viewModelScope.launch {
            val current = _uiState.value as? DoctorUiState.Success ?: return@launch
            val patientId = current.selectedPatient?.id ?: return@launch

            val result = repository.updateMedicalRecord(recordId, newDescription)
            if (result.isSuccess) {
                val updatedRecords = repository.getMedicalRecords(patientId).getOrDefault(current.medicalRecords)
                _uiState.value = current.copy(
                    medicalRecords = updatedRecords,
                    recordToEdit = null,
                    actionSuccessMessage = "Pomyślnie zaktualizowano wpis medyczny."
                )
            } else {
                _uiState.value = current.copy(
                    recordToEdit = null,
                    actionErrorMessage = "Nie udało się zaktualizować wpisu."
                )
            }
        }
    }

    fun deleteMedicalRecord(recordId: String) {
        viewModelScope.launch {
            val current = _uiState.value as? DoctorUiState.Success ?: return@launch
            val patientId = current.selectedPatient?.id ?: return@launch

            val result = repository.deleteMedicalRecord(recordId)
            if (result.isSuccess) {
                val updatedRecords = repository.getMedicalRecords(patientId).getOrDefault(current.medicalRecords)
                _uiState.value = current.copy(
                    medicalRecords = updatedRecords,
                    actionSuccessMessage = "Pomyślnie usunięto wpis medyczny."
                )
            } else {
                _uiState.value = current.copy(
                    actionErrorMessage = "Nie udało się usunąć wpisu."
                )
            }
        }
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
                val updatedPrescriptions = repository.getPrescriptionsForPatient(patientId).getOrDefault(current.prescriptions)
                _uiState.value = current.copy(
                    isIssuingPrescriptionLoading = false,
                    prescriptions = updatedPrescriptions,
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

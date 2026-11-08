package pl.polsl.mobilepatient.data.repository

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import pl.polsl.mobilepatient.data.dto.AppointmentDto
import pl.polsl.mobilepatient.data.dto.AppointmentInsertDto
import pl.polsl.mobilepatient.data.dto.AppointmentWithPatientDto
import pl.polsl.mobilepatient.data.dto.ClinicDto
import pl.polsl.mobilepatient.data.dto.DoctorPatientRelationDto
import pl.polsl.mobilepatient.data.dto.MedicalRecordDto
import pl.polsl.mobilepatient.data.dto.MedicalRecordInsertDto
import pl.polsl.mobilepatient.data.dto.PrescriptionDto
import pl.polsl.mobilepatient.data.dto.PrescriptionInsertDto
import pl.polsl.mobilepatient.data.dto.ProfileDto
import pl.polsl.mobilepatient.data.network.SupabaseProvider

class DoctorRepository {

    private val client = SupabaseProvider.client

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    suspend fun getAssignedPatients(doctorId: String): Result<List<ProfileDto>> {
        return try {
            val relations = client.from("doctor_patients")
                .select {
                    filter {
                        eq("doctor_id", doctorId)
                    }
                }
                .decodeList<DoctorPatientRelationDto>()

            val patientIds = relations.map { it.patient_id }.distinct()
            if (patientIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val patients = client.from("profiles")
                .select {
                    filter {
                        isIn("id", patientIds)
                    }
                }
                .decodeList<ProfileDto>()

            Result.success(patients)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd getAssignedPatients: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllPatients(): Result<List<ProfileDto>> {
        return try {
            val patients = client.from("profiles")
                .select {
                    filter {
                        eq("role", "patient")
                    }
                }
                .decodeList<ProfileDto>()
            Result.success(patients)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd getAllPatients: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun assignPatient(doctorId: String, patientId: String): Result<Unit> {
        return try {
            client.from("doctor_patients")
                .insert(
                    mapOf(
                        "doctor_id" to doctorId,
                        "patient_id" to patientId
                    )
                )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd assignPatient: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun removeAssignedPatient(doctorId: String, patientId: String): Result<Unit> {
        return try {
            client.from("doctor_patients")
                .delete {
                    filter {
                        eq("doctor_id", doctorId)
                        eq("patient_id", patientId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd removeAssignedPatient: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getPatientDetails(patientId: String): Result<ProfileDto> {
        return try {
            val profile = client.from("profiles")
                .select {
                    filter {
                        eq("id", patientId)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()
                ?: return Result.failure(IllegalStateException("Nie znaleziono profilu pacjenta."))

            Result.success(profile)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd getPatientDetails: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getMedicalRecords(patientId: String): Result<List<MedicalRecordDto>> {
        return try {
            val records = client.from("medical_records")
                .select {
                    filter {
                        eq("patient_id", patientId)
                    }
                }
                .decodeList<MedicalRecordDto>()

            val sorted = records.sortedByDescending { it.created_at ?: "" }
            Result.success(sorted)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd getMedicalRecords: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateMedicalRecord(recordId: String, description: String): Result<Unit> {
        return try {
            client.from("medical_records")
                .update(
                    mapOf("description" to description)
                ) {
                    filter {
                        eq("id", recordId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd updateMedicalRecord: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMedicalRecord(recordId: String): Result<Unit> {
        return try {
            client.from("medical_records")
                .delete {
                    filter {
                        eq("id", recordId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd deleteMedicalRecord: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getPrescriptionsForPatient(patientId: String): Result<List<PrescriptionDto>> {
        return try {
            val prescriptions = client.from("prescriptions")
                .select {
                    filter {
                        eq("patient_id", patientId)
                    }
                }
                .decodeList<PrescriptionDto>()

            val sorted = prescriptions.sortedByDescending { it.issued_at ?: "" }
            Result.success(sorted)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd getPrescriptionsForPatient: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getClinics(): Result<List<ClinicDto>> {
        return try {
            val clinics = client.from("clinics")
                .select()
                .decodeList<ClinicDto>()
            Result.success(clinics)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd getClinics: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun addMedicalRecord(record: MedicalRecordInsertDto): Result<MedicalRecordDto> {
        return try {
            val newRecord = client.from("medical_records")
                .insert(record) {
                    select()
                }
                .decodeSingle<MedicalRecordDto>()

            Result.success(newRecord)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd addMedicalRecord: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun createPrescription(prescription: PrescriptionInsertDto): Result<PrescriptionDto> {
        return try {
            val created = client.from("prescriptions")
                .insert(prescription) {
                    select()
                }
                .decodeSingle<PrescriptionDto>()

            Result.success(created)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd createPrescription: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun createAppointment(appointment: AppointmentInsertDto): Result<AppointmentDto> {
        return try {
            val created = client.from("appointments")
                .insert(appointment) {
                    select()
                }
                .decodeSingle<AppointmentDto>()
            Result.success(created)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd createAppointment: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAppointmentsForDoctor(doctorId: String): Result<List<AppointmentWithPatientDto>> {
        return try {
            val appointments = client.from("appointments")
                .select {
                    filter {
                        eq("doctor_id", doctorId)
                    }
                }
                .decodeList<AppointmentDto>()

            if (appointments.isEmpty()) {
                return Result.success(emptyList())
            }

            val patientIds = appointments.map { it.patient_id }.distinct()
            val patientsMap = client.from("profiles")
                .select {
                    filter {
                        isIn("id", patientIds)
                    }
                }
                .decodeList<ProfileDto>()
                .associateBy { it.id }

            val resultList = appointments.map { appt ->
                val patient = patientsMap[appt.patient_id]
                val name = if (patient != null) {
                    "${patient.first_name} ${patient.last_name}"
                } else {
                    "Pacjent ID: ${appt.patient_id.take(8)}"
                }
                AppointmentWithPatientDto(
                    id = appt.id,
                    patient_id = appt.patient_id,
                    doctor_id = appt.doctor_id,
                    clinic_id = appt.clinic_id,
                    scheduled_at = appt.scheduled_at,
                    status = appt.status,
                    notes = appt.notes,
                    created_at = appt.created_at,
                    patient_name = name
                )
            }.sortedBy { it.scheduled_at }

            Result.success(resultList)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd getAppointmentsForDoctor: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateAppointmentStatus(appointmentId: String, newStatus: String): Result<Unit> {
        return try {
            client.from("appointments")
                .update(
                    mapOf("status" to newStatus)
                ) {
                    filter {
                        eq("id", appointmentId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd updateAppointmentStatus: ${e.message}", e)
            Result.failure(e)
        }
    }
}

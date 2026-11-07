package pl.polsl.mobilepatient.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import pl.polsl.mobilepatient.data.dto.AppointmentDto
import pl.polsl.mobilepatient.data.dto.AppointmentWithPatientDto
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
            Result.failure(e)
        }
    }
}

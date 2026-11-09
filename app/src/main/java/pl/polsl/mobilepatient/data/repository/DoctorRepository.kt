package pl.polsl.mobilepatient.data.repository

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlin.time.Duration.Companion.hours
import pl.polsl.mobilepatient.data.dto.AppointmentDto
import pl.polsl.mobilepatient.data.dto.AppointmentInsertDto
import pl.polsl.mobilepatient.data.dto.AppointmentWithPatientDto
import pl.polsl.mobilepatient.data.dto.ClinicDto
import pl.polsl.mobilepatient.data.dto.DoctorPatientRelationDto
import pl.polsl.mobilepatient.data.dto.MedicalFileDto
import pl.polsl.mobilepatient.data.dto.MedicalFileInsertDto
import pl.polsl.mobilepatient.data.dto.MedicalRecordDto
import pl.polsl.mobilepatient.data.dto.MedicalRecordInsertDto
import pl.polsl.mobilepatient.data.dto.PrescriptionDto
import pl.polsl.mobilepatient.data.dto.PrescriptionInsertDto
import pl.polsl.mobilepatient.data.dto.ProfileDto
import pl.polsl.mobilepatient.data.network.SupabaseProvider

class DoctorRepository {

    private val client = SupabaseProvider.client
    private val auditLogRepository = AuditLogRepository()

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

            try {
                auditLogRepository.logEvent(
                    action = "ASSIGN_PATIENT",
                    entityType = "doctor_patients",
                    entityId = patientId,
                    actorId = doctorId
                )
            } catch (_: Exception) {
            }

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

            try {
                auditLogRepository.logEvent(
                    action = "REMOVE_PATIENT",
                    entityType = "doctor_patients",
                    entityId = patientId,
                    actorId = doctorId
                )
            } catch (_: Exception) {
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

            try {
                auditLogRepository.logEvent(
                    action = "UPDATE_RECORD",
                    entityType = "medical_records",
                    entityId = recordId
                )
            } catch (_: Exception) {
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

            try {
                auditLogRepository.logEvent(
                    action = "DELETE_RECORD",
                    entityType = "medical_records",
                    entityId = recordId
                )
            } catch (_: Exception) {
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

    suspend fun getMedicalFilesForPatient(patientId: String): Result<List<MedicalFileDto>> {
        return try {
            val files = client.from("medical_files")
                .select {
                    filter {
                        eq("patient_id", patientId)
                    }
                }
                .decodeList<MedicalFileDto>()

            val bucket = client.storage.from("medical-scans")
            val filesWithUrls = files.map { file ->
                val signedUrl = try {
                    bucket.publicUrl(file.storage_path)
                } catch (_: Exception) {
                    try {
                        bucket.createSignedUrl(path = file.storage_path, expiresIn = 24.hours)
                    } catch (_: Exception) {
                        null
                    }
                }
                file.copy(signed_url = signedUrl)
            }

            Result.success(filesWithUrls.sortedByDescending { it.created_at ?: "" })
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd getMedicalFilesForPatient: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadMedicalFileForPatient(
        patientId: String,
        doctorId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<MedicalFileDto> {
        return try {
            val cleanName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val storagePath = "$patientId/${System.currentTimeMillis()}_$cleanName"

            val bucket = client.storage.from("medical-scans")
            bucket.upload(path = storagePath, data = bytes) {
                upsert = true
            }

            val insertDto = MedicalFileInsertDto(
                patient_id = patientId,
                uploaded_by = doctorId,
                medical_record_id = null,
                storage_path = storagePath,
                file_name = fileName,
                mime_type = mimeType
            )

            val inserted = client.from("medical_files")
                .insert(insertDto) {
                    select()
                }
                .decodeSingle<MedicalFileDto>()

            try {
                auditLogRepository.logEvent(
                    action = "UPLOAD_FILE",
                    entityType = "medical_files",
                    entityId = inserted.id,
                    actorId = doctorId
                )
            } catch (_: Exception) {
            }

            Result.success(inserted)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd uploadMedicalFileForPatient: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMedicalFileForPatient(fileId: String, storagePath: String): Result<Unit> {
        return try {
            try {
                client.storage.from("medical-scans").delete(listOf(storagePath))
            } catch (_: Exception) {}

            client.from("medical_files")
                .delete {
                    filter {
                        eq("id", fileId)
                    }
                }

            try {
                auditLogRepository.logEvent(
                    action = "DELETE_FILE",
                    entityType = "medical_files",
                    entityId = fileId
                )
            } catch (_: Exception) {
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd deleteMedicalFileForPatient: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun downloadFileBytes(storagePath: String): Result<ByteArray> {
        return try {
            val bucket = client.storage.from("medical-scans")
            val bytes = bucket.downloadAuthenticated(storagePath)
            Result.success(bytes)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd downloadFileBytes: ${e.message}", e)
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

            try {
                auditLogRepository.logEvent(
                    action = "CREATE_RECORD",
                    entityType = "medical_records",
                    entityId = newRecord.id,
                    actorId = record.doctor_id
                )
            } catch (_: Exception) {
            }

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

            try {
                auditLogRepository.logEvent(
                    action = "CREATE_PRESCRIPTION",
                    entityType = "prescriptions",
                    entityId = created.id,
                    actorId = prescription.doctor_id
                )
            } catch (_: Exception) {
            }

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

            try {
                auditLogRepository.logEvent(
                    action = "CREATE_APPOINTMENT",
                    entityType = "appointments",
                    entityId = created.id,
                    actorId = appointment.doctor_id
                )
            } catch (_: Exception) {
            }

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

            val patientIds = appointments.map { patient -> patient.patient_id }.distinct()
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
            }.sortedBy { appt -> appt.scheduled_at }

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

            try {
                auditLogRepository.logEvent(
                    action = "UPDATE_APPOINTMENT_STATUS",
                    entityType = "appointments",
                    entityId = appointmentId
                )
            } catch (_: Exception) {
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Błąd updateAppointmentStatus: ${e.message}", e)
            Result.failure(e)
        }
    }
}

package pl.polsl.mobilepatient.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import pl.polsl.mobilepatient.data.dto.AppointmentDto
import pl.polsl.mobilepatient.data.dto.AppointmentInsertDto
import pl.polsl.mobilepatient.data.dto.ClinicDto
import pl.polsl.mobilepatient.data.dto.MedicalFileDto
import pl.polsl.mobilepatient.data.dto.MedicalFileInsertDto
import pl.polsl.mobilepatient.data.dto.MedicalRecordDto
import pl.polsl.mobilepatient.data.dto.PatientAppointmentDetailsDto
import pl.polsl.mobilepatient.data.dto.PrescriptionDto
import pl.polsl.mobilepatient.data.dto.ProfileDto
import pl.polsl.mobilepatient.data.network.SupabaseProvider
import kotlin.time.Duration.Companion.hours

class PatientRepository {

    private val client = SupabaseProvider.client

    suspend fun getCurrentUserId(): String? {
        val direct = client.auth.currentUserOrNull()?.id
            ?: client.auth.currentSessionOrNull()?.user?.id
        if (!direct.isNullOrBlank()) {
            SupabaseProvider.cachedUserId = direct
            return direct
        }

        try {
            client.auth.awaitInitialization()
        } catch (_: Exception) {
        }

        val afterInit = client.auth.currentUserOrNull()?.id
            ?: client.auth.currentSessionOrNull()?.user?.id
        if (!afterInit.isNullOrBlank()) {
            SupabaseProvider.cachedUserId = afterInit
            return afterInit
        }

        return SupabaseProvider.cachedUserId
    }

    suspend fun getMyMedicalRecords(): Result<List<MedicalRecordDto>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(IllegalStateException("Brak aktywnej sesji użytkownika."))

            val records = client.from("medical_records")
                .select {
                    filter {
                        eq("patient_id", userId)
                    }
                }
                .decodeList<MedicalRecordDto>()

            val sorted = records.sortedByDescending { it.created_at ?: "" }
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyPrescriptions(): Result<List<PrescriptionDto>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(IllegalStateException("Brak aktywnej sesji użytkownika."))

            val prescriptions = client.from("prescriptions")
                .select {
                    filter {
                        eq("patient_id", userId)
                    }
                }
                .decodeList<PrescriptionDto>()

            val sorted = prescriptions.sortedByDescending { it.issued_at ?: it.valid_until }
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fulfillPrescription(prescriptionId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(IllegalStateException("Brak aktywnej sesji użytkownika."))

            client.from("prescriptions")
                .update({
                    set("status", "fulfilled")
                }) {
                    filter {
                        eq("id", prescriptionId)
                        eq("patient_id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyMedicalFiles(): Result<List<MedicalFileDto>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(IllegalStateException("Brak aktywnej sesji użytkownika."))

            val files = client.from("medical_files")
                .select {
                    filter {
                        eq("patient_id", userId)
                    }
                }
                .decodeList<MedicalFileDto>()

            val bucket = client.storage.from("skany-medyczne")
            val filesWithUrls = files.map { file ->
                val signedUrl = try {
                    bucket.createSignedUrl(path = file.storage_path, expiresIn = 24.hours)
                } catch (e: Exception) {
                    android.util.Log.e(
                        "PatientRepository",
                        "Błąd createSignedUrl dla ${file.storage_path}",
                        e
                    )
                    try {
                        bucket.publicUrl(file.storage_path)
                    } catch (_: Exception) {
                        null
                    }
                }
                file.copy(signed_url = signedUrl)
            }

            val sorted = filesWithUrls.sortedByDescending { it.created_at ?: "" }
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFileBytes(storagePath: String): Result<ByteArray> {
        return try {
            val bucket = client.storage.from("skany-medyczne")
            val bytes = bucket.downloadAuthenticated(storagePath)
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMedicalFile(fileId: String, storagePath: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(IllegalStateException("Brak aktywnej sesji użytkownika."))

            try {
                client.storage.from("skany-medyczne").delete(listOf(storagePath))
            } catch (e: Exception) {
                android.util.Log.w(
                    "PatientRepository",
                    "Plik $storagePath mógł zostać usunięty ręcznie ze storage: ${e.message}"
                )
            }

            client.from("medical_files")
                .delete {
                    filter {
                        eq("id", fileId)
                        eq("patient_id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadMedicalScan(
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<MedicalFileDto> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(IllegalStateException("Brak aktywnej sesji użytkownika."))

            val cleanName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val storagePath = "$userId/${System.currentTimeMillis()}_$cleanName"

            val bucket = client.storage.from("skany-medyczne")
            bucket.upload(path = storagePath, data = bytes) {
                upsert = true
            }

            val insertDto = MedicalFileInsertDto(
                patient_id = userId,
                uploaded_by = userId,
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

            val signedUrl = try {
                bucket.createSignedUrl(path = storagePath, expiresIn = 24.hours)
            } catch (_: Exception) {
                null
            }

            Result.success(inserted.copy(signed_url = signedUrl))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableClinics(): Result<List<ClinicDto>> {
        return try {
            val clinics = client.from("clinics")
                .select()
                .decodeList<ClinicDto>()
            Result.success(clinics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableDoctors(): Result<List<ProfileDto>> {
        return try {
            val doctors = client.from("profiles")
                .select {
                    filter {
                        eq("role", "doctor")
                    }
                }
                .decodeList<ProfileDto>()
            Result.success(doctors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyPatientAppointments(): Result<List<PatientAppointmentDetailsDto>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(IllegalStateException("Brak aktywnej sesji użytkownika."))

            val appointments = client.from("appointments")
                .select {
                    filter {
                        eq("patient_id", userId)
                    }
                }
                .decodeList<AppointmentDto>()

            val clinics = getAvailableClinics().getOrDefault(emptyList()).associateBy { it.id }
            val doctors = getAvailableDoctors().getOrDefault(emptyList()).associateBy { it.id }

            val detailed = appointments.map { app ->
                val clinic = clinics[app.clinic_id]
                val doctor = doctors[app.doctor_id]
                val docName =
                    if (doctor != null) "dr ${doctor.first_name} ${doctor.last_name}" else "Lekarz prowadzący"

                PatientAppointmentDetailsDto(
                    id = app.id,
                    patient_id = app.patient_id,
                    doctor_id = app.doctor_id,
                    clinic_id = app.clinic_id,
                    scheduled_at = app.scheduled_at,
                    status = app.status,
                    notes = app.notes,
                    created_at = app.created_at,
                    doctor_name = docName,
                    clinic_name = clinic?.name ?: "Placówka medyczna",
                    clinic_address = clinic?.address
                )
            }

            val sorted = detailed.sortedByDescending { it.scheduled_at }
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bookAppointment(appointment: AppointmentInsertDto): Result<AppointmentDto> {
        return try {
            val inserted = client.from("appointments")
                .insert(appointment) {
                    select()
                }
                .decodeSingle<AppointmentDto>()
            Result.success(inserted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelAppointment(appointmentId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(IllegalStateException("Brak aktywnej sesji użytkownika."))

            client.from("appointments")
                .update({
                    set("status", "cancelled")
                }) {
                    filter {
                        eq("id", appointmentId)
                        eq("patient_id", userId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyProfile(): Result<ProfileDto> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(IllegalStateException("Brak aktywnej sesji użytkownika."))
            val email = client.auth.currentUserOrNull()?.email
                ?: client.auth.currentSessionOrNull()?.user?.email

            val profile = client.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()
                ?: return Result.failure(IllegalStateException("Nie znaleziono profilu pacjenta."))

            Result.success(profile.copy(email = email))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

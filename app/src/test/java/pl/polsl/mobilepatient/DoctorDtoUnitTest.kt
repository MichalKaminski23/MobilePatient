package pl.polsl.mobilepatient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import pl.polsl.mobilepatient.data.dto.AppointmentWithPatientDto
import pl.polsl.mobilepatient.data.dto.MedicalRecordInsertDto
import pl.polsl.mobilepatient.data.dto.PrescriptionInsertDto
import pl.polsl.mobilepatient.data.dto.ProfileDto

class DoctorDtoUnitTest {

    @Test
    fun testPatientProfileDtoCreation() {
        val patient = ProfileDto(
            id = "patient-123",
            first_name = "Jan",
            last_name = "Kowalski",
            pesel = "90010112345",
            phone = "123456789",
            address = "ul. Prosta 1, Katowice"
        )

        assertEquals("patient-123", patient.id)
        assertEquals("Jan", patient.first_name)
        assertEquals("Kowalski", patient.last_name)
        assertEquals("90010112345", patient.pesel)
    }

    @Test
    fun testMedicalRecordInsertDtoCreation() {
        val record = MedicalRecordInsertDto(
            patient_id = "patient-123",
            doctor_id = "doctor-456",
            record_type = "visit",
            description = "Pacjent zgłasza ból głowy. Przepisano lek przeciwbólowy."
        )

        assertEquals("visit", record.record_type)
        assertEquals("patient-123", record.patient_id)
        assertEquals("doctor-456", record.doctor_id)
        assertNotNull(record.description)
    }

    @Test
    fun testPrescriptionInsertDtoCreation() {
        val prescription = PrescriptionInsertDto(
            patient_id = "patient-123",
            doctor_id = "doctor-456",
            code = "1234",
            medications = "Paracetamol 500mg - 2x1",
            valid_until = "2026-12-31"
        )

        assertEquals("1234", prescription.code)
        assertEquals("active", prescription.status)
        assertEquals("2026-12-31", prescription.valid_until)
    }

    @Test
    fun testAppointmentWithPatientDtoFormatting() {
        val appointment = AppointmentWithPatientDto(
            id = "appt-001",
            patient_id = "patient-123",
            doctor_id = "doctor-456",
            clinic_id = "clinic-789",
            scheduled_at = "2026-04-10T10:00:00",
            status = "scheduled",
            patient_name = "Jan Kowalski"
        )

        assertEquals("Jan Kowalski", appointment.patient_name)
        assertEquals("scheduled", appointment.status)
    }
}

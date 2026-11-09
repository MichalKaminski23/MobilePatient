package pl.polsl.mobilepatient.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppointmentReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_APPOINTMENT_REMINDER) {
            val appointmentId = intent.getStringExtra(EXTRA_APPOINTMENT_ID) ?: return
            val clinicName = intent.getStringExtra(EXTRA_CLINIC_NAME)
            val doctorName = intent.getStringExtra(EXTRA_DOCTOR_NAME)
            val scheduledAt = intent.getStringExtra(EXTRA_SCHEDULED_AT) ?: ""

            NotificationHelper.showAppointmentNotification(
                context = context,
                appointmentId = appointmentId,
                clinicName = clinicName,
                doctorName = doctorName,
                scheduledAt = scheduledAt
            )
        }
    }

    companion object {
        const val ACTION_APPOINTMENT_REMINDER = "pl.polsl.mobilepatient.ACTION_APPOINTMENT_REMINDER"
        const val EXTRA_APPOINTMENT_ID = "extra_appointment_id"
        const val EXTRA_CLINIC_NAME = "extra_clinic_name"
        const val EXTRA_DOCTOR_NAME = "extra_doctor_name"
        const val EXTRA_SCHEDULED_AT = "extra_scheduled_at"
    }
}

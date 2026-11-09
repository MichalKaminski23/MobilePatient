package pl.polsl.mobilepatient.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pl.polsl.mobilepatient.MainActivity
import pl.polsl.mobilepatient.R
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_APPOINTMENTS_ID = "channel_appointments"
    private const val CHANNEL_APPOINTMENTS_NAME = "Przypomnienia o wizytach"
    private const val CHANNEL_APPOINTMENTS_DESC =
        "Powiadomienia o zbliżających się konsultacjach medycznych"

    const val NOTIFICATION_TEST_ID = 99999

    fun createNotificationChannels(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_APPOINTMENTS_ID,
            CHANNEL_APPOINTMENTS_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_APPOINTMENTS_DESC
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showAppointmentNotification(
        context: Context,
        appointmentId: String,
        clinicName: String?,
        doctorName: String?,
        scheduledAt: String
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedDate = formatScheduledDate(scheduledAt)

        val publicNotification = NotificationCompat.Builder(context, CHANNEL_APPOINTMENTS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Mobilny Pacjent")
            .setContentText("Masz zaplanowaną wizytę lekarską")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val privateTitle = "Przypomnienie o wizycie lekarskiej"
        val privateSummary = buildString {
            append("Termin: $formattedDate")
            if (!clinicName.isNullOrBlank()) {
                append(" • $clinicName")
            }
        }
        val detailedText = buildString {
            append("Termin konsultacji: $formattedDate\n")
            if (!clinicName.isNullOrBlank()) {
                append("Placówka: $clinicName\n")
            }
            if (!doctorName.isNullOrBlank()) {
                append("Lekarz: $doctorName\n")
            }
            append("Pamiętaj o zabraniu dokumentu tożsamości.")
        }

        val privateNotification = NotificationCompat.Builder(context, CHANNEL_APPOINTMENTS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(privateTitle)
            .setContentText(privateSummary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(appointmentId.hashCode(), privateNotification)
        } catch (_: SecurityException) {
        }
    }

    fun showTestNotification(context: Context) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_TEST_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val publicNotification = NotificationCompat.Builder(context, CHANNEL_APPOINTMENTS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Mobilny Pacjent")
            .setContentText("Powiadomienie testowe")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_APPOINTMENTS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Mobilny Pacjent — Test Powiadomień")
            .setContentText("Powiadomienia w zasobniku systemowym działają prawidłowo.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Powiadomienia systemowe są w pełni aktywne.\n" +
                            "Aplikacja będzie bezpiecznie przypominać o terminach konsultacji medycznych."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_TEST_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun scheduleAppointmentReminder(
        context: Context,
        appointmentId: String,
        clinicName: String?,
        doctorName: String?,
        scheduledAt: String,
        delaySeconds: Long? = null
    ): Boolean {
        val alarmManager =
            (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager) ?: return false

        val triggerMillis = if (delaySeconds != null) {
            System.currentTimeMillis() + (delaySeconds * 1000)
        } else {
            val appointmentMillis = parseToEpochMillis(scheduledAt) ?: return false
            val reminderTime = appointmentMillis - (2 * 60 * 60 * 1000)
            val now = System.currentTimeMillis()
            when {
                reminderTime > now -> reminderTime
                appointmentMillis > now -> now + 5000
                else -> return false
            }
        }

        val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            action = AppointmentReminderReceiver.ACTION_APPOINTMENT_REMINDER
            putExtra(AppointmentReminderReceiver.EXTRA_APPOINTMENT_ID, appointmentId)
            putExtra(AppointmentReminderReceiver.EXTRA_CLINIC_NAME, clinicName)
            putExtra(AppointmentReminderReceiver.EXTRA_DOCTOR_NAME, doctorName)
            putExtra(AppointmentReminderReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    } catch (_: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            true
        } catch (_: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun cancelAppointmentReminder(context: Context, appointmentId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            action = AppointmentReminderReceiver.ACTION_APPOINTMENT_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        NotificationManagerCompat.from(context).cancel(appointmentId.hashCode())
    }

    private fun parseToEpochMillis(scheduledAt: String): Long? {
        return try {
            val clean = scheduledAt.replace(" ", "T").take(19)
            val ldt = LocalDateTime.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    private fun formatScheduledDate(scheduledAt: String): String {
        return try {
            val clean = scheduledAt.replace(" ", "T").take(19)
            val ldt = LocalDateTime.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val outputFormatter = DateTimeFormatter.ofPattern(
                "dd.MM.yyyy 'o godz.' HH:mm",
                Locale.forLanguageTag("pl")
            )
            ldt.format(outputFormatter)
        } catch (_: Exception) {
            scheduledAt.replace("T", " ").take(16)
        }
    }
}

package id.djawadwipa.manajemenkontrakan.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import id.djawadwipa.manajemenkontrakan.data.local.AppSettingEntity
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    const val CHANNEL_ID = "due_reminders"
    const val NOTIFICATION_ID = 1001
    private const val PERIODIC_WORK_NAME = "daily-due-reminder"
    private const val IMMEDIATE_WORK_NAME = "immediate-due-reminder"

    fun sync(
        context: Context,
        settings: AppSettingEntity,
    ) {
        val workManager = WorkManager.getInstance(context)
        if (!settings.notificationEnabled) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
            return
        }

        createChannel(context)
        val request = PeriodicWorkRequestBuilder<DueReminderWorker>(
            24,
            TimeUnit.HOURS,
        )
            .setInitialDelay(
                initialDelayMillis(settings.notificationHour),
                TimeUnit.MILLISECONDS,
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun runNow(context: Context) {
        createChannel(context)
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DueReminderWorker>().build(),
        )
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Jatuh tempo dan tunggakan",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description =
                "Pengingat tagihan yang mendekati jatuh tempo atau menunggak"
        }
        manager.createNotificationChannel(channel)
    }

    private fun initialDelayMillis(hour: Int): Long {
        val now = ZonedDateTime.now()
        var target = now
            .withHour(hour.coerceIn(0, 23))
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target).toMillis().coerceAtLeast(0L)
    }
}

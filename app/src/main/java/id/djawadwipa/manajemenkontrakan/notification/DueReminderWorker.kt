package id.djawadwipa.manajemenkontrakan.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.djawadwipa.manajemenkontrakan.MainActivity
import id.djawadwipa.manajemenkontrakan.R
import id.djawadwipa.manajemenkontrakan.data.local.AppDatabase
import id.djawadwipa.manajemenkontrakan.data.local.InvoiceEntity
import id.djawadwipa.manajemenkontrakan.util.toRupiah
import java.time.LocalDate

class DueReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "manajemen_kontrakan.db",
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
            )
            .build()

        return try {
            val settings = database.appSettingDao().get()
                ?: return Result.success()
            if (!settings.notificationEnabled || !canPostNotifications()) {
                return Result.success()
            }

            val today = LocalDate.now().toEpochDay()
            val reminderLimit = today + settings.dueReminderDays
                .coerceIn(0, 30)
            val outstanding = database.invoiceDao().getAll()
                .filter { it.paid < it.amount }
            val overdue = outstanding
                .filter { it.dueDate < today }
                .sortedBy { it.dueDate }
            val dueSoon = outstanding
                .filter { it.dueDate in today..reminderLimit }
                .sortedBy { it.dueDate }

            if (overdue.isEmpty() && dueSoon.isEmpty()) {
                NotificationManagerCompat.from(applicationContext)
                    .cancel(NotificationScheduler.NOTIFICATION_ID)
                return Result.success()
            }

            showNotification(
                overdue = overdue,
                dueSoon = dueSoon,
                today = today,
            )
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        } finally {
            database.close()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        overdue: List<InvoiceEntity>,
        dueSoon: List<InvoiceEntity>,
        today: Long,
    ) {
        NotificationScheduler.createChannel(applicationContext)
        val title = when {
            overdue.isNotEmpty() && dueSoon.isNotEmpty() ->
                "${overdue.size} tunggakan • ${dueSoon.size} segera jatuh tempo"
            overdue.isNotEmpty() ->
                "${overdue.size} tagihan menunggak"
            else ->
                "${dueSoon.size} tagihan segera jatuh tempo"
        }

        val style = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
        (overdue + dueSoon)
            .distinctBy { it.id }
            .take(6)
            .forEach { invoice ->
                val remaining = (invoice.amount - invoice.paid)
                    .coerceAtLeast(0L)
                val timing = if (invoice.dueDate < today) {
                    "terlambat ${today - invoice.dueDate} hari"
                } else {
                    "jatuh tempo ${invoice.dueDate - today} hari lagi"
                }
                style.addLine(
                    "${invoice.unitId} • ${remaining.toRupiah()} • $timing",
                )
            }

        val intent = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationScheduler.CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(
                "Buka aplikasi untuk melihat detail tagihan.",
            )
            .setStyle(style)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(
            NotificationScheduler.NOTIFICATION_ID,
            notification,
        )
    }

    private fun canPostNotifications(): Boolean {
        if (!NotificationManagerCompat.from(applicationContext)
                .areNotificationsEnabled()
        ) {
            return false
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }
}

package id.djawadwipa.manajemenkontrakan.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import id.djawadwipa.manajemenkontrakan.data.local.AppSettingEntity
import id.djawadwipa.manajemenkontrakan.notification.NotificationScheduler
import id.djawadwipa.manajemenkontrakan.ui.components.ScreenHeader

@Composable
fun NotificationSettingsScreen(
    settings: AppSettingEntity,
    onBack: () -> Unit,
    onSave: (AppSettingEntity) -> Unit,
) {
    val context = LocalContext.current
    var enabled by remember(settings.notificationEnabled) {
        mutableStateOf(settings.notificationEnabled)
    }
    var reminderDays by remember(settings.dueReminderDays) {
        mutableStateOf(settings.dueReminderDays.toString())
    }
    var notificationHour by remember(settings.notificationHour) {
        mutableStateOf(settings.notificationHour.toString())
    }
    var pendingSettings by remember {
        mutableStateOf<AppSettingEntity?>(null)
    }

    fun permissionGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        pendingSettings?.let { candidate ->
            onSave(
                candidate.copy(
                    notificationEnabled = granted,
                ),
            )
            enabled = granted
        }
        pendingSettings = null
    }

    val daysValue = reminderDays.toIntOrNull()
    val hourValue = notificationHour.toIntOrNull()
    val valid = daysValue != null && daysValue in 0..30 &&
        hourValue != null && hourValue in 0..23
    val hasPermission = permissionGranted()

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                    )
                }
                ScreenHeader(
                    title = "Notifikasi Tagihan",
                    subtitle = "Pengingat jatuh tempo dan tunggakan",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Aktifkan pengingat",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Aplikasi memeriksa tagihan sekali sehari.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                        )
                    }

                    OutlinedTextField(
                        value = reminderDays,
                        onValueChange = {
                            reminderDays = it.filter(Char::isDigit)
                        },
                        label = { Text("Ingatkan sebelum jatuh tempo") },
                        suffix = { Text("hari") },
                        supportingText = {
                            Text("0–30 hari sebelum jatuh tempo")
                        },
                        isError = daysValue == null || daysValue !in 0..30,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = notificationHour,
                        onValueChange = {
                            notificationHour = it.filter(Char::isDigit)
                        },
                        label = { Text("Jam pengingat") },
                        suffix = { Text(":00") },
                        supportingText = {
                            Text("Gunakan format 0–23")
                        },
                        isError = hourValue == null || hourValue !in 0..23,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        if (hasPermission) {
                            "Izin notifikasi: diberikan"
                        } else {
                            "Izin notifikasi belum diberikan. Android akan meminta izin saat pengingat diaktifkan."
                        },
                        color = if (hasPermission) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Button(
                        enabled = valid,
                        onClick = {
                            val candidate = settings.copy(
                                notificationEnabled = enabled,
                                dueReminderDays = requireNotNull(daysValue),
                                notificationHour = requireNotNull(hourValue),
                            )
                            if (
                                enabled &&
                                Build.VERSION.SDK_INT >=
                                    Build.VERSION_CODES.TIRAMISU &&
                                !permissionGranted()
                            ) {
                                pendingSettings = candidate
                                permissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS,
                                )
                            } else {
                                onSave(candidate)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Text(" Simpan pengingat")
                    }

                    OutlinedButton(
                        enabled = settings.notificationEnabled &&
                            permissionGranted(),
                        onClick = {
                            NotificationScheduler.runNow(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                        )
                        Text(" Periksa dan kirim sekarang")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                        )
                        Text(
                            "Cara kerja",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(
                        "Pengingat hanya menghitung tagihan yang belum lunas. " +
                            "Notifikasi merangkum tagihan menunggak dan tagihan " +
                            "yang akan jatuh tempo dalam rentang hari pilihan.",
                    )
                    Text(
                        "Pemeriksaan tetap lokal di perangkat dan tidak " +
                            "mengirim data kontrakan ke internet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

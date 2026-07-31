package id.djawadwipa.manajemenkontrakan.ui.screens

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import id.djawadwipa.manajemenkontrakan.data.local.AppSettingEntity
import id.djawadwipa.manajemenkontrakan.security.PinSecurity
import id.djawadwipa.manajemenkontrakan.ui.components.ScreenHeader

private enum class PinAction { SET, CHANGE, DISABLE }

@Composable
fun SecuritySettingsScreen(
    settings: AppSettingEntity,
    onBack: () -> Unit,
    onSave: (AppSettingEntity) -> Unit,
    onLockNow: () -> Unit,
) {
    val context = LocalContext.current
    val pinConfigured = settings.pinSalt.isNotBlank() &&
        settings.pinHash.isNotBlank()
    val biometricAvailable = remember(context) {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK,
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
    var pinAction by remember { mutableStateOf<PinAction?>(null) }

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
                    title = "Keamanan & Tampilan",
                    subtitle = "PIN, biometrik, auto-lock, dan tema",
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Palette, contentDescription = null)
                        Text(
                            "Tema aplikasi",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Text(
                        "Pilihan disimpan dan tetap digunakan setelah aplikasi atau perangkat dimulai ulang.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            "SYSTEM" to "Sistem",
                            "LIGHT" to "Terang",
                            "DARK" to "Gelap",
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = settings.themeMode == value,
                                onClick = {
                                    onSave(settings.copy(themeMode = value))
                                },
                                label = { Text(label, maxLines = 1) },
                                leadingIcon = if (value == "DARK") {
                                    {
                                        Icon(
                                            Icons.Default.DarkMode,
                                            contentDescription = null,
                                        )
                                    }
                                } else {
                                    null
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Text(
                            "Kunci aplikasi",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Text(
                        if (pinConfigured) {
                            "PIN tersimpan sebagai hash aman; angka PIN asli tidak disimpan."
                        } else {
                            "Buat PIN 4–8 angka untuk melindungi data kontrakan."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Aktifkan kunci PIN")
                            Text(
                                if (settings.lockEnabled) "Aktif" else "Nonaktif",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.lockEnabled,
                            onCheckedChange = { enabled ->
                                when {
                                    enabled && pinConfigured -> {
                                        onSave(settings.copy(lockEnabled = true))
                                    }
                                    enabled -> pinAction = PinAction.SET
                                    else -> pinAction = PinAction.DISABLE
                                }
                            },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = null,
                                )
                                Text("Buka dengan biometrik")
                            }
                            Text(
                                if (biometricAvailable) {
                                    "Biometrik perangkat tersedia"
                                } else {
                                    "Biometrik belum tersedia atau belum didaftarkan"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.biometricEnabled,
                            enabled = settings.lockEnabled &&
                                pinConfigured &&
                                biometricAvailable,
                            onCheckedChange = {
                                onSave(settings.copy(biometricEnabled = it))
                            },
                        )
                    }

                    Text(
                        "Kunci otomatis setelah aplikasi ditinggalkan",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    listOf(
                        0 to "Segera",
                        1 to "1 menit",
                        5 to "5 menit",
                        15 to "15 menit",
                    ).chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowItems.forEach { (minutes, label) ->
                                FilterChip(
                                    selected = settings.autoLockMinutes == minutes,
                                    onClick = {
                                        onSave(
                                            settings.copy(
                                                autoLockMinutes = minutes,
                                            ),
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            pinAction = if (pinConfigured) {
                                PinAction.CHANGE
                            } else {
                                PinAction.SET
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Text(if (pinConfigured) " Ubah PIN" else " Buat PIN")
                    }

                    if (settings.lockEnabled && pinConfigured) {
                        OutlinedButton(
                            onClick = onLockNow,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null)
                            Text(" Kunci aplikasi sekarang")
                        }
                    }
                }
            }
        }
    }

    pinAction?.let { action ->
        PinDialog(
            action = action,
            settings = settings,
            onDismiss = { pinAction = null },
            onCompleted = { updatedSettings, lockNow ->
                onSave(updatedSettings)
                pinAction = null
                if (lockNow) onLockNow()
            },
        )
    }
}

@Composable
private fun PinDialog(
    action: PinAction,
    settings: AppSettingEntity,
    onDismiss: () -> Unit,
    onCompleted: (AppSettingEntity, Boolean) -> Unit,
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val requiresCurrent = action != PinAction.SET
    val requiresNew = action != PinAction.DISABLE
    val valid = (!requiresCurrent || PinSecurity.isValidFormat(currentPin)) &&
        (!requiresNew || (
            PinSecurity.isValidFormat(newPin) &&
                newPin == confirmation
            ))

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (action) {
                    PinAction.SET -> "Buat PIN aplikasi"
                    PinAction.CHANGE -> "Ubah PIN aplikasi"
                    PinAction.DISABLE -> "Nonaktifkan kunci aplikasi"
                },
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        when (action) {
                            PinAction.SET -> "Gunakan 4–8 angka yang mudah Anda ingat tetapi sulit ditebak."
                            PinAction.CHANGE -> "Verifikasi PIN lama, lalu masukkan PIN baru."
                            PinAction.DISABLE -> "Masukkan PIN saat ini untuk menonaktifkan perlindungan aplikasi."
                        },
                    )
                }
                if (requiresCurrent) {
                    item {
                        PinField(
                            value = currentPin,
                            onValueChange = {
                                currentPin = it
                                error = null
                            },
                            label = "PIN saat ini",
                            isError = error != null,
                        )
                    }
                }
                if (requiresNew) {
                    item {
                        PinField(
                            value = newPin,
                            onValueChange = {
                                newPin = it
                                error = null
                            },
                            label = "PIN baru",
                            isError = false,
                        )
                    }
                    item {
                        PinField(
                            value = confirmation,
                            onValueChange = {
                                confirmation = it
                                error = null
                            },
                            label = "Ulangi PIN baru",
                            isError = confirmation.isNotEmpty() &&
                                confirmation != newPin,
                        )
                    }
                }
                error?.let { message ->
                    item {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    if (
                        requiresCurrent &&
                        !PinSecurity.verify(
                            pin = currentPin,
                            encodedSalt = settings.pinSalt,
                            encodedHash = settings.pinHash,
                        )
                    ) {
                        error = "PIN saat ini salah."
                        return@Button
                    }
                    when (action) {
                        PinAction.DISABLE -> {
                            onCompleted(
                                settings.copy(
                                    lockEnabled = false,
                                    pinSalt = "",
                                    pinHash = "",
                                    biometricEnabled = false,
                                ),
                                false,
                            )
                        }
                        PinAction.SET,
                        PinAction.CHANGE,
                        -> {
                            val record = PinSecurity.create(newPin)
                            onCompleted(
                                settings.copy(
                                    lockEnabled = true,
                                    pinSalt = record.salt,
                                    pinHash = record.hash,
                                ),
                                true,
                            )
                        }
                    }
                },
            ) {
                Text(
                    when (action) {
                        PinAction.SET -> "Aktifkan"
                        PinAction.CHANGE -> "Simpan PIN"
                        PinAction.DISABLE -> "Nonaktifkan"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
) {
    var pinVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(it.filter(Char::isDigit).take(8))
        },
        label = { Text(label) },
        visualTransformation = if (pinVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { pinVisible = !pinVisible }) {
                Icon(
                    if (pinVisible) {
                        Icons.Default.VisibilityOff
                    } else {
                        Icons.Default.Visibility
                    },
                    contentDescription = if (pinVisible) {
                        "Sembunyikan PIN"
                    } else {
                        "Tampilkan PIN"
                    },
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
        ),
        supportingText = {
            Text("4–8 angka")
        },
        isError = isError,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

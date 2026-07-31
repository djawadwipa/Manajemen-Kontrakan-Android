package id.djawadwipa.manajemenkontrakan.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import id.djawadwipa.manajemenkontrakan.R
import id.djawadwipa.manajemenkontrakan.data.local.AppSettingEntity
import id.djawadwipa.manajemenkontrakan.security.PinSecurity

private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

@Composable
fun AppLockScreen(
    settings: AppSettingEntity,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val currentOnUnlocked by rememberUpdatedState(onUnlocked)
    var pin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val biometricAvailable = remember(
        activity,
        settings.biometricEnabled,
    ) {
        activity != null &&
            BiometricManager.from(context)
                .canAuthenticate(AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    val authenticateBiometric: () -> Unit = remember(
        activity,
        biometricAvailable,
    ) {
        {
            if (activity != null && biometricAvailable) {
                val prompt = BiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(activity),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult,
                        ) {
                            error = null
                            currentOnUnlocked()
                        }

                        override fun onAuthenticationFailed() {
                            error = "Biometrik tidak dikenali. Coba lagi atau gunakan PIN."
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence,
                        ) {
                            if (
                                errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                                errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                errorCode != BiometricPrompt.ERROR_CANCELED
                            ) {
                                error = errString.toString()
                            }
                        }
                    },
                )
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Buka Manajemen Kontrakan")
                    .setSubtitle("Gunakan sidik jari atau biometrik perangkat")
                    .setAllowedAuthenticators(AUTHENTICATORS)
                    .setNegativeButtonText("Gunakan PIN")
                    .build()
                prompt.authenticate(info)
            }
        }
    }

    LaunchedEffect(settings.biometricEnabled, biometricAvailable) {
        if (settings.biometricEnabled && biometricAvailable) {
            authenticateBiometric()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_mk),
                contentDescription = "Logo Manajemen Kontrakan",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                    )
                    Text(
                        "Aplikasi terkunci",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        "Masukkan PIN aplikasi untuk membuka data kontrakan.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            pin = it.filter(Char::isDigit).take(8)
                            error = null
                        },
                        label = { Text("PIN aplikasi") },
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
                        isError = error != null,
                        supportingText = error?.let { message ->
                            { Text(message) }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        enabled = PinSecurity.isValidFormat(pin),
                        onClick = {
                            if (
                                PinSecurity.verify(
                                    pin = pin,
                                    encodedSalt = settings.pinSalt,
                                    encodedHash = settings.pinHash,
                                )
                            ) {
                                pin = ""
                                error = null
                                currentOnUnlocked()
                            } else {
                                pin = ""
                                error = "PIN salah. Silakan coba lagi."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Buka aplikasi")
                    }
                    if (settings.biometricEnabled && biometricAvailable) {
                        OutlinedButton(
                            onClick = authenticateBiometric,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                            )
                            Text(" Gunakan biometrik")
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? =
    when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }

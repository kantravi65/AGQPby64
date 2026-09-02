package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import com.example.util.BiometricAuthManager
import com.example.util.GoogleDriveSyncManager
import com.example.util.GoogleSignInHelper
import com.example.util.SettingsManager

@Composable
fun LockScreen(
    settingsManager: SettingsManager,
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var isGoogleSignedIn by remember { mutableStateOf(settingsManager.isGoogleSignedIn && settingsManager.googleAccountEmail.isNotBlank()) }
    var googleAccountEmail by remember { mutableStateOf(settingsManager.googleAccountEmail) }
    var googleAccountName by remember { mutableStateOf(settingsManager.googleAccountName) }

    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val isBiometricEnabled = settingsManager.isBiometricEnabled
    val isAppLockEnabled = settingsManager.isAppLockEnabled

    val scope = rememberCoroutineScope()
    var isAuthenticating by remember { mutableStateOf(false) }

    // Google Sign In Activity Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val res = GoogleSignInHelper.handleSignInResult(result.data, context)
        res.onSuccess { account ->
            scope.launch {
                isAuthenticating = true
                errorMessage = "Authenticating with Firebase..."
                val firebaseResult = com.example.util.FirebaseAuthHelper.authenticateWithFirebase(account)
                if (firebaseResult.isFailure) {
                    errorMessage = "Firebase Auth failed: ${firebaseResult.exceptionOrNull()?.message}"
                    com.example.util.FirebaseAuthHelper.signOut()
                    try { GoogleSignInHelper.signOut(context) } catch (_: Exception) {}
                    isAuthenticating = false
                    return@launch
                }

                val email = account.email ?: ""
                errorMessage = "Checking whitelist status..."
                val isWhitelisted = com.example.util.WhitelistManager.isWhitelisted(email)
                if (!isWhitelisted) {
                    errorMessage = "Login failed: Email '$email' is not whitelisted by owners."
                    com.example.util.FirebaseAuthHelper.signOut()
                    try { GoogleSignInHelper.signOut(context) } catch (_: Exception) {}
                    isAuthenticating = false
                    return@launch
                }

                GoogleDriveSyncManager.saveGoogleAccount(context, settingsManager, account)
                isGoogleSignedIn = true
                googleAccountEmail = settingsManager.googleAccountEmail
                googleAccountName = settingsManager.googleAccountName
                errorMessage = ""
                isAuthenticating = false
                Toast.makeText(context, "Welcome, ${account.displayName ?: account.email}!", Toast.LENGTH_SHORT).show()
                if (!isAppLockEnabled) {
                    onUnlockSuccess()
                }
            }
        }.onFailure { err ->
            errorMessage = err.message ?: "Google Sign-In failed"
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    // Automatically trigger biometric prompt on launch if Google signed in and biometric enabled
    LaunchedEffect(isGoogleSignedIn) {
        if (isGoogleSignedIn && isAppLockEnabled && activity != null && BiometricAuthManager.isBiometricHardwareAvailable(context)) {
            BiometricAuthManager.promptBiometricAuth(
                activity = activity,
                title = "Security Authentication",
                subtitle = "Scan fingerprint to access Question Bank",
                negativeButtonText = "Use PIN Code",
                onSuccess = {
                    onUnlockSuccess()
                },
                onError = { err ->
                    errorMessage = err
                },
                onUsePinFallback = {
                    errorMessage = "Enter your 4-digit PIN code"
                }
            )
        } else if (isGoogleSignedIn && !isAppLockEnabled) {
            onUnlockSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isGoogleSignedIn) {
            // GOOGLE OAUTH LOCK GATE SCREEN
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(3.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "Google OAuth Gate",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Google Sign-In Required",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Restricted Question Repository Access Control",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // OAuth Info Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Only authorized Google accounts registered with this application can gain entry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (errorMessage.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Main Google Sign-In Action Button
                Button(
                    onClick = {
                        try {
                            val intent = GoogleSignInHelper.getSignInIntent(context, settingsManager.googleWebClientId)
                            googleSignInLauncher.launch(intent)
                        } catch (e: Exception) {
                            errorMessage = "Error launching Google Sign-In: ${e.message}"
                        }
                    },
                    enabled = !isAuthenticating,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("button_google_login_gate")
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Authenticating...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Sign in with Google", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }


            }
        } else {
            // PIN / BIOMETRIC LOCK SCREEN (WITH GOOGLE ACCOUNT USER BADGE)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Logged-in Google Account Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = googleAccountEmail,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Switch Account",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    GoogleDriveSyncManager.signOutGoogle(context, settingsManager)
                                    isGoogleSignedIn = false
                                }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(3.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (settingsManager.isGoogleSignedIn && settingsManager.googlePhotoUrl.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = settingsManager.googlePhotoUrl,
                            contentDescription = "Profile Picture",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (googleAccountName.isNotBlank()) googleAccountName else settingsManager.userName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Question Repository Security Lock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4 PIN Dots Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("BIO", "0", "DEL")
                    )

                    keys.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            row.forEach { key ->
                                KeypadButton(
                                    label = key,
                                    isBiometricBtn = key == "BIO",
                                    isDeleteBtn = key == "DEL",
                                    isBiometricEnabled = isBiometricEnabled && activity != null,
                                    onClick = {
                                        when (key) {
                                            "DEL" -> {
                                                if (pinInput.isNotEmpty()) {
                                                    pinInput = pinInput.dropLast(1)
                                                    errorMessage = ""
                                                }
                                            }
                                            "BIO" -> {
                                                if (activity != null && isBiometricEnabled) {
                                                    BiometricAuthManager.promptBiometricAuth(
                                                        activity = activity,
                                                        title = "Fingerprint Authentication",
                                                        subtitle = "Scan finger on device sensor",
                                                        onSuccess = { onUnlockSuccess() },
                                                        onError = { err -> errorMessage = err },
                                                        onUsePinFallback = { errorMessage = "Use PIN Code" }
                                                    )
                                                } else {
                                                    Toast.makeText(context, "Fingerprint auth not enabled", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            else -> {
                                                if (pinInput.length < 4) {
                                                    val newPin = pinInput + key
                                                    pinInput = newPin
                                                    errorMessage = ""

                                                    if (newPin.length == 4) {
                                                        if (newPin == settingsManager.pinCode) {
                                                            onUnlockSuccess()
                                                        } else {
                                                            errorMessage = "Incorrect PIN code. Try again."
                                                            pinInput = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }


}

@Composable
private fun KeypadButton(
    label: String,
    isBiometricBtn: Boolean,
    isDeleteBtn: Boolean,
    isBiometricEnabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isBiometricBtn || isDeleteBtn) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = Modifier
            .size(68.dp)
            .testTag("pin_key_$label")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isBiometricBtn -> {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint Scan",
                        tint = if (isBiometricEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                }
                isDeleteBtn -> {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

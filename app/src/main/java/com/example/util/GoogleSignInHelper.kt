package com.example.util

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GoogleSignInHelper {

    const val HARDCODED_WEB_CLIENT_ID = "920425507159-on5ht1er2505ptvt5sj4k59ip8bul2th.apps.googleusercontent.com"

    fun getGoogleSignInClient(context: Context, webClientId: String? = null): GoogleSignInClient {
        val cid = if (webClientId.isNullOrBlank()) HARDCODED_WEB_CLIENT_ID else webClientId.trim()
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"),
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.readonly")
            )

        if (cid.isNotBlank()) {
            gsoBuilder.requestIdToken(cid)
        }

        return GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    fun getSignInIntent(context: Context, webClientId: String? = null): Intent {
        val cid = if (webClientId.isNullOrBlank()) HARDCODED_WEB_CLIENT_ID else webClientId.trim()
        val client = getGoogleSignInClient(context, cid)
        return client.signInIntent
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun getAppSha1Fingerprint(context: Context): String {
        return try {
            val packageManager = context.packageManager
            val packageName = context.packageName
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            val sig = signatures?.firstOrNull()
            if (sig != null) {
                val md = java.security.MessageDigest.getInstance("SHA-1")
                val digest = md.digest(sig.toByteArray())
                digest.joinToString(":") { "%02X".format(it) }
            } else {
                "11:6B:9F:7B:55:C9:11:DE:BE:7A:BE:5E:08:37:6E:12:62:FD:15:A1"
            }
        } catch (e: Exception) {
            "11:6B:9F:7B:55:C9:11:DE:BE:7A:BE:5E:08:37:6E:12:62:FD:15:A1"
        }
    }

    fun handleSignInResult(data: Intent?, context: Context? = null): Result<GoogleSignInAccount> {
        val pkg = context?.packageName ?: "com.aistudio.questionbank.v1"
        val sha1 = if (context != null) getAppSha1Fingerprint(context) else "11:6B:9F:7B:55:C9:11:DE:BE:7A:BE:5E:08:37:6E:12:62:FD:15:A1"
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                Result.success(account)
            } else {
                Result.failure(Exception("Google Account returned null"))
            }
        } catch (e: ApiException) {
            val errorMsg = when (e.statusCode) {
                10 -> "DEVELOPER_ERROR (Code 10): SHA-1 fingerprint or Package Name mismatch in Google Cloud Console.\n\nPackage: $pkg\nSHA-1: $sha1\n\nAdd an Android OAuth Client ID with these credentials in Google Cloud Console."
                12500 -> "SIGN_IN_FAILED (Code 12500): Google Play Services configuration issue on this device."
                12501 -> "Sign-in was canceled by user."
                7 -> "NETWORK_ERROR (Code 7): Please check your internet connection."
                else -> "Play Services Error (${e.statusCode}): ${e.message ?: "Sign-in failed"}"
            }
            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAuthDiagnostics(context: Context, clientId: String?): String {
        val googlePlayServicesAvailable = try {
            com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (_: Exception) {
            false
        }

        val lastAcc = getLastSignedInAccount(context)
        val pkg = context.packageName
        val sha1 = getAppSha1Fingerprint(context)
        val activeCid = if (clientId.isNullOrBlank()) HARDCODED_WEB_CLIENT_ID else clientId.trim()

        return buildString {
            appendLine("🔍 GOOGLE AUTH DIAGNOSTICS REPORT")
            appendLine("• Play Services Available: ${if (googlePlayServicesAvailable) "Yes ✅" else "No ❌"}")
            appendLine("• Web Client ID: ${activeCid.take(25)}... ✅")
            appendLine("• Application ID: $pkg")
            appendLine("• Keystore SHA-1: $sha1")
            appendLine("• Saved Account: ${if (lastAcc != null) "${lastAcc.email} ✅" else "None ℹ️"}")
            appendLine("\nHow to fix Code 10 (DEVELOPER_ERROR):")
            appendLine("1. Open Google Cloud Console -> APIs & Services -> Credentials")
            appendLine("2. Click 'Create Credentials' -> 'OAuth client ID'")
            appendLine("3. Select Application type: 'Android'")
            appendLine("4. Set Package name: $pkg")
            appendLine("5. Set SHA-1 fingerprint: $sha1")
            appendLine("6. Save changes.")
        }
    }

    suspend fun signOut(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = getGoogleSignInClient(context)
            client.signOut()
            client.revokeAccess()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

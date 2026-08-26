package com.example.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.tasks.await

object FirebaseAuthHelper {
    suspend fun authenticateWithFirebase(account: GoogleSignInAccount): Boolean {
        val auth = FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        return try {
            auth.signInWithCredential(credential).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
}

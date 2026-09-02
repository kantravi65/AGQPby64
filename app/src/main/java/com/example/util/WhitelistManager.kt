package com.example.util

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object WhitelistManager {
    val owners = listOf("kantravi65@gmail.com", "myslv409@gmail.com")

    fun isOwner(email: String?): Boolean {
        return email != null && owners.contains(email.trim().lowercase())
    }

    suspend fun isWhitelisted(email: String): Boolean {
        if (isOwner(email)) return true
        val db = FirebaseFirestore.getInstance()
        return try {
            val doc = db.collection("whitelisted_users").document(email).get().await()
            doc.exists()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getWhitelist(): List<String> {
        val db = FirebaseFirestore.getInstance()
        return try {
            val result = db.collection("whitelisted_users").get().await()
            result.documents.mapNotNull { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addEmailToWhitelist(email: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("whitelisted_users").document(email).set(mapOf("email" to email)).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeEmailFromWhitelist(email: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("whitelisted_users").document(email).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}

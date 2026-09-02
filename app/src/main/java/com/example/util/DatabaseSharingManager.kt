package com.example.util

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class SharedDatabaseItem(
    val id: String = "",
    val senderEmail: String = "",
    val senderName: String = "",
    val recipientEmail: String = "",
    val title: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val questionsCount: Int = 0,
    val booksCount: Int = 0,
    val papersCount: Int = 0,
    val payloadJson: String = "",
    val status: String = "shared" // "shared", "imported", "revoked"
)

object DatabaseSharingManager {

    private const val COLLECTION_NAME = "shared_databases"

    /**
     * Share a database package with a recipient email via Firestore
     */
    suspend fun shareDatabasePackage(
        senderEmail: String,
        senderName: String,
        recipientEmail: String,
        title: String,
        description: String,
        payloadJson: String,
        questionsCount: Int,
        booksCount: Int,
        papersCount: Int
    ): Result<String> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val shareId = "share_" + UUID.randomUUID().toString().replace("-", "").take(16)
            val cleanRecipient = recipientEmail.trim().lowercase()
            val cleanSender = senderEmail.trim().lowercase()

            val shareData = hashMapOf(
                "id" to shareId,
                "senderEmail" to cleanSender,
                "senderName" to senderName.trim().ifBlank { "User" },
                "recipientEmail" to cleanRecipient,
                "title" to title.trim().ifBlank { "Shared Question Bank" },
                "description" to description.trim(),
                "timestamp" to System.currentTimeMillis(),
                "questionsCount" to questionsCount,
                "booksCount" to booksCount,
                "papersCount" to papersCount,
                "payloadJson" to payloadJson,
                "status" to "shared"
            )

            db.collection(COLLECTION_NAME)
                .document(shareId)
                .set(shareData)
                .await()

            Result.success(shareId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Listen in real-time to databases shared with the current user's email
     */
    fun getReceivedSharesFlow(userEmail: String): Flow<List<SharedDatabaseItem>> = callbackFlow {
        val cleanEmail = userEmail.trim().lowercase()
        if (cleanEmail.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val db = FirebaseFirestore.getInstance()
        val listener = db.collection(COLLECTION_NAME)
            .whereEqualTo("recipientEmail", cleanEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            SharedDatabaseItem(
                                id = doc.getString("id") ?: doc.id,
                                senderEmail = doc.getString("senderEmail") ?: "",
                                senderName = doc.getString("senderName") ?: "",
                                recipientEmail = doc.getString("recipientEmail") ?: "",
                                title = doc.getString("title") ?: "Shared Question Bank",
                                description = doc.getString("description") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                questionsCount = (doc.getLong("questionsCount") ?: 0L).toInt(),
                                booksCount = (doc.getLong("booksCount") ?: 0L).toInt(),
                                papersCount = (doc.getLong("papersCount") ?: 0L).toInt(),
                                payloadJson = doc.getString("payloadJson") ?: "",
                                status = doc.getString("status") ?: "shared"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.timestamp }

                    trySend(items)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Listen in real-time to databases sent by the current user's email
     */
    fun getSentSharesFlow(userEmail: String): Flow<List<SharedDatabaseItem>> = callbackFlow {
        val cleanEmail = userEmail.trim().lowercase()
        if (cleanEmail.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val db = FirebaseFirestore.getInstance()
        val listener = db.collection(COLLECTION_NAME)
            .whereEqualTo("senderEmail", cleanEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            SharedDatabaseItem(
                                id = doc.getString("id") ?: doc.id,
                                senderEmail = doc.getString("senderEmail") ?: "",
                                senderName = doc.getString("senderName") ?: "",
                                recipientEmail = doc.getString("recipientEmail") ?: "",
                                title = doc.getString("title") ?: "Shared Question Bank",
                                description = doc.getString("description") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                questionsCount = (doc.getLong("questionsCount") ?: 0L).toInt(),
                                booksCount = (doc.getLong("booksCount") ?: 0L).toInt(),
                                papersCount = (doc.getLong("papersCount") ?: 0L).toInt(),
                                payloadJson = doc.getString("payloadJson") ?: "",
                                status = doc.getString("status") ?: "shared"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.timestamp }

                    trySend(items)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Mark a share as imported
     */
    suspend fun markShareAsImported(shareId: String): Result<Unit> {
        return try {
            val db = FirebaseFirestore.getInstance()
            db.collection(COLLECTION_NAME)
                .document(shareId)
                .update("status", "imported")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete / revoke a shared database document
     */
    suspend fun deleteShare(shareId: String): Result<Unit> {
        return try {
            val db = FirebaseFirestore.getInstance()
            db.collection(COLLECTION_NAME)
                .document(shareId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch list of whitelisted user emails and owners to provide auto-complete recipient suggestions
     */
    suspend fun getKnownUserEmails(): List<String> {
        val list = mutableSetOf<String>()
        list.addAll(WhitelistManager.owners)
        try {
            val whitelist = WhitelistManager.getWhitelist()
            list.addAll(whitelist)
        } catch (_: Exception) {}
        return list.filter { it.isNotBlank() }.sorted()
    }
}

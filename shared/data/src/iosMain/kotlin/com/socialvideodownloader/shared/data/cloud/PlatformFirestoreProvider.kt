package com.socialvideodownloader.shared.data.cloud

/**
 * Bridge interface implemented by Swift to provide Firestore CRUD operations
 * to the Kotlin layer.
 *
 * Firestore iOS SDK is Swift-native and cannot be called directly from Kotlin/Native.
 * This interface decouples the Kotlin implementation from the Swift-side SDK calls.
 *
 * Each document in Firestore is a JSON-encoded string (the Kotlin side handles
 * encoding/decoding of [com.socialvideodownloader.core.domain.model.DownloadRecord]).
 *
 * Collection path convention: `users/{uid}/downloads/{documentId}`
 */
interface PlatformFirestoreProvider {

    /**
     * Write a document to Firestore at the given path.
     * [data] is a JSON string representation of the document.
     * Returns true if the write succeeded.
     */
    suspend fun setDocument(collectionPath: String, documentId: String, data: String): Boolean

    /**
     * Delete a document from Firestore.
     * Returns true if the delete succeeded.
     */
    suspend fun deleteDocument(collectionPath: String, documentId: String): Boolean

    /**
     * Fetch all documents from a Firestore collection.
     * Returns a list of JSON string representations.
     */
    suspend fun fetchCollection(collectionPath: String): List<String>

    /**
     * Get a Firestore document by path.
     * Returns the JSON string representation, or null if not found.
     */
    suspend fun getDocument(collectionPath: String, documentId: String): String?

    /**
     * Returns the UID of the currently authenticated user for path construction.
     * Null if not authenticated.
     */
    fun currentUid(): String?
}

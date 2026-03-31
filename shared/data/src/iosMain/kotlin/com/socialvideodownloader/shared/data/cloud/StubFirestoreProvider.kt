package com.socialvideodownloader.shared.data.cloud

/**
 * Stub [PlatformFirestoreProvider] used until the Swift Firebase Firestore implementation is wired.
 *
 * All write operations succeed (return true) but perform no actual cloud storage.
 * Read operations return empty results.
 *
 * Replace by registering the Swift `FirestoreProvider` via `KoinHelper.registerFirestoreProvider()`
 * once Firebase is integrated via CocoaPods.
 */
class StubFirestoreProvider : PlatformFirestoreProvider {
    override suspend fun setDocument(
        collectionPath: String,
        documentId: String,
        jsonData: String,
    ): Boolean = true // stub: pretend write succeeded

    override suspend fun deleteDocument(
        collectionPath: String,
        documentId: String,
    ): Boolean = true // stub: pretend delete succeeded

    override suspend fun fetchCollection(collectionPath: String): List<String> = emptyList()

    override suspend fun getDocument(
        collectionPath: String,
        documentId: String,
    ): String? = null

    override fun currentUid(): String? = null
}

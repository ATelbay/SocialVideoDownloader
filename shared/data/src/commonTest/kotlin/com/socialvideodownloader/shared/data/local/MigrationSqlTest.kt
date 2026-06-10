package com.socialvideodownloader.shared.data.local

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Host-JVM coverage for the migration wiring and SQL, so a regression is caught by the
 * regular `./gradlew test` / `testDebugUnitTest` on every platform — not only by the
 * native-driver [com.socialvideodownloader.shared.data.SyncQueueIndexMigrationTest] in
 * `iosTest` (which the Android unit-test task never runs because the bundled SQLite
 * native library cannot load on the host JVM).
 *
 * The migrations are executed against a [RecordingConnection] that captures the SQL text
 * instead of touching a real database, so this needs no native driver.
 */
class MigrationSqlTest {
    @Test
    fun allMigrationsCoverContiguousVersionsUpTo6() {
        val pairs = ALL_MIGRATIONS.map { it.startVersion to it.endVersion }
        assertEquals(
            listOf(1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6),
            pairs,
            "migration chain must be contiguous 1->6: $pairs",
        )
    }

    @Test
    fun migration5to6_dropsLegacyIndexAndCreatesCanonical() {
        assertEquals(5, MIGRATION_5_6.startVersion)
        assertEquals(6, MIGRATION_5_6.endVersion)

        val conn = RecordingConnection()
        MIGRATION_5_6.migrate(conn)
        val sql = conn.executed.joinToString("\n")

        assertTrue(
            conn.executed.any {
                it.contains("DROP INDEX", ignoreCase = true) &&
                    it.contains("idx_sync_queue_download_op")
            },
            "5->6 must drop the legacy index; got:\n$sql",
        )
        assertTrue(
            conn.executed.any {
                it.contains("CREATE", ignoreCase = true) &&
                    it.contains("UNIQUE INDEX", ignoreCase = true) &&
                    it.contains("index_sync_queue_downloadId_operation") &&
                    it.contains("(downloadId, operation)")
            },
            "5->6 must (re)create the canonical unique index; got:\n$sql",
        )
    }

    @Test
    fun migration4to5_createsCanonicalIndexName() {
        // The KMP 4->5 must create the canonical index name so a fresh install lands on the
        // same schema the 5->6 normalisation targets.
        val conn = RecordingConnection()
        MIGRATION_4_5.migrate(conn)
        assertTrue(
            conn.executed.any {
                it.contains("CREATE", ignoreCase = true) &&
                    it.contains("index_sync_queue_downloadId_operation")
            },
            "4->5 must create the canonical index name; got:\n${conn.executed.joinToString("\n")}",
        )
    }
}

/** Captures executed SQL; everything else is unsupported (the migrations only call execSQL). */
private class RecordingConnection : SQLiteConnection {
    val executed = mutableListOf<String>()

    override fun prepare(sql: String): SQLiteStatement {
        executed += sql
        return NoOpStatement()
    }

    override fun close() {}
}

private class NoOpStatement : SQLiteStatement {
    override fun step(): Boolean = false

    override fun reset() {}

    override fun clearBindings() {}

    override fun close() {}

    override fun bindBlob(
        index: Int,
        value: ByteArray,
    ) = unsupported()

    override fun bindDouble(
        index: Int,
        value: Double,
    ) = unsupported()

    override fun bindLong(
        index: Int,
        value: Long,
    ) = unsupported()

    override fun bindText(
        index: Int,
        value: String,
    ) = unsupported()

    override fun bindNull(index: Int) = unsupported()

    override fun getBlob(index: Int): ByteArray = unsupported()

    override fun getDouble(index: Int): Double = unsupported()

    override fun getLong(index: Int): Long = unsupported()

    override fun getText(index: Int): String = unsupported()

    override fun isNull(index: Int): Boolean = unsupported()

    override fun getColumnCount(): Int = 0

    override fun getColumnName(index: Int): String = unsupported()

    override fun getColumnType(index: Int): Int = unsupported()

    private fun unsupported(): Nothing = error("NoOpStatement supports only execSQL")
}

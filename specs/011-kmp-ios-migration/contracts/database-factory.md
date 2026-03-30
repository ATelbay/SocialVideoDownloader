# Contract: DatabaseFactory

**Location**: `shared/data/src/commonMain/kotlin/.../platform/DatabaseFactory.kt` (expect/actual)

## Interface

```
expect object DatabaseFactory
  └── fun create(): RoomDatabase.Builder<AppDatabase>
      Create a platform-specific Room database builder.
```

## Platform Implementations

### Android: `actual object DatabaseFactory`
- Uses `Room.databaseBuilder(context, AppDatabase::class.java, "svd_database")`
- `Context` provided via Koin `androidContext()`
- Applies all migrations (1→2, 2→3, 3→4, 4→5)
- Same schema directory as current setup

### iOS: `actual object DatabaseFactory`
- Uses `Room.databaseBuilder<AppDatabase>(name = dbFilePath)`
- `dbFilePath` = `NSDocumentDirectory + "/svd_database.db"`
- Path resolved via `NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, ...)`
- Applies same migrations
- No pre-existing database to migrate (fresh install on iOS)

## Database Definition (commonMain)

```
@Database(
  entities = [DownloadEntity::class, SyncQueueEntity::class],
  version = 5
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun syncQueueDao(): SyncQueueDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
```

Room compiler auto-generates the `actual` for `AppDatabaseConstructor` on each platform. No manual `actual` needed for the constructor — only for the `DatabaseFactory` builder.

## Migration Compatibility
- Android: Opens existing v5 databases created by the current Android-only Room setup. Room KMP uses the same SQLite schema — no data migration needed.
- iOS: Creates a fresh v5 database (no prior versions to migrate from).

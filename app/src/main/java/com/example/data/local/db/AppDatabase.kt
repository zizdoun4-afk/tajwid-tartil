package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RecordingEntity::class,
        CachedSurahEntity::class,
        CachedAyahEntity::class,
        BookmarkEntity::class,
        MemorizationStatusEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(SessionMarkerTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun quranCacheDao(): QuranCacheDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun memorizationDao(): MemorizationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `surahNumber` INTEGER NOT NULL,
                        `ayahNumber` INTEGER NOT NULL,
                        `surahName` TEXT NOT NULL,
                        `ayahText` TEXT NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add markers column to recordings
                db.execSQL("ALTER TABLE `recordings` ADD COLUMN `markers` TEXT DEFAULT NULL")

                // 2. Create memorization_status table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `memorization_status` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `surahNumber` INTEGER NOT NULL,
                        `ayahNumber` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `lastReviewedAtEpochMillis` INTEGER,
                        `nextReviewDueEpochMillis` INTEGER,
                        `reviewCount` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // 3. Unique index for surahNumber and ayahNumber
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_memorization_status_surahNumber_ayahNumber` ON `memorization_status` (`surahNumber`, `ayahNumber`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `recordings` ADD COLUMN `isBest` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tajwid_tartil_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

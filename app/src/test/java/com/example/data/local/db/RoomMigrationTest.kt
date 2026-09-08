package com.example.data.local.db

import android.content.Context
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigrationTest {

    @Test
    fun `test migration 1 to 2 creates bookmarks table`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_db_1_2")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // V1 tables: recordings, cached_surahs, cached_ayahs
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `recordings` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `reciterName` TEXT NOT NULL,
                                `surahNumber` INTEGER NOT NULL,
                                `surahName` TEXT NOT NULL,
                                `ayahNumber` INTEGER NOT NULL,
                                `filePath` TEXT NOT NULL,
                                `durationMs` INTEGER NOT NULL,
                                `recordedAtEpochMillis` INTEGER NOT NULL,
                                `customLabel` TEXT
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(
                        db: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) {}
                })
                .build()
        )

        val db = helper.writableDatabase

        // Execute Migration 1 -> 2
        AppDatabase.MIGRATION_1_2.migrate(db)

        // Verify bookmarks table exists and has expected columns
        val cursor = db.query("PRAGMA table_info(`bookmarks`)")
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()

        assertTrue("id column must exist in bookmarks", columns.contains("id"))
        assertTrue("surahNumber column must exist in bookmarks", columns.contains("surahNumber"))
        assertTrue("ayahNumber column must exist in bookmarks", columns.contains("ayahNumber"))
        assertTrue("surahName column must exist in bookmarks", columns.contains("surahName"))
        assertTrue("ayahText column must exist in bookmarks", columns.contains("ayahText"))
        assertTrue("createdAtEpochMillis column must exist in bookmarks", columns.contains("createdAtEpochMillis"))

        db.close()
    }

    @Test
    fun `test migration 2 to 3 adds markers column and memorization_status table`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_db_2_3")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // V2 state
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `recordings` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `reciterName` TEXT NOT NULL,
                                `surahNumber` INTEGER NOT NULL,
                                `surahName` TEXT NOT NULL,
                                `ayahNumber` INTEGER NOT NULL,
                                `filePath` TEXT NOT NULL,
                                `durationMs` INTEGER NOT NULL,
                                `recordedAtEpochMillis` INTEGER NOT NULL,
                                `customLabel` TEXT
                            )
                            """.trimIndent()
                        )
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

                    override fun onUpgrade(
                        db: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) {}
                })
                .build()
        )

        val db = helper.writableDatabase

        // Execute Migration 2 -> 3
        AppDatabase.MIGRATION_2_3.migrate(db)

        // 1. Verify markers column in recordings
        val recCursor = db.query("PRAGMA table_info(`recordings`)")
        val recColumns = mutableListOf<String>()
        while (recCursor.moveToNext()) {
            recColumns.add(recCursor.getString(recCursor.getColumnIndexOrThrow("name")))
        }
        recCursor.close()
        assertTrue("markers column must exist in recordings", recColumns.contains("markers"))

        // 2. Verify memorization_status table
        val memCursor = db.query("PRAGMA table_info(`memorization_status`)")
        val memColumns = mutableListOf<String>()
        while (memCursor.moveToNext()) {
            memColumns.add(memCursor.getString(memCursor.getColumnIndexOrThrow("name")))
        }
        memCursor.close()

        assertTrue("id column in memorization_status", memColumns.contains("id"))
        assertTrue("surahNumber in memorization_status", memColumns.contains("surahNumber"))
        assertTrue("ayahNumber in memorization_status", memColumns.contains("ayahNumber"))
        assertTrue("status in memorization_status", memColumns.contains("status"))
        assertTrue("lastReviewedAtEpochMillis in memorization_status", memColumns.contains("lastReviewedAtEpochMillis"))
        assertTrue("nextReviewDueEpochMillis in memorization_status", memColumns.contains("nextReviewDueEpochMillis"))
        assertTrue("reviewCount in memorization_status", memColumns.contains("reviewCount"))

        // 3. Test uniqueness constraint on surahNumber + ayahNumber
        db.execSQL("INSERT INTO `memorization_status` (`surahNumber`, `ayahNumber`, `status`, `reviewCount`) VALUES (1, 1, 'NEW', 0)")
        var duplicateCaught = false
        try {
            db.execSQL("INSERT INTO `memorization_status` (`surahNumber`, `ayahNumber`, `status`, `reviewCount`) VALUES (1, 1, 'REVIEW', 1)")
        } catch (e: Exception) {
            duplicateCaught = true
        }
        assertTrue("Unique index must prevent duplicate surahNumber + ayahNumber", duplicateCaught)

        db.close()
    }
}

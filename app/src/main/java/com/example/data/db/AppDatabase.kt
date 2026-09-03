package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BookDao
import com.example.data.dao.PaperDao
import com.example.data.dao.QuestionDao
import com.example.data.dao.TestAttemptDao
import com.example.data.dao.TestSubmissionDao
import com.example.data.model.BookEntity
import com.example.data.model.PaperEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.TestAttemptEntity
import com.example.data.model.TestSubmissionEntity

@Database(
    entities = [
        QuestionEntity::class,
        BookEntity::class,
        PaperEntity::class,
        TestAttemptEntity::class,
        TestSubmissionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun bookDao(): BookDao
    abstract fun paperDao(): PaperDao
    abstract fun testAttemptDao(): TestAttemptDao
    abstract fun testSubmissionDao(): TestSubmissionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `test_submissions` ADD COLUMN `violationsJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `test_submissions` ADD COLUMN `proctorRemarks` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `test_submissions` ADD COLUMN `disputeStatus` TEXT NOT NULL DEFAULT 'None'")
                db.execSQL("ALTER TABLE `test_submissions` ADD COLUMN `isResultDeclared` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `test_submissions` ADD COLUMN `rank` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `test_submissions` ADD COLUMN `evaluatedBy` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ots_database"
                )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

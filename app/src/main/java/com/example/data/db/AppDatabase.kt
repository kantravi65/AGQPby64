package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BookDao
import com.example.data.dao.PaperDao
import com.example.data.dao.QuestionDao
import com.example.data.dao.TestAttemptDao
import com.example.data.model.BookEntity
import com.example.data.model.PaperEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.TestAttemptEntity

@Database(
    entities = [
        QuestionEntity::class,
        BookEntity::class,
        PaperEntity::class,
        TestAttemptEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun bookDao(): BookDao
    abstract fun paperDao(): PaperDao
    abstract fun testAttemptDao(): TestAttemptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ots_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

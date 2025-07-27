package com.example.learningapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Word::class],
    version = 2,
    exportSchema = false
)
abstract class WordDatabase : RoomDatabase() {
    
    abstract fun wordDao(): WordDao
    
    companion object {
        @Volatile
        private var INSTANCE: WordDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE words ADD COLUMN category TEXT
                """)
                database.execSQL("""
                    ALTER TABLE words ADD COLUMN difficulty TEXT
                """)
                database.execSQL("""
                    ALTER TABLE words ADD COLUMN isWordOfTheDay INTEGER NOT NULL DEFAULT 0
                """)
                database.execSQL("""
                    ALTER TABLE words ADD COLUMN wordOfTheDayDate TEXT
                """)
                database.execSQL("""
                    ALTER TABLE words ADD COLUMN searchCount INTEGER NOT NULL DEFAULT 0
                """)
                database.execSQL("""
                    ALTER TABLE words ADD COLUMN lastSearched INTEGER NOT NULL DEFAULT 0
                """)
                database.execSQL("""
                    ALTER TABLE words ADD COLUMN example TEXT
                """)
                database.execSQL("""
                    ALTER TABLE words ADD COLUMN synonyms TEXT
                """)
                database.execSQL("""
                    ALTER TABLE words ADD COLUMN etymology TEXT
                """)
            }
        }
        
        fun getDatabase(context: Context): WordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WordDatabase::class.java,
                    "word_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 
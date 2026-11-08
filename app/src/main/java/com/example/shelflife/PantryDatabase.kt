package com.example.shelflife

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PantryItem::class],
    version = 2,
    exportSchema = false
)
abstract class PantryDatabase : RoomDatabase() {

    abstract fun pantryDao(): PantryDao

    companion object {

        @Volatile
        private var INSTANCE: PantryDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    ALTER TABLE pantry_items
                    ADD COLUMN usageStatus TEXT NOT NULL DEFAULT 'ACTIVE'
                    """
                )

                db.execSQL(
                    """
                    ALTER TABLE pantry_items
                    ADD COLUMN updatedAt INTEGER
                    """
                )
            }
        }

        fun getDatabase(context: Context): PantryDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PantryDatabase::class.java,
                    "shelf_life_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
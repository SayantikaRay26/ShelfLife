package com.example.shelflife

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PantryItem)

    @Query("SELECT * FROM pantry_items ORDER BY id DESC")
    fun getAllItems(): Flow<List<PantryItem>>

    @Query("""
        SELECT * FROM pantry_items
        WHERE usageStatus = 'ACTIVE'
        ORDER BY id DESC
    """)
    suspend fun getAllItemsForNotification(): List<PantryItem>

    @Update
    suspend fun updateItem(item: PantryItem)

    @Delete
    suspend fun deleteItem(item: PantryItem)
}
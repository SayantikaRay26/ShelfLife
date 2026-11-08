package com.example.shelflife

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val quantity: String,
    val category: String,
    val expiryDate: String,

    @ColumnInfo(defaultValue = "'ACTIVE'")
    val usageStatus: String = "ACTIVE",

    val updatedAt: Long? = null
)
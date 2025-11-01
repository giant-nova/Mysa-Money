package com.example.spendwise.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "budgets",
    primaryKeys = ["categoryId", "monthYear"], // A composite key
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")] // Index for faster queries
)
data class Budget(
    val categoryId: Int,
    val monthYear: String, // We'll store this as "YYYY-MM" e.g., "2025-10"
    val amount: Double
)
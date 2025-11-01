package com.example.spendwise.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_expenses",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_DEFAULT // If category is deleted, set to default (0)
        )
    ],
    // We add a default "Uncategorized" category with id 0 later
    // so onDelete doesn't crash
    indices = [Index("categoryId")]
)
data class RecurringExpense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val categoryId: Int,
    val note: String? = null,
    val frequency: Frequency, // "Weekly", "Monthly", "Yearly"
    val nextDueDate: Long, // Timestamp of the next payment
    val startDate: Long // When the subscription started
)

/**
 * Enum to define the allowed frequencies. Room will store this
 * as a String (e.g., "MONTHLY").
 */
enum class Frequency {
    WEEKLY,
    MONTHLY,
    YEARLY
}
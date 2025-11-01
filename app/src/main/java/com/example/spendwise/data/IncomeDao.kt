package com.example.spendwise.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(income: Income)

    @Delete
    suspend fun delete(income: Income)

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomes(): Flow<List<Income>>

    @Query("SELECT SUM(amount) FROM incomes WHERE date BETWEEN :start AND :end")
    fun getTotalIncomeInRange(start: Long, end: Long): Flow<Double?>

    @Query("SELECT * FROM incomes WHERE date >= :timestamp ORDER BY date DESC")
    suspend fun getAllIncomesAfter(timestamp: Long): List<Income>
}
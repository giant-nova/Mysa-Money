package com.example.spendwise.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

// Add this TypeConverter class *inside* your AppDatabase.kt file
// (but outside the AppDatabase class)
class FrequencyConverter {
    @TypeConverter
    fun toFrequency(value: String) = enumValueOf<Frequency>(value)

    @TypeConverter
    fun fromFrequency(value: Frequency) = value.name
}
@Database(
    entities = [
        Expense::class,
        Category::class,
        Budget::class,
        RecurringExpense::class,
        Income::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(FrequencyConverter::class) // to handle the Enum
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun incomeDao(): IncomeDao

    companion object {
        // This is the single instance of the database
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop the old, incorrect table
                db.execSQL("DROP TABLE IF EXISTS expenses")

                // Create the new, correct table with the Foreign Key
                db.execSQL(
                    """
                    CREATE TABLE expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        categoryId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        note TEXT,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                    """
                )
                // Add an index for faster queries
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_categoryId ON expenses(categoryId)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create the new budgets table
                db.execSQL(
                    """
                    CREATE TABLE budgets (
                        categoryId INTEGER NOT NULL,
                        monthYear TEXT NOT NULL,
                        amount REAL NOT NULL,
                        PRIMARY KEY(categoryId, monthYear),
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
                    )
                    """
                )
                // Add the index
                db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets(categoryId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create the new recurring_expenses table
                db.execSQL(
                    """
                    CREATE TABLE recurring_expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        categoryId INTEGER NOT NULL,
                        note TEXT,
                        frequency TEXT NOT NULL,
                        nextDueDate INTEGER NOT NULL,
                        startDate INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET DEFAULT
                    )
                    """
                )

                // Add the index
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_expenses_categoryId ON recurring_expenses(categoryId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create the new 'incomes' table
                db.execSQL(
                    """
                    CREATE TABLE incomes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        note TEXT NOT NULL,
                        date INTEGER NOT NULL
                    )
                    """
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            // Return the instance if it exists, otherwise create it
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spendwise.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Closes the database and nullifies the instance.
         * This is REQUIRED for a database restore to work.
         */
        fun closeInstance() {
            INSTANCE?.let {
                if (it.isOpen) {
                    it.close()
                }
                INSTANCE = null
            }
        }
    }
}
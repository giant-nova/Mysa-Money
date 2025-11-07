package com.giantnovadevs.mysamoney.data

/**
 * A simple data class to hold the result of a SUM query
 * grouped by category.
 */
data class CategoryTotal(
    val categoryId: Int,
    val total: Double
)
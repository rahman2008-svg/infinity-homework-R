package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_equations")
data class SavedEquation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val equation: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val type: String = "algebra" // arithmetic, linear, quadratic, fraction, calculus, system
)

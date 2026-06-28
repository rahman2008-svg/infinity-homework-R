package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EquationDao {
    @Query("SELECT * FROM saved_equations ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<SavedEquation>>

    @Query("SELECT * FROM saved_equations WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<SavedEquation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquation(equation: SavedEquation): Long

    @Query("UPDATE saved_equations SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Query("DELETE FROM saved_equations WHERE id = :id")
    suspend fun deleteEquationById(id: Int)

    @Query("DELETE FROM saved_equations")
    suspend fun clearHistory()
}

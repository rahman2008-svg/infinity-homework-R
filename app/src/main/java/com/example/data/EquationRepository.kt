package com.example.data

import kotlinx.coroutines.flow.Flow

class EquationRepository(private val equationDao: EquationDao) {
    val allHistory: Flow<List<SavedEquation>> = equationDao.getAllHistory()
    val allFavorites: Flow<List<SavedEquation>> = equationDao.getAllFavorites()

    suspend fun insert(equation: SavedEquation): Long {
        return equationDao.insertEquation(equation)
    }

    suspend fun updateFavorite(id: Int, isFavorite: Boolean) {
        equationDao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun deleteById(id: Int) {
        equationDao.deleteEquationById(id)
    }

    suspend fun clearAll() {
        equationDao.clearHistory()
    }
}

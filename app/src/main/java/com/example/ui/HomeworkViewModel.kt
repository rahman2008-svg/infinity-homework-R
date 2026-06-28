package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.EquationRepository
import com.example.data.SavedEquation
import com.example.solver.MathSolver
import com.example.solver.SolveResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeworkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EquationRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = EquationRepository(database.equationDao())
    }

    // History and Favorites flows
    val historyState: StateFlow<List<SavedEquation>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoritesState: StateFlow<List<SavedEquation>> = repository.allFavorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Active Solver Result State
    private val _activeResult = MutableStateFlow<SolveResult?>(null)
    val activeResult: StateFlow<SolveResult?> = _activeResult.asStateFlow()

    // Interactive Math Keyboard input
    private val _keyboardInput = MutableStateFlow("")
    val keyboardInput: StateFlow<String> = _keyboardInput.asStateFlow()

    // Viewfinder dimensions for OCR crop
    private val _cropBoxWidth = MutableStateFlow(280f)
    val cropBoxWidth: StateFlow<Float> = _cropBoxWidth.asStateFlow()
    
    private val _cropBoxHeight = MutableStateFlow(120f)
    val cropBoxHeight: StateFlow<Float> = _cropBoxHeight.asStateFlow()

    // Selected Graph function formula
    private val _graphFunction = MutableStateFlow("x^2 - 4")
    val graphFunction: StateFlow<String> = _graphFunction.asStateFlow()

    // Active Tab state
    private val _currentTab = MutableStateFlow(0) // 0: Scan, 1: Calculator, 2: Graph, 3: History
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun setCropDimensions(width: Float, height: Float) {
        _cropBoxWidth.value = width
        _cropBoxHeight.value = height
    }

    fun onKeyboardInputChanged(newInput: String) {
        _keyboardInput.value = newInput
    }

    fun appendToKeyboard(value: String) {
        _keyboardInput.value += value
    }

    fun clearKeyboard() {
        _keyboardInput.value = ""
    }

    fun backspaceKeyboard() {
        val current = _keyboardInput.value
        if (current.isNotEmpty()) {
            _keyboardInput.value = current.substring(0, current.length - 1)
        }
    }

    // Main solve engine runner
    fun solveEquation(equationStr: String, saveToHistory: Boolean = true) {
        if (equationStr.isBlank()) return
        
        viewModelScope.launch {
            val result = MathSolver.solve(equationStr)
            _activeResult.value = result

            if (saveToHistory) {
                val dbEntity = SavedEquation(
                    equation = result.originalEquation,
                    result = result.finalAnswer,
                    isFavorite = false,
                    type = result.type.lowercase()
                )
                repository.insert(dbEntity)
            }
        }
    }

    fun solveFromKeyboard() {
        solveEquation(_keyboardInput.value, saveToHistory = true)
    }

    fun selectHistoryItem(item: SavedEquation) {
        viewModelScope.launch {
            val result = MathSolver.solve(item.equation)
            _activeResult.value = result
            // Switch tab depending on context, or just keep active result
        }
    }

    fun toggleFavorite(id: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.updateFavorite(id, isFavorite)
        }
    }

    fun deleteEquation(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun setGraphFunction(func: String) {
        _graphFunction.value = func
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeworkViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeworkViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

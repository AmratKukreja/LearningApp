package com.example.learningapp.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.learningapp.data.local.Word
import com.example.learningapp.data.local.WordDatabase
import com.example.learningapp.data.remote.NetworkModule
import com.example.learningapp.data.repository.WordRepository
import com.example.learningapp.service.WordOfTheDayWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WordViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: WordRepository
    
    private val _searchResult = MutableLiveData<Word?>()
    val searchResult: LiveData<Word?> = _searchResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    // Analytics data
    private val _analyticsData = MutableLiveData<AnalyticsData>()
    val analyticsData: LiveData<AnalyticsData> = _analyticsData
    
    private val _mostSearchedWords = MutableLiveData<List<Word>>()
    val mostSearchedWords: LiveData<List<Word>> = _mostSearchedWords
    
    private val _wordOfTheDay = MutableLiveData<Word?>()
    val wordOfTheDay: LiveData<Word?> = _wordOfTheDay
    
    private val _filteredWords = MutableLiveData<List<Word>>()
    val filteredWords: LiveData<List<Word>> = _filteredWords
    
    val allWords: LiveData<List<Word>>
    val favoriteWords: LiveData<List<Word>>
    
    data class AnalyticsData(
        val totalWords: Int,
        val favoriteWords: Int,
        val averageSearches: Double,
        val recentSearches: List<Word>,
        val categoryBreakdown: Map<String, Int>
    )
    
    init {
        val wordDao = WordDatabase.getDatabase(application).wordDao()
        repository = WordRepository(wordDao, NetworkModule.dictionaryApiService)
        allWords = repository.getAllWords()
        favoriteWords = repository.getFavoriteWords()
        
        // Schedule Word of the Day notifications
        WordOfTheDayWorker.scheduleDaily(application)
        
        // Load today's word of the day
        loadWordOfTheDay()
    }
    
    fun searchWord(word: String) {
        if (word.isBlank()) {
            _errorMessage.value = "Please enter a word to search"
            return
        }
        
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                val result = repository.searchWord(word.trim())
                if (result.isSuccess) {
                    val foundWord = result.getOrNull()
                    if (foundWord != null) {
                        _searchResult.value = foundWord
                        // Update search count and timestamp
                        repository.incrementSearchCount(foundWord.word)
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Unknown error occurred"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun toggleFavorite(word: String) {
        viewModelScope.launch {
            repository.toggleFavorite(word)
        }
    }
    
    fun loadAnalytics() {
        viewModelScope.launch {
            try {
                val totalWords = repository.getTotalWordCount()
                val favoriteCount = repository.getFavoriteWordCount()
                val recentSearches = repository.getRecentSearches(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)) // Last 7 days
                val mostSearched = repository.getMostSearchedWords(10)
                
                val averageSearches = if (totalWords > 0) {
                    recentSearches.sumOf { it.searchCount }.toDouble() / totalWords
                } else {
                    0.0
                }
                
                // Calculate category breakdown
                val categoryBreakdown = repository.getAllWords().value
                    ?.groupBy { it.category ?: "Uncategorized" }
                    ?.mapValues { it.value.size }
                    ?: emptyMap()
                
                _analyticsData.value = AnalyticsData(
                    totalWords = totalWords,
                    favoriteWords = favoriteCount,
                    averageSearches = averageSearches,
                    recentSearches = recentSearches,
                    categoryBreakdown = categoryBreakdown
                )
                
                _mostSearchedWords.value = mostSearched
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load analytics: ${e.message}"
            }
        }
    }
    
    fun loadWordOfTheDay() {
        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val wordOfDay = repository.getWordOfTheDay(today)
                _wordOfTheDay.value = wordOfDay
            } catch (e: Exception) {
                // Handle error silently for word of the day
            }
        }
    }
    
    fun searchWordsWithFilter(query: String, partOfSpeech: String? = null, difficulty: String? = null, category: String? = null) {
        viewModelScope.launch {
            try {
                var results = repository.searchWords(query)
                
                partOfSpeech?.let { pos ->
                    results = results.filter { it.partOfSpeech == pos }
                }
                
                difficulty?.let { diff ->
                    results = results.filter { it.difficulty == diff }
                }
                
                category?.let { cat ->
                    results = results.filter { it.category == cat }
                }
                
                _filteredWords.value = results
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
            }
        }
    }
    
    fun updateWordCategory(word: String, category: String?) {
        viewModelScope.launch {
            try {
                repository.updateWordCategory(word, category)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update category: ${e.message}"
            }
        }
    }
    
    fun getWordsByCategory(category: String): LiveData<List<Word>> {
        return repository.getWordsByCategory(category)
    }
    
    fun getAllCategories(): LiveData<List<String>> {
        return repository.getAllCategories()
    }
    
    fun exportFavorites(): String {
        return try {
            val favorites = favoriteWords.value ?: emptyList()
            buildString {
                append("# My Favorite Words\n\n")
                favorites.forEach { word ->
                    append("## ${word.word.replaceFirstChar { it.uppercase() }}\n")
                    append("**Part of Speech:** ${word.partOfSpeech ?: "N/A"}\n")
                    append("**Pronunciation:** ${word.pronunciation ?: "N/A"}\n")
                    append("**Definition:** ${word.definition}\n")
                    if (word.example != null) {
                        append("**Example:** ${word.example}\n")
                    }
                    if (word.category != null) {
                        append("**Category:** ${word.category}\n")
                    }
                    append("**Searched:** ${word.searchCount} times\n\n")
                }
            }
        } catch (e: Exception) {
            "Export failed: ${e.message}"
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearSearchResult() {
        _searchResult.value = null
    }
} 
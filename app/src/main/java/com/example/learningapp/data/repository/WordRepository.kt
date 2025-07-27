package com.example.learningapp.data.repository

import androidx.lifecycle.LiveData
import com.example.learningapp.data.local.Word
import com.example.learningapp.data.local.WordDao
import com.example.learningapp.data.model.WordResponse
import com.example.learningapp.data.remote.DictionaryApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WordRepository(
    private val wordDao: WordDao,
    private val apiService: DictionaryApiService
) {
    
    fun getAllWords(): LiveData<List<Word>> = wordDao.getAllWords()
    
    fun getFavoriteWords(): LiveData<List<Word>> = wordDao.getFavoriteWords()
    
    suspend fun searchWord(word: String): Result<Word> {
        return withContext(Dispatchers.IO) {
            try {
                // First check if word exists in local database
                val localWord = wordDao.getWord(word.lowercase())
                if (localWord != null) {
                    return@withContext Result.success(localWord)
                }
                
                // If not in local database, fetch from API
                val response = apiService.getWordDefinition(word.lowercase())
                if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                    val wordResponse = response.body()!!.first()
                    val newWord = convertToWord(wordResponse)
                    wordDao.insertWord(newWord)
                    Result.success(newWord)
                } else {
                    Result.failure(Exception("Word not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun toggleFavorite(word: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val existingWord = wordDao.getWord(word)
                if (existingWord != null) {
                    val newFavoriteStatus = !existingWord.isFavorite
                    wordDao.updateFavoriteStatus(word, newFavoriteStatus)
                    newFavoriteStatus
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }
    
    // Analytics functions
    suspend fun getTotalWordCount(): Int = withContext(Dispatchers.IO) {
        wordDao.getTotalWordCount()
    }
    
    suspend fun getFavoriteWordCount(): Int = withContext(Dispatchers.IO) {
        wordDao.getFavoriteWordCount()
    }
    
    suspend fun getMostSearchedWords(limit: Int): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getMostSearchedWords(limit)
    }
    
    suspend fun getRecentSearches(since: Long): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getRecentSearches(since)
    }
    
    suspend fun incrementSearchCount(word: String) = withContext(Dispatchers.IO) {
        wordDao.incrementSearchCount(word, System.currentTimeMillis())
    }
    
    // Word of the Day functions
    suspend fun getWordOfTheDay(date: String): Word? = withContext(Dispatchers.IO) {
        wordDao.getWordOfTheDay(date)
    }
    
    suspend fun setWordOfTheDay(word: String, date: String) = withContext(Dispatchers.IO) {
        wordDao.setWordOfTheDay(word, date)
    }
    
    // Category functions
    fun getWordsByCategory(category: String): LiveData<List<Word>> = 
        wordDao.getWordsByCategory(category)
    
    fun getAllCategories(): LiveData<List<String>> = wordDao.getAllCategories()
    
    suspend fun updateWordCategory(word: String, category: String?) = withContext(Dispatchers.IO) {
        wordDao.updateWordCategory(word, category)
    }
    
    // Advanced search functions
    suspend fun searchWords(query: String): List<Word> = withContext(Dispatchers.IO) {
        wordDao.searchWords(query)
    }
    
    suspend fun getWordsByPartOfSpeech(partOfSpeech: String): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getWordsByPartOfSpeech(partOfSpeech)
    }
    
    suspend fun getWordsByDifficulty(difficulty: String): List<Word> = withContext(Dispatchers.IO) {
        wordDao.getWordsByDifficulty(difficulty)
    }
    
    private fun convertToWord(wordResponse: WordResponse): Word {
        val firstMeaning = wordResponse.meanings.firstOrNull()
        val firstDefinition = firstMeaning?.definitions?.firstOrNull()
        val definition = firstDefinition?.definition ?: ""
        val example = firstDefinition?.example
        val pronunciation = wordResponse.phonetics.firstOrNull()?.text
        val partOfSpeech = firstMeaning?.partOfSpeech
        
        // Determine difficulty based on word length and definition complexity (simplified)
        val difficulty = when {
            wordResponse.word.length <= 4 && definition.length <= 50 -> "Easy"
            wordResponse.word.length > 8 || definition.length > 150 -> "Hard"
            else -> "Medium"
        }
        
        return Word(
            word = wordResponse.word,
            definition = definition,
            pronunciation = pronunciation,
            partOfSpeech = partOfSpeech,
            example = example,
            difficulty = difficulty
        )
    }
} 
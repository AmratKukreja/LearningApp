package com.example.learningapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WordDao {
    
    @Query("SELECT * FROM words ORDER BY dateAdded DESC")
    fun getAllWords(): LiveData<List<Word>>
    
    @Query("SELECT * FROM words WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoriteWords(): LiveData<List<Word>>
    
    @Query("SELECT * FROM words WHERE word = :word")
    suspend fun getWord(word: String): Word?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word)
    
    @Update
    suspend fun updateWord(word: Word)
    
    @Query("UPDATE words SET isFavorite = :isFavorite WHERE word = :word")
    suspend fun updateFavoriteStatus(word: String, isFavorite: Boolean)
    
    @Delete
    suspend fun deleteWord(word: Word)
    
    @Query("DELETE FROM words WHERE word = :word")
    suspend fun deleteWordByName(word: String)
    
    // Advanced queries for new features
    @Query("SELECT * FROM words WHERE isWordOfTheDay = 1 AND wordOfTheDayDate = :date LIMIT 1")
    suspend fun getWordOfTheDay(date: String): Word?
    
    @Query("UPDATE words SET isWordOfTheDay = 0")
    suspend fun clearAllWordOfTheDay()
    
    @Query("UPDATE words SET isWordOfTheDay = 1, wordOfTheDayDate = :date WHERE word = :word")
    suspend fun setWordOfTheDay(word: String, date: String)
    
    @Query("SELECT * FROM words WHERE category = :category ORDER BY dateAdded DESC")
    fun getWordsByCategory(category: String): LiveData<List<Word>>
    
    @Query("SELECT DISTINCT category FROM words WHERE category IS NOT NULL ORDER BY category")
    fun getAllCategories(): LiveData<List<String>>
    
    @Query("UPDATE words SET category = :category WHERE word = :word")
    suspend fun updateWordCategory(word: String, category: String?)
    
    @Query("UPDATE words SET searchCount = searchCount + 1, lastSearched = :timestamp WHERE word = :word")
    suspend fun incrementSearchCount(word: String, timestamp: Long)
    
    @Query("SELECT * FROM words ORDER BY searchCount DESC LIMIT :limit")
    suspend fun getMostSearchedWords(limit: Int): List<Word>
    
    @Query("SELECT * FROM words WHERE lastSearched > :since ORDER BY lastSearched DESC")
    suspend fun getRecentSearches(since: Long): List<Word>
    
    @Query("SELECT COUNT(*) FROM words")
    suspend fun getTotalWordCount(): Int
    
    @Query("SELECT COUNT(*) FROM words WHERE isFavorite = 1")
    suspend fun getFavoriteWordCount(): Int
    
    @Query("SELECT * FROM words WHERE word LIKE :query || '%' OR definition LIKE '%' || :query || '%' ORDER BY word")
    suspend fun searchWords(query: String): List<Word>
    
    @Query("SELECT * FROM words WHERE partOfSpeech = :partOfSpeech ORDER BY word")
    suspend fun getWordsByPartOfSpeech(partOfSpeech: String): List<Word>
    
    @Query("SELECT * FROM words WHERE difficulty = :difficulty ORDER BY word")
    suspend fun getWordsByDifficulty(difficulty: String): List<Word>
} 
package com.example.learningapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class Word(
    @PrimaryKey
    val word: String,
    val definition: String,
    val pronunciation: String?,
    val partOfSpeech: String?,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val category: String? = null,
    val difficulty: String? = null, // Easy, Medium, Hard
    val isWordOfTheDay: Boolean = false,
    val wordOfTheDayDate: String? = null,
    val searchCount: Int = 0,
    val lastSearched: Long = 0L,
    val example: String? = null,
    val synonyms: String? = null, // Comma-separated
    val etymology: String? = null
) 
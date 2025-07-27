package com.example.learningapp.data.remote

import com.example.learningapp.data.model.WordResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface DictionaryApiService {
    
    @GET("api/v2/entries/en/{word}")
    suspend fun getWordDefinition(@Path("word") word: String): Response<List<WordResponse>>
    
    companion object {
        const val BASE_URL = "https://api.dictionaryapi.dev/"
    }
} 
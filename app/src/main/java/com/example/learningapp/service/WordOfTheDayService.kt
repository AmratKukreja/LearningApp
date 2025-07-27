package com.example.learningapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.learningapp.MainActivity
import com.example.learningapp.R
import com.example.learningapp.data.local.Word
import com.example.learningapp.data.local.WordDatabase
import com.example.learningapp.data.remote.NetworkModule
import com.example.learningapp.data.repository.WordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WordOfTheDayWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val CHANNEL_ID = "word_of_the_day_channel"
        const val NOTIFICATION_ID = 1001
        
        private val dailyWords = listOf(
            "serendipity", "ephemeral", "mellifluous", "wanderlust", "petrichor",
            "solitude", "luminous", "resilience", "tranquil", "eloquent",
            "magnificent", "harmonious", "ethereal", "profound", "sublime",
            "vivacious", "enigmatic", "pristine", "serene", "majestic"
        )
        
        fun scheduleDaily(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WordOfTheDayWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(calculateDelayToNextMorning(), TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "word_of_the_day",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
        
        private fun calculateDelayToNextMorning(): Long {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 8)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            return calendar.timeInMillis - System.currentTimeMillis()
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = WordDatabase.getDatabase(context)
            val repository = WordRepository(database.wordDao(), NetworkModule.dictionaryApiService)
            
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            // Check if we already have a word of the day for today
            val existingWordOfTheDay = database.wordDao().getWordOfTheDay(today)
            if (existingWordOfTheDay != null) {
                showNotification(existingWordOfTheDay)
                return@withContext Result.success()
            }
            
            // Clear previous words of the day
            database.wordDao().clearAllWordOfTheDay()
            
            // Select a word for today (based on day of year for consistency)
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val selectedWord = dailyWords[dayOfYear % dailyWords.size]
            
            // Try to get the word from API or database
            val wordResult = repository.searchWord(selectedWord)
            if (wordResult.isSuccess) {
                val word = wordResult.getOrNull()
                if (word != null) {
                    // Set as word of the day
                    database.wordDao().setWordOfTheDay(word.word, today)
                    
                    // Get updated word and show notification
                    val updatedWord = database.wordDao().getWord(word.word)
                    if (updatedWord != null) {
                        showNotification(updatedWord.copy(isWordOfTheDay = true, wordOfTheDayDate = today))
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    private fun showNotification(word: Word) {
        createNotificationChannel()
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("word_of_the_day", word.word)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_favorite)
            .setContentTitle("Word of the Day: ${word.word.replaceFirstChar { it.uppercase() }}")
            .setContentText(word.definition.take(100) + if (word.definition.length > 100) "..." else "")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${word.definition}\n\nPronunciation: ${word.pronunciation ?: "N/A"}\nPart of Speech: ${word.partOfSpeech ?: "N/A"}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Word of the Day",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily vocabulary notifications"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
} 
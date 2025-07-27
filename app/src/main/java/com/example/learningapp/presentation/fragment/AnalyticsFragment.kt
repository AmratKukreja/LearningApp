package com.example.learningapp.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learningapp.data.local.Word
import com.example.learningapp.databinding.FragmentAnalyticsBinding
import com.example.learningapp.presentation.adapter.WordAdapter
import com.example.learningapp.presentation.viewmodel.WordViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordViewModel by activityViewModels()
    private lateinit var mostSearchedAdapter: WordAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadAnalytics()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        mostSearchedAdapter = WordAdapter(
            onWordClick = { word -> showWordDetails(word) },
            showFavoriteIcon = false
        )
        
        binding.mostSearchedRecycler.apply {
            adapter = mostSearchedAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadAnalytics() {
        lifecycleScope.launch {
            viewModel.loadAnalytics()
        }
    }

    private fun observeViewModel() {
        viewModel.analyticsData.observe(viewLifecycleOwner) { analytics ->
            updateAnalyticsUI(analytics)
        }

        viewModel.mostSearchedWords.observe(viewLifecycleOwner) { words ->
            mostSearchedAdapter.submitList(words)
        }

        viewModel.wordOfTheDay.observe(viewLifecycleOwner) { word ->
            updateWordOfTheDay(word)
        }
    }

    private fun updateAnalyticsUI(analytics: WordViewModel.AnalyticsData) {
        binding.apply {
            totalWordsText.text = "Total Words: ${analytics.totalWords}"
            favoriteWordsText.text = "Favorite Words: ${analytics.favoriteWords}"
            averageSearchesText.text = "Avg. Searches: ${"%.1f".format(analytics.averageSearches)}"
            
            // Calculate learning streak (simplified)
            val streak = calculateLearningStreak(analytics.recentSearches)
            streakText.text = "Learning Streak: $streak days"
            
            // Show category breakdown
            if (analytics.categoryBreakdown.isNotEmpty()) {
                categoryBreakdownText.text = buildString {
                    append("Categories:\n")
                    analytics.categoryBreakdown.forEach { (category, count) ->
                        append("• $category: $count\n")
                    }
                }
                categoryBreakdownText.visibility = View.VISIBLE
            } else {
                categoryBreakdownText.visibility = View.GONE
            }
        }
    }

    private fun updateWordOfTheDay(word: Word?) {
        if (word != null) {
            binding.apply {
                wordOfTheDayCard.visibility = View.VISIBLE
                wordOfTheDayTitle.text = word.word.replaceFirstChar { it.uppercase() }
                wordOfTheDayDefinition.text = word.definition
                wordOfTheDayPronunciation.text = word.pronunciation ?: "No pronunciation available"
                
                wordOfTheDayCard.setOnClickListener {
                    showWordDetails(word)
                }
            }
        } else {
            binding.wordOfTheDayCard.visibility = View.GONE
        }
    }

    private fun calculateLearningStreak(recentSearches: List<Word>): Int {
        if (recentSearches.isEmpty()) return 0
        
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        
        var streak = 0
        var currentDay = today
        
        val searchDays = recentSearches.map { word ->
            calendar.timeInMillis = word.lastSearched
            calendar.get(Calendar.DAY_OF_YEAR)
        }.toSet().sorted().reversed()
        
        for (day in searchDays) {
            if (day == currentDay || day == currentDay - 1) {
                streak++
                currentDay = day - 1
            } else {
                break
            }
        }
        
        return streak
    }

    private fun showWordDetails(word: Word) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(word.word.replaceFirstChar { it.uppercase() })
            .setMessage(buildString {
                append("${word.partOfSpeech ?: ""}\n\n")
                append("${word.definition}\n\n")
                if (word.pronunciation != null) {
                    append("Pronunciation: ${word.pronunciation}\n")
                }
                if (word.example != null) {
                    append("Example: ${word.example}\n")
                }
                append("Searched: ${word.searchCount} times")
                if (word.lastSearched > 0) {
                    val lastSearchedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(word.lastSearched))
                    append("\nLast searched: $lastSearchedDate")
                }
            })
            .setPositiveButton("Add to Favorites") { _, _ ->
                viewModel.toggleFavorite(word.word)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
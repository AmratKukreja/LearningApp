package com.example.learningapp.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learningapp.R
import com.example.learningapp.data.local.Word
import com.example.learningapp.databinding.FragmentSearchBinding
import com.example.learningapp.presentation.adapter.WordAdapter
import com.example.learningapp.presentation.viewmodel.WordViewModel

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordViewModel by activityViewModels()
    private lateinit var recentWordsAdapter: WordAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchFunctionality()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        recentWordsAdapter = WordAdapter(
            onWordClick = { word -> displayWordDetails(word) },
            showFavoriteIcon = true
        )
        
        binding.recentlySearchedRecycler.apply {
            adapter = recentWordsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearchFunctionality() {
        binding.searchButton.setOnClickListener {
            performSearch()
        }

        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val query = binding.searchEditText.text.toString().trim()
        if (query.isNotEmpty()) {
            viewModel.searchWord(query)
        }
    }

    private fun observeViewModel() {
        viewModel.allWords.observe(viewLifecycleOwner) { words ->
            recentWordsAdapter.submitList(words)
        }

        viewModel.searchResult.observe(viewLifecycleOwner) { word ->
            word?.let {
                displayWordDetails(it)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.errorText.text = error
                binding.errorText.visibility = View.VISIBLE
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            } else {
                binding.errorText.visibility = View.GONE
            }
        }
    }

    private fun displayWordDetails(word: Word) {
        binding.wordDetailsCard.visibility = View.VISIBLE
        binding.wordText.text = word.word.replaceFirstChar { it.uppercase() }
        binding.pronunciationText.text = word.pronunciation ?: "No pronunciation available"
        binding.partOfSpeechText.text = word.partOfSpeech ?: "Unknown"
        binding.definitionText.text = word.definition

        updateFavoriteButton(word)

        binding.favoriteButton.setOnClickListener {
            viewModel.toggleFavorite(word.word)
        }
    }

    private fun updateFavoriteButton(word: Word) {
        if (word.isFavorite) {
            binding.favoriteButton.text = getString(R.string.remove_from_favorites)
            binding.favoriteButton.setIconResource(R.drawable.ic_favorite)
        } else {
            binding.favoriteButton.text = getString(R.string.add_to_favorites)
            binding.favoriteButton.setIconResource(R.drawable.ic_favorite)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
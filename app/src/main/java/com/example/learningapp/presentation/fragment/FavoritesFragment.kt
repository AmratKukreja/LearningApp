package com.example.learningapp.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learningapp.data.local.Word
import com.example.learningapp.databinding.FragmentFavoritesBinding
import com.example.learningapp.presentation.adapter.WordAdapter
import com.example.learningapp.presentation.viewmodel.WordViewModel

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordViewModel by activityViewModels()
    private lateinit var favoritesAdapter: WordAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = WordAdapter(
            onWordClick = { word -> showWordDetails(word) },
            showFavoriteIcon = true
        )
        
        binding.favoritesRecycler.apply {
            adapter = favoritesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewModel.favoriteWords.observe(viewLifecycleOwner) { favoriteWords ->
            if (favoriteWords.isEmpty()) {
                binding.emptyStateText.visibility = View.VISIBLE
                binding.favoritesRecycler.visibility = View.GONE
            } else {
                binding.emptyStateText.visibility = View.GONE
                binding.favoritesRecycler.visibility = View.VISIBLE
                favoritesAdapter.submitList(favoriteWords)
            }
        }
    }

    private fun showWordDetails(word: Word) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(word.word.replaceFirstChar { it.uppercase() })
            .setMessage("${word.partOfSpeech ?: ""}\n\n${word.definition}")
            .setPositiveButton("Remove from Favorites") { _, _ ->
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
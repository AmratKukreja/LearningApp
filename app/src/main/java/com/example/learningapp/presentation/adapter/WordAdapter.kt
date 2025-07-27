package com.example.learningapp.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.learningapp.R
import com.example.learningapp.data.local.Word

class WordAdapter(
    private val onWordClick: (Word) -> Unit,
    private val showFavoriteIcon: Boolean = false
) : ListAdapter<Word, WordAdapter.WordViewHolder>(WordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val wordTitle: TextView = itemView.findViewById(R.id.word_title)
        private val wordDefinition: TextView = itemView.findViewById(R.id.word_definition)
        private val wordPartOfSpeech: TextView = itemView.findViewById(R.id.word_part_of_speech)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.favorite_icon)

        fun bind(word: Word) {
            wordTitle.text = word.word.replaceFirstChar { it.uppercase() }
            wordDefinition.text = word.definition
            wordPartOfSpeech.text = word.partOfSpeech ?: ""
            
            if (showFavoriteIcon && word.isFavorite) {
                favoriteIcon.visibility = View.VISIBLE
            } else {
                favoriteIcon.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onWordClick(word)
            }
        }
    }

    class WordDiffCallback : DiffUtil.ItemCallback<Word>() {
        override fun areItemsTheSame(oldItem: Word, newItem: Word): Boolean {
            return oldItem.word == newItem.word
        }

        override fun areContentsTheSame(oldItem: Word, newItem: Word): Boolean {
            return oldItem == newItem
        }
    }
} 
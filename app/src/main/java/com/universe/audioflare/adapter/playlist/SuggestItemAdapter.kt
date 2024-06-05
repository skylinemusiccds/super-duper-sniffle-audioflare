package com.universe.audioflare.adapter.playlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import com.universe.audioflare.R
import com.universe.audioflare.data.model.browse.album.Track
import com.universe.audioflare.databinding.ItemSuggestItemYoutubePlaylistBinding
import com.universe.audioflare.extension.connectArtists
import com.universe.audioflare.extension.toListName

class SuggestItemAdapter(private var listTrack: ArrayList<Track>): Adapter<SuggestItemAdapter.SuggestItemViewHolder>() {
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
    interface OnAddItemClickListener {
        fun onAddItemClick(position: Int)
    }
    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }
    fun setOnAddItemClickListener(listener: OnAddItemClickListener) {
        onAddItemClickListener = listener
    }
    private lateinit var onItemClickListener: OnItemClickListener
    private lateinit var onAddItemClickListener: OnAddItemClickListener
    inner class SuggestItemViewHolder(private val binding: ItemSuggestItemYoutubePlaylistBinding, onItemClickListener: OnItemClickListener, onAddItemClickListener: OnAddItemClickListener): ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onItemClickListener.onItemClick(bindingAdapterPosition)
            }
            binding.btAdd.setOnClickListener {
                onAddItemClickListener.onAddItemClick(bindingAdapterPosition)
            }
        }
        fun bind(track: Track) {
            binding.tvSongTitle.text = track.title
            binding.tvSongArtist.text = track.artists.toListName().connectArtists()
            binding.ivThumbnail.load(track.thumbnails?.last()?.url) {
                crossfade(true)
                placeholder(R.drawable.holder)
            }
            binding.tvSongTitle.isSelected = true
            binding.tvSongArtist.isSelected = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestItemViewHolder {
        val binding = ItemSuggestItemYoutubePlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SuggestItemViewHolder(binding, onItemClickListener, onAddItemClickListener)
    }

    override fun getItemCount(): Int {
        return listTrack.size
    }

    override fun onBindViewHolder(holder: SuggestItemViewHolder, position: Int) {
        holder.bind(listTrack[position])
    }
    fun updateList(newList: ArrayList<Track>) {
        listTrack = newList
        notifyDataSetChanged()
    }
}
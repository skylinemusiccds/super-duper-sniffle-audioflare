package com.universe.audioflare.adapter.playlist

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.request.ImageRequest
import com.universe.audioflare.R
import com.universe.audioflare.data.db.entities.SongEntity
import com.universe.audioflare.data.model.browse.album.Track
import com.universe.audioflare.databinding.ItemPopularSongBinding
import com.universe.audioflare.extension.connectArtists
import com.universe.audioflare.extension.toListName
import com.universe.audioflare.extension.toVideoIdList

class PlaylistItemAdapter(private var playlistItemList: ArrayList<Any>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var mListener: OnItemClickListener
    private lateinit var optionListener: OnOptionClickListener
    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }

    interface OnOptionClickListener {
        fun onOptionClick(position: Int)
    }

    fun setOnClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    fun setOnOptionClickListener(listener: OnOptionClickListener) {
        optionListener = listener
    }

    private var downloadedList = arrayListOf<SongEntity>()
    private var playingTrackVideoId: String? = null
    fun setDownloadedList(downloadedList: ArrayList<SongEntity>?) {
        val oldList = arrayListOf<SongEntity>()
        oldList.addAll(this.downloadedList)
        this.downloadedList = downloadedList ?: arrayListOf()
        playlistItemList.mapIndexed { index, result ->
            if (result is SongEntity || result is Track) {
                val videoId = when (result) {
                    is Track -> result.videoId
                    is SongEntity -> result.videoId
                    else -> null
                }
                if (downloadedList != null) {
                    if (downloadedList.toVideoIdList().contains(videoId) && !oldList.toVideoIdList()
                            .contains(videoId)
                    ) {
                        notifyItemChanged(index)
                    } else if (!downloadedList.toVideoIdList()
                            .contains(videoId) && oldList.toVideoIdList().contains(videoId)
                    ) {
                        notifyItemChanged(index)
                    }
                }
            }
        }
    }

    fun setNowPlaying(it: String?) {
        val oldPlayingTrackVideoId = playingTrackVideoId
        playingTrackVideoId = it
        playlistItemList.mapIndexed { index, result ->
            if (result is SongEntity || result is Track) {
                val videoId = when (result) {
                    is Track -> result.videoId
                    is SongEntity -> result.videoId
                    else -> null
                }
                if (videoId == playingTrackVideoId) {
                    notifyItemChanged(index)
                } else if (videoId == oldPlayingTrackVideoId) {
                    notifyItemChanged(index)
                }

            }
        }
    }

    inner class TrackViewHolder(
        val binding: ItemPopularSongBinding,
        rootListener: OnItemClickListener,
        mOptionListener: OnOptionClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                rootListener.onItemClick(bindingAdapterPosition)
            }
            binding.btMore.setOnClickListener {
                mOptionListener.onOptionClick(bindingAdapterPosition)
            }
        }

        fun bind (track: Track) {
            binding.tvSongTitle.text = track.title
            binding.tvSongArtist.text = track.artists.toListName().connectArtists()
            binding.ivThumbnail.load(track.thumbnails?.last()?.url)
            binding.tvSongTitle.isSelected = true
            binding.tvSongArtist.isSelected = true
            if (downloadedList.toVideoIdList().contains(track.videoId)) {
                binding.ivDownloaded.visibility = View.VISIBLE
            } else {
                binding.ivDownloaded.visibility = View.GONE
            }
            if (playingTrackVideoId == track.videoId) {
                binding.ivPlaying.visibility = View.VISIBLE
                binding.ivThumbnail.visibility = View.GONE
            } else {
                binding.ivPlaying.visibility = View.GONE
                binding.ivThumbnail.visibility = View.VISIBLE
            }
        }
    }
    inner class LocalPlaylistTrackViewHolder(val binding: ItemPopularSongBinding, rootListener: OnItemClickListener, mOptionListener: OnOptionClickListener): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                rootListener.onItemClick(bindingAdapterPosition)
            }
            binding.btMore.setOnClickListener {
                mOptionListener.onOptionClick(bindingAdapterPosition)
            }
        }
        fun bind (song: SongEntity) {
            binding.tvSongTitle.text = song.title
            binding.tvSongArtist.text = song.artistName?.connectArtists()
            binding.tvSongTitle.isSelected = true
            binding.tvSongArtist.isSelected = true
            binding.ivThumbnail.load(song.thumbnails)
            if (downloadedList.toVideoIdList().contains(song.videoId)) {
                binding.ivDownloaded.visibility = View.VISIBLE
            } else {
                binding.ivDownloaded.visibility = View.GONE
            }
            if (playingTrackVideoId == song.videoId) {
                binding.ivPlaying.visibility = View.VISIBLE
                binding.ivThumbnail.visibility = View.GONE
            } else {
                binding.ivPlaying.visibility = View.GONE
                binding.ivThumbnail.visibility = View.VISIBLE
            }
        }
    }
    fun updateList(newList: ArrayList<Any>){
        Log.d("PlaylistItemAdapter", "updateList: $newList")
        playlistItemList.clear()
        newList.forEach {
            playlistItemList.add(it)
        }
        Log.d("PlaylistItemAdapter", "updateList: $playlistItemList")
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TRACK -> TrackViewHolder(ItemPopularSongBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener, optionListener)
            VIEW_TYPE_LOCAL_PLAYLIST_TRACK -> LocalPlaylistTrackViewHolder(ItemPopularSongBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener, optionListener)
            else -> throw IllegalArgumentException("Invalid type of data $viewType")
        }
    }

    override fun getItemCount(): Int = playlistItemList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TrackViewHolder -> holder.bind(playlistItemList[position] as Track)
            is LocalPlaylistTrackViewHolder -> holder.bind(playlistItemList[position] as SongEntity)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (playlistItemList[position]) {
            is Track -> VIEW_TYPE_TRACK
            is SongEntity -> VIEW_TYPE_LOCAL_PLAYLIST_TRACK
            else -> throw IllegalArgumentException("Invalid type of data $position")
        }
    }

    fun getListTrack(): ArrayList<Any> {
        return playlistItemList
    }

    companion object {
        private const val VIEW_TYPE_TRACK = 0
        private const val VIEW_TYPE_LOCAL_PLAYLIST_TRACK = 1
    }

    fun ImageView.load(url: String?) {
        if (url != null) {
            ImageLoader(context).enqueue(
                ImageRequest.Builder(context)
                    .placeholder(R.drawable.holder)
                    .crossfade(true)
                    .data(url)
                    .diskCacheKey(url)
                    .target(this)
                    .build()
            )
        }
    }

    fun setLikedTrack(position: Int, like: Boolean) {
        if (playlistItemList[position] is SongEntity) {
            val track = playlistItemList[position] as SongEntity
            playlistItemList[position] = track.copy(liked = like)
            notifyItemChanged(position)
        }
    }
}
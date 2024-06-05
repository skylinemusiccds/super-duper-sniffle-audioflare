package com.universe.audioflare.adapter.artist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.universe.audioflare.data.db.entities.SongEntity
import com.universe.audioflare.data.model.browse.artist.ResultSong
import com.universe.audioflare.databinding.ItemPopularSongBinding
import com.universe.audioflare.extension.connectArtists
import com.universe.audioflare.extension.toListName
import com.universe.audioflare.extension.toVideoIdList

class PopularAdapter(private var popularList: ArrayList<ResultSong>): RecyclerView.Adapter<PopularAdapter.ViewHolder>() {
    private lateinit var mListener: OnItemClickListener
    private lateinit var mOptionsListener: OnOptionsClickListener
    interface OnItemClickListener{
        fun onItemClick(position: Int, type: String = "song")
    }
    interface OnOptionsClickListener{
        fun onOptionsClick(position: Int)
    }

    fun setOnClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    fun setOnOptionsClickListener(listener: OnOptionsClickListener) {
        mOptionsListener = listener
    }

    fun getCurrentList(): ArrayList<ResultSong> {
        return popularList
    }

    private var downloadedList = arrayListOf<SongEntity>()
    private var playingTrackVideoId: String? = null

    fun setDownloadedList(downloadedList: ArrayList<SongEntity>?) {
        val oldList = arrayListOf<SongEntity>()
        oldList.addAll(this.downloadedList)
        this.downloadedList = downloadedList ?: arrayListOf()
        popularList.mapIndexed { index, result ->
            val videoId = result.videoId
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

    fun setNowPlaying(it: String?) {
        val oldPlayingTrackVideoId = playingTrackVideoId
        playingTrackVideoId = it
        popularList.mapIndexed { index, result ->
            val videoId = result.videoId
            if (videoId == playingTrackVideoId) {
                notifyItemChanged(index)
            } else if (videoId == oldPlayingTrackVideoId) {
                notifyItemChanged(index)
            }
        }
    }


    inner class ViewHolder(
        val binding: ItemPopularSongBinding,
        listener: OnItemClickListener,
        optionListener: OnOptionsClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
            binding.btMore.setOnClickListener {
                optionListener.onOptionsClick(bindingAdapterPosition)
            }
        }
    }
    fun updateList(newList: ArrayList<ResultSong>){
        popularList.clear()
        popularList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemPopularSongBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener, mOptionsListener)
    }

    override fun getItemCount(): Int {
        return popularList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = popularList[position]
        with(holder.binding){
            tvSongTitle.text = song.title
            val artistName = song.artists.toListName().connectArtists()
            tvSongArtist.text = artistName
            tvSongTitle.isSelected = true
            tvSongArtist.isSelected = true
            ivThumbnail.load(song.thumbnails.lastOrNull()?.url) {
                crossfade(true)
                placeholder(com.universe.audioflare.R.drawable.holder)
            }
            if (downloadedList.toVideoIdList().contains(song.videoId)) {
                ivDownloaded.visibility = View.VISIBLE
            } else {
                ivDownloaded.visibility = View.GONE
            }
            if (playingTrackVideoId == song.videoId) {
                ivPlaying.visibility = View.VISIBLE
                ivThumbnail.visibility = View.GONE
            } else {
                ivPlaying.visibility = View.GONE
                ivThumbnail.visibility = View.VISIBLE
            }
        }
    }

    fun getItem(position: Int): ResultSong {
        return popularList[position]
    }
}
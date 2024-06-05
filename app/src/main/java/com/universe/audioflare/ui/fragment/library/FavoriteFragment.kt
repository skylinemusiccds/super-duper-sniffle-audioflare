package com.universe.audioflare.ui.fragment.library

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.universe.audioflare.R
import com.universe.audioflare.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.universe.audioflare.adapter.playlist.AddToAPlaylistAdapter
import com.universe.audioflare.adapter.search.SearchItemAdapter
import com.universe.audioflare.common.Config
import com.universe.audioflare.common.DownloadState
import com.universe.audioflare.data.db.entities.LocalPlaylistEntity
import com.universe.audioflare.data.db.entities.PairSongLocalPlaylist
import com.universe.audioflare.data.db.entities.SongEntity
import com.universe.audioflare.data.model.browse.album.Track
import com.universe.audioflare.data.model.searchResult.songs.Artist
import com.universe.audioflare.data.queue.Queue
import com.universe.audioflare.databinding.BottomSheetAddToAPlaylistBinding
import com.universe.audioflare.databinding.BottomSheetNowPlayingBinding
import com.universe.audioflare.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.universe.audioflare.databinding.FragmentFavoriteBinding
import com.universe.audioflare.extension.connectArtists
import com.universe.audioflare.extension.navigateSafe
import com.universe.audioflare.extension.removeConflicts
import com.universe.audioflare.extension.setEnabledAll
import com.universe.audioflare.extension.toTrack
import com.universe.audioflare.service.test.download.MusicDownloadService
import com.universe.audioflare.viewModel.FavoriteViewModel
import com.universe.audioflare.viewModel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@AndroidEntryPoint
class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<FavoriteViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private lateinit var likedAdapter: SearchItemAdapter
    private lateinit var listLiked: ArrayList<Any>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        return binding.root
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listLiked = ArrayList<Any>()
        likedAdapter = SearchItemAdapter(arrayListOf(), requireContext())
        binding.rvFavorite.apply {
            adapter = likedAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewModel.getListLikedSong()
        viewModel.listLikedSong.observe(viewLifecycleOwner){ liked ->
            listLiked.clear()
            val tempLiked = mutableListOf<SongEntity>()
            for (i in liked.size - 1 downTo 0) {
                tempLiked.add(liked[i])
            }
            listLiked.addAll(tempLiked)
            likedAdapter.updateList(listLiked)
        }


        likedAdapter.setOnClickListener(object : SearchItemAdapter.onItemClickListener {
            override fun onItemClick(position: Int, type: String) {
                val song = listLiked[position] as SongEntity
                val args = Bundle()
                args.putString("type", Config.ALBUM_CLICK)
                args.putString("videoId", song.videoId)
                args.putString("from", getString(R.string.favorite))
                args.putInt("index", position)
                Queue.initPlaylist(Queue.LOCAL_PLAYLIST_ID_LIKED, getString(R.string.favorite), Queue.PlaylistType.LOCAL_PLAYLIST)
                Queue.setNowPlaying(song.toTrack())
                Queue.addAll(listLiked.map { (it as SongEntity).toTrack()} as ArrayList<Track>)
                Queue.removeTrackWithIndex(position)
                findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
            }

            override fun onOptionsClick(position: Int, type: String) {
                val dialog = BottomSheetDialog(requireContext())
                val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
                with(bottomSheetView) {
                    btSleepTimer.visibility = View.GONE
                    tvFavorite.text = getString(R.string.liked)
                    cbFavorite.isChecked = true
                    val song = listLiked[position] as SongEntity
                    tvSongTitle.text = song.title
                    tvSongTitle.isSelected = true
                    tvSongArtist.text = song.artistName?.connectArtists()
                    tvSongArtist.isSelected = true
                    ivThumbnail.load(song.thumbnails)
                    if (song.albumName != null) {
                        setEnabledAll(btAlbum, true)
                        tvAlbum.text = song.albumName
                    } else {
                        tvAlbum.text = getString(R.string.no_album)
                        setEnabledAll(btAlbum, false)
                    }
                    btAlbum.setOnClickListener {
                        val albumId = song.albumId
                        if (albumId != null) {
                            findNavController().navigateSafe(
                                R.id.action_global_albumFragment,
                                Bundle().apply {
                                    putString("browseId", albumId)
                                })
                            dialog.dismiss()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.no_album),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    when (song.downloadState) {
                        DownloadState.STATE_PREPARING -> {
                            tvDownload.text = getString(R.string.preparing)
                            ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                            setEnabledAll(btDownload, true)
                        }

                        DownloadState.STATE_NOT_DOWNLOADED -> {
                            tvDownload.text = getString(R.string.download)
                            ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                            setEnabledAll(btDownload, true)
                        }

                        DownloadState.STATE_DOWNLOADING -> {
                            tvDownload.text = getString(R.string.downloading)
                            ivDownload.setImageResource(R.drawable.baseline_downloading_white)
                            setEnabledAll(btDownload, true)
                        }

                        DownloadState.STATE_DOWNLOADED -> {
                            tvDownload.text = getString(R.string.downloaded)
                            ivDownload.setImageResource(R.drawable.baseline_downloaded)
                            setEnabledAll(btDownload, true)
                        }
                    }
                    btChangeLyricsProvider.visibility = View.GONE
                    btAddQueue.setOnClickListener {
                        sharedViewModel.addToQueue(song.toTrack())
                    }
                    btPlayNext.setOnClickListener {
                        sharedViewModel.playNext(song.toTrack())
                    }
                    btRadio.setOnClickListener {
                        val args = Bundle()
                        args.putString("radioId", "RDAMVM${song.videoId}")
                        args.putString(
                            "videoId",
                            song.videoId
                        )
                        dialog.dismiss()
                        findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
                    }
                    btLike.setOnClickListener {
                        if (cbFavorite.isChecked){
                            cbFavorite.isChecked = false
                            tvFavorite.text = getString(R.string.like)
                            viewModel.updateLikeStatus(song.videoId, 0)
                            viewModel.getListLikedSong()
                            viewModel.listLikedSong.observe(viewLifecycleOwner){ liked ->
                                listLiked.clear()
                                listLiked.addAll(liked)
                                likedAdapter.updateList(listLiked)
                            }
                        }
                        else {
                            cbFavorite.isChecked = true
                            tvFavorite.text = getString(R.string.liked)
                            viewModel.updateLikeStatus(song.videoId, 1)
                            viewModel.getListLikedSong()
                            viewModel.listLikedSong.observe(viewLifecycleOwner) { liked ->
                                listLiked.clear()
                                listLiked.addAll(liked)
                                likedAdapter.updateList(listLiked)
                            }
                        }
                        lifecycleScope.launch {
                            if (sharedViewModel.simpleMediaServiceHandler?.nowPlaying?.first()?.mediaId == song.videoId) {
                                delay(500)
                                sharedViewModel.refreshSongDB()
                            }
                        }
                    }

                    btSeeArtists.setOnClickListener {
                        val subDialog = BottomSheetDialog(requireContext())
                        val subBottomSheetView = BottomSheetSeeArtistOfNowPlayingBinding.inflate(layoutInflater)
                        Log.d("FavoriteFragment", "onOptionsClick: ${song.artistId}")
                        if (!song.artistName.isNullOrEmpty()) {
                            val tempArtist = mutableListOf<Artist>()
                             for (i in 0 until song.artistName.size) {
                                tempArtist.add(Artist(name = song.artistName[i], id = song.artistId?.get(i)))
                            }
                            Log.d("FavoriteFragment", "onOptionsClick: $tempArtist")
                            val artistAdapter = SeeArtistOfNowPlayingAdapter(tempArtist)
                            subBottomSheetView.rvArtists.apply {
                                adapter = artistAdapter
                                layoutManager = LinearLayoutManager(requireContext())
                            }
                            artistAdapter.setOnClickListener(object : SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                override fun onItemClick(position: Int) {
                                    val artist = tempArtist[position]
                                    if (artist.id != null) {
                                        findNavController().navigateSafe(R.id.action_global_artistFragment, Bundle().apply {
                                            putString("channelId", artist.id)
                                        })
                                        subDialog.dismiss()
                                        dialog.dismiss()
                                    }
                                }

                            })
                        }

                        subDialog.setCancelable(true)
                        subDialog.setContentView(subBottomSheetView.root)
                        subDialog.show()
                    }
                    btAddPlaylist.setOnClickListener {
                        viewModel.getAllLocalPlaylist()
                        val listLocalPlaylist: ArrayList<LocalPlaylistEntity> = arrayListOf()
                        val addPlaylistDialog = BottomSheetDialog(requireContext())
                        val viewAddPlaylist = BottomSheetAddToAPlaylistBinding.inflate(layoutInflater)
                        val addToAPlaylistAdapter = AddToAPlaylistAdapter(arrayListOf())
                        addToAPlaylistAdapter.setVideoId(song.videoId)
                        viewAddPlaylist.rvLocalPlaylists.apply {
                            adapter = addToAPlaylistAdapter
                            layoutManager = LinearLayoutManager(requireContext())
                        }
                        viewModel.localPlaylist.observe(viewLifecycleOwner) {list ->
                            Log.d("Check Local Playlist", list.toString())
                            listLocalPlaylist.clear()
                            listLocalPlaylist.addAll(list)
                            addToAPlaylistAdapter.updateList(listLocalPlaylist)
                        }
                        addToAPlaylistAdapter.setOnItemClickListener(object : AddToAPlaylistAdapter.OnItemClickListener{
                            override fun onItemClick(position: Int) {
                                val playlist = listLocalPlaylist[position]
                                val tempTrack = ArrayList<String>()
                                viewModel.updateInLibrary(song.videoId)
                                if (playlist.tracks != null) {
                                    tempTrack.addAll(playlist.tracks)
                                }
                                if (!tempTrack.contains(song.videoId) && playlist.syncedWithYouTubePlaylist == 1 && playlist.youtubePlaylistId != null) {
                                    viewModel.addToYouTubePlaylist(playlist.id, playlist.youtubePlaylistId, song.videoId)
                                }
                                if (!tempTrack.contains(song.videoId)) {
                                    viewModel.insertPairSongLocalPlaylist(
                                        PairSongLocalPlaylist(
                                            playlistId = playlist.id,
                                            songId = song.videoId,
                                            position = playlist.tracks?.size ?: 0,
                                            inPlaylist = LocalDateTime.now()
                                        )
                                    )
                                    tempTrack.add(song.videoId)
                                }
                                viewModel.updateLocalPlaylistTracks(
                                    tempTrack.removeConflicts(),
                                    playlist.id
                                )
                                addPlaylistDialog.dismiss()
                                dialog.dismiss()
                            }
                        })
                        addPlaylistDialog.setContentView(viewAddPlaylist.root)
                        addPlaylistDialog.setCancelable(true)
                        addPlaylistDialog.show()
                    }
                    btDownload.setOnClickListener {
                        if (tvDownload.text == getString(R.string.download)){
                            Log.d("Download", "onClick: ${song.videoId}")
                            viewModel.updateDownloadState(
                                song.videoId,
                                DownloadState.STATE_PREPARING
                            )
                            val downloadRequest =
                                DownloadRequest.Builder(song.videoId, song.videoId.toUri())
                                    .setData(song.title.toByteArray())
                                    .setCustomCacheKey(song.videoId)
                                    .build()
                            viewModel.updateDownloadState(
                                song.videoId,
                                DownloadState.STATE_DOWNLOADING
                            )
                            viewModel.getDownloadStateFromService(song.videoId)
                            DownloadService.sendAddDownload(
                                requireContext(),
                                MusicDownloadService::class.java,
                                downloadRequest,
                                false
                            )
                            lifecycleScope.launch {
                                viewModel.downloadState.collect { download ->
                                    if (download != null) {
                                        when (download.state) {
                                            Download.STATE_DOWNLOADING -> {
                                                viewModel.updateDownloadState(
                                                    song.videoId,
                                                    DownloadState.STATE_DOWNLOADING
                                                )
                                                tvDownload.text = getString(R.string.downloading)
                                                ivDownload.setImageResource(R.drawable.baseline_downloading_white)
                                                setEnabledAll(btDownload, true)
                                            }

                                            Download.STATE_FAILED -> {
                                                viewModel.updateDownloadState(
                                                    song.videoId,
                                                    DownloadState.STATE_NOT_DOWNLOADED
                                                )
                                                tvDownload.text = getString(R.string.download)
                                                ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                                                setEnabledAll(btDownload, true)
                                                Toast.makeText(
                                                    requireContext(),
                                                    getString(androidx.media3.exoplayer.R.string.exo_download_failed),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                            Download.STATE_COMPLETED -> {
                                                viewModel.updateDownloadState(
                                                    song.videoId,
                                                    DownloadState.STATE_DOWNLOADED
                                                )
                                                Toast.makeText(
                                                    requireContext(),
                                                    androidx.media3.exoplayer.R.string.exo_download_completed,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                tvDownload.text = getString(R.string.downloaded)
                                                ivDownload.setImageResource(R.drawable.baseline_downloaded)
                                                setEnabledAll(btDownload, true)
                                            }

                                            else -> {
                                                Log.d("Download", "${download.state}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else if (tvDownload.text == getString(R.string.downloaded) || tvDownload.text == getString(R.string.downloading)){
                            DownloadService.sendRemoveDownload(
                                requireContext(),
                                MusicDownloadService::class.java,
                                song.videoId,
                                false
                            )
                            viewModel.updateDownloadState(
                                song.videoId,
                                DownloadState.STATE_NOT_DOWNLOADED
                            )
                            tvDownload.text = getString(R.string.download)
                            ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                            setEnabledAll(btDownload, true)
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.removed_download),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    btShare.setOnClickListener {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        val url = "https://youtube.com/watch?v=${song.videoId}"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                        val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share_url))
                        startActivity(chooserIntent)
                    }
                }
                dialog.setCancelable(true)
                dialog.setContentView(bottomSheetView.root)
                dialog.show()
            }

        })

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val job1 = launch {
                    sharedViewModel.downloadList.collect {
                        likedAdapter.setDownloadedList(it)
                    }
                }
                val job2 = launch {
                    combine(sharedViewModel.simpleMediaServiceHandler?.nowPlaying ?: flowOf<MediaItem?>(null), sharedViewModel.isPlaying) { nowPlaying, isPlaying ->
                        Pair(nowPlaying, isPlaying)
                    }.collect {
                        if (it.first != null && it.second) {
                            likedAdapter.setNowPlaying(it.first!!.mediaId)
                        } else {
                            likedAdapter.setNowPlaying(null)
                        }
                    }
                }
                job1.join()
                job2.join()
            }
        }
    }
}
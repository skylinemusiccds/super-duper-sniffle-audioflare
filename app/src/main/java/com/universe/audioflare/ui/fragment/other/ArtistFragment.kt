package com.universe.audioflare.ui.fragment.other

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.request.CachePolicy
import coil.size.Size
import coil.transform.Transformation
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.universe.audioflare.R
import com.universe.audioflare.adapter.artist.AlbumsAdapter
import com.universe.audioflare.adapter.artist.FeaturedOnAdapter
import com.universe.audioflare.adapter.artist.PopularAdapter
import com.universe.audioflare.adapter.artist.RelatedArtistsAdapter
import com.universe.audioflare.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.universe.audioflare.adapter.artist.SinglesAdapter
import com.universe.audioflare.adapter.artist.VideoAdapter
import com.universe.audioflare.adapter.playlist.AddToAPlaylistAdapter
import com.universe.audioflare.common.Config
import com.universe.audioflare.data.db.entities.ArtistEntity
import com.universe.audioflare.data.db.entities.LocalPlaylistEntity
import com.universe.audioflare.data.db.entities.PairSongLocalPlaylist
import com.universe.audioflare.data.model.browse.album.Track
import com.universe.audioflare.data.model.browse.artist.ResultAlbum
import com.universe.audioflare.data.model.browse.artist.ResultPlaylist
import com.universe.audioflare.data.model.browse.artist.ResultRelated
import com.universe.audioflare.data.model.browse.artist.ResultSingle
import com.universe.audioflare.data.model.browse.artist.ResultSong
import com.universe.audioflare.data.model.browse.artist.ResultVideo
import com.universe.audioflare.data.queue.Queue
import com.universe.audioflare.databinding.BottomSheetAddToAPlaylistBinding
import com.universe.audioflare.databinding.BottomSheetNowPlayingBinding
import com.universe.audioflare.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.universe.audioflare.databinding.FragmentArtistBinding
import com.universe.audioflare.extension.connectArtists
import com.universe.audioflare.extension.navigateSafe
import com.universe.audioflare.extension.removeConflicts
import com.universe.audioflare.extension.setEnabledAll
import com.universe.audioflare.extension.toListName
import com.universe.audioflare.extension.toSongEntity
import com.universe.audioflare.extension.toTrack
import com.universe.audioflare.utils.Resource
import com.universe.audioflare.viewModel.ArtistViewModel
import com.universe.audioflare.viewModel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.abs

@AndroidEntryPoint
class ArtistFragment: Fragment(){
    private val viewModel by viewModels<ArtistViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private var _binding: FragmentArtistBinding? = null
    private val binding get() = _binding!!

    private lateinit var popularAdapter: PopularAdapter
    private lateinit var singlesAdapter: SinglesAdapter
    private lateinit var albumsAdapter: AlbumsAdapter
    private lateinit var featuredOnAdapter: FeaturedOnAdapter
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var relatedArtistsAdapter: RelatedArtistsAdapter

    private var gradientDrawable: GradientDrawable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireArguments().clear()
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getLocation()
        if (viewModel.gradientDrawable.value != null){
            gradientDrawable = viewModel.gradientDrawable.value
        }
        binding.rootFull.visibility = View.GONE
        binding.loadingLayout.visibility = View.VISIBLE
        popularAdapter = PopularAdapter(arrayListOf())
        singlesAdapter = SinglesAdapter(arrayListOf())
        albumsAdapter = AlbumsAdapter(arrayListOf())
        videoAdapter = VideoAdapter(arrayListOf())
        featuredOnAdapter = FeaturedOnAdapter(arrayListOf())
        relatedArtistsAdapter = RelatedArtistsAdapter(arrayListOf(), requireContext())
        binding.rvPopularSongs.apply {
            adapter = popularAdapter
            layoutManager = LinearLayoutManager(context)
        }
        binding.rvSingles.apply {
            adapter = singlesAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        binding.rvAlbum.apply {
            adapter = albumsAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        binding.rvVideo.apply {
            adapter = videoAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        binding.rvFeaturedOn.apply {
            adapter = featuredOnAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        binding.rvRelatedArtists.apply {
            adapter = relatedArtistsAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        val channelId = requireArguments().getString("channelId")
        if (channelId != null) {
            fetchData(channelId)
        }

        binding.toolBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.btShuffle.setOnClickListener {
            val id = viewModel.artistBrowse.value?.data?.songs?.browseId
            if (id != null) {
                val args = Bundle()
                args.putString("id", id)
                findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
            }
            else {
                Snackbar.make(binding.root, getString(R.string.error), Snackbar.LENGTH_LONG).show()
            }
        }
        binding.btMoreAlbum.setOnClickListener {
            if  (viewModel.artistBrowse.value?.data?.channelId != null) {
                val id = "MPAD${viewModel.artistBrowse.value?.data?.channelId}"
                val args = Bundle()
                args.putString("id", id)
                args.putString("type", "album")
                findNavController().navigateSafe(R.id.action_global_moreAlbumsFragment, args)
            }
            else {
                Snackbar.make(binding.root, getString(R.string.error), Snackbar.LENGTH_LONG).show()
            }
        }
        binding.btMoreSingles.setOnClickListener {
            if (viewModel.artistBrowse.value?.data?.channelId != null) {
                val id = "MPAD${viewModel.artistBrowse.value?.data?.channelId}"
                val args = Bundle()
                args.putString("id", id)
                args.putString("type", "single")
                findNavController().navigateSafe(R.id.action_global_moreAlbumsFragment, args)
            }
            else {
                Snackbar.make(binding.root, getString(R.string.error), Snackbar.LENGTH_LONG).show()
            }
        }
        binding.btMoreVideos.setOnClickListener {
            val id = viewModel.artistBrowse.value?.data?.videoList
            if (id != null){
                val args = Bundle()
                args.putString("id", id)
                findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
            }
            else {
                Snackbar.make(binding.root, getString(R.string.error), Snackbar.LENGTH_LONG).show()
            }
        }
        relatedArtistsAdapter.setOnClickListener(object: RelatedArtistsAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                val bundle = Bundle()
                bundle.putString("channelId", relatedArtistsAdapter.getItem(position).browseId)
                findNavController().navigateSafe(R.id.action_global_artistFragment, bundle)
            }
        })
        singlesAdapter.setOnClickListener(object: SinglesAdapter.OnItemClickListener{
            override fun onItemClick(position: Int, type: String) {
                val args = Bundle()
                args.putString("browseId", singlesAdapter.getItem(position).browseId)
                findNavController().navigateSafe(R.id.action_global_albumFragment, args)
            }
        })
        albumsAdapter.setOnClickListener(object: AlbumsAdapter.OnItemClickListener{
            override fun onItemClick(position: Int, type: String) {
                val args = Bundle()
                args.putString("browseId", albumsAdapter.getItem(position).browseId)
                findNavController().navigateSafe(R.id.action_global_albumFragment, args)
            }
        })
        popularAdapter.setOnClickListener(object: PopularAdapter.OnItemClickListener{
            override fun onItemClick(position: Int, type: String) {
                val songClicked = popularAdapter.getCurrentList()[position]
                val videoId = songClicked.videoId
                Queue.initPlaylist(
                    "RDAMVM$videoId",
                    "\"${viewModel.artistBrowse.value?.data?.name}\" ${getString(R.string.popular)}",
                    Queue.PlaylistType.RADIO,
                )
                val firstQueue: Track = songClicked.toTrack()
                Queue.setNowPlaying(firstQueue)
                val args = Bundle()
                args.putString("videoId", videoId)
                args.putString(
                    "from",
                    "\"${viewModel.artistBrowse.value?.data?.name}\" ${getString(R.string.popular)}"
                )
                args.putString("type", Config.SONG_CLICK)
                findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
            }
        })
        featuredOnAdapter.setOnClickListener(object : FeaturedOnAdapter.OnItemClickListener {
            override fun onItemClick(position: Int, type: String) {
                val args = Bundle()
                args.putString("id", featuredOnAdapter.getItem(position).id)
                findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
            }
        })
        videoAdapter.setOnClickListener(object : VideoAdapter.OnItemClickListener {
            override fun onItemClick(position: Int, type: String) {
                val songClicked = videoAdapter.getCurrentList()[position]
                val videoId = songClicked.videoId
                Queue.initPlaylist(
                    "RDAMVM$videoId",
                    "\"${viewModel.artistBrowse.value?.data?.name}\" ${getString(R.string.videos)}",
                    Queue.PlaylistType.RADIO,
                )
                val firstQueue: Track = songClicked.toTrack()
                Queue.setNowPlaying(firstQueue)
                val args = Bundle()
                args.putString("videoId", videoId)
                args.putString(
                    "from",
                    "\"${viewModel.artistBrowse.value?.data?.name}\" ${getString(R.string.videos)}"
                )
                args.putString("type", Config.VIDEO_CLICK)
                findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
            }
        })
        popularAdapter.setOnOptionsClickListener(object : PopularAdapter.OnOptionsClickListener{
            @UnstableApi
            override fun onOptionsClick(position: Int) {
                val song = popularAdapter.getCurrentList()[position]
                viewModel.getSongEntity(song.toTrack().toSongEntity())
                val dialog = BottomSheetDialog(requireContext())
                val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
                with(bottomSheetView) {
                    btSleepTimer.visibility = View.GONE
                    viewModel.songEntity.observe(viewLifecycleOwner) { songEntity ->
                        if (songEntity != null) {
                            if (songEntity.liked) {
                                tvFavorite.text = getString(R.string.liked)
                                cbFavorite.isChecked = true
                            } else {
                                tvFavorite.text = getString(R.string.like)
                                cbFavorite.isChecked = false
                            }
                        }
                    }
                    btAddQueue.setOnClickListener {
                        sharedViewModel.addToQueue(song.toTrack())
                    }
                    btPlayNext.setOnClickListener {
                        sharedViewModel.playNext(song.toTrack())
                    }
                    setEnabledAll(btAlbum, true)
                    tvAlbum.text = song.album.name
                    btAlbum.setOnClickListener {
                        val albumId = song.album.id
                        findNavController().navigateSafe(
                            R.id.action_global_albumFragment,
                            Bundle().apply {
                                putString("browseId", albumId)
                            })
                        dialog.dismiss()
                    }
                    btChangeLyricsProvider.visibility = View.GONE
                    tvSongTitle.text = song.title
                    tvSongTitle.isSelected = true
                    tvSongArtist.text = song.artists.toListName().connectArtists()
                    tvSongArtist.isSelected = true
                    ivThumbnail.load(song.thumbnails.lastOrNull()?.url)
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
                        if (cbFavorite.isChecked) {
                            cbFavorite.isChecked = false
                            tvFavorite.text = getString(R.string.like)
                            viewModel.updateLikeStatus(song.videoId, 0)
                        } else {
                            cbFavorite.isChecked = true
                            tvFavorite.text = getString(R.string.liked)
                            viewModel.updateLikeStatus(song.videoId, 1)
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
                        val subBottomSheetView =
                            BottomSheetSeeArtistOfNowPlayingBinding.inflate(layoutInflater)
                        if (!song.artists.isNullOrEmpty()) {
                            val artistAdapter = SeeArtistOfNowPlayingAdapter(song.artists)
                            subBottomSheetView.rvArtists.apply {
                                adapter = artistAdapter
                                layoutManager = LinearLayoutManager(requireContext())
                            }
                            artistAdapter.setOnClickListener(object :
                                SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                override fun onItemClick(position: Int) {
                                    val artist = song.artists[position]
                                    if (artist.id != null) {
                                        findNavController().navigateSafe(
                                            R.id.action_global_artistFragment,
                                            Bundle().apply {
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
                    btDownload.visibility = View.GONE
                    btAddPlaylist.setOnClickListener {
                        viewModel.getLocalPlaylist()
                        val listLocalPlaylist: ArrayList<LocalPlaylistEntity> = arrayListOf()
                        val addPlaylistDialog = BottomSheetDialog(requireContext())
                        val viewAddPlaylist =
                            BottomSheetAddToAPlaylistBinding.inflate(layoutInflater)
                        val addToAPlaylistAdapter = AddToAPlaylistAdapter(arrayListOf())
                        addToAPlaylistAdapter.setVideoId(song.videoId)
                        viewAddPlaylist.rvLocalPlaylists.apply {
                            adapter = addToAPlaylistAdapter
                            layoutManager = LinearLayoutManager(requireContext())
                        }
                        viewModel.listLocalPlaylist.observe(viewLifecycleOwner) { list ->
                            Log.d("Check Local Playlist", list.toString())
                            listLocalPlaylist.clear()
                            listLocalPlaylist.addAll(list)
                            addToAPlaylistAdapter.updateList(listLocalPlaylist)
                        }
                        addToAPlaylistAdapter.setOnItemClickListener(object :
                            AddToAPlaylistAdapter.OnItemClickListener {
                            override fun onItemClick(position: Int) {
                                val playlist = listLocalPlaylist[position]
                                viewModel.updateInLibrary(song.videoId)
                                val tempTrack = ArrayList<String>()
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
                    btShare.setOnClickListener {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        val url = "https://youtube.com/watch?v=${song.videoId}"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                        val chooserIntent =
                            Intent.createChooser(shareIntent, getString(R.string.share_url))
                        startActivity(chooserIntent)
                    }
                    dialog.setCancelable(true)
                    dialog.setContentView(bottomSheetView.root)
                    dialog.show()
                }
            }
        })
        binding.topAppBarLayout.addOnOffsetChangedListener { it, verticalOffset ->
            Log.d("ArtistFragment", "Offset: $verticalOffset" + "Total: ${it.totalScrollRange}")
            if(abs(it.totalScrollRange) == abs(verticalOffset)) {
                    binding.toolBar.background = viewModel.gradientDrawable.value
                    if (viewModel.gradientDrawable.value != null ){
                        if (viewModel.gradientDrawable.value?.colors != null){
                            requireActivity().window.statusBarColor = viewModel.gradientDrawable.value?.colors!!.first()
                        }
                    }
                }
            else
                {
                    binding.toolBar.background = null
                    requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
                    Log.d("ArtistFragment", "Expanded")
                }
            }
        binding.btRadio.setOnClickListener {
            val radioId = viewModel.artistBrowse.value?.data?.radioId
            if (radioId != null){
                val args = Bundle()
                args.putString("radioId", radioId)
                args.putString("channelId", viewModel.artistBrowse.value?.data?.channelId)
                findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
            }
            else {
                Snackbar.make(binding.root, getString(R.string.error), Snackbar.LENGTH_LONG).show()
            }
        }
        binding.btFollow.setOnClickListener {
            val id = viewModel.artistEntity.value?.channelId
            if (id  != null) {
                Log.d("ChannelId", id)
                if (binding.btFollow.text == getString(R.string.follow)){
                    viewModel.updateFollowed(1, id)
                    binding.btFollow.text = getString(R.string.followed)
                }
                else {
                    viewModel.updateFollowed(0, id)
                    binding.btFollow.text =  getString(R.string.follow)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val followJob = launch {
                    viewModel.followed.collect { followed ->
                        if (followed) {
                            binding.btFollow.text =  getString(R.string.followed)
                        }
                        else {
                            binding.btFollow.text =  getString(R.string.follow)
                        }
                    }
                }
                val artistJob = launch {
                    viewModel.artistBrowse.collectLatest { response ->
                        when(response){
                            is Resource.Success -> {
                                response.data.let {
                                    with(binding){
                                        if (it != null){
                                            viewModel.insertArtist(ArtistEntity(it.channelId!!, it.name,
                                                it.thumbnails?.first()?.url
                                            ))
                                        }
                                        topAppBar.title = it?.name.toString()
                                        if (it?.thumbnails?.size!! > 1){
                                            loadImage(it.thumbnails[1].url)
                                        }
                                        else {
                                            loadImage(it.thumbnails[0].url)
                                        }
                                        if (viewModel.gradientDrawable.value == null){
                                            viewModel.gradientDrawable.observe(viewLifecycleOwner) { gd ->
                                                binding.cardBelowAppBarLayout.background = gd
                                                binding.aboutContainer.background = gd
                                                Log.d("Load Gradient from Image", gd.toString())
                                            }
                                        }
                                        else {
                                            Log.d("Load Gradient from Cache", gradientDrawable.toString())
                                            binding.cardBelowAppBarLayout.background = gradientDrawable
                                            binding.aboutContainer.background = gradientDrawable
                                        }
                                        tvSubscribers.text = it.subscribers
                                        if (it.views == null){
                                            tvViews.text = ""
                                        }
                                        else {
                                            tvViews.text = it.views.toString()
                                        }
                                        tvDescription.originalText = (it.description ?: getString(R.string.no_description)).toString()
                                        if (it.songs?.results != null) {
                                            popularAdapter.updateList(it.songs.results as ArrayList<ResultSong>)
                                            if (it.songs.results.isEmpty()) {
                                                binding.tvPopular.visibility = View.GONE
                                            }
                                        } else {
                                            binding.tvPopular.visibility = View.GONE
                                        }
                                        if (it.singles?.results != null) {
                                            singlesAdapter.updateList(it.singles.results as ArrayList<ResultSingle>)
                                            if (it.singles.results.isEmpty()) {
                                                binding.tvSingles.visibility = View.GONE
                                            }
                                        } else {
                                            binding.tvSingles.visibility = View.GONE
                                        }
                                        if (it.albums?.results != null) {
                                            albumsAdapter.updateList(it.albums.results as ArrayList<ResultAlbum>)
                                            if (it.albums.results.isEmpty()) {
                                                binding.tvAlbums.visibility = View.GONE
                                            }
                                        } else {
                                            binding.tvAlbums.visibility = View.GONE
                                        }
                                        if (it.video != null) {
                                            videoAdapter.updateList(it.video as ArrayList<ResultVideo>)
                                            if (it.video.isEmpty()) {
                                                binding.tvVideo.visibility = View.GONE
                                            }
                                        } else {
                                            binding.tvVideo.visibility = View.GONE
                                        }
                                        if (it.featuredOn != null) {
                                            featuredOnAdapter.updateList(it.featuredOn as ArrayList<ResultPlaylist>)
                                            if (it.featuredOn.isEmpty()) {
                                                binding.tvFeaturedOn.visibility = View.GONE
                                            }
                                        } else {
                                            binding.tvFeaturedOn.visibility = View.GONE
                                        }
                                        if (it.related?.results != null) {
                                            relatedArtistsAdapter.updateList(it.related.results as ArrayList<ResultRelated>)
                                            if (it.related.results.isEmpty()) {
                                                binding.tvRelatedArtists.visibility = View.GONE
                                            }
                                        } else {
                                            binding.tvRelatedArtists.visibility = View.GONE
                                        }
                                    }
                                    binding.loadingLayout.visibility = View.GONE
                                    binding.topAppBarLayout.visibility = View.VISIBLE
                                    binding.rootLayout.visibility = View.VISIBLE
                                    binding.rootFull.visibility = View.VISIBLE
                                }
                            }
                            is Resource.Error -> {
                                binding.loadingLayout.visibility = View.GONE
                                Snackbar.make(binding.root, "${response.message}", Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.retry)) {
                                        if (channelId != null) {
                                            fetchData(channelId)
                                        }
                                    }
                                    .show()
                                findNavController().popBackStack()
                            }

                            else -> {
                                binding.loadingLayout.visibility = View.VISIBLE
                                binding.topAppBarLayout.visibility = View.GONE
                                binding.rootLayout.visibility = View.GONE
                                binding.rootFull.visibility = View.GONE
                            }
                        }
                    }
                }
                val job1 = launch {
                    sharedViewModel.downloadList.collect {
                        popularAdapter.setDownloadedList(it)
                    }
                }
                val job2 = launch {
                    combine(sharedViewModel.simpleMediaServiceHandler?.nowPlaying ?: flowOf<MediaItem?>(null), sharedViewModel.isPlaying) { nowPlaying, isPlaying ->
                        Pair(nowPlaying, isPlaying)
                    }.collect {
                        if (it.first != null && it.second) {
                            popularAdapter.setNowPlaying(it.first!!.mediaId)
                        } else {
                            popularAdapter.setNowPlaying(null)
                        }
                    }
                }
                artistJob.join()
                followJob.join()
                job1.join()
                job2.join()
            }
        }

    }
    private fun fetchData(channelId: String){
        viewModel.browseArtist(channelId)
    }

    private fun loadImage(url: String) {
        binding.ivArtistImage.load(url) {
            memoryCachePolicy(CachePolicy.DISABLED)
            placeholder(R.drawable.holder_video)
            crossfade(true)
            transformations(object : Transformation {
                override val cacheKey: String
                    get() = "paletteTransformer"

                override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                    val p = Palette.from(input).generate()
                    val defaultColor = 0x000000
                    var startColor = p.getDarkVibrantColor(defaultColor)
                    Log.d("Check Start Color", "transform: $startColor")
                    if (startColor == defaultColor) {
                        startColor = p.getDarkMutedColor(defaultColor)
                        if (startColor == defaultColor){
                            startColor = p.getVibrantColor(defaultColor)
                            if (startColor == defaultColor){
                                startColor = p.getMutedColor(defaultColor)
                                if (startColor == defaultColor){
                                    startColor = p.getLightVibrantColor(defaultColor)
                                    if (startColor == defaultColor){
                                        startColor = p.getLightMutedColor(defaultColor)
                                    }
                                }
                            }
                        }
                        Log.d("Check Start Color", "transform: $startColor")
                    }
//                    val centerColor = 0x6C6C6C
                    val endColor = resources.getColor(R.color.md_theme_dark_background, null)
                    startColor = ColorUtils.setAlphaComponent(startColor, 150)
                    Log.d("Check End Color", "transform: $endColor")
                    val gd = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(startColor, endColor)
                    )
                    gd.cornerRadius = 0f
                    gd.gradientType = GradientDrawable.LINEAR_GRADIENT
                    gd.gradientRadius = 0.2f
                    viewModel.gradientDrawable.postValue(gd)
                    return input
                }

            })
        }

    }
}
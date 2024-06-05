package com.universe.audioflare.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.universe.audioflare.R
import com.universe.audioflare.common.DownloadState
import com.universe.audioflare.common.DownloadState.STATE_DOWNLOADED
import com.universe.audioflare.common.DownloadState.STATE_DOWNLOADING
import com.universe.audioflare.common.DownloadState.STATE_NOT_DOWNLOADED
import com.universe.audioflare.data.db.entities.LocalPlaylistEntity
import com.universe.audioflare.data.db.entities.PairSongLocalPlaylist
import com.universe.audioflare.data.db.entities.SetVideoIdEntity
import com.universe.audioflare.data.db.entities.SongEntity
import com.universe.audioflare.data.model.browse.album.Track
import com.universe.audioflare.data.repository.MainRepository
import com.universe.audioflare.extension.toListVideoId
import com.universe.audioflare.extension.toSongEntity
import com.universe.audioflare.service.test.download.DownloadUtils
import com.universe.audioflare.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class LocalPlaylistViewModel
    @Inject
    constructor(
        private val mainRepository: MainRepository,
        private val application: Application,
    ) : AndroidViewModel(application) {
        @Inject
        lateinit var downloadUtils: DownloadUtils

        val id: MutableLiveData<Long> = MutableLiveData()

        private var _localPlaylist: MutableStateFlow<LocalPlaylistEntity?> =
            MutableStateFlow(null)
        val localPlaylist: StateFlow<LocalPlaylistEntity?> = _localPlaylist

        private var _listTrack: MutableStateFlow<List<SongEntity>?> = MutableStateFlow(null)
        val listTrack: StateFlow<List<SongEntity>?> = _listTrack

        private var _listPair: MutableStateFlow<List<PairSongLocalPlaylist>?> =
            MutableStateFlow(null)
        val listPair: StateFlow<List<PairSongLocalPlaylist>?> = _listPair

        private var _offset: MutableStateFlow<Int> = MutableStateFlow(0)
        val offset: StateFlow<Int> = _offset

        fun setOffset(offset: Int) {
            _offset.value = offset
        }

        var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()

        private var _listSuggestions: MutableStateFlow<ArrayList<Track>?> =
            MutableStateFlow(arrayListOf())
        val listSuggestions: StateFlow<ArrayList<Track>?> = _listSuggestions

        private var reloadParams: MutableStateFlow<String?> = MutableStateFlow(null)

        var loading: MutableStateFlow<Boolean> = MutableStateFlow(false)

        private val _loadingMore = MutableStateFlow<Boolean>(false)
        val loadingMore: StateFlow<Boolean> get() = _loadingMore

        private var _filter: MutableStateFlow<FilterState> =
            MutableStateFlow(FilterState.OlderFirst)
        val filter: StateFlow<FilterState> = _filter

        fun setFilter(filterState: FilterState) {
            _filter.value = filterState
        }

        private var _brush: MutableStateFlow<List<Color>> =
            MutableStateFlow(
                listOf(
                    Color.Black,
                    Color(
                        application.resources.getColor(R.color.md_theme_dark_background, null),
                    ),
                ),
            )
        val brush: StateFlow<List<Color>> = _brush

        fun setBrush(brush: List<Color>) {
            _brush.value = brush
        }

        fun resetBrush() {
            Log.w("resetBrush", "resetBrush: ")
            _brush.value =
                listOf(
                    Color.Black,
                    Color(
                        application.resources.getColor(R.color.md_theme_dark_background, null),
                    ),
                )
        }

        init {
            viewModelScope.launch {
                val checkDownloadedJob =
                    launch {
                        listTrack.collect {
                            if (it != null) {
                                mainRepository.getPlaylistPairSong(id.value!!).collect {
                                    Log.w("Pair LocalPlaylistViewModel", "init: ${it?.size}")
                                }
                                val temp: ArrayList<SongEntity> = arrayListOf()
                                var count = 0
                                it.forEach { track ->
                                    temp.add(track)
                                    if (track.downloadState == STATE_DOWNLOADED) {
                                        count++
                                    }
                                }
                                localPlaylist.value?.id?.let { id ->
                                    if (count == it.size &&
                                        localPlaylist.value?.downloadState != STATE_DOWNLOADED
                                    ) {
                                        updatePlaylistDownloadState(
                                            id,
                                            STATE_DOWNLOADED,
                                        )
                                        getLocalPlaylist(id)
                                    } else if (
                                        count != it.size &&
                                        localPlaylist.value?.downloadState != STATE_NOT_DOWNLOADED &&
                                        localPlaylist.value?.downloadState != STATE_DOWNLOADING
                                    ) {
                                        updatePlaylistDownloadState(
                                            id,
                                            STATE_NOT_DOWNLOADED,
                                        )
                                        getLocalPlaylist(id)
                                    }
                                }
                            }
                        }
                    }
                checkDownloadedJob.join()
            }
        }

        fun getSuggestions(ytPlaylistId: String) {
            loading.value = true
            viewModelScope.launch {
                mainRepository.getSuggestionPlaylist(ytPlaylistId).collect {
                    _listSuggestions.value = it?.second
                    reloadParams.value = it?.first
                    withContext(Dispatchers.Main) {
                        loading.value = false
                    }
                }
            }
        }

        private val _songEntity = MutableStateFlow<SongEntity?>(null)
        val songEntity: StateFlow<SongEntity?> get() = _songEntity

        fun getSongEntity(song: SongEntity) {
            viewModelScope.launch {
                mainRepository.insertSong(song).first().let {
                    println("Insert song $it")
                }
                delay(200)
                mainRepository.getSongById(song.videoId).collect {
                    if (it != null) _songEntity.emit(it)
                }
            }
        }

        fun reloadSuggestion() {
            loading.value = true
            viewModelScope.launch {
                val param = reloadParams.value
                if (param != null) {
                    mainRepository.reloadSuggestionPlaylist(param).collect {
                        _listSuggestions.value = it?.second
                        reloadParams.value = it?.first
                        withContext(Dispatchers.Main) {
                            loading.value = false
                        }
                    }
                } else {
                    Toast.makeText(
                        application,
                        application.getString(R.string.error),
                        Toast.LENGTH_SHORT,
                    ).show()
                    withContext(Dispatchers.Main) {
                        loading.value = false
                    }
                }
            }
        }

        fun getLocalPlaylist(id: Long) {
            viewModelScope.launch {
                mainRepository.getLocalPlaylist(id).collect {
                    _localPlaylist.emit(it)
                }
            }
        }

        fun getListTrack(
            playlistId: Long,
            offset: Int,
            filterState: FilterState,
        ) {
            viewModelScope.launch {
                _loadingMore.value = true
                val pairJob =
                    launch {
                        mainRepository.getPlaylistPairSongByOffset(
                            playlistId,
                            offset,
                            filterState,
                        ).singleOrNull().let { listPairPlaylist ->
                            Log.w("Pair", "getListTrack: $listPairPlaylist")
                            if (listPairPlaylist != null) {
                                if (_listPair.value == null || offset == 0) {
                                    _listPair.value = listPairPlaylist
                                } else {
                                    val temp: ArrayList<PairSongLocalPlaylist> = arrayListOf()
                                    temp.addAll(_listPair.value ?: emptyList())
                                    temp.addAll(listPairPlaylist)
                                    _listPair.value = temp
                                }
                                setOffset(offset + 1)
                                Log.w("Pair LocalPlaylistViewModel", "offset: ${_offset.value}")
                                Log.w("Pair LocalPlaylistViewModel", "listPair: $listPairPlaylist")
                                mainRepository.getSongsByListVideoId(
                                    listPairPlaylist.map { it.songId },
                                ).collect { list ->
                                    val temp = mutableListOf<SongEntity>()
                                    temp.addAll(listTrack.value ?: emptyList())
                                    val newList =
                                        listPairPlaylist.mapNotNull { pair ->
                                            list.find { it.videoId == pair.songId }
                                        }
                                    temp.addAll(newList)
                                    _listTrack.value = temp
                                    _loadingMore.value = false
                                    Log.w("Pair", "getListTrack: ${_listTrack.value}")
                                    if (listTrack.value?.size == localPlaylist.value?.tracks?.size && localPlaylist.value?.tracks != null) {
                                        setOffset(-1)
                                    }
                                }
                            } else {
                                setOffset(-1)
                            }
                        }
                    }
                pairJob.join()
//                mainRepository.getSongsByListVideoIdOffset(list, offset).collect {
//                    val temp: ArrayList<SongEntity> = arrayListOf()
//                    var count = 0
//                    it.forEach { track ->
//                        temp.add(track)
//                        if (track.downloadState == DownloadState.STATE_DOWNLOADED) {
//                            count++
//                        }
//                    }
//                    if (_listTrack.value == null) {
//                        _listTrack.value = (temp)
//                    } else {
//                        val temp2: ArrayList<SongEntity> = arrayListOf()
//                        temp2.addAll(_listTrack.value!!)
//                        temp2.addAll(temp)
//                        _listTrack.value = temp2
//                    }
//                    localPlaylist.value?.id?.let {
//                            id ->
//                        getPairSongLocalPlaylist(id)
//                        if (count == it.size &&
//                            localPlaylist.value?.downloadState != DownloadState.STATE_DOWNLOADED
//                        ) {
//                            updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADED)
//                            getLocalPlaylist(id)
//                        } else if (
//                            count != it.size &&
//                            localPlaylist.value?.downloadState != DownloadState.STATE_NOT_DOWNLOADED &&
//                            localPlaylist.value?.downloadState != DownloadState.STATE_DOWNLOADING
//                        ) {
//                            updatePlaylistDownloadState(
//                                id,
//                                DownloadState.STATE_NOT_DOWNLOADED,
//                            )
//                            getLocalPlaylist(id)
//                        }
//                    }
//                }
            }
        }

        val playlistDownloadState: MutableStateFlow<Int> =
            MutableStateFlow(STATE_NOT_DOWNLOADED)

        fun updatePlaylistDownloadState(
            id: Long,
            state: Int,
        ) {
            viewModelScope.launch {
                mainRepository.getLocalPlaylist(id).collect { playlist ->
                    _localPlaylist.value = playlist
                    mainRepository.updateLocalPlaylistDownloadState(state, id)
                    playlistDownloadState.value = state
                }
            }
        }

        val listJob: MutableStateFlow<ArrayList<SongEntity>> = MutableStateFlow(arrayListOf())

//        var downloadState: StateFlow<List<Download?>>
//        viewModelScope.launch {
//            downloadState = downloadUtils.getAllDownloads().stateIn(viewModelScope)
//            downloadState.collectLatest { down ->
//                if (down.isNotEmpty()){
//                    var count = 0
//                    down.forEach { downloadItem ->
//                        if (downloadItem?.state == Download.STATE_COMPLETED) {
//                            count++
//                        }
//                        else if (downloadItem?.state == Download.STATE_FAILED) {
//                            updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADING)
//                        }
//                    }
//                    if (count == down.size) {
//                        mainRepository.getLocalPlaylist(id).collect{ playlist ->
//                            mainRepository.getSongsByListVideoId(playlist.tracks!!).collect{ tracks ->
//                                tracks.forEach { track ->
//                                    if (track.downloadState != DownloadState.STATE_DOWNLOADED) {
//                                        mainRepository.updateDownloadState(track.videoId, DownloadState.STATE_NOT_DOWNLOADED)
//                                        Toast.makeText(getApplication(), "Download Failed", Toast.LENGTH_SHORT).show()
//                                    }
//                                }
//                            }
//                        }
//                        Log.d("Check Downloaded", "Downloaded")
//                        updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADED)
//                        Toast.makeText(getApplication(), "Download Completed", Toast.LENGTH_SHORT).show()
//                    }
//                    else {
//                        updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADING)
//                    }
//                }
//                else {
//                    updatePlaylistDownloadState(id, DownloadState.STATE_NOT_DOWNLOADED)
//                }
//            }
//        }
//    }

        @UnstableApi
        fun getDownloadStateFromService(videoId: String) {
            viewModelScope.launch {
                val downloadState = downloadUtils.getDownload(videoId).stateIn(viewModelScope)
                downloadState.collect { down ->
                    if (down != null) {
                        when (down.state) {
                            Download.STATE_COMPLETED -> {
                                mainRepository.getSongById(videoId).collect { song ->
                                    if (song?.downloadState != STATE_DOWNLOADED) {
                                        mainRepository.updateDownloadState(
                                            videoId,
                                            STATE_DOWNLOADED,
                                        )
                                        _listTrack.value?.find { it.videoId == videoId }?.copy(downloadState = STATE_DOWNLOADED)?.let { copy ->
                                            val temp: ArrayList<SongEntity> = arrayListOf()
                                            temp.addAll(listTrack.value ?: emptyList())
                                            temp.replaceAll { s -> if (s.videoId == videoId) copy else s }
                                            _listTrack.value = temp.toList()
                                        }
                                    }
                                }
                                Log.d("Check Downloaded", "Downloaded")
                            }

                            Download.STATE_FAILED -> {
                                mainRepository.getSongById(videoId).collect { song ->
                                    if (song?.downloadState != STATE_NOT_DOWNLOADED) {
                                        mainRepository.updateDownloadState(
                                            videoId,
                                            STATE_NOT_DOWNLOADED,
                                        )
                                    }
                                }
                                Log.d("Check Downloaded", "Failed")
                            }

                            Download.STATE_DOWNLOADING -> {
                                mainRepository.getSongById(videoId).collect { song ->
                                    if (song?.downloadState != DownloadState.STATE_DOWNLOADING) {
                                        mainRepository.updateDownloadState(
                                            videoId,
                                            DownloadState.STATE_DOWNLOADING,
                                        )
                                    }
                                }
                                Log.d("Check Downloaded", "Downloading ${down.percentDownloaded}")
                            }

                            Download.STATE_QUEUED -> {
                                mainRepository.getSongById(videoId).collect { song ->
                                    if (song?.downloadState != DownloadState.STATE_PREPARING) {
                                        mainRepository.updateDownloadState(
                                            videoId,
                                            DownloadState.STATE_PREPARING,
                                        )
                                    }
                                }
                                Log.d("Check Downloaded", "Queued")
                            }

                            else -> {
                                Log.d("Check Downloaded", "Not Downloaded")
                            }
                        }
                    }
                }
            }
        }

        fun updatePlaylistTitle(
            title: String,
            id: Long,
        ) {
            viewModelScope.launch {
                mainRepository.updateLocalPlaylistTitle(title, id)
                delay(100)
                getLocalPlaylist(id)
            }
        }

        fun deletePlaylist(id: Long) {
            viewModelScope.launch {
                mainRepository.deleteLocalPlaylist(id)
            }
        }

        fun updatePlaylistThumbnail(
            uri: String,
            id: Long,
        ) {
            viewModelScope.launch {
                mainRepository.updateLocalPlaylistThumbnail(uri, id)
                delay(100)
                getLocalPlaylist(id)
            }
        }

        fun clearLocalPlaylist() {
            _localPlaylist.value = null
        }

        fun updateDownloadState(
            videoId: String,
            state: Int,
        ) {
            viewModelScope.launch {
                mainRepository.updateDownloadState(videoId, state)
            }
        }

        fun deleteItem(
            song: SongEntity?,
            id: Long,
        ) {
            viewModelScope.launch {
                if (song != null) {
                    val songPosition = listPair.value?.find { it.songId == song.videoId }?.position
                    if (songPosition != null) {
                        val tempPair = mutableListOf<PairSongLocalPlaylist>()
                        tempPair.addAll(listPair.value ?: listOf())
                        for (i in songPosition + 1 until tempPair.size) {
                            tempPair.replaceAll { if (it == tempPair[i]) it.copy(position = it.position - 1) else it }
                        }
                        tempPair.removeAt(songPosition)
                        _listPair.value = tempPair

                        mainRepository.deletePairSongLocalPlaylist(id, song.videoId)
                        delay(500)
                        val temp = mutableListOf<SongEntity>()
                        temp.addAll(_listTrack.value ?: listOf())
                        temp.remove(song)
                        _listTrack.value = temp
                    }
                }
            }
        }

        @UnstableApi
        fun downloadFullPlaylistState(id: Long) {
            viewModelScope.launch {
                downloadUtils.downloads.collect { download ->
                    playlistDownloadState.value =
                        if (listJob.value.all { download[it.videoId]?.state == Download.STATE_COMPLETED }) {
                            mainRepository.updateLocalPlaylistDownloadState(
                                STATE_DOWNLOADED,
                                id,
                            )
                            STATE_DOWNLOADED
                        } else if (listJob.value.all {
                                download[it.videoId]?.state == Download.STATE_QUEUED ||
                                    download[it.videoId]?.state == Download.STATE_DOWNLOADING ||
                                    download[it.videoId]?.state == Download.STATE_COMPLETED
                            }
                        ) {
                            mainRepository.updateLocalPlaylistDownloadState(
                                DownloadState.STATE_DOWNLOADING,
                                id,
                            )
                            DownloadState.STATE_DOWNLOADING
                        } else {
                            mainRepository.updateLocalPlaylistDownloadState(
                                STATE_NOT_DOWNLOADED,
                                id,
                            )
                            STATE_NOT_DOWNLOADED
                        }
                }
            }
        }

        private var _listSetVideoId: MutableStateFlow<ArrayList<SetVideoIdEntity>?> =
            MutableStateFlow(null)
        val listSetVideoId: StateFlow<ArrayList<SetVideoIdEntity>?> = _listSetVideoId

        fun getSetVideoId(youtubePlaylistId: String) {
            viewModelScope.launch {
                mainRepository.getYouTubeSetVideoId(youtubePlaylistId).collect {
                    _listSetVideoId.value = it
                }
            }
        }

        fun removeYouTubePlaylistItem(
            youtubePlaylistId: String,
            videoId: String,
        ) {
            viewModelScope.launch {
                mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                    localPlaylist.value?.id!!,
                    LocalPlaylistEntity.YouTubeSyncState.Syncing,
                )
                mainRepository.removeYouTubePlaylistItem(youtubePlaylistId, videoId).collect {
                    if (it == 200) {
                        Toast.makeText(
                            application,
                            application.getString(R.string.removed_from_YouTube_playlist),
                            Toast.LENGTH_SHORT,
                        ).show()
                        mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                            localPlaylist.value?.id!!,
                            LocalPlaylistEntity.YouTubeSyncState.Synced,
                        )
                    } else {
                        Toast.makeText(
                            application,
                            application.getString(R.string.error),
                            Toast.LENGTH_SHORT,
                        ).show()
                        mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                            localPlaylist.value?.id!!,
                            LocalPlaylistEntity.YouTubeSyncState.NotSynced,
                        )
                    }
                }
            }
        }

        fun syncPlaylistWithYouTubePlaylist(playlist: LocalPlaylistEntity) {
            viewModelScope.launch {
                mainRepository.createYouTubePlaylist(playlist).collect {
                    if (it != null) {
                        val ytId = "VL$it"
                        mainRepository.updateLocalPlaylistYouTubePlaylistId(playlist.id, ytId)
                        mainRepository.updateLocalPlaylistYouTubePlaylistSynced(playlist.id, 1)
                        mainRepository.getLocalPlaylistByYoutubePlaylistId(ytId).collect { yt ->
                            if (yt != null) {
                                mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                                    yt.id,
                                    LocalPlaylistEntity.YouTubeSyncState.Synced,
                                )
                                mainRepository.getLocalPlaylist(playlist.id).collect { last ->
                                    _localPlaylist.emit(last)
                                    Toast.makeText(
                                        application,
                                        application.getString(R.string.synced),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(
                            application,
                            application.getString(R.string.error),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }

        fun unsyncPlaylistWithYouTubePlaylist(playlist: LocalPlaylistEntity) {
            viewModelScope.launch {
                mainRepository.updateLocalPlaylistYouTubePlaylistId(playlist.id, null)
                mainRepository.updateLocalPlaylistYouTubePlaylistSynced(playlist.id, 0)
                mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                    playlist.id,
                    LocalPlaylistEntity.YouTubeSyncState.NotSynced,
                )
                mainRepository.getLocalPlaylist(playlist.id).collect { last ->
                    if (last.syncedWithYouTubePlaylist == 0) {
                        _localPlaylist.emit(last)
                        Toast.makeText(
                            application,
                            application.getString(R.string.unsynced),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }

        fun updateYouTubePlaylistTitle(
            title: String,
            youtubePlaylistId: String,
        ) {
            viewModelScope.launch {
                mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                    localPlaylist.value?.id!!,
                    LocalPlaylistEntity.YouTubeSyncState.Syncing,
                )
                mainRepository.editYouTubePlaylist(title, youtubePlaylistId).collect { status ->
                    if (status == 200) {
                        mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                            localPlaylist.value?.id!!,
                            LocalPlaylistEntity.YouTubeSyncState.Synced,
                        )
                        Toast.makeText(
                            application,
                            application.getString(R.string.synced),
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                            localPlaylist.value?.id!!,
                            LocalPlaylistEntity.YouTubeSyncState.NotSynced,
                        )
                        Toast.makeText(
                            application,
                            application.getString(R.string.error),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }

        fun updateListTrackSynced(
            id: Long,
            list: List<String>,
            youtubeId: String,
        ) {
            viewModelScope.launch {
                mainRepository.getPlaylistData(youtubeId).collect { yt ->
                    if (yt is Resource.Success) {
                        if (yt.data != null) {
                            val listTrack: ArrayList<String> = arrayListOf()
                            listTrack.addAll(list)
                            yt.data.tracks.forEach { track ->
                                if (!list.contains(track.videoId)) {
                                    listTrack.add(track.videoId)
                                    mainRepository.insertSong(track.toSongEntity()).first().let {
                                        println("Insert song $it")
                                    }
                                    mainRepository.insertPairSongLocalPlaylist(
                                        PairSongLocalPlaylist(
                                            playlistId = id,
                                            songId = track.videoId,
                                            position = yt.data.tracks.indexOf(track),
                                            inPlaylist = LocalDateTime.now(),
                                        ),
                                    )
                                }
                            }
                            mainRepository.updateLocalPlaylistTracks(listTrack, id)
                            if (yt.data.tracks.size < list.size) {
                                list.forEach { track2 ->
                                    if (!yt.data.tracks.toListVideoId().contains(track2)) {
                                        mainRepository.addYouTubePlaylistItem(youtubeId, track2)
                                            .collect { status ->
                                                if (status == "STATUS_SUCCEEDED") {
                                                    Toast.makeText(
                                                        application,
                                                        application.getString(
                                                            R.string.added_to_youtube_playlist,
                                                        ),
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        application,
                                                        application.getString(R.string.error),
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                            }
                                    }
                                }
                            }
                            Toast.makeText(
                                application,
                                application.getString(R.string.synced),
                                Toast.LENGTH_SHORT,
                            ).show()
                            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                                id,
                                LocalPlaylistEntity.YouTubeSyncState.Synced,
                            )
                            mainRepository.getLocalPlaylist(id).collect { last ->
                                _localPlaylist.emit(last)
                            }
                        }
                    }
                }
            }
        }

        fun updateInLibrary(videoId: String) {
            viewModelScope.launch {
                mainRepository.updateSongInLibrary(LocalDateTime.now(), videoId)
            }
        }

        fun addToYouTubePlaylist(
            localPlaylistId: Long,
            youtubePlaylistId: String,
            videoId: String,
        ) {
            viewModelScope.launch {
                mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                    localPlaylistId,
                    LocalPlaylistEntity.YouTubeSyncState.Syncing,
                )
                mainRepository.addYouTubePlaylistItem(youtubePlaylistId, videoId).collect { response ->
                    if (response == "STATUS_SUCCEEDED") {
                        mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                            localPlaylistId,
                            LocalPlaylistEntity.YouTubeSyncState.Synced,
                        )
                        Toast.makeText(
                            getApplication(),
                            application.getString(R.string.added_to_youtube_playlist),
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                            localPlaylistId,
                            LocalPlaylistEntity.YouTubeSyncState.NotSynced,
                        )
                        Toast.makeText(
                            getApplication(),
                            application.getString(R.string.error),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }

        fun updateLocalPlaylistTracks(
            list: List<String>,
            id: Long,
        ) {
            viewModelScope.launch {
                mainRepository.getSongsByListVideoId(list).collect { values ->
                    var count = 0
                    values.forEach { song ->
                        if (song.downloadState == STATE_DOWNLOADED) {
                            count++
                        }
                    }
                    mainRepository.updateLocalPlaylistTracks(list, id)
                    Toast.makeText(
                        application,
                        application.getString(R.string.added_to_playlist),
                        Toast.LENGTH_SHORT,
                    ).show()
                    if (count == values.size) {
                        mainRepository.updateLocalPlaylistDownloadState(
                            STATE_DOWNLOADED,
                            id,
                        )
                    } else {
                        mainRepository.updateLocalPlaylistDownloadState(
                            STATE_NOT_DOWNLOADED,
                            id,
                        )
                    }
                    getLocalPlaylist(id)
                    getPairSongLocalPlaylist(id)
                }
            }
        }

        fun insertSong(song: Track) {
            viewModelScope.launch {
                mainRepository.insertSong(song.toSongEntity()).collect {
                    println("Insert Song $it")
                }
            }
        }

        fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
            viewModelScope.launch {
                mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
            }
        }

        fun getPairSongLocalPlaylist(id: Long) {
            viewModelScope.launch {
                mainRepository.getPlaylistPairSong(id).collect {
                    _listPair.value = (it)
                    Log.w("Pair", "getPairSongLocalPlaylist: $it")
                }
            }
        }

        fun removeListSuggestion() {
            _listSuggestions.value = null
        }

        fun removeData() {
            _localPlaylist.value = null
            _listTrack.value = null
            _listPair.value = null
        }

        fun updateLikeStatus(
            videoId: String,
            likeStatus: Int,
        ) {
            viewModelScope.launch {
                mainRepository.updateLikeStatus(likeStatus = likeStatus, videoId = videoId)
                delay(150)
                mainRepository.getSongById(videoId).collect { song ->
                    if (song != null) {
                        val temp = mutableListOf<SongEntity>()
                        temp.addAll(_listTrack.value ?: emptyList())
                        temp.replaceAll { s -> if (s.videoId == videoId) song else s }
                        _listTrack.value = temp.toList()
                    }
                }
            }
        }

        fun clearListPair() {
            _listPair.value = null
        }

        fun addSuggestTrackToListTrack(track: Track) {
            viewModelScope.launch {
                _listSuggestions.value?.remove(track)
                localPlaylist.value?.let {
                    runBlocking {
                        mainRepository.insertSong(track.toSongEntity()).firstOrNull()?.let {
                            println("Insert Song $it")
                        }
                        insertPairSongLocalPlaylist(
                            PairSongLocalPlaylist(
                                playlistId = it.id,
                                songId = track.videoId,
                                position = it.tracks?.size ?: 0,
                            ),
                        )
                    }
                    val temp = it.tracks?.toMutableList() ?: mutableListOf<String>()
                    temp.add(track.videoId)
                    if (it.youtubePlaylistId != null) {
                        addToYouTubePlaylist(it.id, it.youtubePlaylistId, track.videoId)
                    }
                    runBlocking {
                        updateLocalPlaylistTracks(temp, it.id)
                    }
                    if (offset.value > 0) {
                        setOffset(offset.value - 1)
                    }
                    getListTrack(it.id, offset.value, filter.value)
                }
            }
        }

        private val _fullListTracks = MutableStateFlow<MutableList<SongEntity>?>(null)
        val fullListTracks: StateFlow<MutableList<SongEntity>?> get() = _fullListTracks

        fun getAllTracksOfPlaylist(id: Long) {
            viewModelScope.launch {
                Log.w("Pair", "getAllTracksOfPlaylist: $id")
                val list: MutableList<SongEntity> = mutableListOf()
                var os = 0
                while (os >= 0) {
                    mainRepository.getPlaylistPairSongByOffset(id, os, FilterState.OlderFirst).singleOrNull().let { pairSongLocalPlaylists ->
                        if (!pairSongLocalPlaylists.isNullOrEmpty()) {
                            Log.w("Pair", "getAllTracksOfPlaylist: ${pairSongLocalPlaylists.size}")
                            mainRepository.getSongsByListVideoId(pairSongLocalPlaylists.map { it.songId }).firstOrNull().let {
                                if (!it.isNullOrEmpty()) {
                                    Log.w("Pair", "getAllTracksOfPlaylist: $it")
                                    list.addAll(it)
                                    os++
                                } else {
                                    os = -1
                                }
                            }
                        } else {
                            os = -1
                        }
                    }
                }
                _fullListTracks.value = list
                Log.w("Pair", "getAllTracksOfPlaylist: ${_fullListTracks.value}")
            }
        }

        fun removeFullListTracks() {
            _fullListTracks.value = null
        }

        fun clearListTracks() {
            _listTrack.value = null
        }
    }

sealed class FilterState {
    data object OlderFirst : FilterState()

    data object NewerFirst : FilterState()
}
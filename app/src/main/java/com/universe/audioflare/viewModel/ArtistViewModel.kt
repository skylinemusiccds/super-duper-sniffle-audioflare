package com.universe.audioflare.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.universe.audioflare.R
import com.universe.audioflare.common.DownloadState
import com.universe.audioflare.common.SELECTED_LANGUAGE
import com.universe.audioflare.data.dataStore.DataStoreManager
import com.universe.audioflare.data.db.entities.ArtistEntity
import com.universe.audioflare.data.db.entities.LocalPlaylistEntity
import com.universe.audioflare.data.db.entities.PairSongLocalPlaylist
import com.universe.audioflare.data.db.entities.SongEntity
import com.universe.audioflare.data.model.browse.artist.ArtistBrowse
import com.universe.audioflare.data.repository.MainRepository
import com.universe.audioflare.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(private val application: Application, private val mainRepository: MainRepository, private var dataStoreManager: DataStoreManager): AndroidViewModel(application){
    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()
    private val _artistBrowse: MutableStateFlow<Resource<ArtistBrowse>?> = MutableStateFlow(null)
    var artistBrowse: StateFlow<Resource<ArtistBrowse>?> = _artistBrowse
    var loading = MutableLiveData<Boolean>()
    private var _artistEntity: MutableLiveData<ArtistEntity> = MutableLiveData()
    var artistEntity: LiveData<ArtistEntity> = _artistEntity
    private var _followed: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var followed: StateFlow<Boolean> = _followed

    private var _songEntity: MutableLiveData<SongEntity?> = MutableLiveData()
    val songEntity: LiveData<SongEntity?> = _songEntity
    private var _listLocalPlaylist: MutableLiveData<List<LocalPlaylistEntity>> = MutableLiveData()
    val listLocalPlaylist: LiveData<List<LocalPlaylistEntity>> = _listLocalPlaylist


    private var regionCode: String? = null
    private var language: String? = null

    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    fun browseArtist(channelId: String){
        loading.value = true
        _artistBrowse.value = null
        viewModelScope.launch {
            Log.d("ArtistViewModel", "lang: $language")
//            mainRepository.browseArtist(channelId, regionCode!!, SUPPORTED_LANGUAGE.serverCodes[SUPPORTED_LANGUAGE.codes.indexOf(language!!)]).collect { values ->
//                _artistBrowse.value = values
//            }
            mainRepository.getArtistData(channelId).collect {
                _artistBrowse.emit(it)
            }
            withContext(Dispatchers.Main){
                loading.value = false
            }
        }
    }

    fun insertArtist(artist: ArtistEntity){
        viewModelScope.launch {
            mainRepository.insertArtist(artist)
            mainRepository.updateArtistInLibrary(LocalDateTime.now(), artist.channelId)
            mainRepository.getArtistById(artist.channelId).collect{
                _artistEntity.value = it
                _followed.value = it.followed
                Log.d("ArtistViewModel", "insertArtist: ${it.followed}")
            }
        }
    }

    fun updateFollowed(followed: Int, channelId: String){
        viewModelScope.launch {
            _followed.value = followed == 1
            mainRepository.updateFollowedStatus(channelId, followed)
            Log.d("ArtistViewModel", "updateFollowed: ${_followed.value}")
        }
    }
    fun getLocation() {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    fun getSongEntity(song: SongEntity) {
        viewModelScope.launch {
            mainRepository.insertSong(song).first().let {
                println("Insert Song $it")
            }
            mainRepository.getSongById(song.videoId).collect { values ->
                _songEntity.value = values
            }
        }
    }

    fun updateLikeStatus(videoId: String, likeStatus: Int) {
        viewModelScope.launch {
            mainRepository.updateLikeStatus(likeStatus = likeStatus, videoId = videoId)
        }
    }

    fun getLocalPlaylist() {
        viewModelScope.launch {
            mainRepository.getAllLocalPlaylists().collect { values ->
                _listLocalPlaylist.postValue(values)
            }
        }
    }

    fun updateLocalPlaylistTracks(list: List<String>, id: Long) {
        viewModelScope.launch {
            mainRepository.getSongsByListVideoId(list).collect { values ->
                var count = 0
                values.forEach { song ->
                    if (song.downloadState == DownloadState.STATE_DOWNLOADED){
                        count++
                    }
                }
                mainRepository.updateLocalPlaylistTracks(list, id)
                Toast.makeText(getApplication(), application.getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
                if (count == values.size) {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_DOWNLOADED, id)
                }
                else {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_NOT_DOWNLOADED, id)
                }
            }
        }
    }
    fun addToYouTubePlaylist(localPlaylistId: Long, youtubePlaylistId: String, videoId: String) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.Syncing)
            mainRepository.addYouTubePlaylistItem(youtubePlaylistId, videoId).collect { response ->
                if (response == "STATUS_SUCCEEDED") {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.Synced)
                    Toast.makeText(getApplication(), application.getString(R.string.added_to_youtube_playlist), Toast.LENGTH_SHORT).show()
                }
                else {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.NotSynced)
                    Toast.makeText(getApplication(), application.getString(R.string.error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateInLibrary(videoId: String) {
        viewModelScope.launch {
            mainRepository.updateSongInLibrary(LocalDateTime.now(), videoId)
        }
    }

    fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
        viewModelScope.launch {
            mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }
    }

}
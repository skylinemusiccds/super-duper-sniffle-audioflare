package com.universe.audioflare.data.db

import com.universe.audioflare.data.db.entities.AlbumEntity
import com.universe.audioflare.data.db.entities.ArtistEntity
import com.universe.audioflare.data.db.entities.FollowedArtistSingleAndAlbum
import com.universe.audioflare.data.db.entities.GoogleAccountEntity
import com.universe.audioflare.data.db.entities.LocalPlaylistEntity
import com.universe.audioflare.data.db.entities.LyricsEntity
import com.universe.audioflare.data.db.entities.NewFormatEntity
import com.universe.audioflare.data.db.entities.NotificationEntity
import com.universe.audioflare.data.db.entities.PairSongLocalPlaylist
import com.universe.audioflare.data.db.entities.PlaylistEntity
import com.universe.audioflare.data.db.entities.QueueEntity
import com.universe.audioflare.data.db.entities.SearchHistory
import com.universe.audioflare.data.db.entities.SetVideoIdEntity
import com.universe.audioflare.data.db.entities.SongEntity
import com.universe.audioflare.data.db.entities.SongInfoEntity
import com.universe.audioflare.viewModel.FilterState
import java.time.LocalDateTime
import javax.inject.Inject

class LocalDataSource
    @Inject
    constructor(private val databaseDao: DatabaseDao) {
        suspend fun getAllRecentData() = databaseDao.getAllRecentData()

        suspend fun getAllDownloadedPlaylist() = databaseDao.getAllDownloadedPlaylist()

        suspend fun getSearchHistory() = databaseDao.getSearchHistory()

        suspend fun deleteSearchHistory() = databaseDao.deleteSearchHistory()

        suspend fun insertSearchHistory(searchHistory: SearchHistory) = databaseDao.insertSearchHistory(searchHistory)

        suspend fun getAllSongs() = databaseDao.getAllSongs()

        suspend fun getRecentSongs(
            limit: Int,
            offset: Int,
        ) = databaseDao.getRecentSongs(limit, offset)

        suspend fun getSongByListVideoId(
            primaryKeyList: List<String>,
            offset: Int,
        ) = databaseDao.getSongByListVideoId(primaryKeyList, offset)

        suspend fun getSongByListVideoIdFull(primaryKeyList: List<String>) = databaseDao.getSongByListVideoIdFull(primaryKeyList)

        suspend fun getDownloadedSongs() = databaseDao.getDownloadedSongs()

        suspend fun getDownloadedSongsAsFlow() = databaseDao.getDownloadedSongsAsFlow()

        suspend fun getDownloadingSongs() = databaseDao.getDownloadingSongs()

        suspend fun getLikedSongs() = databaseDao.getLikedSongs()

        suspend fun getLibrarySongs() = databaseDao.getLibrarySongs()

        suspend fun getSong(videoId: String) = databaseDao.getSong(videoId)

        fun getSongAsFlow(videoId: String) = databaseDao.getSongAsFlow(videoId)

        suspend fun insertSong(song: SongEntity) = databaseDao.insertSong(song)

        suspend fun updateListenCount(videoId: String) = databaseDao.updateTotalPlayTime(videoId)

        suspend fun updateLiked(
            liked: Int,
            videoId: String,
        ) = databaseDao.updateLiked(liked, videoId)

        suspend fun updateDurationSeconds(
            durationSeconds: Int,
            videoId: String,
        ) = databaseDao.updateDurationSeconds(durationSeconds, videoId)

        suspend fun updateSongInLibrary(
            inLibrary: LocalDateTime,
            videoId: String,
        ) = databaseDao.updateSongInLibrary(inLibrary, videoId)

        suspend fun getMostPlayedSongs() = databaseDao.getMostPlayedSongs()

        suspend fun updateDownloadState(
            downloadState: Int,
            videoId: String,
        ) = databaseDao.updateDownloadState(downloadState, videoId)

        suspend fun getAllArtists() = databaseDao.getAllArtists()

        suspend fun insertArtist(artist: ArtistEntity) = databaseDao.insertArtist(artist)

        suspend fun updateFollowed(
            followed: Int,
            channelId: String,
        ) = databaseDao.updateFollowed(followed, channelId)

        suspend fun getArtist(channelId: String) = databaseDao.getArtist(channelId)

        suspend fun getFollowedArtists() = databaseDao.getFollowedArtists()

        suspend fun updateArtistInLibrary(
            inLibrary: LocalDateTime,
            channelId: String,
        ) = databaseDao.updateArtistInLibrary(inLibrary, channelId)

        suspend fun getAllAlbums() = databaseDao.getAllAlbums()

        suspend fun insertAlbum(album: AlbumEntity) = databaseDao.insertAlbum(album)

        suspend fun updateAlbumLiked(
            liked: Int,
            albumId: String,
        ) = databaseDao.updateAlbumLiked(liked, albumId)

        suspend fun getAlbum(albumId: String) = databaseDao.getAlbum(albumId)

        suspend fun getLikedAlbums() = databaseDao.getLikedAlbums()

        suspend fun updateAlbumInLibrary(
            inLibrary: LocalDateTime,
            albumId: String,
        ) = databaseDao.updateAlbumInLibrary(inLibrary, albumId)

        suspend fun updateAlbumDownloadState(
            downloadState: Int,
            albumId: String,
        ) = databaseDao.updateAlbumDownloadState(downloadState, albumId)

        suspend fun getAllPlaylists() = databaseDao.getAllPlaylists()

        suspend fun insertPlaylist(playlist: PlaylistEntity) = databaseDao.insertPlaylist(playlist)

        suspend fun insertRadioPlaylist(playlist: PlaylistEntity) = databaseDao.insertRadioPlaylist(playlist)

        suspend fun updatePlaylistLiked(
            liked: Int,
            playlistId: String,
        ) = databaseDao.updatePlaylistLiked(liked, playlistId)

        suspend fun getPlaylist(playlistId: String) = databaseDao.getPlaylist(playlistId)

        suspend fun getLikedPlaylists() = databaseDao.getLikedPlaylists()

        suspend fun updatePlaylistInLibrary(
            inLibrary: LocalDateTime,
            playlistId: String,
        ) = databaseDao.updatePlaylistInLibrary(inLibrary, playlistId)

        suspend fun updatePlaylistDownloadState(
            downloadState: Int,
            playlistId: String,
        ) = databaseDao.updatePlaylistDownloadState(downloadState, playlistId)

        suspend fun getAllLocalPlaylists() = databaseDao.getAllLocalPlaylists()

        suspend fun getLocalPlaylist(id: Long) = databaseDao.getLocalPlaylist(id)

        suspend fun insertLocalPlaylist(localPlaylist: LocalPlaylistEntity) = databaseDao.insertLocalPlaylist(localPlaylist)

        suspend fun deleteLocalPlaylist(id: Long) = databaseDao.deleteLocalPlaylist(id)

        suspend fun updateLocalPlaylistTitle(
            title: String,
            id: Long,
        ) = databaseDao.updateLocalPlaylistTitle(title, id)

        suspend fun updateLocalPlaylistThumbnail(
            thumbnail: String,
            id: Long,
        ) = databaseDao.updateLocalPlaylistThumbnail(thumbnail, id)

        suspend fun updateLocalPlaylistTracks(
            tracks: List<String>,
            id: Long,
        ) = databaseDao.updateLocalPlaylistTracks(tracks, id)

        suspend fun updateLocalPlaylistInLibrary(
            inLibrary: LocalDateTime,
            id: Long,
        ) = databaseDao.updateLocalPlaylistInLibrary(inLibrary, id)

        suspend fun updateLocalPlaylistDownloadState(
            downloadState: Int,
            id: Long,
        ) = databaseDao.updateLocalPlaylistDownloadState(downloadState, id)

        suspend fun getDownloadedLocalPlaylists() = databaseDao.getDownloadedLocalPlaylists()

        suspend fun updateLocalPlaylistYouTubePlaylistId(
            id: Long,
            ytId: String?,
        ) = databaseDao.updateLocalPlaylistYouTubePlaylistId(id, ytId)

        suspend fun updateLocalPlaylistYouTubePlaylistSynced(
            id: Long,
            synced: Int,
        ) = databaseDao.updateLocalPlaylistYouTubePlaylistSynced(id, synced)

        suspend fun updateLocalPlaylistYouTubePlaylistSyncState(
            id: Long,
            syncState: Int,
        ) = databaseDao.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)

        suspend fun getSavedLyrics(videoId: String) = databaseDao.getLyrics(videoId)

        suspend fun insertLyrics(lyrics: LyricsEntity) = databaseDao.insertLyrics(lyrics)

        suspend fun getPreparingSongs() = databaseDao.getPreparingSongs()

        suspend fun insertNewFormat(format: NewFormatEntity) = databaseDao.insertNewFormat(format)

        suspend fun getNewFormat(videoId: String) = databaseDao.getNewFormat(videoId)

        suspend fun insertSongInfo(songInfo: SongInfoEntity) = databaseDao.insertSongInfo(songInfo)

        suspend fun getSongInfo(videoId: String) = databaseDao.getSongInfo(videoId)

        suspend fun recoverQueue(queueEntity: QueueEntity) = databaseDao.recoverQueue(queueEntity)

        suspend fun getQueue() = databaseDao.getQueue()

        suspend fun deleteQueue() = databaseDao.deleteQueue()

        suspend fun getLocalPlaylistByYoutubePlaylistId(playlistId: String) = databaseDao.getLocalPlaylistByYoutubePlaylistId(playlistId)

        suspend fun insertSetVideoId(setVideoIdEntity: SetVideoIdEntity) = databaseDao.insertSetVideoId(setVideoIdEntity)

        suspend fun getSetVideoId(videoId: String) = databaseDao.getSetVideoId(videoId)

        suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) =
            databaseDao.insertPairSongLocalPlaylist(pairSongLocalPlaylist)

        suspend fun getPlaylistPairSong(playlistId: Long) = databaseDao.getPlaylistPairSong(playlistId)

        suspend fun getPlaylistPairSongByOffset(
            playlistId: Long,
            offset: Int,
            filterState: FilterState,
        ) = if (filterState == FilterState.OlderFirst) {
            databaseDao.getPlaylistPairSongByOffsetAsc(
                playlistId,
                offset * 50,
            )
        } else {
            databaseDao.getPlaylistPairSongByOffsetDesc(playlistId, offset * 50)
        }

        suspend fun deletePairSongLocalPlaylist(
            playlistId: Long,
            videoId: String,
        ) = databaseDao.deletePairSongLocalPlaylist(playlistId, videoId)

        suspend fun getGoogleAccounts() = databaseDao.getAllGoogleAccount()

        suspend fun insertGoogleAccount(googleAccountEntity: GoogleAccountEntity) = databaseDao.insertGoogleAccount(googleAccountEntity)

        suspend fun getUsedGoogleAccount() = databaseDao.getUsedGoogleAccount()

        suspend fun deleteGoogleAccount(email: String) = databaseDao.deleteGoogleAccount(email)

        suspend fun updateGoogleAccountUsed(
            email: String,
            isUsed: Boolean,
        ) = databaseDao.updateGoogleAccountUsed(isUsed, email)

        suspend fun setInLibrary(
            videoId: String,
            inLibrary: LocalDateTime,
        ) = databaseDao.setInLibrary(videoId, inLibrary)

        suspend fun insertFollowedArtistSingleAndAlbum(followedArtistSingleAndAlbum: FollowedArtistSingleAndAlbum) =
            databaseDao.insertFollowedArtistSingleAndAlbum(followedArtistSingleAndAlbum)

        suspend fun deleteFollowedArtistSingleAndAlbum(channelId: String) = databaseDao.deleteFollowedArtistSingleAndAlbum(channelId)

        suspend fun getFollowedArtistSingleAndAlbum(channelId: String) = databaseDao.getFollowedArtistSingleAndAlbum(channelId)

        suspend fun getAllFollowedArtistSingleAndAlbums() = databaseDao.getAllFollowedArtistSingleAndAlbum()

        suspend fun insertNotification(notificationEntity: NotificationEntity) = databaseDao.insertNotification(notificationEntity)

        suspend fun getAllNotification() = databaseDao.getAllNotification()

        suspend fun deleteNotification(id: Long) = databaseDao.deleteNotification(id)
    }
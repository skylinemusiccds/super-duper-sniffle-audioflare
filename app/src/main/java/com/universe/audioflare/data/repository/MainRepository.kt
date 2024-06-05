package com.universe.audioflare.data.repository

import android.content.Context
import android.util.Log
import com.universe.kotlinytmusicscraper.YouTube
import com.universe.kotlinytmusicscraper.models.AccountInfo
import com.universe.kotlinytmusicscraper.models.MediaType
import com.universe.kotlinytmusicscraper.models.MusicShelfRenderer
import com.universe.kotlinytmusicscraper.models.SearchSuggestions
import com.universe.kotlinytmusicscraper.models.SongItem
import com.universe.kotlinytmusicscraper.models.WatchEndpoint
import com.universe.kotlinytmusicscraper.models.musixmatch.MusixmatchCredential
import com.universe.kotlinytmusicscraper.models.musixmatch.MusixmatchTranslationLyricsResponse
import com.universe.kotlinytmusicscraper.models.musixmatch.SearchMusixmatchResponse
import com.universe.kotlinytmusicscraper.models.response.SearchResponse
import com.universe.kotlinytmusicscraper.models.response.spotify.CanvasResponse
import com.universe.kotlinytmusicscraper.models.AudioFlare.GithubResponse
import com.universe.kotlinytmusicscraper.models.sponsorblock.SkipSegments
import com.universe.kotlinytmusicscraper.models.youtube.YouTubeInitialPage
import com.universe.kotlinytmusicscraper.pages.BrowseResult
import com.universe.kotlinytmusicscraper.pages.PlaylistPage
import com.universe.kotlinytmusicscraper.pages.SearchPage
import com.universe.audioflare.R
import com.universe.audioflare.common.VIDEO_QUALITY
import com.universe.audioflare.data.dataStore.DataStoreManager
import com.universe.audioflare.data.db.LocalDataSource
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
import com.universe.audioflare.data.model.browse.album.AlbumBrowse
import com.universe.audioflare.data.model.browse.album.Track
import com.universe.audioflare.data.model.browse.artist.ArtistBrowse
import com.universe.audioflare.data.model.browse.playlist.Author
import com.universe.audioflare.data.model.browse.playlist.PlaylistBrowse
import com.universe.audioflare.data.model.explore.mood.Genre
import com.universe.audioflare.data.model.explore.mood.Mood
import com.universe.audioflare.data.model.explore.mood.MoodsMoment
import com.universe.audioflare.data.model.explore.mood.genre.GenreObject
import com.universe.audioflare.data.model.explore.mood.moodmoments.MoodsMomentObject
import com.universe.audioflare.data.model.home.HomeItem
import com.universe.audioflare.data.model.home.chart.Chart
import com.universe.audioflare.data.model.metadata.Lyrics
import com.universe.audioflare.data.model.podcast.PodcastBrowse
import com.universe.audioflare.data.model.searchResult.albums.AlbumsResult
import com.universe.audioflare.data.model.searchResult.artists.ArtistsResult
import com.universe.audioflare.data.model.searchResult.playlists.PlaylistsResult
import com.universe.audioflare.data.model.searchResult.songs.Artist
import com.universe.audioflare.data.model.searchResult.songs.SongsResult
import com.universe.audioflare.data.model.searchResult.songs.Thumbnail
import com.universe.audioflare.data.model.searchResult.videos.VideosResult
import com.universe.audioflare.data.parser.parseAlbumData
import com.universe.audioflare.data.parser.parseArtistData
import com.universe.audioflare.data.parser.parseChart
import com.universe.audioflare.data.parser.parseGenreObject
import com.universe.audioflare.data.parser.parseLibraryPlaylist
import com.universe.audioflare.data.parser.parseMixedContent
import com.universe.audioflare.data.parser.parseMoodsMomentObject
import com.universe.audioflare.data.parser.parseNewRelease
import com.universe.audioflare.data.parser.parsePlaylistData
import com.universe.audioflare.data.parser.parsePodcast
import com.universe.audioflare.data.parser.parsePodcastContinueData
import com.universe.audioflare.data.parser.parsePodcastData
import com.universe.audioflare.data.parser.parseSetVideoId
import com.universe.audioflare.data.parser.search.parseSearchAlbum
import com.universe.audioflare.data.parser.search.parseSearchArtist
import com.universe.audioflare.data.parser.search.parseSearchPlaylist
import com.universe.audioflare.data.parser.search.parseSearchSong
import com.universe.audioflare.data.parser.search.parseSearchVideo
import com.universe.audioflare.data.parser.toListThumbnail
import com.universe.audioflare.data.queue.Queue
import com.universe.audioflare.extension.bestMatchingIndex
import com.universe.audioflare.extension.toListTrack
import com.universe.audioflare.extension.toLyrics
import com.universe.audioflare.extension.toTrack
import com.universe.audioflare.utils.Resource
import com.universe.audioflare.viewModel.FilterState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

// @ActivityRetainedScoped
@Singleton
class MainRepository
    @Inject
    constructor(
        private val localDataSource: LocalDataSource,
        private val dataStoreManager: DataStoreManager,
        @ApplicationContext private val context: Context,
    ) {
        // Database
        suspend fun getSearchHistory(): Flow<List<SearchHistory>> =
            flow {
                emit(localDataSource.getSearchHistory())
            }.flowOn(Dispatchers.IO)

        suspend fun insertSearchHistory(searchHistory: SearchHistory) =
            withContext(Dispatchers.IO) { localDataSource.insertSearchHistory(searchHistory) }

        suspend fun deleteSearchHistory() =
            withContext(Dispatchers.IO) {
                localDataSource.deleteSearchHistory()
            }

        suspend fun getAllSongs(): Flow<List<SongEntity>> =
            flow {
                emit(localDataSource.getAllSongs())
            }.flowOn(Dispatchers.IO)

        suspend fun setInLibrary(
            videoId: String,
            inLibrary: LocalDateTime,
        ) = withContext(Dispatchers.IO) { localDataSource.setInLibrary(videoId, inLibrary) }

        suspend fun getSongsByListVideoId(listVideoId: List<String>): Flow<List<SongEntity>> =
            flow {
                emit(
                    localDataSource.getSongByListVideoIdFull(listVideoId),
                )
            }.flowOn(Dispatchers.IO)

        suspend fun getSongsByListVideoIdOffset(
            listVideoId: List<String>,
            offset: Int,
        ): Flow<List<SongEntity>> =
            flow<List<SongEntity>> {
                emit(localDataSource.getSongByListVideoId(listVideoId, offset))
            }.flowOn(Dispatchers.IO)

        suspend fun getDownloadedSongs(): Flow<List<SongEntity>?> = flow { emit(localDataSource.getDownloadedSongs()) }.flowOn(Dispatchers.IO)

        suspend fun getDownloadedSongsAsFlow() = localDataSource.getDownloadedSongsAsFlow()

        suspend fun getDownloadingSongs(): Flow<List<SongEntity>?> = flow { emit(localDataSource.getDownloadingSongs()) }.flowOn(Dispatchers.IO)

        suspend fun getPreparingSongs(): Flow<List<SongEntity>> =
            flow {
                emit(localDataSource.getPreparingSongs())
            }.flowOn(Dispatchers.IO)

        suspend fun getLikedSongs(): Flow<List<SongEntity>> =
            flow {
                emit(localDataSource.getLikedSongs())
            }.flowOn(Dispatchers.IO)

        suspend fun getLibrarySongs(): Flow<List<SongEntity>> =
            flow {
                emit(localDataSource.getLibrarySongs())
            }.flowOn(Dispatchers.IO)

        suspend fun getSongById(id: String): Flow<SongEntity?> =
            flow {
                emit(localDataSource.getSong(id))
            }.flowOn(Dispatchers.IO)

        fun getSongAsFlow(id: String) = localDataSource.getSongAsFlow(id)

        suspend fun insertSong(songEntity: SongEntity): Flow<Long> =
            flow<Long> { emit(localDataSource.insertSong(songEntity)) }.flowOn(Dispatchers.IO)

        suspend fun updateListenCount(videoId: String) =
            withContext(Dispatchers.IO) {
                localDataSource.updateListenCount(videoId)
            }

        suspend fun updateLikeStatus(
            videoId: String,
            likeStatus: Int,
        ) = withContext(Dispatchers.Main) { localDataSource.updateLiked(likeStatus, videoId) }

        suspend fun updateSongInLibrary(
            inLibrary: LocalDateTime,
            videoId: String,
        ) = withContext(
            Dispatchers.Main,
        ) { localDataSource.updateSongInLibrary(inLibrary, videoId) }

        suspend fun updateDurationSeconds(
            durationSeconds: Int,
            videoId: String,
        ) = withContext(Dispatchers.Main) {
            localDataSource.updateDurationSeconds(
                durationSeconds,
                videoId,
            )
        }

        suspend fun getMostPlayedSongs(): Flow<List<SongEntity>> = flow { emit(localDataSource.getMostPlayedSongs()) }.flowOn(Dispatchers.IO)

        suspend fun updateDownloadState(
            videoId: String,
            downloadState: Int,
        ) = withContext(Dispatchers.Main) {
            localDataSource.updateDownloadState(
                downloadState,
                videoId,
            )
        }

        suspend fun getAllArtists(): Flow<List<ArtistEntity>> =
            flow {
                emit(localDataSource.getAllArtists())
            }.flowOn(Dispatchers.IO)

        suspend fun getArtistById(id: String): Flow<ArtistEntity> =
            flow {
                emit(localDataSource.getArtist(id))
            }.flowOn(Dispatchers.IO)

        suspend fun insertArtist(artistEntity: ArtistEntity) =
            withContext(Dispatchers.IO) {
                localDataSource.insertArtist(artistEntity)
            }

        suspend fun updateFollowedStatus(
            channelId: String,
            followedStatus: Int,
        ) = withContext(
            Dispatchers.Main,
        ) { localDataSource.updateFollowed(followedStatus, channelId) }

        suspend fun getFollowedArtists(): Flow<List<ArtistEntity>> = flow { emit(localDataSource.getFollowedArtists()) }.flowOn(Dispatchers.IO)

        suspend fun updateArtistInLibrary(
            inLibrary: LocalDateTime,
            channelId: String,
        ) = withContext(Dispatchers.Main) {
            localDataSource.updateArtistInLibrary(
                inLibrary,
                channelId,
            )
        }

        suspend fun getAllAlbums(): Flow<List<AlbumEntity>> =
            flow {
                emit(localDataSource.getAllAlbums())
            }.flowOn(Dispatchers.IO)

        suspend fun getAlbum(id: String): Flow<AlbumEntity> =
            flow {
                emit(localDataSource.getAlbum(id))
            }.flowOn(Dispatchers.IO)

        suspend fun getLikedAlbums(): Flow<List<AlbumEntity>> =
            flow {
                emit(localDataSource.getLikedAlbums())
            }.flowOn(Dispatchers.IO)

        suspend fun insertAlbum(albumEntity: AlbumEntity) =
            withContext(Dispatchers.IO) {
                localDataSource.insertAlbum(albumEntity)
            }

        suspend fun updateAlbumLiked(
            albumId: String,
            likeStatus: Int,
        ) = withContext(Dispatchers.Main) { localDataSource.updateAlbumLiked(likeStatus, albumId) }

        suspend fun updateAlbumInLibrary(
            inLibrary: LocalDateTime,
            albumId: String,
        ) = withContext(
            Dispatchers.Main,
        ) { localDataSource.updateAlbumInLibrary(inLibrary, albumId) }

        suspend fun updateAlbumDownloadState(
            albumId: String,
            downloadState: Int,
        ) = withContext(Dispatchers.Main) {
            localDataSource.updateAlbumDownloadState(
                downloadState,
                albumId,
            )
        }

        suspend fun getAllPlaylists(): Flow<List<PlaylistEntity>> =
            flow {
                emit(localDataSource.getAllPlaylists())
            }.flowOn(Dispatchers.IO)

        suspend fun getPlaylist(id: String): Flow<PlaylistEntity?> =
            flow {
                emit(localDataSource.getPlaylist(id))
            }.flowOn(Dispatchers.IO)

        suspend fun getLikedPlaylists(): Flow<List<PlaylistEntity>> = flow { emit(localDataSource.getLikedPlaylists()) }.flowOn(Dispatchers.IO)

        suspend fun insertPlaylist(playlistEntity: PlaylistEntity) = withContext(Dispatchers.IO) { localDataSource.insertPlaylist(playlistEntity) }

        suspend fun insertRadioPlaylist(playlistEntity: PlaylistEntity) =
            withContext(Dispatchers.IO) { localDataSource.insertRadioPlaylist(playlistEntity) }

        suspend fun updatePlaylistLiked(
            playlistId: String,
            likeStatus: Int,
        ) = withContext(Dispatchers.Main) {
            localDataSource.updatePlaylistLiked(
                likeStatus,
                playlistId,
            )
        }

        suspend fun updatePlaylistInLibrary(
            inLibrary: LocalDateTime,
            playlistId: String,
        ) = withContext(Dispatchers.Main) {
            localDataSource.updatePlaylistInLibrary(
                inLibrary,
                playlistId,
            )
        }

        suspend fun updatePlaylistDownloadState(
            playlistId: String,
            downloadState: Int,
        ) = withContext(Dispatchers.Main) {
            localDataSource.updatePlaylistDownloadState(
                downloadState,
                playlistId,
            )
        }

        suspend fun getAllLocalPlaylists(): Flow<List<LocalPlaylistEntity>> =
            flow { emit(localDataSource.getAllLocalPlaylists()) }.flowOn(Dispatchers.IO)

        suspend fun getLocalPlaylist(id: Long): Flow<LocalPlaylistEntity> = flow { emit(localDataSource.getLocalPlaylist(id)) }.flowOn(Dispatchers.IO)

        suspend fun insertLocalPlaylist(localPlaylistEntity: LocalPlaylistEntity) =
            withContext(Dispatchers.IO) { localDataSource.insertLocalPlaylist(localPlaylistEntity) }

        suspend fun deleteLocalPlaylist(id: Long) =
            withContext(Dispatchers.IO) {
                localDataSource.deleteLocalPlaylist(id)
            }

        suspend fun updateLocalPlaylistTitle(
            title: String,
            id: Long,
        ) = withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistTitle(title, id) }

        suspend fun updateLocalPlaylistThumbnail(
            thumbnail: String,
            id: Long,
        ) = withContext(
            Dispatchers.IO,
        ) { localDataSource.updateLocalPlaylistThumbnail(thumbnail, id) }

        suspend fun updateLocalPlaylistTracks(
            tracks: List<String>,
            id: Long,
        ) = withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistTracks(tracks, id) }

        suspend fun updateLocalPlaylistDownloadState(
            downloadState: Int,
            id: Long,
        ) = withContext(Dispatchers.IO) {
            localDataSource.updateLocalPlaylistDownloadState(
                downloadState,
                id,
            )
        }

        suspend fun updateLocalPlaylistInLibrary(
            inLibrary: LocalDateTime,
            id: Long,
        ) = withContext(
            Dispatchers.IO,
        ) { localDataSource.updateLocalPlaylistInLibrary(inLibrary, id) }

        suspend fun getDownloadedLocalPlaylists(): Flow<List<LocalPlaylistEntity>> =
            flow { emit(localDataSource.getDownloadedLocalPlaylists()) }.flowOn(Dispatchers.IO)

        suspend fun getAllRecentData(): Flow<List<Any>> =
            flow {
                emit(localDataSource.getAllRecentData())
            }.flowOn(Dispatchers.IO)

        suspend fun getAllDownloadedPlaylist(): Flow<List<Any>> = flow { emit(localDataSource.getAllDownloadedPlaylist()) }.flowOn(Dispatchers.IO)

        suspend fun getRecentSong(
            limit: Int,
            offset: Int,
        ) = localDataSource.getRecentSongs(limit, offset)

        suspend fun getSavedLyrics(videoId: String): Flow<LyricsEntity?> =
            flow { emit(localDataSource.getSavedLyrics(videoId)) }.flowOn(Dispatchers.IO)

        suspend fun insertLyrics(lyricsEntity: LyricsEntity) =
            withContext(Dispatchers.IO) {
                localDataSource.insertLyrics(lyricsEntity)
            }

        suspend fun insertNewFormat(newFormat: NewFormatEntity) =
            withContext(Dispatchers.IO) {
                localDataSource.insertNewFormat(newFormat)
            }

        suspend fun getNewFormat(videoId: String): Flow<NewFormatEntity?> =
            flow { emit(localDataSource.getNewFormat(videoId)) }.flowOn(Dispatchers.Main)

        suspend fun insertSongInfo(songInfo: SongInfoEntity) =
            withContext(Dispatchers.IO) {
                localDataSource.insertSongInfo(songInfo)
            }

        suspend fun getSongInfoEntiy(videoId: String): Flow<SongInfoEntity?> =
            flow { emit(localDataSource.getSongInfo(videoId)) }.flowOn(Dispatchers.Main)

        suspend fun recoverQueue(temp: List<Track>) {
            val queueEntity = QueueEntity(listTrack = temp)
            withContext(Dispatchers.IO) { localDataSource.recoverQueue(queueEntity) }
        }

        suspend fun removeQueue() {
            withContext(Dispatchers.IO) { localDataSource.deleteQueue() }
        }

        suspend fun getSavedQueue(): Flow<List<QueueEntity>?> =
            flow {
                emit(localDataSource.getQueue())
            }.flowOn(Dispatchers.IO)

        suspend fun getLocalPlaylistByYoutubePlaylistId(playlistId: String): Flow<LocalPlaylistEntity?> =
            flow { emit(localDataSource.getLocalPlaylistByYoutubePlaylistId(playlistId)) }.flowOn(
                Dispatchers.IO,
            )

        suspend fun insertSetVideoId(setVideoId: SetVideoIdEntity) = withContext(Dispatchers.IO) { localDataSource.insertSetVideoId(setVideoId) }

        suspend fun getSetVideoId(videoId: String): Flow<SetVideoIdEntity?> =
            flow { emit(localDataSource.getSetVideoId(videoId)) }.flowOn(Dispatchers.IO)

        suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) =
            withContext(Dispatchers.IO) {
                localDataSource.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
            }

        suspend fun getPlaylistPairSong(playlistId: Long): Flow<List<PairSongLocalPlaylist>?> =
            flow<List<PairSongLocalPlaylist>?> {
                emit(localDataSource.getPlaylistPairSong(playlistId))
            }.flowOn(Dispatchers.IO)

        suspend fun getPlaylistPairSongByOffset(
            playlistId: Long,
            offset: Int,
            filterState: FilterState,
        ): Flow<List<PairSongLocalPlaylist>?> =
            flow {
                emit(localDataSource.getPlaylistPairSongByOffset(playlistId, offset, filterState))
            }.flowOn(Dispatchers.IO)

        suspend fun deletePairSongLocalPlaylist(
            playlistId: Long,
            videoId: String,
        ) = withContext(Dispatchers.IO) {
            localDataSource.deletePairSongLocalPlaylist(playlistId, videoId)
        }

        suspend fun updateLocalPlaylistYouTubePlaylistId(
            id: Long,
            ytId: String?,
        ) = withContext(Dispatchers.IO) {
            localDataSource.updateLocalPlaylistYouTubePlaylistId(id, ytId)
        }

        suspend fun updateLocalPlaylistYouTubePlaylistSynced(
            id: Long,
            synced: Int,
        ) = withContext(Dispatchers.IO) {
            localDataSource.updateLocalPlaylistYouTubePlaylistSynced(id, synced)
        }

        suspend fun updateLocalPlaylistYouTubePlaylistSyncState(
            id: Long,
            syncState: Int,
        ) = withContext(Dispatchers.IO) {
            localDataSource.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)
        }

        suspend fun insertGoogleAccount(googleAccountEntity: GoogleAccountEntity) =
            withContext(Dispatchers.IO) {
                localDataSource.insertGoogleAccount(googleAccountEntity)
            }

        suspend fun getGoogleAccounts(): Flow<List<GoogleAccountEntity>?> =
            flow<List<GoogleAccountEntity>?> { emit(localDataSource.getGoogleAccounts()) }.flowOn(
                Dispatchers.IO,
            )

        suspend fun getUsedGoogleAccount(): Flow<GoogleAccountEntity?> =
            flow<GoogleAccountEntity?> { emit(localDataSource.getUsedGoogleAccount()) }.flowOn(
                Dispatchers.IO,
            )

        suspend fun deleteGoogleAccount(email: String) =
            withContext(Dispatchers.IO) {
                localDataSource.deleteGoogleAccount(email)
            }

        suspend fun updateGoogleAccountUsed(
            email: String,
            isUsed: Boolean,
        ) = withContext(Dispatchers.IO) { localDataSource.updateGoogleAccountUsed(email, isUsed) }

        suspend fun insertFollowedArtistSingleAndAlbum(followedArtistSingleAndAlbum: FollowedArtistSingleAndAlbum) =
            withContext(Dispatchers.IO) {
                localDataSource.insertFollowedArtistSingleAndAlbum(followedArtistSingleAndAlbum)
            }

        suspend fun deleteFollowedArtistSingleAndAlbum(channelId: String) =
            withContext(Dispatchers.IO) {
                localDataSource.deleteFollowedArtistSingleAndAlbum(channelId)
            }

        suspend fun getAllFollowedArtistSingleAndAlbums(): Flow<List<FollowedArtistSingleAndAlbum>?> =
            flow {
                emit(localDataSource.getAllFollowedArtistSingleAndAlbums())
            }.flowOn(Dispatchers.IO)

        suspend fun getFollowedArtistSingleAndAlbum(channelId: String): Flow<FollowedArtistSingleAndAlbum?> =
            flow {
                emit(localDataSource.getFollowedArtistSingleAndAlbum(channelId))
            }.flowOn(Dispatchers.IO)

        suspend fun insertNotification(notificationEntity: NotificationEntity) =
            withContext(Dispatchers.IO) {
                localDataSource.insertNotification(notificationEntity)
            }

        suspend fun getAllNotifications(): Flow<List<NotificationEntity>?> =
            flow {
                emit(localDataSource.getAllNotification())
            }.flowOn(Dispatchers.IO)

        suspend fun deleteNotification(id: Long) =
            withContext(Dispatchers.IO) {
                localDataSource.deleteNotification(id)
            }

        suspend fun getAccountInfo() =
            flow<AccountInfo?> {
                YouTube.accountInfo().onSuccess { accountInfo ->
                    emit(accountInfo)
                }.onFailure {
                    it.printStackTrace()
                    emit(null)
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getHomeData(): Flow<Resource<ArrayList<HomeItem>>> =
            flow {
                runCatching {
                    val limit = dataStoreManager.homeLimit.first()
                    YouTube.customQuery(browseId = "FEmusic_home").onSuccess { result ->
                        val list: ArrayList<HomeItem> = arrayListOf()
                        if (result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(
                                0,
                            )?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                                0,
                            )?.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.strapline?.runs?.get(
                                0,
                            )?.text != null
                        ) {
                            val accountName =
                                result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(
                                    0,
                                )?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                                    0,
                                )?.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.strapline?.runs?.get(
                                    0,
                                )?.text ?: ""
                            val accountThumbUrl =
                                result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(
                                    0,
                                )?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                                    0,
                                )?.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer
                                    ?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.get(
                                        0,
                                    )?.url?.replace("s88", "s352") ?: ""
                            if (accountName != "" && accountThumbUrl != "") {
                                dataStoreManager.putString("AccountName", accountName)
                                dataStoreManager.putString("AccountThumbUrl", accountThumbUrl)
                            }
                        }
                        var continueParam =
                            result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(
                                0,
                            )?.tabRenderer?.content?.sectionListRenderer?.continuations?.get(
                                0,
                            )?.nextContinuationData?.continuation
                        val data =
                            result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(
                                0,
                            )?.tabRenderer?.content?.sectionListRenderer?.contents
                        list.addAll(parseMixedContent(data, context))
                        var count = 0
                        while (count < limit && continueParam != null) {
                            YouTube.customQuery(browseId = "", continuation = continueParam)
                                .onSuccess { response ->
                                    continueParam =
                                        response.continuationContents?.sectionListContinuation?.continuations?.get(
                                            0,
                                        )?.nextContinuationData?.continuation
                                    Log.d("Repository", "continueParam: $continueParam")
                                    val dataContinue =
                                        response.continuationContents?.sectionListContinuation?.contents
                                    list.addAll(parseMixedContent(dataContinue, context))
                                    count++
                                    Log.d("Repository", "count: $count")
                                }.onFailure {
                                    Log.e("Repository", "Error: ${it.message}")
                                    count++
                                }
                        }
                        Log.d("Repository", "List size: ${list.size}")
                        emit(Resource.Success<ArrayList<HomeItem>>(list))
                    }.onFailure { error ->
                        emit(Resource.Error<ArrayList<HomeItem>>(error.message.toString()))
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getNewRelease(): Flow<Resource<ArrayList<HomeItem>>> =
            flow {
                YouTube.newRelease().onSuccess { result ->
                    emit(Resource.Success<ArrayList<HomeItem>>(parseNewRelease(result, context)))
                }.onFailure { error ->
                    emit(Resource.Error<ArrayList<HomeItem>>(error.message.toString()))
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getChartData(countryCode: String = "KR"): Flow<Resource<Chart>> =
            flow {
                runCatching {
                    YouTube.customQuery("FEmusic_charts", country = countryCode).onSuccess { result ->
                        val data =
                            result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(
                                0,
                            )?.tabRenderer?.content?.sectionListRenderer
                        val chart = parseChart(data)
                        if (chart != null) {
                            emit(Resource.Success<Chart>(chart))
                        } else {
                            emit(Resource.Error<Chart>("Error"))
                        }
                    }.onFailure { error ->
                        emit(Resource.Error<Chart>(error.message.toString()))
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getMoodAndMomentsData(): Flow<Resource<Mood>> =
            flow {
                runCatching {
                    YouTube.moodAndGenres().onSuccess { result ->
                        val listMoodMoments: ArrayList<MoodsMoment> = arrayListOf()
                        val listGenre: ArrayList<Genre> = arrayListOf()
                        result[0].let { moodsmoment ->
                            for (item in moodsmoment.items) {
                                listMoodMoments.add(
                                    MoodsMoment(
                                        params = item.endpoint.params ?: "",
                                        title = item.title,
                                    ),
                                )
                            }
                        }
                        result[1].let { genres ->
                            for (item in genres.items) {
                                listGenre.add(
                                    Genre(
                                        params = item.endpoint.params ?: "",
                                        title = item.title,
                                    ),
                                )
                            }
                        }
                        emit(Resource.Success<Mood>(Mood(listGenre, listMoodMoments)))
                    }.onFailure { e ->
                        emit(Resource.Error<Mood>(e.message.toString()))
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getMoodData(params: String): Flow<Resource<MoodsMomentObject>> =
            flow {
                runCatching {
                    YouTube.customQuery(
                        browseId = "FEmusic_moods_and_genres_category",
                        params = params,
                    )
                        .onSuccess { result ->
                            val data = parseMoodsMomentObject(result)
                            if (data != null) {
                                emit(Resource.Success<MoodsMomentObject>(data))
                            } else {
                                emit(Resource.Error<MoodsMomentObject>("Error"))
                            }
                        }
                        .onFailure { e ->
                            emit(Resource.Error<MoodsMomentObject>(e.message.toString()))
                        }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getGenreData(params: String): Flow<Resource<GenreObject>> =
            flow {
                kotlin.runCatching {
                    YouTube.customQuery(
                        browseId = "FEmusic_moods_and_genres_category",
                        params = params,
                    )
                        .onSuccess { result ->
                            val data = parseGenreObject(result)
                            if (data != null) {
                                emit(Resource.Success<GenreObject>(data))
                            } else {
                                emit(Resource.Error<GenreObject>("Error"))
                            }
                        }
                        .onFailure { e ->
                            emit(Resource.Error<GenreObject>(e.message.toString()))
                        }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getContinueTrack(
            playlistId: String,
            continuation: String,
        ): Flow<ArrayList<Track>?> =
            flow {
                runCatching {
                    Queue.setContinuation(playlistId, null)
                    YouTube.next(
                        WatchEndpoint(playlistId = playlistId),
                        continuation = continuation,
                    )
                        .onSuccess { next ->
                            val data: ArrayList<SongItem> = arrayListOf()
                            data.addAll(next.items)
                            val nextContinuation = next.continuation
                            if (nextContinuation != null) {
                                Queue.setContinuation(playlistId, nextContinuation)
                            } else {
                                Queue.setContinuation(playlistId, null)
                            }
                            emit(data.toListTrack())
                        }.onFailure { exception ->
                            exception.printStackTrace()
                            Queue.setContinuation(playlistId, null)
                            emit(null)
                        }
                }
            }

        suspend fun getRadio(
            radioId: String,
            originalTrack: SongEntity? = null,
            artist: ArtistEntity? = null,
        ): Flow<Resource<PlaylistBrowse>> =
            flow {
                runCatching {
                    YouTube.next(endpoint = WatchEndpoint(playlistId = radioId)).onSuccess { next ->
                        val data: ArrayList<SongItem> = arrayListOf()
                        data.addAll(next.items)
                        var continuation = next.continuation
                        Log.w("Radio", "data: ${data.size}")
                        var count = 0
                        while (continuation != null && count < 3) {
                            YouTube.next(
                                endpoint = WatchEndpoint(playlistId = radioId),
                                continuation = continuation,
                            ).onSuccess { nextContinue ->
                                data.addAll(nextContinue.items)
                                continuation = nextContinue.continuation
                                if (data.size >= 50) {
                                    val nextContinuation = nextContinue.continuation
                                    if (nextContinuation != null) {
                                        Queue.setContinuation(radioId, nextContinuation)
                                    }
                                    continuation = null
                                }
                                Log.w("Radio", "data: ${data.size}")
                                count++
                                if (count == 3) {
                                    val nextContinuation = nextContinue.continuation
                                    if (nextContinuation != null) {
                                        Queue.setContinuation(radioId, nextContinuation)
                                    }
                                }
                            }.onFailure {
                                if (count == 3) {
                                    val nextContinuation = continuation
                                    if (nextContinuation != null) {
                                        Queue.setContinuation(radioId, nextContinuation)
                                    }
                                }
                                continuation = null
                                count++
                            }
                        }
                        val listTrackResult = data.toListTrack()
                        if (originalTrack != null) {
                            listTrackResult.add(0, originalTrack.toTrack())
                        }
                        Log.w("Repository", "data: ${data.size}")
                        val playlistBrowse =
                            PlaylistBrowse(
                                author = Author(id = "", name = "YouTube Music"),
                                description =
                                    context.getString(
                                        R.string.auto_created_by_youtube_music,
                                    ),
                                duration = "",
                                durationSeconds = 0,
                                id = radioId,
                                privacy = "PRIVATE",
                                thumbnails =
                                    listOf(
                                        Thumbnail(
                                            544,
                                            originalTrack?.thumbnails ?: artist?.thumbnails ?: "",
                                            544,
                                        ),
                                    ),
                                title = "${originalTrack?.title ?: artist?.name} ${
                                    context.getString(
                                        R.string.radio,
                                    )
                                }",
                                trackCount = listTrackResult.size,
                                tracks = listTrackResult,
                                year = LocalDateTime.now().year.toString(),
                            )
                        Log.w("Repository", "playlistBrowse: $playlistBrowse")
                        emit(Resource.Success<PlaylistBrowse>(playlistBrowse))
                    }
                        .onFailure { exception ->
                            exception.printStackTrace()
                            emit(Resource.Error<PlaylistBrowse>(exception.message.toString()))
                        }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun reloadSuggestionPlaylist(reloadParams: String): Flow<Pair<String?, ArrayList<Track>?>?> =
            flow {
                runCatching {
                    YouTube.customQuery(browseId = "", continuation = reloadParams, setLogin = true)
                        .onSuccess { values ->
                            val data = values.continuationContents?.musicShelfContinuation?.contents
                            val dataResult:
                                ArrayList<SearchResponse.ContinuationContents.MusicShelfContinuation.Content> =
                                arrayListOf()
                            if (!data.isNullOrEmpty()) {
                                dataResult.addAll(data)
                            }
                            val reloadParamsNew =
                                values.continuationContents?.musicShelfContinuation?.continuations?.get(
                                    0,
                                )?.reloadContinuationData?.continuation
                            if (dataResult.isNotEmpty()) {
                                val listTrack: ArrayList<Track> = arrayListOf()
                                dataResult.forEach {
                                    listTrack.add(
                                        (
                                            SearchPage.toYTItem(
                                                it.musicResponsiveListItemRenderer,
                                            ) as SongItem
                                        ).toTrack(),
                                    )
                                }
                                emit(Pair(reloadParamsNew, listTrack))
                            } else {
                                emit(null)
                            }
                        }.onFailure { exception ->
                            exception.printStackTrace()
                            emit(null)
                        }
                }
            }

        suspend fun getSuggestionPlaylist(ytPlaylistId: String): Flow<Pair<String?, ArrayList<Track>?>?> =
            flow {
                runCatching {
                    var id = ""
                    if (!ytPlaylistId.startsWith("VL")) {
                        id += "VL$ytPlaylistId"
                    } else {
                        id += ytPlaylistId
                    }
                    YouTube.customQuery(browseId = id, setLogin = true).onSuccess { result ->
                        println(result)
                        var continueParam =
                            result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(
                                0,
                            )?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                                0,
                            )?.musicPlaylistShelfRenderer?.continuations?.get(
                                0,
                            )?.nextContinuationData?.continuation
                                ?: result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(
                                    0,
                                )?.tabRenderer?.content?.sectionListRenderer?.continuations?.get(
                                    0,
                                )?.nextContinuationData?.continuation
                        val dataResult: ArrayList<MusicShelfRenderer.Content> = arrayListOf()
                        var reloadParams: String? = null
                        println("continueParam: $continueParam")
                        while (continueParam != null) {
                            YouTube.customQuery(
                                browseId = "",
                                continuation = continueParam,
                                setLogin = true,
                            ).onSuccess { values ->
                                val data =
                                    values.continuationContents?.sectionListContinuation?.contents?.get(
                                        0,
                                    )?.musicShelfRenderer?.contents
                                println("data: $data")
                                if (!data.isNullOrEmpty()) {
                                    dataResult.addAll(data)
                                }
                                reloadParams =
                                    values.continuationContents?.sectionListContinuation?.contents?.get(
                                        0,
                                    )?.musicShelfRenderer?.continuations?.get(
                                        0,
                                    )?.reloadContinuationData?.continuation
                                continueParam =
                                    values.continuationContents?.musicPlaylistShelfContinuation?.continuations?.get(
                                        0,
                                    )?.nextContinuationData?.continuation
                                println("reloadParams: $reloadParams")
                                println("continueParam: $continueParam")
                            }.onFailure {
                                Log.e("Repository", "Error: ${it.message}")
                                continueParam = null
                            }
                        }
                        println("dataResult: ${dataResult.size}")
                        if (dataResult.isNotEmpty()) {
                            val listTrack: ArrayList<Track> = arrayListOf()
                            dataResult.forEach {
                                listTrack.add(
                                    (
                                        PlaylistPage.fromMusicResponsiveListItemRenderer(
                                            it.musicResponsiveListItemRenderer,
                                        ) as SongItem
                                    ).toTrack(),
                                )
                            }
                            println("listTrack: $listTrack")
                            emit(Pair(reloadParams, listTrack))
                        } else {
                            emit(null)
                        }
                    }
                        .onFailure { exception ->
                            exception.printStackTrace()
                            emit(null)
                        }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getPodcastData(podcastId: String): Flow<Resource<PodcastBrowse>> =
            flow {
                runCatching {
                    YouTube.customQuery(browseId = podcastId).onSuccess { result ->
                        val listEpisode = arrayListOf<PodcastBrowse.EpisodeItem>()
                        val thumbnail =
                            result.background?.musicThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail()
                        val title =
                            result.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer?.title?.runs?.firstOrNull()?.text
                        val author =
                            result.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer?.let {
                                    Artist(
                                        id = it.straplineTextOne?.runs?.firstOrNull()?.navigationEndpoint?.browseEndpoint?.browseId,
                                        name = it.straplineTextOne?.runs?.firstOrNull()?.text ?: "",
                                    )
                                }
                        val authorThumbnail =
                            result.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer?.let {
                                    it.straplineThumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url
                                }
                        val description =
                            result.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer
                                ?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer
                                ?.description?.musicDescriptionShelfRenderer?.description?.runs?.map {
                                    it.text
                                }?.joinToString("")
                        val data =
                            result.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()
                                ?.musicShelfRenderer?.contents
                        parsePodcastData(data, author).let {
                            listEpisode.addAll(it)
                        }
                        var continueParam =
                            result.contents?.twoColumnBrowseResultsRenderer?.secondaryContents
                                ?.sectionListRenderer?.contents?.firstOrNull()
                                ?.musicShelfRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation
                        while (continueParam != null) {
                            YouTube.customQuery(continuation = continueParam, browseId = "")
                                .onSuccess { continueData ->
                                    parsePodcastContinueData(
                                        continueData.continuationContents?.musicShelfContinuation?.contents,
                                        author,
                                    ).let {
                                        listEpisode.addAll(it)
                                    }
                                    continueParam =
                                        continueData.continuationContents?.musicShelfContinuation?.continuations?.firstOrNull()
                                            ?.nextContinuationData?.continuation
                                }
                                .onFailure {
                                    it.printStackTrace()
                                    continueParam = null
                                }
                        }
                        if (author != null) {
                            emit(
                                Resource.Success<PodcastBrowse>(
                                    PodcastBrowse(
                                        title = title ?: "",
                                        author = author,
                                        authorThumbnail = authorThumbnail,
                                        thumbnail = thumbnail ?: emptyList<Thumbnail>(),
                                        description = description,
                                        listEpisode = listEpisode,
                                    ),
                                ),
                            )
                        } else {
                            emit(Resource.Error<PodcastBrowse>("Error"))
                        }
                    }.onFailure { error ->
                        emit(Resource.Error<PodcastBrowse>(error.message.toString()))
                    }
                }
            }

        suspend fun getPlaylistData(playlistId: String): Flow<Resource<PlaylistBrowse>> =
            flow {
                runCatching {
                    var id = ""
                    id +=
                        if (!playlistId.startsWith("VL")) {
                            "VL$playlistId"
                        } else {
                            playlistId
                        }
                    Log.d("Repository", "playlist id: $id")
                    YouTube.customQuery(browseId = id, setLogin = true).onSuccess { result ->
                        val listContent: ArrayList<MusicShelfRenderer.Content> = arrayListOf()
                        val data: List<MusicShelfRenderer.Content>? =
                            result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(
                                0,
                            )?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                                0,
                            )?.musicPlaylistShelfRenderer?.contents
                        if (data != null) {
                            Log.d("Data", "data: $data")
                            Log.d("Data", "data size: ${data.size}")
                            listContent.addAll(data)
                        }
                        val header =
                            result.header?.musicDetailHeaderRenderer
                                ?: result.header?.musicEditablePlaylistDetailHeaderRenderer
                        Log.d("Header", "header: $header")
                        var continueParam =
                            result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(
                                0,
                            )?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                                0,
                            )?.musicPlaylistShelfRenderer?.continuations?.get(
                                0,
                            )?.nextContinuationData?.continuation
                        var count = 0
                        Log.d("Repository", "playlist data: ${listContent.size}")
                        Log.d("Repository", "continueParam: $continueParam")
                        while (continueParam != null) {
                            YouTube.customQuery(
                                browseId = "",
                                continuation = continueParam,
                                setLogin = true,
                            ).onSuccess { values ->
                                Log.d("Continue", "continue: $continueParam")
                                val dataMore: List<MusicShelfRenderer.Content>? =
                                    values.continuationContents?.musicPlaylistShelfContinuation?.contents
                                if (dataMore != null) {
                                    listContent.addAll(dataMore)
                                }
                                continueParam =
                                    values.continuationContents?.musicPlaylistShelfContinuation?.continuations?.get(
                                        0,
                                    )?.nextContinuationData?.continuation
                                count++
                            }.onFailure {
                                Log.e("Continue", "Error: ${it.message}")
                                continueParam = null
                                count++
                            }
                        }
                        Log.d("Repository", "playlist final data: ${listContent.size}")
                        parsePlaylistData(header, listContent, playlistId, context)?.let { playlist ->
                            emit(Resource.Success<PlaylistBrowse>(playlist))
                        } ?: emit(Resource.Error<PlaylistBrowse>("Error"))
                    }.onFailure { e ->
                        emit(Resource.Error<PlaylistBrowse>(e.message.toString()))
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getAlbumData(browseId: String): Flow<Resource<AlbumBrowse>> =
            flow {
                runCatching {
                    YouTube.album(browseId, withSongs = true).onSuccess { result ->
                        emit(Resource.Success<AlbumBrowse>(parseAlbumData(result)))
                    }.onFailure { e ->
                        Log.d("Album", "Error: ${e.message}")
                        emit(Resource.Error<AlbumBrowse>(e.message.toString()))
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getAlbumMore(
            browseId: String,
            params: String,
        ): Flow<BrowseResult?> =
            flow {
                runCatching {
                    YouTube.browse(browseId = browseId, params = params).onSuccess { result ->
                        Log.w("Album More", "result: $result")
                        emit(result)
                    }.onFailure {
                        it.printStackTrace()
                        emit(null)
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getArtistData(channelId: String): Flow<Resource<ArtistBrowse>> =
            flow {
                runCatching {
                    YouTube.artist(channelId).onSuccess { result ->
                        emit(Resource.Success<ArtistBrowse>(parseArtistData(result, context)))
                    }.onFailure { e ->
                        Log.d("Artist", "Error: ${e.message}")
                        emit(Resource.Error<ArtistBrowse>(e.message.toString()))
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getSearchDataSong(query: String): Flow<Resource<ArrayList<SongsResult>>> =
            flow {
                runCatching {
                    YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).onSuccess { result ->
                        val listSongs: ArrayList<SongsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchSong(result).let { list ->
                            listSongs.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            YouTube.searchContinuation(countinueParam).onSuccess { values ->
                                parseSearchSong(values).let { list ->
                                    listSongs.addAll(list)
                                }
                                count++
                                countinueParam = values.continuation
                            }.onFailure {
                                Log.e("Continue", "Error: ${it.message}")
                                countinueParam = null
                                count++
                            }
                        }

                        emit(Resource.Success<ArrayList<SongsResult>>(listSongs))
                    }.onFailure { e ->
                        Log.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<SongsResult>>(e.message.toString()))
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getSearchDataVideo(query: String): Flow<Resource<ArrayList<VideosResult>>> =
            flow {
                runCatching {
                    YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).onSuccess { result ->
                        val listSongs: ArrayList<VideosResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchVideo(result).let { list ->
                            listSongs.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            YouTube.searchContinuation(countinueParam).onSuccess { values ->
                                parseSearchVideo(values).let { list ->
                                    listSongs.addAll(list)
                                }
                                count++
                                countinueParam = values.continuation
                            }.onFailure {
                                Log.e("Continue", "Error: ${it.message}")
                                countinueParam = null
                                count++
                            }
                        }

                        emit(Resource.Success<ArrayList<VideosResult>>(listSongs))
                    }.onFailure { e ->
                        Log.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<VideosResult>>(e.message.toString()))
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getSearchDataPodcast(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
            flow {
                runCatching {
                    YouTube.search(query, YouTube.SearchFilter.FILTER_PODCAST).onSuccess { result ->
                        println(query)
                        val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        Log.w("Podcast", "result: $result")
                        parsePodcast(result.listPodcast).let { list ->
                            listPlaylist.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            YouTube.searchContinuation(countinueParam).onSuccess { values ->
                                parsePodcast(values.listPodcast).let { list ->
                                    listPlaylist.addAll(list)
                                }
                                count++
                                countinueParam = values.continuation
                            }.onFailure {
                                Log.e("Continue", "Error: ${it.message}")
                                countinueParam = null
                                count++
                            }
                        }
                        emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                    }.onFailure { e ->
                        Log.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getSearchDataFeaturedPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
            flow {
                runCatching {
                    YouTube.search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                        .onSuccess { result ->
                            val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                            var countinueParam = result.continuation
                            parseSearchPlaylist(result).let { list ->
                                listPlaylist.addAll(list)
                            }
                            var count = 0
                            while (count < 2 && countinueParam != null) {
                                YouTube.searchContinuation(countinueParam).onSuccess { values ->
                                    parseSearchPlaylist(values).let { list ->
                                        listPlaylist.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Log.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                            }
                            emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                        }.onFailure { e ->
                            Log.d("Search", "Error: ${e.message}")
                            emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                        }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getSearchDataArtist(query: String): Flow<Resource<ArrayList<ArtistsResult>>> =
            flow {
                runCatching {
                    YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).onSuccess { result ->
                        val listArtist: ArrayList<ArtistsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchArtist(result).let { list ->
                            listArtist.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            YouTube.searchContinuation(countinueParam).onSuccess { values ->
                                parseSearchArtist(values).let { list ->
                                    listArtist.addAll(list)
                                }
                                count++
                                countinueParam = values.continuation
                            }.onFailure {
                                Log.e("Continue", "Error: ${it.message}")
                                countinueParam = null
                                count++
                            }
                        }
                        emit(Resource.Success<ArrayList<ArtistsResult>>(listArtist))
                    }.onFailure { e ->
                        Log.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<ArtistsResult>>(e.message.toString()))
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getSearchDataAlbum(query: String): Flow<Resource<ArrayList<AlbumsResult>>> =
            flow {
                runCatching {
                    YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).onSuccess { result ->
                        val listAlbum: ArrayList<AlbumsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchAlbum(result).let { list ->
                            listAlbum.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            YouTube.searchContinuation(countinueParam).onSuccess { values ->
                                parseSearchAlbum(values).let { list ->
                                    listAlbum.addAll(list)
                                }
                                count++
                                countinueParam = values.continuation
                            }.onFailure {
                                Log.e("Continue", "Error: ${it.message}")
                                countinueParam = null
                                count++
                            }
                        }
                        emit(Resource.Success<ArrayList<AlbumsResult>>(listAlbum))
                    }.onFailure { e ->
                        Log.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<AlbumsResult>>(e.message.toString()))
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getSearchDataPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
            flow {
                runCatching {
                    YouTube.search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                        .onSuccess { result ->
                            val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                            var countinueParam = result.continuation
                            parseSearchPlaylist(result).let { list ->
                                listPlaylist.addAll(list)
                            }
                            var count = 0
                            while (count < 2 && countinueParam != null) {
                                YouTube.searchContinuation(countinueParam).onSuccess { values ->
                                    parseSearchPlaylist(values).let { list ->
                                        listPlaylist.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Log.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                            }
                            emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                        }.onFailure { e ->
                            Log.d("Search", "Error: ${e.message}")
                            emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                        }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getSuggestQuery(query: String): Flow<Resource<SearchSuggestions>> =
            flow {
                runCatching {
//            YouTube.getSuggestQuery(query).onSuccess {
//                emit(Resource.Success<ArrayList<String>>(it))
//            }.onFailure { e ->
//                Log.d("Suggest", "Error: ${e.message}")
//                emit(Resource.Error<ArrayList<String>>(e.message.toString()))
//            }
                    YouTube.getYTMusicSearchSuggestions(query).onSuccess {
                        emit(Resource.Success(it))
                    }.onFailure { e ->
                        Log.d("Suggest", "Error: ${e.message}")
                        emit(Resource.Error<SearchSuggestions>(e.message.toString()))
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getRelatedData(videoId: String): Flow<Resource<ArrayList<Track>>> =
            flow {
                runCatching {
                    Queue.setContinuation("RDAMVM$videoId", null)
                    YouTube.next(WatchEndpoint(playlistId = "RDAMVM$videoId"))
                        .onSuccess { next ->
                            val data: ArrayList<SongItem> = arrayListOf()
                            data.addAll(next.items)
                            val nextContinuation = next.continuation
                            if (nextContinuation != null) {
                                Queue.setContinuation("RDAMVM$videoId", nextContinuation)
                            } else {
                                Queue.setContinuation("RDAMVM$videoId", null)
                            }
                            emit(Resource.Success<ArrayList<Track>>(data.toListTrack()))
                        }.onFailure { exception ->
                            exception.printStackTrace()
                            Queue.setContinuation("RDAMVM$videoId", null)
                            emit(Resource.Error<ArrayList<Track>>(exception.message.toString()))
                        }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getYouTubeCaption(videoId: String): Flow<Resource<Lyrics>> =
            flow {
                runCatching {
                    YouTube.getYouTubeCaption(videoId).onSuccess { lyrics ->
                        Log.w("Lyrics", "lyrics: ${lyrics.toLyrics()}")
                        emit(Resource.Success<Lyrics>(lyrics.toLyrics()))
                    }.onFailure { e ->
                        Log.d("Lyrics", "Error: ${e.message}")
                        emit(Resource.Error<Lyrics>(e.message.toString()))
                    }
                }
            }

        suspend fun getCanvas(
            videoId: String,
            duration: Int,
        ): Flow<CanvasResponse?> =
            flow {
                runCatching {
                    getSongById(videoId).first().let { song ->
                        val q =
                            "${song?.title} ${song?.artistName?.firstOrNull() ?: ""}".replace(
                                Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                                " ",
                            ).replace(
                                Regex("( và | & | и | e | und |, |和| dan)"),
                                " ",
                            ).replace("  ", " ").replace(Regex("([()])"), "").replace(".", " ")
                                .replace("  ", " ")
                        var spotifyPersonalToken = ""
                        if (dataStoreManager.spotifyPersonalToken.first()
                                .isNotEmpty() &&
                            dataStoreManager.spotifyPersonalTokenExpires.first() > System.currentTimeMillis() &&
                            dataStoreManager.spotifyPersonalTokenExpires.first() != 0L
                        ) {
                            spotifyPersonalToken = dataStoreManager.spotifyPersonalToken.first()
                            Log.d("Lyrics", "spotifyPersonalToken: $spotifyPersonalToken")
                        } else if (dataStoreManager.spdc.first().isNotEmpty()) {
                            YouTube.getPersonalToken(dataStoreManager.spdc.first()).onSuccess {
                                spotifyPersonalToken = it.accessToken
                                dataStoreManager.setSpotifyPersonalToken(spotifyPersonalToken)
                                dataStoreManager.setSpotifyPersonalTokenExpires(
                                    it.accessTokenExpirationTimestampMs,
                                )
                                Log.d("Lyrics", "spotifyPersonalToken: $spotifyPersonalToken")
                            }.onFailure {
                                it.printStackTrace()
                                emit(null)
                            }
                        }
                        if (spotifyPersonalToken.isNotEmpty()) {
                            var clientToken = dataStoreManager.spotifyClientToken.first()
                            Log.d("Lyrics", "clientToken: $clientToken")
                            YouTube.searchSpotifyTrack(q, clientToken).onSuccess { searchResponse ->
                                val track =
                                    if (duration != 0) {
                                        searchResponse.tracks.items.find {
                                            abs(
                                                ((it.duration_ms / 1000) - duration),
                                            ) < 1
                                        }
                                            ?: searchResponse.tracks.items.firstOrNull()
                                    } else {
                                        searchResponse.tracks.items.firstOrNull()
                                    }
                                Log.d("Lyrics", "track: $track")
                                if (track != null) {
                                    YouTube.getSpotifyCanvas(
                                        track.id,
                                        spotifyPersonalToken,
                                    ).onSuccess {
                                        Log.w("Spotify Canvas", "canvas: $it")
                                        emit(it)
                                    }.onFailure {
                                        it.printStackTrace()
                                        emit(null)
                                    }
                                } else {
                                    emit(null)
                                }
                            }.onFailure { throwable ->
                                throwable.printStackTrace()
                                YouTube.getClientToken().onSuccess { tokenResponse ->
                                    clientToken = tokenResponse.accessToken
                                    Log.w("Lyrics", "clientToken: $clientToken")
                                    dataStoreManager.setSpotifyClientToken(clientToken)
                                    YouTube.searchSpotifyTrack(q, clientToken)
                                        .onSuccess { searchResponse ->
                                            val track =
                                                if (duration != 0) {
                                                    searchResponse.tracks.items.find {
                                                        abs(
                                                            ((it.duration_ms / 1000) - duration),
                                                        ) < 1
                                                    }
                                                        ?: searchResponse.tracks.items.firstOrNull()
                                                } else {
                                                    searchResponse.tracks.items.firstOrNull()
                                                }
                                            Log.d("Lyrics", "track: $track")
                                            if (track != null) {
                                                YouTube.getSpotifyCanvas(
                                                    track.id,
                                                    spotifyPersonalToken,
                                                )
                                                    .onSuccess {
                                                        Log.w("Spotify Canvas", "canvas: $it")
                                                        emit(it)
                                                    }.onFailure {
                                                        it.printStackTrace()
                                                        emit(null)
                                                    }
                                            } else {
                                                emit(null)
                                            }
                                        }
                                }.onFailure {
                                    it.printStackTrace()
                                    emit(null)
                                }
                            }
                        }
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getSpotifyLyrics(
            query: String,
            duration: Int?,
        ): Flow<Resource<Lyrics>> =
            flow {
                runCatching {
                    val q =
                        query.replace(
                            Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                            " ",
                        ).replace(
                            Regex("( và | & | и | e | und |, |和| dan)"),
                            " ",
                        ).replace("  ", " ").replace(Regex("([()])"), "").replace(".", " ")
                            .replace("  ", " ")
                    Log.d("Lyrics", "query: $q")
                    var spotifyPersonalToken = ""
                    if (dataStoreManager.spotifyPersonalToken.first()
                            .isNotEmpty() && dataStoreManager.spotifyPersonalTokenExpires.first() > System.currentTimeMillis() && dataStoreManager.spotifyPersonalTokenExpires.first() != 0L
                    ) {
                        spotifyPersonalToken = dataStoreManager.spotifyPersonalToken.first()
                        Log.d("Lyrics", "spotifyPersonalToken: $spotifyPersonalToken")
                    } else if (dataStoreManager.spdc.first().isNotEmpty()) {
                        YouTube.getPersonalToken(dataStoreManager.spdc.first()).onSuccess {
                            spotifyPersonalToken = it.accessToken
                            dataStoreManager.setSpotifyPersonalToken(spotifyPersonalToken)
                            dataStoreManager.setSpotifyPersonalTokenExpires(
                                it.accessTokenExpirationTimestampMs,
                            )
                            Log.d("Lyrics", "spotifyPersonalToken: $spotifyPersonalToken")
                        }.onFailure {
                            it.printStackTrace()
                            emit(Resource.Error<Lyrics>("Not found"))
                        }
                    }
                    if (spotifyPersonalToken.isNotEmpty()) {
                        var clientToken = dataStoreManager.spotifyClientToken.first()
                        Log.d("Lyrics", "clientToken: $clientToken")
                        YouTube.searchSpotifyTrack(q, clientToken).onSuccess { searchResponse ->
                            val track =
                                if (duration != null && duration != 0) {
                                    searchResponse.tracks.items.find {
                                        abs(
                                            ((it.duration_ms / 1000) - duration),
                                        ) < 1
                                    }
                                        ?: searchResponse.tracks.items.firstOrNull()
                                } else {
                                    searchResponse.tracks.items.firstOrNull()
                                }
                            Log.d("Lyrics", "track: $track")
                            if (track != null) {
                                YouTube.getSpotifyLyrics(track.id, spotifyPersonalToken).onSuccess {
                                    emit(Resource.Success<Lyrics>(it.toLyrics()))
                                }.onFailure {
                                    it.printStackTrace()
                                    emit(Resource.Error<Lyrics>("Not found"))
                                }
                            } else {
                                emit(Resource.Error<Lyrics>("Not found"))
                            }
                        }.onFailure { throwable ->
                            throwable.printStackTrace()
                            YouTube.getClientToken().onSuccess {
                                clientToken = it.accessToken
                                Log.w("Lyrics", "clientToken: $clientToken")
                                dataStoreManager.setSpotifyClientToken(clientToken)
                                YouTube.searchSpotifyTrack(q, clientToken).onSuccess { searchResponse ->
                                    val track =
                                        if (duration != null && duration != 0) {
                                            searchResponse.tracks.items.find {
                                                abs(
                                                    ((it.duration_ms / 1000) - duration),
                                                ) < 1
                                            }
                                                ?: searchResponse.tracks.items.firstOrNull()
                                        } else {
                                            searchResponse.tracks.items.firstOrNull()
                                        }
                                    Log.d("Lyrics", "track: $track")
                                    if (track != null) {
                                        YouTube.getSpotifyLyrics(track.id, spotifyPersonalToken)
                                            .onSuccess {
                                                emit(Resource.Success<Lyrics>(it.toLyrics()))
                                            }.onFailure {
                                                it.printStackTrace()
                                                emit(Resource.Error<Lyrics>("Not found"))
                                            }
                                    } else {
                                        emit(Resource.Error<Lyrics>("Not found"))
                                    }
                                }
                            }.onFailure {
                                it.printStackTrace()
                                emit(Resource.Error<Lyrics>("Not found"))
                            }
                        }
                    }
                }
            }

        suspend fun getLyricsData(
            sartist: String,
            strack: String,
            durationInt: Int? = null,
        ): Flow<Pair<String, Resource<Lyrics>>> =
            flow {
                runCatching {
//            val q = query.replace(Regex("\\([^)]*?(feat.|ft.|cùng với|con)[^)]*?\\)"), "")
//                .replace("  ", " ")
                    val qartist =
                        sartist.replace(
                            Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                            " ",
                        ).replace(
                            Regex("( và | & | и | e | und |, |和| dan)"),
                            " ",
                        ).replace("  ", " ").replace(Regex("([()])"), "").replace(".", " ")
                    val qtrack =
                        strack.replace(
                            Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                            " ",
                        ).replace(
                            Regex("( và | & | и | e | und |, |和| dan)"),
                            " ",
                        ).replace("  ", " ").replace(Regex("([()])"), "").replace(".", " ")
                    val q = "$qtrack $qartist"
                    Log.d("Lyrics", "query: $q")
                    var musixMatchUserToken = YouTube.musixmatchUserToken
                    if (musixMatchUserToken == null) {
                        YouTube.getMusixmatchUserToken().onSuccess { usertoken ->
                            YouTube.musixmatchUserToken = usertoken.message.body.user_token
                            musixMatchUserToken = usertoken.message.body.user_token
                        }
                            .onFailure { throwable ->
                                throwable.printStackTrace()
                                emit(Pair("", Resource.Error<Lyrics>("Not found")))
                            }
                    }
                    YouTube.searchMusixmatchTrackId(q, musixMatchUserToken!!)
                        .onSuccess { searchResult ->
                            Log.d("Lyrics", "searchResult: $searchResult")
                            val trackList =
                                if (!searchResult.message.body.track_list.isNullOrEmpty()) {
                                    searchResult.message.body.track_list
                                } else {
                                    searchResult.message.body.macro_result_list?.track_list
                                }
                            if (!trackList.isNullOrEmpty()) {
                                Log.d("Lyrics", "trackList: $trackList")
                                val list = arrayListOf<String>()
                                for (i in trackList) {
                                    list.add(i.track.track_name + " " + i.track.artist_name)
                                }
                                var id = ""
                                var track: SearchMusixmatchResponse.Message.Body.Track.TrackX? = null
                                Log.d("DURATION", "duration: $durationInt")
                                val bestMatchingIndex = bestMatchingIndex(q, list)
                                if (durationInt != null && durationInt != 0) {
                                    val trackLengthList = arrayListOf<Int>()
                                    for (i in trackList) {
                                        trackLengthList.add(i.track.track_length)
                                    }
                                    val closestIndex =
                                        trackLengthList.minByOrNull {
                                            kotlin.math.abs(
                                                it - durationInt,
                                            )
                                        }
                                    if (closestIndex != null && kotlin.math.abs(
                                            closestIndex - durationInt,
                                        ) < 2
                                    ) {
                                        id +=
                                            trackList.find {
                                                it.track.track_length == closestIndex
                                            }?.track?.track_id.toString()
                                        track =
                                            trackList.find { it.track.track_length == closestIndex }?.track
                                    }
                                    if (id == "" &&
                                        list.get(bestMatchingIndex).contains(
                                            trackList.get(
                                                bestMatchingIndex,
                                            ).track.track_name,
                                        ) &&
                                        q.contains(
                                            trackList.get(
                                                bestMatchingIndex,
                                            ).track.track_name,
                                        )
                                    ) {
                                        Log.w(
                                            "Lyrics",
                                            "item: ${
                                                trackList.get(
                                                    bestMatchingIndex,
                                                ).track.track_name
                                            }",
                                        )
                                        id +=
                                            trackList.get(
                                                bestMatchingIndex,
                                            ).track.track_id.toString()
                                        track =
                                            trackList.get(
                                                bestMatchingIndex,
                                            ).track
                                    }
                                } else if (list.get(bestMatchingIndex)
                                        .contains(
                                            trackList.get(
                                                bestMatchingIndex,
                                            ).track.track_name,
                                        ) &&
                                    q.contains(
                                        trackList.get(
                                            bestMatchingIndex,
                                        ).track.track_name,
                                    )
                                ) {
                                    Log.w(
                                        "Lyrics",
                                        "item: ${
                                            trackList.get(
                                                bestMatchingIndex,
                                            ).track.track_name
                                        }",
                                    )
                                    id +=
                                        trackList.get(
                                            bestMatchingIndex,
                                        ).track.track_id.toString()
                                    track =
                                        trackList.get(
                                            bestMatchingIndex,
                                        ).track
                                }
                                Log.d("DURATION", "id: $id")
                                Log.w(
                                    "item lyrics",
                                    track.toString(),
                                )
                                if (id != "" && track != null) {
                                    YouTube.getMusixmatchLyricsByQ(track, musixMatchUserToken!!)
                                        .onSuccess {
                                            if (it != null) {
                                                emit(
                                                    Pair(
                                                        id,
                                                        Resource.Success<Lyrics>(it.toLyrics()),
                                                    ),
                                                )
                                            } else {
                                                Log.w("Lyrics", "Error: Lỗi getLyrics $it")
                                                emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                                            }
                                        }.onFailure { throwable ->
                                            throwable.printStackTrace()
                                            YouTube.getLrclibLyrics(qtrack, qartist, durationInt).onSuccess {
                                                it?.let { emit(Pair(id, Resource.Success<Lyrics>(it.toLyrics()))) }
                                            }.onFailure {
                                                it.printStackTrace()
                                                emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                                            }
                                        }
                                } else {
                                    YouTube.fixSearchMusixmatch(
                                        q_artist = qartist,
                                        q_track = qtrack,
                                        q_duration = (durationInt ?: 0).toString(),
                                        userToken = musixMatchUserToken!!,
                                    )
                                        .onSuccess {
                                            val trackX = it.message.body.track
                                            if (trackX != null && abs(trackX.track_length - (durationInt ?: 0)) <= 10) {
                                                YouTube.getMusixmatchLyricsByQ(trackX, musixMatchUserToken!!).onSuccess {
                                                    if (it != null) {
                                                        emit(
                                                            Pair(
                                                                trackX.track_id.toString(),
                                                                Resource.Success<Lyrics>(it.toLyrics()),
                                                            ),
                                                        )
                                                    } else {
                                                        Log.w("Lyrics", "Error: Lỗi getLyrics $it")
                                                        YouTube.getLrclibLyrics(qtrack, qartist, durationInt)
                                                        emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                                                    }
                                                }.onFailure {
                                                    it.printStackTrace()
                                                    emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                                                }
                                            } else {
                                                YouTube.getLrclibLyrics(qtrack, qartist, durationInt).onSuccess {
                                                    it?.let { emit(Pair(trackX?.track_id.toString(), Resource.Success<Lyrics>(it.toLyrics()))) }
                                                }.onFailure {
                                                    it.printStackTrace()
                                                    emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                                                }
                                            }
                                        }
                                        .onFailure {
                                            YouTube.getLrclibLyrics(qtrack, qartist, durationInt).onSuccess {
                                                it?.let { emit(Pair(id, Resource.Success<Lyrics>(it.toLyrics()))) }
                                            }.onFailure {
                                                it.printStackTrace()
                                                emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                                            }
                                        }
                                }
                            } else {
                                emit(Pair("", Resource.Error<Lyrics>("Not found")))
                            }
                        }
                        .onFailure { throwable ->
                            throwable.printStackTrace()
                            emit(Pair("", Resource.Error<Lyrics>("Not found")))
                        }

//            YouTube.authentication().onSuccess { token ->
//                if (token.accessToken != null) {
//                    YouTube.getSongId(token.accessToken!!, q).onSuccess { spotifyResult ->
//                        Log.d("SongId", "id: ${spotifyResult.tracks?.items?.get(0)?.id}")
//                        if (!spotifyResult.tracks?.items.isNullOrEmpty()) {
//                            val list = arrayListOf<String>()
//                            for (index in spotifyResult.tracks?.items!!.indices) {
//                                list.add(
//                                    (spotifyResult.tracks?.items?.get(index)?.name ?: "") + " " + (spotifyResult.tracks?.items?.get(
//                                        index
//                                    )?.artists?.connectArtistsSpotify() ?: "")
//                                )
//                            }
//                            Log.w("Lyrics", "list: $list")
//                            var id = ""
//                            val bestMatchingIndex = bestMatchingIndex(q, list)
//                            if (q.contains(spotifyResult.tracks?.items?.get(bestMatchingIndex)?.name.toString()) && q.contains(spotifyResult.tracks?.items?.get(bestMatchingIndex)?.artists?.firstOrNull()?.name.toString())) {
//                                id += spotifyResult.tracks?.items?.get(bestMatchingIndex)?.id
//                                Log.w("Lyrics", "item: ${spotifyResult.tracks?.items?.get(bestMatchingIndex)?.name}")
//                            }
//                            else {
//                                id += spotifyResult.tracks?.items?.get(0)?.id
//                                Log.w("Lyrics", "item: ${spotifyResult.tracks?.items?.get(0)?.name}")
//                            }
//                            if (id != "") {
//                                if (dataStoreManager.spotifyAccessTokenExpire.first() != 0L && dataStoreManager.spotifyAccessToken.first() != "" && (dataStoreManager.spotifyAccessTokenExpire.first()
//                                        .toLong()) > Instant.now().toEpochMilli()) {
//                                    Log.d(
//                                        "Lyrics",
//                                        "token: ${dataStoreManager.spotifyAccessToken.first()}"
//                                    )
//                                    YouTube.getLyrics(
//                                        id,
//                                        dataStoreManager.spotifyAccessToken.first()
//                                    ).onSuccess { lyrics ->
//                                        emit(Resource.Success<Lyrics>(lyrics.toLyrics()))
//                                    }.onFailure { throwable ->
//                                        Log.d(
//                                            "Lyrics",
//                                            "Error: Lỗi getLyrics ${throwable.message}"
//                                        )
//                                        spotifyResult.tracks?.items?.firstOrNull()?.id?.let { it2 ->
//                                            YouTube.getLyrics(
//                                                it2,
//                                                dataStoreManager.spotifyAccessToken.first()
//                                            ).onSuccess {
//                                                emit(Resource.Success<Lyrics>(it.toLyrics()))
//                                            }
//                                                .onFailure {
//                                                    Log.d(
//                                                        "Lyrics",
//                                                        "Error: Lỗi getLyrics lần 2 ${it.message}"
//                                                    )
//                                                    emit(Resource.Error<Lyrics>("Not found"))
//                                                }
//                                        }
//                                        emit(Resource.Error<Lyrics>("Not found"))
//                                    }
//                                }
//                                else {
//                                    YouTube.getAccessToken()
//                                        .onSuccess { value: AccessToken ->
//                                            dataStoreManager.setSpotifyAccessToken(value.accessToken!!)
//                                            dataStoreManager.setSpotifyAccessTokenExpire(
//                                                value.accessTokenExpirationTimestampMs!!
//                                            )
//                                            Log.d(
//                                                "Lyrics",
//                                                "token: ${value.accessToken}"
//                                            )
//                                            YouTube.getLyrics(id, value.accessToken)
//                                                .onSuccess { lyrics ->
//                                                    emit(Resource.Success<Lyrics>(lyrics.toLyrics()))
//                                                }.onFailure { throwable ->
//                                                    throwable.printStackTrace()
//                                                    spotifyResult.tracks?.items?.firstOrNull()?.id?.let { it2 ->
//                                                        YouTube.getLyrics(
//                                                            it2,
//                                                            value.accessToken
//                                                        ).onSuccess {
//                                                            emit(
//                                                                Resource.Success<Lyrics>(
//                                                                    it.toLyrics()
//                                                                )
//                                                            )
//                                                        }
//                                                            .onFailure {
//                                                                Log.d(
//                                                                    "Lyrics",
//                                                                    "Error: Lỗi getLyrics lần 2 ${it.message}"
//                                                                )
//                                                                emit(
//                                                                    Resource.Error<Lyrics>(
//                                                                        "Not found"
//                                                                    )
//                                                                )
//                                                            }
//                                                    }
//                                                    emit(Resource.Error<Lyrics>("Not found"))
//                                                }
//                                        }
//                                        .onFailure { e ->
//                                            e.printStackTrace()
//                                            emit(Resource.Error<Lyrics>("Not found"))
//                                        }
//                                }
//                            }
//                            else {
//                                Log.w("Lyrics", "Can't find song id")
//                                emit(Resource.Error<Lyrics>("Not found"))
//                            }
//                            //                            bestMatchingIndex(q, list).let {
// //                            bestMatchingIndex(q, list).let { index ->
// //                                spotifyResult.tracks?.items?.get(index)?.let { item ->
// //                                    if (list[index].contains(item.name.toString())) {
// //                                        Log.w("Lyrics", "item: ${item.name}")
// //                                        item.id?.let { it1 ->
// //                                            Log.d("Lyrics", "id: $it1")
// //                                            if (dataStoreManager.spotifyAccessTokenExpire.first() != 0L && dataStoreManager.spotifyAccessToken.first() != "" && (dataStoreManager.spotifyAccessTokenExpire.first()
// //                                                    .toLong()) > Instant.now().toEpochMilli()
// //                                            ) {
// //                                                Log.d(
// //                                                    "Lyrics",
// //                                                    "token: ${dataStoreManager.spotifyAccessToken.first()}"
// //                                                )
// //                                                YouTube.getLyrics(
// //                                                    it1,
// //                                                    dataStoreManager.spotifyAccessToken.first()
// //                                                ).onSuccess { lyrics ->
// //                                                    emit(Resource.Success<Lyrics>(lyrics.toLyrics()))
// //                                                }.onFailure {
// //                                                    Log.d(
// //                                                        "Lyrics",
// //                                                        "Error: Lỗi getLyrics ${it.message}"
// //                                                    )
// //                                                    spotifyResult.tracks?.items?.firstOrNull()?.id?.let { it2 ->
// //                                                        YouTube.getLyrics(
// //                                                            it2,
// //                                                            dataStoreManager.spotifyAccessToken.first()
// //                                                        ).onSuccess {
// //                                                            emit(Resource.Success<Lyrics>(it.toLyrics()))
// //                                                        }
// //                                                            .onFailure {
// //                                                                Log.d(
// //                                                                    "Lyrics",
// //                                                                    "Error: Lỗi getLyrics lần 2 ${it.message}"
// //                                                                )
// //                                                                emit(Resource.Error<Lyrics>("Not found"))
// //                                                            }
// //                                                    }
// //                                                    emit(Resource.Error<Lyrics>("Not found"))
// //                                                }
// //                                            } else {
// //                                                YouTube.getAccessToken()
// //                                                    .onSuccess { value: AccessToken ->
// //                                                        dataStoreManager.setSpotifyAccessToken(value.accessToken!!)
// //                                                        dataStoreManager.setSpotifyAccessTokenExpire(
// //                                                            value.accessTokenExpirationTimestampMs!!
// //                                                        )
// //                                                        Log.d(
// //                                                            "Lyrics",
// //                                                            "token: ${value.accessToken}"
// //                                                        )
// //                                                        YouTube.getLyrics(it1, value.accessToken)
// //                                                            .onSuccess { lyrics ->
// //                                                                emit(Resource.Success<Lyrics>(lyrics.toLyrics()))
// //                                                            }.onFailure {
// //                                                                it.printStackTrace()
// //                                                                spotifyResult.tracks?.items?.firstOrNull()?.id?.let { it2 ->
// //                                                                    YouTube.getLyrics(
// //                                                                        it2,
// //                                                                        value.accessToken
// //                                                                    ).onSuccess {
// //                                                                        emit(
// //                                                                            Resource.Success<Lyrics>(
// //                                                                                it.toLyrics()
// //                                                                            )
// //                                                                        )
// //                                                                    }
// //                                                                        .onFailure {
// //                                                                            Log.d(
// //                                                                                "Lyrics",
// //                                                                                "Error: Lỗi getLyrics lần 2 ${it.message}"
// //                                                                            )
// //                                                                            emit(
// //                                                                                Resource.Error<Lyrics>(
// //                                                                                    "Not found"
// //                                                                                )
// //                                                                            )
// //                                                                        }
// //                                                                }
// //                                                                emit(Resource.Error<Lyrics>("Not found"))
// //                                                            }
// //                                                    }
// //                                                    .onFailure { e ->
// //                                                        e.printStackTrace()
// //                                                        emit(Resource.Error<Lyrics>("Not found"))
// //                                                    }
// //                                            }
// //                                        }
// //                                    }
// //                                    else {
// //
// //                                    }
// //                                }
// //                            }
//
//                        }
//                    }
//                } else {
//                    emit(Resource.Error<Lyrics>("Not found"))
//                }
//            }.onFailure {
//                Log.d("SongId", "Error: ${it.message}")
//                emit(Resource.Error<Lyrics>("Not found"))
//            }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getTranslateLyrics(id: String): Flow<MusixmatchTranslationLyricsResponse?> =
            flow {
                runCatching {
                    YouTube.musixmatchUserToken?.let {
                        YouTube.getMusixmatchTranslateLyrics(
                            id,
                            it,
                            dataStoreManager.translationLanguage.first(),
                        )
                            .onSuccess { lyrics ->
                                Log.w("Translate Lyrics", "lyrics: $lyrics")
                                emit(lyrics)
                            }
                            .onFailure {
                                it.printStackTrace()
                                emit(null)
                            }
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getSongInfo(videoId: String): Flow<SongInfoEntity?> =
            flow {
                runCatching {
                    YouTube.getSongInfo(videoId).onSuccess { songInfo ->
                        val song =
                            SongInfoEntity(
                                videoId = songInfo.videoId,
                                author = songInfo.author,
                                authorId = songInfo.authorId,
                                authorThumbnail = songInfo.authorThumbnail,
                                description = songInfo.description,
                                uploadDate = songInfo.uploadDate,
                                subscribers = songInfo.subscribers,
                                viewCount = songInfo.viewCount,
                                like = songInfo.like,
                                dislike = songInfo.dislike,
                            )
                        emit(song)
                        insertSongInfo(
                            song,
                        )
                    }.onFailure {
                        it.printStackTrace()
                        emit(getSongInfoEntiy(videoId).firstOrNull())
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getStream(
            videoId: String,
            itag: Int,
        ): Flow<String?> =
            flow {
                YouTube.player(videoId).onSuccess { data ->
                    val acceptToPlayVideo =
                        runBlocking { dataStoreManager.watchVideoInsteadOfPlayingAudio.first() == DataStoreManager.TRUE }
                    val videoItag =
                        VIDEO_QUALITY.itags.getOrNull(
                            VIDEO_QUALITY.items.indexOf(dataStoreManager.videoQuality.first()),
                        )
                            ?: 22
                    val response = data.second
                    if (data.third == MediaType.Song) {
                        Log.w(
                            "Stream",
                            "response: is SONG",
                        )
                    } else {
                        Log.w("Stream", "response: is VIDEO")
                    }
                    Log.w("Stream: ", data.toString())
                    var format =
                        if (acceptToPlayVideo) {
                            if (data.third == MediaType.Song) {
                                if (response.streamingData?.adaptiveFormats?.find { it.itag == 141 } != null) {
                                    response.streamingData?.adaptiveFormats?.find { it.itag == 141 }
                                } else {
                                    response.streamingData?.adaptiveFormats?.find { it.itag == itag }
                                }
                            } else {
                                response.streamingData?.formats?.find { it.itag == videoItag }
                            }
                        } else {
                            if (response.streamingData?.adaptiveFormats?.find { it.itag == 141 } != null) {
                                response.streamingData?.adaptiveFormats?.find { it.itag == 141 }
                            } else {
                                response.streamingData?.adaptiveFormats?.find { it.itag == itag }
                            }
                        }
                    if (format == null) {
                        format = response.streamingData?.adaptiveFormats?.lastOrNull()
                    }
                    Log.w("Stream", "format: $format")
                    runBlocking {
                        insertNewFormat(
                            NewFormatEntity(
                                videoId = videoId,
                                itag = format?.itag ?: itag,
                                mimeType =
                                    Regex("""([^;]+);\s*codecs=["']([^"']+)["']""").find(
                                        format?.mimeType ?: "",
                                    )?.groupValues?.getOrNull(1) ?: format?.mimeType ?: "",
                                codecs =
                                    Regex("""([^;]+);\s*codecs=["']([^"']+)["']""").find(
                                        format?.mimeType ?: "",
                                    )?.groupValues?.getOrNull(2) ?: format?.mimeType ?: "",
                                bitrate = format?.bitrate,
                                sampleRate = format?.audioSampleRate,
                                contentLength = format?.contentLength,
                                loudnessDb = response.playerConfig?.audioConfig?.loudnessDb?.toFloat(),
                                lengthSeconds = response.videoDetails?.lengthSeconds?.toInt(),
                                playbackTrackingVideostatsPlaybackUrl =
                                    response.playbackTracking?.videostatsPlaybackUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                playbackTrackingAtrUrl =
                                    response.playbackTracking?.atrUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                playbackTrackingVideostatsWatchtimeUrl =
                                    response.playbackTracking?.videostatsWatchtimeUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                cpn = data.first,
                            ),
                        )
                    }
                    if (data.first != null) {
                        emit(format?.url?.plus("&cpn=${data.first}"))
                    } else {
                        emit(format?.url)
                    }
//                insertFormat(
//                    FormatEntity(
//                        videoId = videoId,
//                        itag = format?.itag ?: itag,
//                        mimeType = format?.mimeType,
//                        bitrate = format?.bitrate?.toLong(),
//                        contentLength = format?.contentLength,
//                        lastModified = format?.lastModified,
//                        loudnessDb = response.playerConfig?.audioConfig?.loudnessDb?.toFloat(),
//                        uploader = response.videoDetails?.author?.replace(Regex(" - Topic| - Chủ đề|"), ""),
//                        uploaderId = response.videoDetails?.channelId,
//                        uploaderThumbnail = response.videoDetails?.authorAvatar,
//                        uploaderSubCount = response.videoDetails?.authorSubCount,
//                        description = response.videoDetails?.description,
//                        youtubeCaptionsUrl = response.captions?.playerCaptionsTracklistRenderer?.captionTracks?.get(
//                            0
//                        )?.baseUrl?.replace("&fmt=srv3", ""),
//                        lengthSeconds = response.videoDetails?.lengthSeconds?.toInt(),
//                    )
//                )
                }.onFailure {
                    it.printStackTrace()
                    Log.e("Stream", "Error: ${it.message}")
                    emit(null)
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getLibraryPlaylist(): Flow<ArrayList<PlaylistsResult>?> =
            flow {
                YouTube.getLibraryPlaylists().onSuccess { data ->
                    val input =
                        data.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(
                            0,
                        )?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                            0,
                        )?.gridRenderer?.items
                            ?: null
                    if (input != null) {
                        Log.w("Library", "input: ${input.size}")
                        val list = parseLibraryPlaylist(input)
                        emit(list)
                    } else {
                        emit(null)
                    }
                }
                    .onFailure { e ->
                        Log.e("Library", "Error: ${e.message}")
                        e.printStackTrace()
                        emit(null)
                    }
            }.flowOn(Dispatchers.IO)

        suspend fun initPlayback(
            playback: String,
            atr: String,
            watchTime: String,
            cpn: String,
            playlistId: String?,
        ): Flow<Pair<Int, Float>> =
            flow {
                YouTube.initPlayback(playback, atr, watchTime, cpn, playlistId).onSuccess { response ->
                    emit(response)
                }.onFailure {
                    Log.e("InitPlayback", "Error: ${it.message}")
                    emit(Pair(0, 0f))
                }
            }.flowOn(Dispatchers.IO)

        suspend fun getSkipSegments(videoId: String): Flow<List<SkipSegments>?> =
            flow {
                YouTube.getSkipSegments(videoId).onSuccess {
                    emit(it)
                }.onFailure {
                    emit(null)
                }
            }.flowOn(Dispatchers.IO)

        fun getFullMetadata(videoId: String): Flow<YouTubeInitialPage?> =
            flow {
                Log.w("getFullMetadata", "videoId: $videoId")
                YouTube.getFullMetadata(videoId).onSuccess {
                    emit(it)
                }.onFailure {
                    Log.e("getFullMetadata", "Error: ${it.message}")
                    emit(null)
                }
            }.flowOn(Dispatchers.IO)

        fun checkForUpdate(): Flow<GithubResponse?> =
            flow {
                YouTube.checkForUpdate().onSuccess {
                    emit(it)
                }
                    .onFailure {
                        emit(null)
                    }
            }

        suspend fun getYouTubeSetVideoId(youtubePlaylistId: String): Flow<ArrayList<SetVideoIdEntity>?> =
            flow {
                runCatching {
                    var id = ""
                    if (!youtubePlaylistId.startsWith("VL")) {
                        id += "VL$youtubePlaylistId"
                    } else {
                        id += youtubePlaylistId
                    }
                    Log.d("Repository", "playlist id: $id")
                    YouTube.customQuery(browseId = id, setLogin = true).onSuccess { result ->
                        val listContent: ArrayList<MusicShelfRenderer.Content> = arrayListOf()
                        val data: List<MusicShelfRenderer.Content>? =
                            result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(
                                0,
                            )?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                                0,
                            )?.musicPlaylistShelfRenderer?.contents
                        if (data != null) {
                            Log.d("Data", "data: $data")
                            Log.d("Data", "data size: ${data.size}")
                            listContent.addAll(data)
                        }
                        var continueParam =
                            result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(
                                0,
                            )?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                                0,
                            )?.musicPlaylistShelfRenderer?.continuations?.get(
                                0,
                            )?.nextContinuationData?.continuation
                        var count = 0
                        Log.d("Repository", "playlist data: ${listContent.size}")
                        Log.d("Repository", "continueParam: $continueParam")
                        while (continueParam != null) {
                            YouTube.customQuery(
                                browseId = "",
                                continuation = continueParam,
                                setLogin = true,
                            ).onSuccess { values ->
                                Log.d("Continue", "continue: $continueParam")
                                val dataMore: List<MusicShelfRenderer.Content>? =
                                    values.continuationContents?.musicPlaylistShelfContinuation?.contents
                                if (dataMore != null) {
                                    listContent.addAll(dataMore)
                                }
                                continueParam =
                                    values.continuationContents?.musicPlaylistShelfContinuation?.continuations?.get(
                                        0,
                                    )?.nextContinuationData?.continuation
                                count++
                            }.onFailure {
                                Log.e("Continue", "Error: ${it.message}")
                                continueParam = null
                                count++
                            }
                        }
                        Log.d("Repository", "playlist final data: ${listContent.size}")
                        parseSetVideoId(listContent).let { playlist ->
                            Log.d("Repository", "playlist final data setVideoId: $playlist")
                            playlist.forEach { item ->
                                insertSetVideoId(item)
                            }
                            emit(playlist)
                        }
                    }.onFailure { e ->
                        e.printStackTrace()
                        emit(null)
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun createYouTubePlaylist(playlist: LocalPlaylistEntity): Flow<String?> =
            flow {
                runCatching {
                    YouTube.createPlaylist(playlist.title, playlist.tracks).onSuccess {
                        emit(it.playlistId)
                    }.onFailure {
                        it.printStackTrace()
                        emit(null)
                    }
                }
            }

        suspend fun editYouTubePlaylist(
            title: String,
            youtubePlaylistId: String,
        ): Flow<Int> =
            flow {
                runCatching {
                    YouTube.editPlaylist(youtubePlaylistId, title).onSuccess { response ->
                        emit(response)
                    }.onFailure {
                        it.printStackTrace()
                        emit(0)
                    }
                }
            }

        suspend fun removeYouTubePlaylistItem(
            youtubePlaylistId: String,
            videoId: String,
        ) = flow {
            runCatching {
                getSetVideoId(videoId).collect { setVideoId ->
                    if (setVideoId?.setVideoId != null) {
                        YouTube.removeItemYouTubePlaylist(
                            youtubePlaylistId,
                            videoId,
                            setVideoId.setVideoId,
                        ).onSuccess {
                            emit(it)
                        }.onFailure {
                            emit(0)
                        }
                    } else {
                        emit(0)
                    }
                }
            }
        }

        suspend fun addYouTubePlaylistItem(
            youtubePlaylistId: String,
            videoId: String,
        ) = flow {
            runCatching {
                YouTube.addPlaylistItem(youtubePlaylistId, videoId).onSuccess {
                    if (it.playlistEditResults.isNotEmpty()) {
                        for (playlistEditResult in it.playlistEditResults) {
                            insertSetVideoId(
                                SetVideoIdEntity(
                                    playlistEditResult.playlistEditVideoAddedResultData.videoId,
                                    playlistEditResult.playlistEditVideoAddedResultData.setVideoId,
                                ),
                            )
                        }
                        emit(it.status)
                    } else {
                        emit("FAILED")
                    }
                }.onFailure {
                    emit("FAILED")
                }
            }
        }

        suspend fun loginToMusixMatch(
            email: String,
            password: String,
        ): Flow<MusixmatchCredential?> =
            flow {
                runCatching {
                    if (YouTube.musixmatchUserToken != null && YouTube.musixmatchUserToken != "") {
                        YouTube.postMusixmatchCredentials(
                            email,
                            password,
                            YouTube.musixmatchUserToken!!,
                        ).onSuccess { response ->
                            emit(response)
                        }.onFailure {
                            it.printStackTrace()
                            emit(null)
                        }
                    } else {
                        YouTube.getMusixmatchUserToken().onSuccess { usertoken ->
                            YouTube.musixmatchUserToken = usertoken.message.body.user_token
                            delay(2000)
                            YouTube.postMusixmatchCredentials(
                                email,
                                password,
                                YouTube.musixmatchUserToken!!,
                            ).onSuccess { response ->
                                emit(response)
                            }.onFailure {
                                it.printStackTrace()
                                emit(null)
                            }
                        }
                            .onFailure { throwable ->
                                throwable.printStackTrace()
                                emit(null)
                            }
                    }
                }
            }

        suspend fun updateWatchTime(
            playbackTrackingVideostatsWatchtimeUrl: String,
            watchTimeList: ArrayList<Float>,
            cpn: String,
            playlistId: String?,
        ): Flow<Int> =
            flow {
                runCatching {
                    YouTube.updateWatchTime(
                        playbackTrackingVideostatsWatchtimeUrl,
                        watchTimeList,
                        cpn,
                        playlistId,
                    ).onSuccess { response ->
                        emit(response)
                    }.onFailure {
                        it.printStackTrace()
                        emit(0)
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun updateWatchTimeFull(
            watchTime: String,
            cpn: String,
            playlistId: String?,
        ): Flow<Int> =
            flow {
                runCatching {
                    YouTube.updateWatchTimeFull(watchTime, cpn, playlistId).onSuccess { response ->
                        emit(response)
                    }.onFailure {
                        it.printStackTrace()
                        emit(0)
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun addToYouTubeLiked(mediaId: String?): Flow<Int> =
            flow {
                if (mediaId != null) {
                    runCatching {
                        YouTube.addToLiked(mediaId).onSuccess {
                            Log.d("Liked", "Success: $it")
                            emit(it)
                        }.onFailure {
                            it.printStackTrace()
                            emit(0)
                        }
                    }
                }
            }.flowOn(Dispatchers.IO)

        suspend fun removeFromYouTubeLiked(mediaId: String?): Flow<Int> =
            flow {
                if (mediaId != null) {
                    runCatching {
                        YouTube.removeFromLiked(mediaId).onSuccess {
                            Log.d("Liked", "Success: $it")
                            emit(it)
                        }.onFailure {
                            it.printStackTrace()
                            emit(0)
                        }
                    }
                }
            }.flowOn(Dispatchers.IO)
    }
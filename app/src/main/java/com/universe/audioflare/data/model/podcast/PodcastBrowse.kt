package com.universe.audioflare.data.model.podcast

import com.universe.audioflare.data.model.searchResult.songs.Artist
import com.universe.audioflare.data.model.searchResult.songs.Thumbnail

data class PodcastBrowse(
    val title: String,
    val author: Artist,
    val authorThumbnail: String?,
    val thumbnail: List<Thumbnail>,
    val description: String?,
    val listEpisode: List<EpisodeItem>
) {
    data class EpisodeItem(
        val title: String,
        val author: Artist,
        val description: String?,
        val thumbnail: List<Thumbnail>,
        val createdDay: String?,
        val durationString: String?,
        val videoId: String,
    )
}
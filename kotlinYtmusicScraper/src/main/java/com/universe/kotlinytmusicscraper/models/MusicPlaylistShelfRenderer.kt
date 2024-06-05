package com.universe.kotlinytmusicscraper.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicPlaylistShelfRenderer(
    val playlistId: String?,
    val contents: List<MusicShelfRenderer.Content>,
    val collapsedItemCount: Int,
    val continuations: List<Continuation>?,
)

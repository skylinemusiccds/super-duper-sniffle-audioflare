package com.universe.audioflare.data.model.browse.playlist


import com.google.gson.annotations.SerializedName

data class AlbumPlaylist(
    @SerializedName("id")
    val id: Any,
    @SerializedName("name")
    val name: String
)
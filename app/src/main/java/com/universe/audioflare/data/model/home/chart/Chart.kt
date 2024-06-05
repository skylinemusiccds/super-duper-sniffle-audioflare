package com.universe.audioflare.data.model.home.chart


import com.google.gson.annotations.SerializedName
import com.universe.audioflare.data.model.browse.album.Track

data class Chart(
    @SerializedName("artists")
    val artists: Artists,
    @SerializedName("countries")
    val countries: Countries?,
    @SerializedName("videos")
    val videos: Videos,
    val songs: ArrayList<Track>? = null,
    val trending: ArrayList<Track>? = null
)
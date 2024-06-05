package com.universe.kotlinytmusicscraper.models.subscriptionButton


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LongSubscriberCountText(
    @SerialName("runs")
    val runs: List<Run>
)
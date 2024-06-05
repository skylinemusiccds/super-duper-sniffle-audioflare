package com.universe.kotlinytmusicscraper.models.response

import com.universe.kotlinytmusicscraper.models.Button
import com.universe.kotlinytmusicscraper.models.Continuation
import com.universe.kotlinytmusicscraper.models.Menu
import com.universe.kotlinytmusicscraper.models.MusicShelfRenderer
import com.universe.kotlinytmusicscraper.models.ResponseContext
import com.universe.kotlinytmusicscraper.models.Runs
import com.universe.kotlinytmusicscraper.models.SectionListRenderer
import com.universe.kotlinytmusicscraper.models.SubscriptionButton
import com.universe.kotlinytmusicscraper.models.Tabs
import com.universe.kotlinytmusicscraper.models.ThumbnailRenderer
import kotlinx.serialization.Serializable

@Serializable
data class BrowseResponse(
    val contents: Contents?,
    val continuationContents: ContinuationContents?,
    val header: Header?,
    val microformat: Microformat?,
    val responseContext: ResponseContext,
    val background: Background?,
) {
    @Serializable
    data class Background(
        val musicThumbnailRenderer: ThumbnailRenderer.MusicThumbnailRenderer?,
    )

    @Serializable
    data class Contents(
        val singleColumnBrowseResultsRenderer: Tabs?,
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer?,
        val sectionListRenderer: SectionListRenderer?,
    ) {
        @Serializable
        data class TwoColumnBrowseResultsRenderer(
            val secondaryContents: SecondaryContents?,
            val tabs: List<Tabs.Tab>?,
        ) {
            @Serializable
            data class SecondaryContents(
                val sectionListRenderer: SectionListRenderer?,
            )
        }
    }

    @Serializable
    data class ContinuationContents(
        val sectionListContinuation: SectionListContinuation?,
        val musicPlaylistShelfContinuation: MusicPlaylistShelfContinuation?,
        val musicShelfContinuation: SearchResponse.ContinuationContents.MusicShelfContinuation?,
    ) {
        @Serializable
        data class SectionListContinuation(
            val contents: List<SectionListRenderer.Content>,
            val continuations: List<Continuation>?,
        )

        @Serializable
        data class MusicPlaylistShelfContinuation(
            val contents: List<MusicShelfRenderer.Content>,
            val continuations: List<Continuation>?,
        )
    }

    @Serializable
    data class Header(
        val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer?,
        val musicDetailHeaderRenderer: MusicDetailHeaderRenderer?,
        val musicEditablePlaylistDetailHeaderRenderer: MusicEditablePlaylistDetailHeaderRenderer?,
        val musicVisualHeaderRenderer: MusicVisualHeaderRenderer?,
        val musicHeaderRenderer: MusicHeaderRenderer?,
    ) {
        @Serializable
        data class MusicImmersiveHeaderRenderer(
            val title: Runs,
            val description: Runs?,
            val thumbnail: ThumbnailRenderer?,
            val playButton: Button?,
            val startRadioButton: Button?,
            val subscriptionButton: SubscriptionButton?,
            val menu: Menu,
        )

        @Serializable
        data class MusicDetailHeaderRenderer(
            val title: Runs,
            val subtitle: Runs,
            val secondSubtitle: Runs,
            val description: Runs?,
            val thumbnail: ThumbnailRenderer,
            val menu: Menu,
        )

        @Serializable
        data class MusicEditablePlaylistDetailHeaderRenderer(
            val header: Header,
        ) {
            @Serializable
            data class Header(
                val musicDetailHeaderRenderer: MusicDetailHeaderRenderer,
            )
        }

        @Serializable
        data class MusicVisualHeaderRenderer(
            val title: Runs,
            val foregroundThumbnail: ThumbnailRenderer,
            val thumbnail: ThumbnailRenderer?,
        )

        @Serializable
        data class MusicHeaderRenderer(
            val title: Runs,
        )
    }

    @Serializable
    data class Microformat(
        val microformatDataRenderer: MicroformatDataRenderer?,
    ) {
        @Serializable
        data class MicroformatDataRenderer(
            val urlCanonical: String?,
        )
    }
}

package com.universe.audioflare.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ripple
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.universe.audioflare.R
import com.universe.audioflare.common.DownloadState
import com.universe.audioflare.data.db.entities.SongEntity
import com.universe.audioflare.data.model.browse.album.Track
import com.universe.audioflare.data.model.searchResult.songs.Artist
import com.universe.audioflare.extension.connectArtists
import com.universe.audioflare.extension.toListName
import com.universe.audioflare.ui.theme.typo
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent

@Composable
fun PlaylistItems(
    track: Track? = null,
    songEntity: SongEntity? = null,
    isPlaying: Boolean,
    onMoreClickListener: ((videoId: String) -> Unit)? = null,
    onClickListener: ((videoId: String) -> Unit)? = null,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.audio_playing_animation),
    )
    Box(
        modifier =
            Modifier.clickable(
                onClick = {
                    onClickListener?.invoke(track?.videoId ?: songEntity?.videoId ?: "")
                },
                indication = ripple(),
                interactionSource =
                    remember {
                        MutableInteractionSource()
                    },
            ),
    ) {
        Row(
            Modifier
                .padding(vertical = 10.dp, horizontal = 15.dp)
                .fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.size(50.dp)) {
                Crossfade(isPlaying) {
                    if (it) {
                        LottieAnimation(composition, iterations = LottieConstants.IterateForever)
                    } else {
                        CoilImage(
                            imageModel = {
                                track?.thumbnails?.lastOrNull()?.url ?: songEntity?.thumbnails
                            },
                            imageOptions =
                                ImageOptions(
                                    contentScale = ContentScale.FillWidth,
                                    alignment = Alignment.Center,
                                ),
                            previewPlaceholder = painterResource(id = R.drawable.holder),
                            component =
                                rememberImageComponent {
                                    CrossfadePlugin(
                                        duration = 550,
                                    )
                                },
                            modifier =
                                Modifier
                                    .fillMaxSize(),
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = track?.title ?: songEntity?.title ?: "",
                    style = typo.labelMedium,
                    maxLines = 1,
                    color = Color.White,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                            .focusable(),
                )
                Row {
                    AnimatedVisibility(
                        visible =
                            if (songEntity != null) {
                                songEntity.downloadState == DownloadState.STATE_DOWNLOADED
                            } else {
                                false
                            },
                    ) {
                        Row {
                            Icon(
                                painter = painterResource(id = R.drawable.download_for_offline_white),
                                tint = Color.White,
                                contentDescription = "",
                                modifier = Modifier.size(20.dp).padding(2.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                    }
                    Text(
                        text =
                            (
                                track?.artists?.toListName()?.connectArtists()
                                    ?: songEntity?.artistName?.connectArtists()
                            ) ?: "",
                        style = typo.bodyMedium,
                        maxLines = 1,
                        color = Color(0xC4FFFFFF),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                                .focusable(),
                    )
                }
            }
            RippleIconButton(resId = R.drawable.baseline_more_vert_24, fillMaxSize = false) {
                val videoId = track?.videoId ?: songEntity?.videoId
                videoId?.let { onMoreClickListener?.invoke(it) }
            }
        }
    }
}

@Composable
fun SuggestItems(
    track: Track,
    isPlaying: Boolean,
    onClickListener: (() -> Unit)? = null,
    onAddClickListener: (() -> Unit)? = null,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.audio_playing_animation),
    )
    Box(
        modifier =
            Modifier.clickable(
                onClick = onClickListener ?: {},
                indication = ripple(),
                interactionSource =
                    remember {
                        MutableInteractionSource()
                    },
            ),
    ) {
        Row(
            Modifier
                .padding(vertical = 10.dp)
                .fillMaxWidth(),
        ) {
            Box(modifier = Modifier.size(40.dp)) {
                Crossfade(isPlaying) {
                    if (it) {
                        LottieAnimation(composition, iterations = LottieConstants.IterateForever)
                    } else {
                        CoilImage(
                            imageModel = {
                                track.thumbnails?.lastOrNull()?.url
                            },
                            imageOptions =
                                ImageOptions(
                                    contentScale = ContentScale.FillWidth,
                                    alignment = Alignment.Center,
                                ),
                            previewPlaceholder = painterResource(id = R.drawable.holder),
                            component =
                                rememberImageComponent {
                                    CrossfadePlugin(
                                        duration = 550,
                                    )
                                },
                            modifier =
                                Modifier
                                    .wrapContentHeight()
                                    .fillMaxWidth(),
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = track.title,
                    style = typo.titleSmall,
                    maxLines = 1,
                    color = Color.White,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                            .focusable(),
                )
                Text(
                    text =
                        (
                            track.artists?.toListName()?.connectArtists()
                        ) ?: "",
                    style = typo.bodySmall,
                    maxLines = 1,
                    color = Color(0xC4FFFFFF),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                            .focusable(),
                )
            }
            RippleIconButton(
                resId = R.drawable.baseline_add_24,
                fillMaxSize = false,
                onClick =
                    onAddClickListener ?: {
                    },
            )
        }
    }
}

@Preview
@Composable
fun PlaylistItemsPreview() {
    PlaylistItems(
        isPlaying = false,
        track =
            Track(
                album = null,
                artists =
                    listOf(
                        Artist(
                            id = null,
                            name = "Janai",
                        ),
                    ),
                duration = null,
                durationSeconds = null,
                isAvailable = false,
                isExplicit = false,
                likeStatus = null,
                thumbnails = listOf(),
                title = "Ernest",
                videoId = "Delaina",
                videoType = null,
                category = null,
                feedbackTokens = null,
                resultType = null,
                year = null,
            ),
    )
}
package com.universe.audioflare.ui.component

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.wear.compose.material3.ripple
import coil.compose.AsyncImage
import com.universe.audioflare.R
import com.universe.audioflare.common.Config
import com.universe.audioflare.data.model.browse.album.Track
import com.universe.audioflare.data.model.explore.mood.genre.ItemsPlaylist
import com.universe.audioflare.data.model.explore.mood.moodmoments.Item
import com.universe.audioflare.data.model.home.Content
import com.universe.audioflare.data.model.home.HomeItem
import com.universe.audioflare.data.model.home.chart.ItemArtist
import com.universe.audioflare.data.model.home.chart.ItemVideo
import com.universe.audioflare.data.queue.Queue
import com.universe.audioflare.extension.connectArtists
import com.universe.audioflare.extension.generateRandomColor
import com.universe.audioflare.extension.navigateSafe
import com.universe.audioflare.extension.toListName
import com.universe.audioflare.extension.toTrack
import com.universe.audioflare.ui.theme.typo
import com.universe.audioflare.viewModel.HomeViewModel
import com.universe.audioflare.viewModel.SharedViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@UnstableApi
@Composable
fun HomeItem(
    homeViewModel: HomeViewModel,
    sharedViewModel: SharedViewModel,
    navController: NavController,
    data: HomeItem
) {
    val coroutineScope = rememberCoroutineScope()
    var bottomSheetShow by remember { mutableStateOf(false) }
    if (bottomSheetShow) {
        NowPlayingBottomSheet(
            isBottomSheetVisible = bottomSheetShow,
            onDismiss = { bottomSheetShow = false },
            sharedViewModel = sharedViewModel,
            songEntity = homeViewModel.songEntity.collectAsState(),
            navController = navController,
            onToggleLike = { checked ->
                val track = homeViewModel.songEntity.value
                if (track != null) {
                    if (checked) {
                        homeViewModel.updateLikeStatus(track.videoId, true)
                    } else {
                        homeViewModel.updateLikeStatus(track.videoId, false)
                    }
                    coroutineScope.launch {
                        if (sharedViewModel.simpleMediaServiceHandler?.nowPlaying?.first()?.mediaId == track.videoId) {
                            delay(500)
                            sharedViewModel.refreshSongDB()
                        }
                    }
                }
            },
            getLocalPlaylist = {
                homeViewModel.getAllLocalPlaylist()
            },
            listLocalPlaylist = homeViewModel.localPlaylist.collectAsState(),
        )
    }

    Column {
        Row(
            modifier = if (data.channelId != null) {
                Modifier
                    .focusable(true)
                    .clickable(
                        onClick = {
                            val args = Bundle()
                            args.putString("channelId", data.channelId)
                            navController.navigateSafe(R.id.action_global_artistFragment, args)
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple()
                    )
            } else Modifier
        ) {
            AnimatedVisibility(
                visible = (data.thumbnail?.lastOrNull() != null),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                CoilImage(
                    imageModel = { data.thumbnail?.lastOrNull()?.url },
                    imageOptions = ImageOptions(
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                    ),
                    previewPlaceholder = painterResource(id = R.drawable.holder),
                    component = rememberImageComponent {
                        CrossfadePlugin(
                            duration = 550
                        )
                    },
                    modifier = Modifier
                        .size(45.dp)
                        .clip(
                            CircleShape
                        )
                )
            }
            Column(
                Modifier
                    .padding(vertical = 8.dp)
                    .padding(start = 10.dp)
            ) {
                AnimatedVisibility(visible = (data.subtitle != null && data.subtitle != "")) {
                    Text(
                        text = data.subtitle ?: "",
                        style = typo.bodyMedium,
                    )
                }
                Text(
                    text = data.title,
                    style = typo.headlineMedium,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        LazyRow(
            modifier = Modifier.padding(
                vertical = 15.dp
            )
        ) {
            items(data.contents) { temp ->
                if (temp != null) {
                    if ((temp.playlistId != null && temp.videoId == null) || (temp.playlistId != null && temp.videoId == "")) {
                        if (temp.playlistId.startsWith("UC"))
                            HomeItemArtist(onClick = {
                                val args = Bundle()
                                val channelId =
                                    temp.playlistId
                                Log.d("HomeItemAdapter", "onArtistItemClick: $channelId")
                                args.putString("channelId", channelId)
                                navController.navigateSafe(R.id.action_global_artistFragment, args)
                            }, data = temp)
                        else
                            HomeItemContentPlaylist(onClick = {
                                val args = Bundle()
                                args.putString("id", temp.playlistId)
                                navController.navigateSafe(
                                    R.id.action_global_playlistFragment,
                                    args
                                )
                            }, data = temp)
                    } else if ((temp.browseId != null && temp.videoId == null) || (temp.browseId != null && temp.videoId == "")) {
                        if (temp.browseId.startsWith("UC"))
                            HomeItemArtist(onClick = {
                                val args = Bundle()
                                val channelId =
                                    temp.browseId
                                Log.d("HomeItemAdapter", "onArtistItemClick: $channelId")
                                args.putString("channelId", channelId)
                                navController.navigateSafe(R.id.action_global_artistFragment, args)
                            }, data = temp)
                        else
                            HomeItemContentPlaylist(onClick = {
                                val args = Bundle()
                                args.putString("browseId", temp.browseId)
                                navController.navigateSafe(R.id.action_global_albumFragment, args)
                            }, data = temp)
                    } else if (temp.thumbnails.firstOrNull()?.width != temp.thumbnails.firstOrNull()?.height) {
                        HomeItemVideo(onClick = {
                            val args = Bundle()
                            args.putString("videoId", temp.videoId)
                            args.putString("from", temp.title)
                            Queue.initPlaylist(
                                "RDAMVM${temp.videoId}",
                                temp.title,
                                Queue.PlaylistType.RADIO
                            )
                            val firstQueue: Track = temp.toTrack()
                            Queue.setNowPlaying(firstQueue)
                            args.putString("type", Config.SONG_CLICK)
                            navController.navigateSafe(
                                R.id.action_global_nowPlayingFragment,
                                args
                            )
                        }, onLongClick = {
                            homeViewModel.getSongEntity(temp.toTrack())
                            bottomSheetShow = true
                        },
                            data = temp
                        )
                    } else {
                        HomeItemSong(onClick = {
                            val args = Bundle()
                            args.putString("videoId", temp.videoId)
                            args.putString("from", temp.title)
                            Queue.initPlaylist(
                                "RDAMVM${temp.videoId}",
                                temp.title,
                                Queue.PlaylistType.RADIO
                            )
                            val firstQueue: Track = temp.toTrack()
                            Queue.setNowPlaying(firstQueue)
                            args.putString("type", Config.SONG_CLICK)
                            navController.navigateSafe(
                                R.id.action_global_nowPlayingFragment,
                                args
                            )
                        }, onLongClick = {
                            homeViewModel.getSongEntity(temp.toTrack())
                            bottomSheetShow = true
                        },
                            data = temp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeItemContentPlaylist(
    onClick: () -> Unit,
    data: Any,
) {
    Box(
        Modifier
            .wrapContentSize()
            .focusable(true)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)

        ) {
            AsyncImage(
                model =
                when (data) {
                    is Content -> data.thumbnails.lastOrNull()?.url
                    is com.universe.audioflare.data.model.explore.mood.genre.Content -> data.thumbnail?.lastOrNull()?.url
                    is com.universe.audioflare.data.model.explore.mood.moodmoments.Content -> data.thumbnails?.lastOrNull()?.url
                    else -> null
                },
                placeholder = painterResource(id = R.drawable.holder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(180.dp)
                    .clip(
                        RoundedCornerShape(10.dp)
                    )
            )
            Text(
                text =
                when (data) {
                    is Content -> data.title
                    is com.universe.audioflare.data.model.explore.mood.genre.Content -> data.title.title
                    is com.universe.audioflare.data.model.explore.mood.moodmoments.Content -> data.title
                    else -> ""
                },
                style = typo.titleSmall,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .width(180.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .padding(top = 10.dp)
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                    .focusable()
            )
            Text(
                text =
                when (data) {
                    is Content -> data.description
                        ?: if (data.playlistId == null) (if (!data.artists.isNullOrEmpty()) data.artists.toListName()
                            .connectArtists() else stringResource(id = R.string.album)) else stringResource(
                            id = R.string.playlist
                        )

                    is com.universe.audioflare.data.model.explore.mood.genre.Content -> data.title.subtitle
                    is com.universe.audioflare.data.model.explore.mood.moodmoments.Content -> data.subtitle
                    else -> ""
                },
                style = typo.bodySmall,
                maxLines = 1,
                modifier = Modifier
                    .width(180.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .padding(top = 10.dp)
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                    .focusable()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickPicksItem(
    onClick: () -> Unit,
    data: Content
) {
    val configuration = LocalConfiguration.current
    Box(
        modifier = Modifier
            .wrapContentSize()
            .focusable(true)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
    ) {
        Row(
            modifier = Modifier
                .width(configuration.screenWidthDp.dp)
                .padding(10.dp)
        ) {
            CoilImage(
                imageModel = {
                    data.thumbnails.lastOrNull()?.url
                }, // loading a network image or local resource using an URL.
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                ),
                previewPlaceholder = painterResource(id = R.drawable.holder),
                component = rememberImageComponent {
                    CrossfadePlugin(
                        duration = 550
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(50.dp)
                    .clip(
                        RoundedCornerShape(10)
                    )
            )
            Column(
                Modifier
                    .padding(
                        start = 20.dp
                    )
                    .align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = data.title, style = typo.titleSmall, maxLines = 1, color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                        .focusable()
                        .padding(
                            bottom = 3.dp
                        )
                )

                Text(
                    text = data.artists.toListName().connectArtists(),
                    style = typo.bodySmall,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                        .focusable()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeItemSong(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    data: Content
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable(true)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
        ) {
            CoilImage(
                imageModel = {
                    var thumbUrl = data.thumbnails.lastOrNull()?.url
                    if (thumbUrl?.contains("w120") == true) {
                        thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
                    }
                    thumbUrl
                },
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                ),
                previewPlaceholder = painterResource(id = R.drawable.holder),
                component = rememberImageComponent {
                    CrossfadePlugin(
                        duration = 550
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(180.dp)
                    .clip(
                        RoundedCornerShape(10.dp)
                    )
            )
            Text(
                text = data.title,
                style = typo.titleSmall,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .width(180.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .padding(top = 10.dp)
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                    .focusable()
            )
            Text(
                text = data.artists.toListName().connectArtists(),
                style = typo.bodySmall,
                maxLines = 1,
                modifier = Modifier
                    .width(180.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                    .focusable()
                    .padding(vertical = 3.dp)
            )
            Text(
                text = data.album?.name ?: stringResource(id = R.string.songs),
                style = typo.bodySmall,
                maxLines = 1,
                modifier = Modifier
                    .width(180.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                    .focusable()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeItemVideo(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    data: Content
) {
    Box(
        Modifier
            .fillMaxSize()
            .focusable(true)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
        ) {
            CoilImage(
                imageModel = { data.thumbnails.lastOrNull()?.url },
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                ),
                previewPlaceholder = painterResource(id = R.drawable.holder_video),
                component = rememberImageComponent {
                    CrossfadePlugin(
                        duration = 550
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(320.dp)
                    .height(180.dp)
                    .clip(
                        RoundedCornerShape(10.dp)
                    )
            )
            Text(
                text = data.title,
                style = typo.titleSmall,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .width(320.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .padding(top = 10.dp)
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                    .focusable()
            )
            Text(
                text = data.artists.toListName().connectArtists(),
                style = typo.bodySmall,
                maxLines = 1,
                modifier = Modifier
                    .width(320.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                    .focusable()
                    .padding(vertical = 3.dp)
            )
            Text(
                text = stringResource(id = R.string.videos),
                style = typo.bodySmall,
                maxLines = 1,
                modifier = Modifier
                    .width(320.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                    .focusable()
            )
        }
    }
}

@Composable
fun HomeItemArtist(
    onClick: () -> Unit,
    data: Content
) {
    Box(
        Modifier
            .fillMaxSize()
            .focusable(true)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
        ) {
            CoilImage(
                imageModel = { data.thumbnails.lastOrNull()?.url },
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                ),
                previewPlaceholder = painterResource(id = R.drawable.holder),
                component = rememberImageComponent {
                    CrossfadePlugin(
                        duration = 550
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(180.dp)
                    .clip(
                        CircleShape
                    )
            )
            Text(
                text = data.title,
                style = typo.titleSmall,
                color = Color.White,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(180.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .padding(top = 10.dp)
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                    .focusable()
            )
            Text(
                text = stringResource(id = R.string.artists),
                style = typo.bodySmall,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(180.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                    .focusable()
                    .padding(vertical = 3.dp)
            )
            Text(
                text = "",
                style = typo.bodySmall,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(180.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                    .focusable()
            )
        }
    }
}

@Composable
fun MoodMomentAndGenreHomeItem(
    title: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        onClick = onClick,
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier
            .width(180.dp)
            .height(50.dp)
            .padding(8.dp)
    ) {
        Row {
            Box(
                Modifier
                    .width(10.dp)
                    .height(64.dp)
                    .background(generateRandomColor())
            )
            Text(
                text = title,
                style = typo.titleSmall,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterVertically)
            )
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemVideoChart(
    onClick: () -> Unit,
    data: ItemVideo,
    position: Int
) {
    Box(
        Modifier
            .wrapContentSize()
            .focusable(true)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
        ) {
            CoilImage(
                imageModel = { data.thumbnails.lastOrNull()?.url },
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                ),
                previewPlaceholder = painterResource(id = R.drawable.holder_video),
                component = rememberImageComponent {
                    CrossfadePlugin(
                        duration = 550
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(280.dp)
                    .height(160.dp)
                    .clip(
                        RoundedCornerShape(10)
                    )
            )
            Row {
                Text(
                    text = position.toString(),
                    style = typo.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .width(40.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .align(Alignment.CenterVertically)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                        .focusable()
                )
                Column(Modifier.padding(start = 10.dp)) {
                    Text(
                        text = data.title,
                        style = typo.titleMedium,
                        maxLines = 1,
                        color = Color.White,
                        modifier = Modifier
                            .width(210.dp)
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .padding(top = 10.dp)
                            .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                            .focusable()
                    )
                    Text(
                        text = data.artists.toListName().connectArtists(),
                        style = typo.bodyMedium,
                        modifier = Modifier
                            .width(210.dp)
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                            .focusable()
                            .padding(vertical = 3.dp)
                    )
                    Text(
                        text = data.views,
                        style = typo.bodySmall,
                        modifier = Modifier
                            .width(210.dp)
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                            .focusable()
                            .padding(end = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ItemArtistChart(
    onClick: () -> Unit,
    data: ItemArtist,
    context: Context
) {
    Box(
        Modifier
            .wrapContentSize()
            .focusable(true)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = data.rank,
                style = typo.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .wrapContentSize(Alignment.Center)
                    .align(Alignment.CenterVertically)
                    .padding(end = 20.dp)
            )
            CoilImage(
                imageModel = { data.thumbnails.lastOrNull()?.url },
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                ),
                previewPlaceholder = painterResource(id = R.drawable.holder),
                component = rememberImageComponent {
                    CrossfadePlugin(
                        duration = 550
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(60.dp)
                    .clip(
                        CircleShape
                    )
            )
            Column(
                Modifier
                    .padding(start = 15.dp)
                    .width(180.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = data.title, style = typo.titleMedium, modifier = Modifier
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                        .focusable()
                )
                Text(
                    text = if (data.subscribers.contains(
                            context.getString(R.string.subscribers).replace("%1\$s ", "")
                        )
                    ) data.subscribers else stringResource(
                        id = R.string.subscribers, data.subscribers
                    ), style = typo.bodySmall, modifier = Modifier
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                        .focusable()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemTrackChart(
    onClick: () -> Unit,
    data: Track,
    position: Int?
) {
    val configuration = LocalConfiguration.current
    Box(
        modifier = Modifier
            .wrapContentSize()
            .focusable(true)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
    ) {
        Row(
            modifier = Modifier
                .width(configuration.screenWidthDp.dp)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Crossfade(targetState = position != null, label = "Chart Track Position") {
                if (it) {
                    Row {
                        Text(
                            text = position.toString(),
                            style = typo.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(40.dp)
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .align(Alignment.CenterVertically)
                                .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                                .focusable()
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
            }
            CoilImage(
                imageModel = {
                    data.thumbnails?.lastOrNull()?.url
                }, // loading a network image or local resource using an URL.
                imageOptions = ImageOptions(
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                ),
                previewPlaceholder = painterResource(id = R.drawable.holder),
                component = rememberImageComponent {
                    CrossfadePlugin(
                        duration = 550
                    )
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(50.dp)
                    .clip(
                        RoundedCornerShape(10)
                    )
            )
            Column(
                Modifier
                    .padding(
                        start = 20.dp
                    )
                    .align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = data.title, style = typo.titleSmall, maxLines = 1, color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                        .focusable()
                        .padding(
                            bottom = 3.dp
                        )
                )

                Text(
                    text = data.artists.toListName().connectArtists(),
                    style = typo.bodySmall,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                        .focusable()
                )
            }
        }
    }
}

@Composable
fun MoodAndGenresContentItem(data: Any?, navController: NavController) {

    Column(
        modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically, unbounded = true)
    ) {
        Text(
            text = when (data) {
                is ItemsPlaylist -> (data).header
                is Item -> (data).header
                else -> ""
            },
            style = typo.titleLarge,
            color = Color.White,
            modifier = Modifier
                .padding(
                    vertical = 13.dp,
                    horizontal = 15.dp
                )
                .fillMaxWidth()
        )
        LazyRow(
            modifier = Modifier.padding(
                15.dp
            )
        ) {
            val itemList = when (data) {
                is ItemsPlaylist -> (data).contents
                is Item -> (data).contents
                else -> listOf()
            }
            items(itemList) { item ->
                HomeItemContentPlaylist(onClick = {
                    navController.navigateSafe(R.id.action_global_playlistFragment, Bundle().apply {
                        putString(
                            "id",
                            if (item is com.universe.audioflare.data.model.explore.mood.genre.Content) item.playlistBrowseId else (item as com.universe.audioflare.data.model.explore.mood.moodmoments.Content).playlistBrowseId
                        )
                    })
                }, data = item)
            }
        }
    }
}
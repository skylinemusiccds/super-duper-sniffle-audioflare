package com.universe.audioflare.ui.screen.home

import android.content.Context
import android.os.Bundle
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.wear.compose.material3.ripple
import com.universe.audioflare.R
import com.universe.audioflare.data.db.entities.NotificationEntity
import com.universe.audioflare.extension.formatTimeAgo
import com.universe.audioflare.extension.navigateSafe
import com.universe.audioflare.ui.component.CenterLoadingBox
import com.universe.audioflare.ui.component.EndOfPage
import com.universe.audioflare.ui.component.RippleIconButton
import com.universe.audioflare.ui.theme.typo
import com.universe.audioflare.viewModel.NotificationViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.placeholder.placeholder.PlaceholderPlugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = viewModel(),
) {
    val context = LocalContext.current
    val listNotification by viewModel.listNotification.collectAsState()
    Column {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.notification),
                    style = typo.titleMedium,
                )
            },
            navigationIcon = {
                RippleIconButton(resId = R.drawable.baseline_arrow_back_ios_new_24) {
                    navController.popBackStack()
                }
            },
        )
        Crossfade(targetState = listNotification) {
            if (it == null) {
                Box(
                    Modifier.fillMaxSize(),
                ) {
                    CenterLoadingBox(modifier = Modifier.align(Alignment.Center))
                }
            } else if (it.isNotEmpty()) {
                LazyColumn(modifier = Modifier.padding(15.dp)) {
                    items(it) { notification ->
                        NotificationItem(
                            notification = notification,
                            context = context,
                            navController,
                        )
                    }
                    item {
                        EndOfPage()
                    }
                }
            } else {
                Box(
                    Modifier.fillMaxSize(),
                ) {
                    Text(
                        text = stringResource(id = R.string.no_notification),
                        style = typo.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: NotificationEntity,
    context: Context,
    navController: NavController,
) {
    Box(
        modifier =
            Modifier
                .padding(5.dp)
                .fillMaxWidth(),
    ) {
        Column {
            Row(
                Modifier.clickable(
                    onClick = {
                        navController.navigateSafe(
                            R.id.action_global_artistFragment,
                            Bundle().apply {
                                putString("channelId", notification.channelId)
                            },
                        )
                    },
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },
                    indication = ripple(),
                ),
            ) {
                CoilImage(
                    imageModel = {
                        notification.thumbnail
                    }, // loading a network image or local resource using an URL.
                    imageOptions =
                        ImageOptions(
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                        ),
                    previewPlaceholder = painterResource(id = R.drawable.holder),
                    component =
                        rememberImageComponent {
                            +CrossfadePlugin(
                                duration = 550,
                            )
                            +PlaceholderPlugin.Loading(painterResource(id = R.drawable.holder))
                            +PlaceholderPlugin.Failure(painterResource(id = R.drawable.holder))
                        },
                    modifier =
                        Modifier
                            .align(Alignment.CenterVertically)
                            .size(50.dp)
                            .clip(
                                CircleShape,
                            ),
                )
                Spacer(modifier = Modifier.padding(5.dp))
                Column {
                    Text(text = stringResource(id = R.string.new_release), style = typo.titleSmall)
                    Spacer(modifier = Modifier.padding(3.dp))
                    Text(text = notification.name, style = typo.headlineMedium)
                }
            }
            LazyRow(
                Modifier.padding(top = 15.dp),
            ) {
                items(notification.single) { single ->
                    ItemAlbumNotification(
                        isAlbum = false,
                        browseId = single["browseId"] ?: "",
                        title = single["title"] ?: "",
                        thumbnail = single["thumbnails"],
                        navController,
                    )
                }
                items(notification.album) { album ->
                    ItemAlbumNotification(
                        isAlbum = true,
                        browseId = album["browseId"] ?: "",
                        title = album["title"] ?: "",
                        thumbnail = album["thumbnails"],
                        navController = navController,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        Text(
            text = notification.time.formatTimeAgo(context),
            style = typo.titleSmall,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 15.dp),
        )
    }
}

@Composable
fun ItemAlbumNotification(
    isAlbum: Boolean,
    browseId: String,
    title: String,
    thumbnail: String?,
    navController: NavController,
) {
    Box(
        modifier =
            Modifier
                .clickable(
                    onClick = {
                        navController.navigateSafe(
                            R.id.action_global_albumFragment,
                            Bundle().apply {
                                putString("browseId", browseId)
                            },
                        )
                    },
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },
                    indication = ripple(),
                ),
    ) {
        Column(
            Modifier.padding(5.dp),
        ) {
            CoilImage(
                imageModel = {
                    thumbnail
                }, // loading a network image or local resource using an URL.
                imageOptions =
                    ImageOptions(
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                    ),
                previewPlaceholder = painterResource(id = R.drawable.holder),
                component =
                    rememberImageComponent {
                        +CrossfadePlugin(
                            duration = 550,
                        )
                        +PlaceholderPlugin.Loading(painterResource(id = R.drawable.holder))
                        +PlaceholderPlugin.Failure(painterResource(id = R.drawable.holder))
                    },
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(150.dp)
                        .clip(
                            RoundedCornerShape(10),
                        ),
            )
            Text(
                text = title,
                style = typo.titleSmall,
                color = Color.White,
                maxLines = 1,
                modifier =
                    Modifier
                        .width(150.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .padding(top = 10.dp)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                        .focusable(),
            )
            Text(
                text = if (isAlbum) stringResource(id = R.string.album) else stringResource(id = R.string.singles),
                style = typo.bodySmall,
                maxLines = 1,
                modifier =
                    Modifier
                        .width(150.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .padding(top = 10.dp)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                        .focusable(),
            )
        }
    }
}
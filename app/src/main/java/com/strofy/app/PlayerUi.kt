package com.strofy.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player

@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val t = Playback.current ?: return
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clip(RoundedCornerShape(28.dp)).clickable(onClick = onOpen),
        shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column {
            Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Thumb(t.thumb, 44.dp, 10.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (Playback.buffering) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                IconButton(onClick = { Playback.toggle() }) {
                    Icon(if (Playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Ltr {
                val frac = if (Playback.duration > 0) (Playback.position.toFloat() / Playback.duration).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth().height(2.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayer(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val t = Playback.current
    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf<Float?>(null) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(32.dp)) }
                Text("قيد التشغيل", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { showQueue = true }) { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) }
            }
            if (t == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("لا يوجد مقطع قيد التشغيل") }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (showLyrics) LyricsPane(t)
                    else Thumb(t.thumb, androidx.compose.ui.unit.Dp.Unspecified, 20.dp, Modifier.fillMaxWidth().aspectRatio(1f).padding(vertical = 8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(t.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    val liked = Store.isLiked(t)
                    IconButton(onClick = { Store.toggleLike(t) }) {
                        Icon(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
                Ltr {
                    Column {
                        val frac = if (Playback.duration > 0) Playback.position.toFloat() / Playback.duration else 0f
                        Slider(
                            value = drag ?: frac.coerceIn(0f, 1f),
                            onValueChange = { drag = it },
                            onValueChangeFinished = { drag?.let { Playback.seek((it * Playback.duration).toLong()) }; drag = null }
                        )
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            val shown = drag?.let { (it * Playback.duration).toLong() } ?: Playback.position
                            Text(fmt(shown / 1000).ifEmpty { "0:00" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(fmt(Playback.duration / 1000).ifEmpty { "0:00" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { Playback.toggleShuffle() }) {
                                Icon(Icons.Filled.Shuffle, null, tint = if (Playback.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { Playback.previous() }) { Icon(Icons.Filled.SkipPrevious, null, Modifier.size(38.dp)) }
                            FilledIconButton(onClick = { Playback.toggle() }, modifier = Modifier.size(68.dp)) {
                                if (Playback.buffering) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else Icon(if (Playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, Modifier.size(38.dp))
                            }
                            IconButton(onClick = { Playback.next() }) { Icon(Icons.Filled.SkipNext, null, Modifier.size(38.dp)) }
                            IconButton(onClick = { Playback.cycleRepeat() }) {
                                Icon(
                                    if (Playback.repeat == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat, null,
                                    tint = if (Playback.repeat != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = { showLyrics = !showLyrics }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (showLyrics) "عرض الغلاف" else "عرض الكلمات")
                }
            }
        }
    }

    if (showQueue) ModalBottomSheet(onDismissRequest = { showQueue = false }) {
        Text("قائمة التشغيل", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        if (Playback.queue.isEmpty()) EmptyState("قائمة التشغيل فارغة")
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(Playback.queue.toList()) { i, q ->
                Row(
                    Modifier.fillMaxWidth().clickable { Playback.jumpTo(i); showQueue = false }.padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Thumb(q.thumb, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(q.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (q.id == Playback.current?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Text(q.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsPane(t: Track) {
    var lines by remember(t.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var state by remember(t.id) { mutableIntStateOf(0) } // 0 loading, 1 found, 2 none
    LaunchedEffect(t.id) {
        state = 0
        val r = try { Lyrics.fetch(t) } catch (e: Exception) { emptyList() }
        lines = r
        state = if (r.isEmpty()) 2 else 1
    }
    when (state) {
        0 -> CircularProgressIndicator()
        2 -> Text("لم يتم العثور على الكلمات", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> {
            val synced = (lines.firstOrNull()?.t ?: -1L) >= 0
            val idx = if (synced) lines.indexOfLast { it.t <= Playback.position } else -1
            val listState = rememberLazyListState()
            LaunchedEffect(idx) { if (idx >= 0) listState.animateScrollToItem((idx - 2).coerceAtLeast(0)) }
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(vertical = 60.dp)) {
                itemsIndexed(lines) { i, l ->
                    Text(
                        l.text,
                        fontSize = Store.lyricsSize.sp, fontWeight = FontWeight.Bold,
                        color = if (!synced || i == idx) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth().clickable(enabled = synced) { Playback.seek(l.t) }.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

package com.strofy.app

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

val Accents = listOf(
    "أزرق" to Color(0xFF007AFF), "أحمر" to Color(0xFFE83838), "برتقالي" to Color(0xFFF27321),
    "كهرماني" to Color(0xFFFAB326), "أخضر" to Color(0xFF2EB852), "تركوازي" to Color(0xFF1FA699),
    "بنفسجي" to Color(0xFF8E5CF6), "وردي" to Color(0xFFEC4899)
)

@Composable
fun StrofyTheme(content: @Composable () -> Unit) {
    val dark = when (Store.themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val accent = Accents[Store.accent.coerceIn(0, Accents.lastIndex)].second
    val scheme = if (dark) darkColorScheme(
        primary = accent, onPrimary = Color.White, background = Color.Black, onBackground = Color.White,
        surface = Color(0xFF1C1C1E), onSurface = Color.White,
        surfaceVariant = Color(0xFF2C2C2E), onSurfaceVariant = Color(0xFF9A9AA0)
    ) else lightColorScheme(
        primary = accent, onPrimary = Color.White, background = Color(0xFFF2F2F7), onBackground = Color.Black,
        surface = Color.White, onSurface = Color.Black,
        surfaceVariant = Color(0xFFE5E5EA), onSurfaceVariant = Color(0xFF6A6A70)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Forces left-to-right for media controls / progress bars. */
@Composable
fun Ltr(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}

@Composable
fun ScreenHeader(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun AccountAvatar(onClick: () -> Unit) {
    Image(
        painterResource(R.drawable.app_logo), null,
        Modifier.padding(horizontal = 8.dp).size(34.dp).clip(RoundedCornerShape(9.dp)).clickable(onClick = onClick)
    )
}

@Composable
fun Thumb(url: String, size: Dp, radius: Dp = 8.dp, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url.ifBlank { null }, contentDescription = null, contentScale = ContentScale.Crop,
        modifier = (if (size == Dp.Unspecified) modifier else modifier.size(size)).clip(RoundedCornerShape(radius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp))
}

@Composable
fun ChipRow(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(items) { i, label ->
            FilterChip(selected = selected == i, onClick = { onSelect(i) }, label = { Text(label) })
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String = "") {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LoadingBox() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorBox(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("تعذّر التحميل. تحقق من الإنترنت وحاول مجدداً.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) { Text("إعادة المحاولة") }
    }
}

@Composable
fun TrackRow(t: Track, list: List<Track>, index: Int, modifier: Modifier = Modifier, playlistIndex: Int = -1) {
    var menu by remember { mutableStateOf(false) }
    val playing = Playback.current?.id == t.id
    Row(
        modifier.fillMaxWidth().clickable { Playback.play(list, index) }.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumb(t.thumb, 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium,
                color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            val sub = listOf(t.artist, fmt(t.duration)).filter { it.isNotBlank() }.joinToString(" • ")
            Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val p = Downloads.progress[t.id]
        if (p != null) CircularProgressIndicator(progress = { p }, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            TrackMenu(t, menu, playlistIndex) { menu = false }
        }
    }
}

@Composable
fun TrackCard(t: Track, list: List<Track>, index: Int) {
    Column(Modifier.width(140.dp).clickable { Playback.play(list, index) }) {
        Thumb(t.thumb, 140.dp, 12.dp)
        Spacer(Modifier.height(6.dp))
        Text(t.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TrackMenu(t: Track, expanded: Boolean, playlistIndex: Int = -1, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var addDialog by remember { mutableStateOf(false) }
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        val liked = Store.isLiked(t)
        MenuItem(if (liked) "إلغاء الإعجاب" else "إعجاب", if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder) {
            Store.toggleLike(t); onDismiss()
        }
        MenuItem("التشغيل التالي", Icons.Filled.SkipNext) { Playback.playNext(t); onDismiss() }
        MenuItem("إضافة إلى قائمة تشغيل", Icons.Filled.PlaylistAdd) { addDialog = true; onDismiss() }
        if (Store.isDownloaded(t.id)) {
            MenuItem("حذف التحميل", Icons.Filled.Delete) { Store.removeDownload(t); onDismiss() }
        } else {
            MenuItem("تحميل", Icons.Filled.Download) { Downloads.start(t); onDismiss() }
        }
        if (playlistIndex >= 0) {
            MenuItem("إزالة من القائمة", Icons.Filled.Delete) { Store.removeFromPlaylist(playlistIndex, t); onDismiss() }
        }
        MenuItem("مشاركة", Icons.Filled.Share) {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "${t.title} - ${t.artist}\n${t.url}")
            ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            onDismiss()
        }
    }
    if (addDialog) AddToPlaylistDialog(t) { addDialog = false }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text) }, leadingIcon = { Icon(icon, null) }, onClick = onClick)
}

@Composable
fun AddToPlaylistDialog(t: Track, onDismiss: () -> Unit) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة إلى قائمة تشغيل") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (creating) {
                    OutlinedTextField(name, { name = it }, label = { Text("اسم القائمة") }, singleLine = true)
                } else {
                    Store.playlists.forEachIndexed { i, p ->
                        TextButton(onClick = { Store.addToPlaylist(i, t); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                            Text(p.name, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                        }
                    }
                    TextButton(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("+ قائمة جديدة", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                }
            }
        },
        confirmButton = {
            if (creating) TextButton(onClick = {
                if (name.isNotBlank()) {
                    Store.createPlaylist(name.trim())
                    Store.addToPlaylist(Store.playlists.lastIndex, t)
                    onDismiss()
                }
            }) { Text("إنشاء") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

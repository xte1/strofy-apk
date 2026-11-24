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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** A generic page that shows a list of tracks. */
class Detail(val title: String, val playlistIndex: Int = -1, val tracks: () -> List<Track>)

// ---------------------------------------------------------------- Home feed

object Feed {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    class Section(val title: String, val query: String) {
        var items by mutableStateOf<List<Track>>(emptyList())
        var loading by mutableStateOf(false)
        var failed by mutableStateOf(false)
        fun load(force: Boolean = false) {
            if (loading || (items.isNotEmpty() && !force)) return
            loading = true; failed = false
            scope.launch {
                try { items = Yt.search(query) } catch (e: Exception) { failed = true }
                loading = false
            }
        }
    }

    val quick = Section("", "اغاني عراقية جديدة")
    val sections = listOf(
        Section("أغاني عراقية رائجة", "اغاني عراقية 2026"),
        Section("أحدث الأغاني العربية", "اغاني عربية جديدة"),
        Section("خليجي", "اغاني خليجية"),
        Section("مصري", "اغاني مصرية"),
        Section("لبناني", "اغاني لبنانية"),
        Section("عالمي رائج", "top hits 2026")
    )
    val moods = listOf(
        "استرخاء" to "اغاني هادئة للاسترخاء", "تمرين" to "workout music", "نشاط" to "اغاني حماسية",
        "مزاج جيد" to "اغاني فرح ومزاج", "حزين" to "اغاني حزينة", "رومانسي" to "اغاني رومانسية",
        "سفر" to "اغاني للسفر", "تركيز" to "موسيقى للتركيز"
    ).map { Section(it.first, it.second) }
    var chip by mutableIntStateOf(-1)

    fun loadAll() { quick.load(); sections.forEach { it.load() } }
    fun refresh() { quick.load(true); sections.forEach { it.load(true) } }
}

@Composable
private fun TrackSectionRow(s: Feed.Section) {
    SectionTitle(s.title)
    when {
        s.items.isNotEmpty() -> LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(s.items) { i, t -> TrackCard(t, s.items, i) }
        }
        s.failed -> ErrorBox { s.load(true) }
        else -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
    }
}

@Composable
fun HomeScreen(onAccount: () -> Unit, openDetail: (Detail) -> Unit) {
    LaunchedEffect(Unit) { Feed.loadAll() }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            ScreenHeader("الرئيسية") {
                IconButton(onClick = { openDetail(Detail("سجل الاستماع") { Store.history.toList() }) }) {
                    Icon(Icons.Filled.History, null)
                }
                AccountAvatar(onAccount)
            }
        }
        item {
            ChipRow(Feed.moods.map { it.title }, Feed.chip) { i ->
                Feed.chip = if (Feed.chip == i) -1 else i
                if (Feed.chip >= 0) Feed.moods[Feed.chip].load()
            }
        }
        if (Feed.chip >= 0) {
            item { TrackSectionRow(Feed.moods[Feed.chip]) }
            val s = Feed.moods[Feed.chip]
            itemsIndexed(s.items) { i, t -> TrackRow(t, s.items, i) }
        } else {
            if (Store.showQuick) {
                item { SectionTitle("اختيارات سريعة") }
                val picks = (Store.history.take(6) + Feed.quick.items).distinctBy { it.id }.take(12)
                if (picks.isEmpty()) {
                    item {
                        if (Feed.quick.failed) ErrorBox { Feed.quick.load(true) }
                        else LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    }
                } else {
                    val rows = picks.chunked(2)
                    itemsIndexed(rows) { r, pair ->
                        Row(Modifier.fillMaxWidth()) {
                            pair.forEachIndexed { c, t -> TrackRow(t, picks, r * 2 + c, Modifier.weight(1f)) }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Feed.sections.forEach { s -> item { TrackSectionRow(s) } }
        }
    }
}

// ---------------------------------------------------------------- Search

object SearchState {
    var query by mutableStateOf("")
    var submitted by mutableStateOf("")
    var results by mutableStateOf<List<Track>>(emptyList())
    var loading by mutableStateOf(false)
    var failed by mutableStateOf(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun submit(q: String) {
        val text = q.trim()
        if (text.isEmpty()) return
        query = text; submitted = text; loading = true; failed = false
        Store.addRecent(text)
        scope.launch {
            try { results = Yt.search(text) } catch (e: Exception) { results = emptyList(); failed = true }
            loading = false
        }
    }
}

@Composable
fun SearchScreen() {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("بحث")
        OutlinedTextField(
            value = SearchState.query, onValueChange = { SearchState.query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("ابحث عن أغنية أو فنان") }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (SearchState.query.isNotEmpty()) IconButton(onClick = { SearchState.query = ""; SearchState.submitted = ""; SearchState.results = emptyList() }) {
                    Icon(Icons.Filled.Close, null)
                }
            },
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { SearchState.submit(SearchState.query) })
        )
        Spacer(Modifier.height(8.dp))
        when {
            SearchState.loading -> LoadingBox()
            SearchState.failed -> ErrorBox { SearchState.submit(SearchState.submitted) }
            SearchState.submitted.isEmpty() -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                if (Store.recent.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("عمليات البحث الأخيرة", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { Store.clearRecent() }) { Text("مسح") }
                        }
                    }
                    itemsIndexed(Store.recent.toList()) { _, q ->
                        Row(Modifier.fillMaxWidth().clickable { SearchState.submit(q) }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp)); Text(q)
                        }
                    }
                } else item { EmptyState("ابحث عن موسيقاك", "ستظهر هنا عمليات بحثك الأخيرة.") }
            }
            SearchState.results.isEmpty() -> EmptyState("لا توجد نتائج", "لم يتم العثور على نتائج لـ \"${SearchState.submitted}\"")
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                itemsIndexed(SearchState.results) { i, t -> TrackRow(t, SearchState.results, i) }
            }
        }
    }
}

// ---------------------------------------------------------------- Library

@Composable
private fun LibraryCard(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.55f)))), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LibraryScreen(onAccount: () -> Unit, openDetail: (Detail) -> Unit) {
    var filter by remember { mutableIntStateOf(0) }
    var newDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var deleteIndex by remember { mutableIntStateOf(-1) }

    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            ScreenHeader("المكتبة") {
                IconButton(onClick = { newDialog = true }) { Icon(Icons.Filled.Add, null) }
                AccountAvatar(onAccount)
            }
        }
        item { ChipRow(listOf("قوائم التشغيل", "الألبومات", "الفنانون", "البودكاست"), filter) { filter = it } }
        item { Spacer(Modifier.height(8.dp)) }
        when (filter) {
            0 -> {
                item {
                    LibraryCard(Icons.Filled.Favorite, "الأغاني المفضلة", "${Store.liked.size} أغنية", Color(0xFFE83838)) {
                        openDetail(Detail("الأغاني المفضلة") { Store.liked.toList() })
                    }
                }
                item {
                    LibraryCard(Icons.Filled.Download, "التنزيلات", if (Store.downloads.isEmpty()) "بدون إنترنت" else "${Store.downloads.size} أغنية", Color(0xFF2EB852)) {
                        openDetail(Detail("التنزيلات") { Store.downloads.toList() })
                    }
                }
                item {
                    LibraryCard(Icons.Filled.Star, "أفضل 100 لي", "الأكثر استماعاً", Color(0xFFF27321)) {
                        openDetail(Detail("أفضل 100 لي") { Store.top100() })
                    }
                }
                itemsIndexed(Store.playlists.toList()) { i, p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            LibraryCard(Icons.Filled.MusicNote, p.name, "${p.tracks.size} أغنية", Color(0xFF8E5CF6)) {
                                openDetail(Detail(p.name, i) { Store.playlists.getOrNull(i)?.tracks ?: emptyList() })
                            }
                        }
                        IconButton(onClick = { deleteIndex = i }) { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            2 -> {
                val artists = (Store.liked + Store.history + Store.downloads).filter { it.artist.isNotBlank() }.groupBy { it.artist }
                if (artists.isEmpty()) item { EmptyState("لا يوجد فنانون بعد", "ستظهر هنا أسماء الفنانين الذين تستمع إليهم.") }
                itemsIndexed(artists.keys.toList()) { _, a ->
                    LibraryCard(Icons.Filled.MusicNote, a, "${artists[a]?.distinctBy { it.id }?.size ?: 0} أغنية", Color(0xFF1FA699)) {
                        openDetail(Detail(a) { artists[a]?.distinctBy { it.id } ?: emptyList() })
                    }
                }
            }
            else -> item { EmptyState(if (filter == 1) "لا توجد ألبومات" else "لا يوجد بودكاست", "ستظهر هنا عناصرك المحفوظة.") }
        }
    }

    if (newDialog) AlertDialog(
        onDismissRequest = { newDialog = false },
        title = { Text("قائمة تشغيل جديدة") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("اسم القائمة") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { Store.createPlaylist(name.trim()); name = ""; newDialog = false } }) { Text("إنشاء") } },
        dismissButton = { TextButton(onClick = { newDialog = false }) { Text("إلغاء") } }
    )
    if (deleteIndex >= 0) AlertDialog(
        onDismissRequest = { deleteIndex = -1 },
        title = { Text("حذف القائمة") },
        text = { Text("هل أنت متأكد أنك تريد حذف \"${Store.playlists.getOrNull(deleteIndex)?.name ?: ""}\"؟") },
        confirmButton = { TextButton(onClick = { Store.deletePlaylist(deleteIndex); deleteIndex = -1 }) { Text("حذف") } },
        dismissButton = { TextButton(onClick = { deleteIndex = -1 }) { Text("إلغاء") } }
    )
}

// ---------------------------------------------------------------- Explore

private val Genres = listOf(
    Triple("عراقي", "اغاني عراقية", Color(0xFF0E7C3A)), Triple("خليجي", "اغاني خليجية", Color(0xFF1F6FEB)),
    Triple("مصري", "اغاني مصرية", Color(0xFFC0392B)), Triple("لبناني", "اغاني لبنانية", Color(0xFF8E44AD)),
    Triple("مغربي", "اغاني مغربية", Color(0xFFD35400)), Triple("شعبي", "اغاني شعبية", Color(0xFF16A085)),
    Triple("هيب هوب", "arabic hip hop", Color(0xFF2C3E50)), Triple("بوب", "pop hits", Color(0xFFE84393)),
    Triple("روك", "rock classics", Color(0xFF7F8C8D)), Triple("إلكترونيك", "electronic music", Color(0xFF00A8CC)),
    Triple("كلاسيكي", "اغاني طرب قديمة", Color(0xFF9C6B30)), Triple("جاز", "jazz", Color(0xFF34495E)),
    Triple("قرآن وأناشيد", "اناشيد", Color(0xFF27AE60)), Triple("أغاني أطفال", "اغاني اطفال", Color(0xFFF39C12))
)

@Composable
fun ExploreScreen(openDetail: (Detail) -> Unit) {
    val scope = rememberCoroutineScope()
    var loadingGenre by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("استكشف")
        if (loadingGenre.isNotEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        if (error) Text("تعذّر تحميل التصنيف. حاول مجدداً.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(Genres.chunked(2)) { _, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { (title, query, color) ->
                        Box(
                            Modifier.weight(1f).height(88.dp).clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.6f))))
                                .clickable {
                                    error = false; loadingGenre = title
                                    scope.launch {
                                        try {
                                            val r = Yt.search(query)
                                            openDetail(Detail(title) { r })
                                        } catch (e: Exception) { error = true }
                                        loadingGenre = ""
                                    }
                                }.padding(14.dp),
                            contentAlignment = Alignment.BottomStart
                        ) { Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Detail

@Composable
fun DetailScreen(d: Detail, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val tracks = d.tracks()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text(d.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        if (tracks.isEmpty()) {
            EmptyState("لا توجد أغانٍ هنا بعد", "ستظهر الأغاني هنا عند إضافتها.")
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { Playback.play(tracks, 0) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("تشغيل الكل")
                }
                OutlinedButton(onClick = { Playback.playShuffled(tracks) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Shuffle, null); Spacer(Modifier.width(6.dp)); Text("عشوائي")
                }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                itemsIndexed(tracks) { i, t -> TrackRow(t, tracks, i, playlistIndex = d.playlistIndex) }
            }
        }
    }
}

package com.strofy.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(onDismiss: () -> Unit, onSettings: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painterResource(R.drawable.app_logo), null, Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)))
                Spacer(Modifier.width(14.dp))
                Text("STROFY", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSettings() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Settings, null)
                Spacer(Modifier.width(12.dp))
                Text("الإعدادات", Modifier.weight(1f), fontSize = 17.sp)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SettingsRoot(onClose: () -> Unit) {
    var page by remember { mutableStateOf("root") }
    BackHandler { if (page == "root") onClose() else page = "root" }
    val title = when (page) {
        "appearance" -> "المظهر"; "player" -> "المشغّل"; "downloads" -> "التنزيلات"
        "content" -> "المحتوى"; "about" -> "حول التطبيق"; else -> "الإعدادات"
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (page == "root") onClose() else page = "root" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                Text(title, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.width(48.dp))
            }
            when (page) {
                "appearance" -> AppearancePage()
                "player" -> PlayerPage()
                "downloads" -> DownloadsPage()
                "content" -> ContentPage()
                "about" -> AboutPage()
                else -> Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NavRow("المظهر", "السمة ولون التمييز") { page = "appearance" }
                    NavRow("المشغّل", "جودة الصوت والكلمات") { page = "player" }
                    NavRow("التنزيلات", "الاستماع بدون إنترنت") { page = "downloads" }
                    NavRow("المحتوى", "الخصوصية والسجل") { page = "content" }
                    NavRow("حول التطبيق", "STROFY") { page = "about" }
                }
            }
        }
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

@Composable
private fun OptionRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text(label, color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
private fun AppearancePage() {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            Text("السمة", fontWeight = FontWeight.SemiBold)
            OptionRow(listOf("النظام", "فاتح", "داكن"), Store.themeMode) { Store.setTheme(it) }
            Text("اختر كيف يتبع التطبيق مظهر جهازك.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card {
            Text("لون التمييز", fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Accents.forEachIndexed { i, (_, color) ->
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(color).clickable { Store.setAccent(i) },
                        contentAlignment = Alignment.Center
                    ) { if (Store.accent == i) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
            }
            Text("يُطبَّق على الأزرار والتحديدات والعناصر المختارة.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlayerPage() {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            Text("جودة البث", fontWeight = FontWeight.SemiBold)
            OptionRow(listOf("عالية", "متوسطة", "منخفضة"), Store.quality) { Store.setQuality(it) }
            Text("تُطبَّق على المقاطع التي تبدأ بعد التغيير.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card {
            Text("حجم خط الكلمات: ${Store.lyricsSize}", fontWeight = FontWeight.SemiBold)
            Slider(Store.lyricsSize.toFloat(), { Store.setLyricsSize(it.toInt()) }, valueRange = 16f..34f)
        }
    }
}

@Composable
private fun DownloadsPage() {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            Text("${Store.downloads.size} مقطع محمّل", fontWeight = FontWeight.SemiBold)
            Text("احفظ الأغاني للاستماع بدون إنترنت من قائمة الخيارات بجانب كل أغنية.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { Store.clearDownloads() }, enabled = Store.downloads.isNotEmpty()) { Text("حذف جميع التنزيلات") }
            Text("سيؤدي هذا إلى حذف جميع المقاطع المحمّلة من جهازك نهائياً.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ContentPage() {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card {
            Text("الصفحة الرئيسية", fontWeight = FontWeight.SemiBold)
            SwitchRow("إظهار الاختيارات السريعة", Store.showQuick) { Store.setShowQuick(it) }
        }
        Card {
            Text("الخصوصية", fontWeight = FontWeight.SemiBold)
            SwitchRow("حفظ سجل البحث", Store.keepSearch) { Store.setKeepSearch(it) }
            SwitchRow("حفظ سجل التشغيل", Store.keepHistory) { Store.setKeepHistory(it) }
            Text("عند الإيقاف لا يتم تسجيل عمليات البحث والتشغيل على الجهاز.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { Store.clearRecent() }) { Text("مسح البحث") }
                OutlinedButton(onClick = { Store.clearHistory() }) { Text("مسح السجل") }
            }
        }
    }
}

@Composable
private fun AboutPage() {
    val uri = LocalUriHandler.current
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.app_logo), null, Modifier.size(100.dp).shadow(10.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)))
                Spacer(Modifier.height(12.dp))
                Text("STROFY", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            Text("لمحة عن التطبيق", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
            Card {
                Text(
                    "STROFY رفيقك الموسيقي الأنيق. استمع إلى ملايين الأغاني والألبومات وقوائم التشغيل بجودة عالية وتجربة سلسة بالكامل بالعربية، مع كلمات الأغاني المتزامنة، والتحميل للاستماع بدون إنترنت، وتوصيات تناسب ذوقك. صُمّم ليمنحك عالماً من الموسيقى بلمسة واحدة، وبواجهة بسيطة وجميلة تحبّها.",
                    lineHeight = 24.sp
                )
            }
            Text("معلومات المطوّر", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
            Card {
                SocialRow("انستجرام", "eng_azawy") { uri.openUri("https://instagram.com/eng_azawy") }
                HorizontalDivider()
                SocialRow("تليجرام", "bavarite") { uri.openUri("https://t.me/bavarite") }
                HorizontalDivider()
                SocialRow("تيك توك", "eyesiq") { uri.openUri("https://www.tiktok.com/@eyesiq") }
            }
        }
        Text(
            "صنع في العراق 🇮🇶", Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SocialRow(platform: String, handle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(platform, Modifier.weight(1f), fontSize = 16.sp)
        CompositionLocalProviderLtrText("@$handle")
    }
}

@Composable
private fun CompositionLocalProviderLtrText(text: String) {
    Ltr { Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp) }
}

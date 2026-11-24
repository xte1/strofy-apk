package com.strofy.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun App() {
    var tab by remember { mutableIntStateOf(0) }          // 0 home, 1 library, 2 explore, 3 search
    var showPlayer by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Detail?>(null) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        0 -> HomeScreen({ showAccount = true }) { detail = it }
                        1 -> LibraryScreen({ showAccount = true }) { detail = it }
                        2 -> ExploreScreen { detail = it }
                        else -> SearchScreen()
                    }
                    detail?.let { d -> DetailScreen(d) { detail = null } }
                }
                MiniPlayer { showPlayer = true }
                BottomBar(tab) { tab = it; detail = null }
            }

            AnimatedVisibility(
                visible = showPlayer,
                enter = slideInVertically { it }, exit = slideOutVertically { it }
            ) { FullPlayer { showPlayer = false } }

            if (showSettings) SettingsRoot { showSettings = false }
            if (showAccount) AccountSheet(
                onDismiss = { showAccount = false },
                onSettings = { showAccount = false; showSettings = true }
            )
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(Modifier.weight(1f), shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.padding(6.dp)) {
                NavItem("الرئيسية", Icons.Filled.Home, selected == 0, Modifier.weight(1f)) { onSelect(0) }
                NavItem("المكتبة", Icons.Filled.LibraryMusic, selected == 1, Modifier.weight(1f)) { onSelect(1) }
                NavItem("استكشف", Icons.Filled.Whatshot, selected == 2, Modifier.weight(1f)) { onSelect(2) }
            }
        }
        Box(
            Modifier.padding(start = 10.dp).size(60.dp).clip(CircleShape)
                .background(if (selected == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                .clickable { onSelect(3) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Search, null, tint = if (selected == 3) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier.clip(RoundedCornerShape(26.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = tint)
        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

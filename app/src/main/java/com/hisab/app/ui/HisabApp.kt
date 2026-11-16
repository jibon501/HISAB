package com.hisab.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisab.app.HisabViewModel
import kotlinx.coroutines.launch

private data class NavTab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    NavTab("ড্যাশবোর্ড", Icons.Default.Dashboard),
    NavTab("ধার", Icons.Default.SwapVert), // ভেতরে দিছি / নিছি বাছাই
    NavTab("লগবুক", Icons.Default.Receipt),
    NavTab("নোট", Icons.Default.Description),
    NavTab("অ্যাকাউন্ট", Icons.Default.AccountBalanceWallet),
)

private const val LOGBOOK_PAGE = 2

/**
 * অ্যাপের মূল কাঠামো: উপরে লোগো, ক্যাশ ব্যালেন্স ও "নতুন লেনদেন" বাটন; নিচে ৫টি ট্যাব।
 * স্ক্রিন ধরে ডানে-বামে টানলেও ট্যাব বদলায়
 */
@Composable
fun HisabApp(vm: HisabViewModel = viewModel()) {
    val pager = rememberPagerState(pageCount = { TABS.size })
    val scope = rememberCoroutineScope()
    // উপরের "নতুন লেনদেন" বাটন চাপলে বাড়ে; লগবুক এটা দেখে নতুন লেনদেনের ফর্ম খোলে
    var addTick by remember { mutableIntStateOf(0) }

    // ব্যাক চাপলে আগে ড্যাশবোর্ডে ফিরবে, ড্যাশবোর্ডে থাকলে তবেই অ্যাপ বন্ধ হবে
    BackHandler(enabled = pager.currentPage != 0) {
        scope.launch { pager.animateScrollToPage(0) }
    }

    Scaffold(
        containerColor = Slate50,
        // ফোনের স্ট্যাটাস বারের জায়গা সিস্টেম নিজেই ছাড়ে; এখানে আবার যোগ করলে উপরে দ্বিগুণ ফাঁকা হয়
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            HisabTopBar(cash = tk(vm.summary.cash)) {
                addTick++
                scope.launch { pager.animateScrollToPage(LOGBOOK_PAGE) }
            }
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 6.dp) {
                Column {
                    HorizontalDivider(color = Slate200)
                    NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                        TABS.forEachIndexed { i, t ->
                            val selected = pager.currentPage == i
                            NavigationBarItem(
                                selected = selected,
                                onClick = { scope.launch { pager.animateScrollToPage(i) } },
                                icon = { Icon(t.icon, contentDescription = t.label, modifier = Modifier.size(20.dp)) },
                                label = {
                                    Text(
                                        t.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Emerald700,
                                    selectedTextColor = Emerald700,
                                    indicatorColor = Emerald50,
                                    unselectedIconColor = Slate500,
                                    unselectedTextColor = Slate500,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { pad ->
        // সব পেজ সবসময় আঁকা থাকে, তাই ট্যাব বদলালে ড্যাশবোর্ড নতুন করে আঁকা হয়ে লাফায় না
        HorizontalPager(
            state = pager,
            modifier = Modifier.padding(pad),
            beyondViewportPageCount = TABS.size,
            // পেজ ছোট হলে পেজার ডিফল্টভাবে মাঝখানে বসায়; তাই উপরে বসাতে হয়
            verticalAlignment = Alignment.Top,
        ) { page ->
            Box(Modifier.fillMaxSize()) {
                when (page) {
                    0 -> DashboardScreen(vm)
                    1 -> DebtsScreen(vm)
                    2 -> TransactionsScreen(vm, addTick)
                    3 -> NotesScreen(vm)
                    else -> AccountsScreen(vm)
                }
            }
        }
    }
}

/** উপরের বার: পান্না সবুজ লোগো "হি", নাম, ক্যাশ ব্যালেন্স ও "নতুন লেনদেন" বাটন */
@Composable
private fun HisabTopBar(cash: String, onAdd: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 2.dp) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Emerald600),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("হি", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("হিসাব", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)
                            Surface(shape = RoundedCornerShape(4.dp), color = Emerald100) {
                                Text(
                                    "HISAB",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald800,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                )
                            }
                        }
                        Text("ক্যাশ ব্যালেন্স: $cash", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Emerald700)
                    }
                }
                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 40.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("নতুন লেনদেন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = Slate200)
        }
    }
}

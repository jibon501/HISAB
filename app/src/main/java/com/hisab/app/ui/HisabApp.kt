package com.hisab.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisab.app.HisabViewModel
import com.hisab.app.data.Kind
import kotlinx.coroutines.launch

private data class NavTab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    NavTab("ড্যাশবোর্ড", Icons.Default.Dashboard),
    NavTab("লেনদেন", Icons.Default.Receipt),
    NavTab("ধার দেওয়া", Icons.Default.TrendingUp),
    NavTab("ধার নেওয়া", Icons.Default.TrendingDown),
    NavTab("অ্যাকাউন্ট", Icons.Default.AccountBalanceWallet),
)

/** অ্যাপের মূল কাঠামো: নিচে ৪টি ট্যাব। স্ক্রিন ধরে ডানে-বামে টানলেও ট্যাব বদলায় */
@Composable
fun HisabApp(vm: HisabViewModel = viewModel()) {
    val pager = rememberPagerState(pageCount = { TABS.size })
    val scope = rememberCoroutineScope()

    // ব্যাক চাপলে আগে ড্যাশবোর্ডে ফিরবে, ড্যাশবোর্ডে থাকলে তবেই অ্যাপ বন্ধ হবে
    BackHandler(enabled = pager.currentPage != 0) {
        scope.launch { pager.animateScrollToPage(0) }
    }

    Scaffold(
        // ফোনের স্ট্যাটাস বারের জায়গা সিস্টেম নিজেই ছাড়ে; এখানে আবার যোগ করলে উপরে দ্বিগুণ ফাঁকা হয়
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                TABS.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = pager.currentPage == i,
                        onClick = { scope.launch { pager.animateScrollToPage(i) } },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
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
                    1 -> TransactionsScreen(vm)
                    2 -> DebtsScreen(vm, Kind.LENT)
                    3 -> DebtsScreen(vm, Kind.LOAN)
                    else -> AccountsScreen(vm)
                }
            }
        }
    }
}

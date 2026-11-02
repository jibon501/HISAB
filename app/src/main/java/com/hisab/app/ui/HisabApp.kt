package com.hisab.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisab.app.HisabViewModel
import kotlinx.coroutines.launch

private data class NavTab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    NavTab("ড্যাশবোর্ড", Icons.Default.Dashboard),
    NavTab("লেনদেন", Icons.Default.Receipt),
    NavTab("দেনা-পাওনা", Icons.Default.SwapHoriz),
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
        HorizontalPager(state = pager, modifier = Modifier.padding(pad)) { page ->
            Box {
                when (page) {
                    0 -> DashboardScreen(vm)
                    1 -> TransactionsScreen(vm)
                    2 -> DebtsScreen(vm)
                    else -> AccountsScreen(vm)
                }
            }
        }
    }
}

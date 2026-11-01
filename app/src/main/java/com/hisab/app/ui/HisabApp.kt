package com.hisab.app.ui

import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisab.app.HisabViewModel

private data class NavTab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    NavTab("ড্যাশবোর্ড", Icons.Default.Dashboard),
    NavTab("লেনদেন", Icons.Default.Receipt),
    NavTab("দেনা-পাওনা", Icons.Default.SwapHoriz),
    NavTab("অ্যাকাউন্ট", Icons.Default.AccountBalanceWallet),
)

/** অ্যাপের মূল কাঠামো: নিচে ৪টি ট্যাব */
@Composable
fun HisabApp(vm: HisabViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        val m = Modifier.padding(pad)
        androidx.compose.foundation.layout.Box(m) {
            when (tab) {
                0 -> DashboardScreen(vm)
                1 -> TransactionsScreen(vm)
                2 -> DebtsScreen(vm)
                else -> AccountsScreen(vm)
            }
        }
    }
}

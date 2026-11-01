@file:OptIn(ExperimentalMaterial3Api::class)

package com.hisab.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hisab.app.HisabViewModel

/** অ্যাকাউন্ট ও সম্পদের তালিকা (ধরন অনুযায়ী ভাগ করা) */
@Composable
fun AccountsScreen(vm: HisabViewModel) {
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("অ্যাকাউন্ট ও সম্পদ") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "নতুন অ্যাকাউন্ট")
            }
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            TYPE_LABELS.forEach { (type, label) ->
                val group = vm.accounts.filter { it.type == type }
                if (group.isNotEmpty()) {
                    item(key = "h_$type") {
                        Text(
                            label,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(group, key = { it.id }) { a ->
                        ListItem(
                            leadingContent = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                            headlineContent = { Text(a.name) },
                            trailingContent = {
                                Text(tk(a.balance), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        val ctx = LocalContext.current
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("bank") }
        var opening by remember { mutableStateOf("") }
        FormDialog(
            title = "নতুন অ্যাকাউন্ট",
            confirmText = "যোগ করুন",
            onDismiss = { showAdd = false },
            onConfirm = {
                if (name.isBlank()) {
                    ctx.toast("অ্যাকাউন্টের নাম দিন")
                } else {
                    vm.addAccount(name.trim(), type, opening.toDoubleOrNull() ?: 0.0)
                    showAdd = false
                }
            },
        ) {
            TextInput(name, { name = it }, "অ্যাকাউন্টের নাম")
            Picker("ধরন", TYPE_LABELS.keys.toList(), type, { TYPE_LABELS.getValue(it) }) { type = it }
            MoneyField(opening, { opening = it }, "শুরুর ব্যালেন্স")
        }
    }
}

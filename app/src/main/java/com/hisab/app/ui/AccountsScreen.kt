@file:OptIn(ExperimentalMaterial3Api::class)

package com.hisab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.hisab.app.data.Account

/** অ্যাকাউন্ট ও সম্পদের তালিকা। অ্যাকাউন্টে ট্যাপ করলে নাম বদল বা মোছা যায় */
@Composable
fun AccountsScreen(vm: HisabViewModel) {
    val ctx = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Account?>(null) }
    var deleting by remember { mutableStateOf<Account?>(null) }

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
                            modifier = Modifier.clickable { editing = a },
                            leadingContent = { AccountAvatar(a) },
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
        AddAccountDialog(
            onDismiss = { showAdd = false },
            onSave = { name, type, opening, icon ->
                vm.addAccount(name, type, opening, icon)
                showAdd = false
            },
        )
    }

    editing?.let { a ->
        EditAccountDialog(
            account = a,
            onDismiss = { editing = null },
            onSave = { name ->
                vm.updateAccount(a.id, name, a.icon)
                editing = null
            },
            onDelete = {
                deleting = a
                editing = null
            },
        )
    }

    deleting?.let { a ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("\"${a.name}\" মুছবেন?") },
            text = { Text("যে অ্যাকাউন্টে কোনো লেনদেন নেই শুধু সেটাই মোছা যায়।") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAccount(a.id) { ok ->
                        if (!ok) ctx.toast("এই অ্যাকাউন্টে লেনদেন আছে, তাই মোছা গেল না")
                    }
                    deleting = null
                }) { Text("মুছুন", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("বাতিল") } },
        )
    }
}

@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, opening: Double, icon: String) -> Unit,
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("bank") }
    var opening by remember { mutableStateOf("") }

    FormDialog(
        title = "নতুন অ্যাকাউন্ট",
        confirmText = "যোগ করুন",
        onDismiss = onDismiss,
        onConfirm = {
            if (name.isBlank()) ctx.toast("অ্যাকাউন্টের নাম দিন")
            else onSave(name.trim(), type, opening.toDoubleOrNull() ?: 0.0, defaultIconKey(type))
        },
    ) {
        Picker("ধরন", TYPE_LABELS.keys.toList(), type, { TYPE_LABELS.getValue(it) }) {
            type = it
        }
        BankNameField(type, name) { name = it }
        MoneyField(opening, { opening = it }, "শুরুর ব্যালেন্স")
    }
}

/** নাম বদল (সংরক্ষণ) অথবা অ্যাকাউন্ট মোছার অপশন */
@Composable
private fun EditAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit,
    onDelete: () -> Unit,
) {
    val ctx = LocalContext.current
    var name by remember(account.id) { mutableStateOf(account.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("অ্যাকাউন্ট এডিট") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BankNameField(account.type, name) { name = it }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("অ্যাকাউন্ট মুছুন", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) ctx.toast("নাম খালি রাখা যাবে না") else onSave(name.trim())
            }) { Text("সংরক্ষণ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}

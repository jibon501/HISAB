@file:OptIn(ExperimentalMaterial3Api::class)

package com.hisab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.hisab.app.data.fmtDate
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
import com.hisab.app.HisabViewModel
import com.hisab.app.data.OTHER_ACCOUNT
import com.hisab.app.data.Account

/** অ্যাকাউন্ট ও সম্পদের তালিকা। অ্যাকাউন্টে ট্যাপ করলে নাম বদল বা মোছা যায় */
@Composable
fun AccountsScreen(vm: HisabViewModel) {
    val ctx = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Account?>(null) }
    var viewing by remember { mutableStateOf<Account?>(null) } // যে অ্যাকাউন্টের সব লেনদেন দেখা হচ্ছে
    var deleting by remember { mutableStateOf<Account?>(null) }

    // নিজে যোগ করা ধরনগুলো (সবার শেষে, "অন্যান্য"-এর আগে)
    val customTypes = vm.accounts.map { it.type }.distinct().filter { it !in TYPE_LABELS }
    val typeOrder = TYPE_LABELS.keys.filter { it != "other" } + customTypes + "other"

    Scaffold(
        // উপরের অংশের জায়গা বাইরের কাঠামোই ধরে, তাই এখানে আলাদা ইনসেট নেই
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // ভাসমান "+": নতুন অ্যাকাউন্ট (ট্রান্সফার বাটন এক্সপোর্ট ও ইম্পোর্টের মাঝখানে)
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "নতুন অ্যাকাউন্ট")
            }
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            item(key = "excel_bar") { ExcelBar(vm) { showTransfer = true } }
            typeOrder.forEach { type ->
                val label = typeLabel(type)
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
                            // চাপলে এই অ্যাকাউন্টের সব লেনদেন দেখায়
                            modifier = Modifier.clickable { viewing = a },
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
            customTypes = customTypes,
            onDismiss = { showAdd = false },
            onSave = { name, type, opening, icon ->
                vm.addAccount(name, type, opening, icon)
                showAdd = false
            },
        )
    }

    if (showTransfer) {
        TransferDialog(
            accounts = vm.accounts,
            onDismiss = { showTransfer = false },
            onSave = { from, to, amount ->
                vm.transfer(from, to, amount)
                showTransfer = false
            },
        )
    }

    viewing?.let { a ->
        // ব্যালেন্স বদলালে (নতুন লেনদেন) তালিকাও নতুন করে আসে
        val balance = vm.accounts.firstOrNull { it.id == a.id }?.balance ?: a.balance
        AccountTxnsDialog(
            account = a,
            balance = balance,
            txns = remember(a.id, vm.txns, vm.accounts) { vm.accountTxns(a.id) },
            // ডিফল্ট "অন্যান্য" অ্যাকাউন্ট মোছা বা নাম বদল করা যায় না
            canEdit = a.name != OTHER_ACCOUNT,
            onEdit = { editing = a; viewing = null },
            onDismiss = { viewing = null },
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
    customTypes: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, opening: Double, icon: String) -> Unit,
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("bank") }
    var opening by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf("") }

    // তালিকার ধরন + আগে যোগ করা নিজের ধরন + "নতুন ধরন"
    val options = TYPE_LABELS.keys.filter { it != "other" } + customTypes + NEW_TYPE

    FormDialog(
        title = "নতুন অ্যাকাউন্ট",
        confirmText = "যোগ করুন",
        onDismiss = onDismiss,
        onConfirm = {
            val typed = newType.trim()
            if (type == NEW_TYPE && typed.isEmpty()) {
                ctx.toast("নতুন ধরনের নাম দিন")
            } else if (name.isBlank()) {
                ctx.toast("অ্যাকাউন্টের নাম দিন")
            } else {
                // লেখা নাম উপরের কোনো ধরনের নামের সমান হলে সেই ধরনই ধরা হয়
                val finalType = if (type != NEW_TYPE) type
                else TYPE_LABELS.entries.firstOrNull { it.value == typed }?.key ?: typed
                onSave(name.trim(), finalType, opening.toDoubleOrNull() ?: 0.0, defaultIconKey(finalType))
            }
        },
    ) {
        Picker(
            "ধরন", options, type,
            { if (it == NEW_TYPE) "+ নতুন ধরন যোগ করুন" else typeLabel(it) },
        ) {
            type = it
        }
        if (type == NEW_TYPE) {
            TextInput(newType, { newType = it }, "নতুন ধরনের নাম")
        }
        BankNameField(type, name) { name = it }
        MoneyField(opening, { opening = it }, "শুরুর ব্যালেন্স")
    }
}

/** ধরনের তালিকা থেকে "নতুন ধরন" বাছার বিশেষ মান */
private const val NEW_TYPE = "__new_type__"

/** নাম বদল। নিচের বাটন: সংরক্ষণ, মুছুন (বক্সের বাইরে চাপলে বা ব্যাক দিলে বন্ধ হয়) */
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
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    if (name.isBlank()) ctx.toast("নাম খালি রাখা যাবে না") else onSave(name.trim())
                }) { Text("সংরক্ষণ") }
                TextButton(onClick = onDelete) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
            }
        },
    )
}

@Composable
private fun TransferDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (from: Long, to: Long, amount: Double) -> Unit,
) {
    val ctx = LocalContext.current
    var from by remember { mutableStateOf<Account?>(null) }
    var to by remember { mutableStateOf<Account?>(null) }
    var amount by remember { mutableStateOf("") }

    FormDialog(
        title = "ট্রান্সফার (যেমন: ব্যাংক থেকে DPS)",
        confirmText = "ট্রান্সফার",
        onDismiss = onDismiss,
        onConfirm = {
            val a = amount.toDoubleOrNull()
            val f = from
            val t = to
            if (a == null || a <= 0 || f == null || t == null || f.id == t.id) {
                ctx.toast("সঠিক তথ্য দিন (দুটি আলাদা অ্যাকাউন্ট লাগবে)")
            } else {
                onSave(f.id, t.id, a)
            }
        },
    ) {
        val label: (Account) -> String = { "${it.name} (${tk(it.balance)})" }
        Picker("যে অ্যাকাউন্ট থেকে", accounts.withMoney(), from, label) { from = it }
        Picker("যে অ্যাকাউন্টে", accounts, to, label) { to = it }
        MoneyField(amount) { amount = it }
    }
}

/** একটি অ্যাকাউন্টের যাবতীয় লেনদেন (নতুন আগে)। নিচে "সম্পাদনা" (নাম বদল/মোছা) ও "বন্ধ" */
@Composable
private fun AccountTxnsDialog(
    account: Account,
    balance: Double,
    txns: List<com.hisab.app.data.Txn>,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(account.name)
                Text("ব্যালেন্স: ${tk(balance)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            if (txns.isEmpty()) {
                Text("কোনো লেনদেন নেই")
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(txns, key = { it.id }) { t ->
                        val credit = t.type == "credit"
                        val color = if (t.cleared) Color.Gray else if (credit) Color(0xFF2E7D32) else Color(0xFFC62828)
                        val deco = if (t.cleared) TextDecoration.LineThrough else null
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    t.category + if (t.note.isNotBlank()) " • ${t.note}" else "",
                                    fontSize = 14.sp,
                                    color = if (t.cleared) Color.Gray else Color.Unspecified,
                                    textDecoration = deco,
                                )
                                Text(fmtDate(t.date), fontSize = 11.sp, color = Color.Gray, textDecoration = deco)
                            }
                            Text(
                                (if (credit) "+" else "-") + tk(t.amount),
                                color = color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textDecoration = deco,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (canEdit) TextButton(onClick = onEdit) { Text("সম্পাদনা") }
                TextButton(onClick = onDismiss) { Text("বন্ধ") }
            }
        },
    )
}

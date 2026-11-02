@file:OptIn(ExperimentalMaterial3Api::class)

package com.hisab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hisab.app.HisabViewModel
import com.hisab.app.data.Account
import com.hisab.app.data.Txn

private val GREEN = Color(0xFF2E7D32)
private val RED = Color(0xFFC62828)

/** আয়-ব্যয়ের তালিকা, খাত অনুযায়ী ফিল্টার ও ট্রান্সফার */
@Composable
fun TransactionsScreen(vm: HisabViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var showCats by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Txn?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vm.filter?.let { "লেনদেন • $it" } ?: "লেনদেন") },
                actions = {
                    IconButton(onClick = { showCats = true }) {
                        Icon(Icons.Default.Category, contentDescription = "খাত")
                    }
                    IconButton(onClick = { showTransfer = true }) {
                        Icon(Icons.Default.CompareArrows, contentDescription = "ট্রান্সফার")
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "খাত ফিল্টার",
                                tint = if (vm.filter != null) Color(0xFFFB8C00) else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("সব খাত") },
                                onClick = { vm.applyFilter(null); menu = false },
                            )
                            (vm.categories.map { it.name }.distinct() + SYSTEM_CATS).forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c) },
                                    onClick = { vm.applyFilter(c); menu = false },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("নতুন লেনদেন") },
            )
        },
    ) { pad ->
        if (vm.txns.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("কোনো লেনদেন নেই")
            }
        } else {
            LazyColumn(Modifier.padding(pad)) {
                items(vm.txns, key = { it.id }) { t -> TxnRow(t) { toDelete = t } }
            }
        }
    }

    if (showAdd) {
        AddTxnDialog(
            accounts = vm.accounts,
            expenseCats = vm.categories.filter { it.kind == "expense" }.map { it.name },
            incomeCats = vm.categories.filter { it.kind == "income" }.map { it.name },
            onDismiss = { showAdd = false },
            onSave = { type, amount, accId, cat, note, baki ->
                // তালিকায় না থাকা নতুন খাত লিখলে নিজে থেকে সেভ হয়ে যায়
                vm.addCategory(cat, if (type == "debit") "expense" else "income")
                if (baki) vm.addBakiExpense(amount, cat, note)
                else if (accId != null) vm.addTxn(type, amount, accId, cat, note)
                showAdd = false
            },
        )
    }
    if (showCats) {
        CategoriesDialog(
            categories = vm.categories,
            onAdd = { name, kind -> vm.addCategory(name, kind) },
            onRename = { id, name -> vm.renameCategory(id, name) },
            onDelete = { id -> vm.deleteCategory(id) },
            onDismiss = { showCats = false },
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
    toDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("লেনদেন মুছবেন?") },
            text = { Text("${t.category} • ${tk(t.amount)}\nঅ্যাকাউন্টের ব্যালেন্স সেই অনুযায়ী ঠিক হয়ে যাবে।") },
            confirmButton = {
                TextButton(onClick = { vm.deleteTxn(t.id); toDelete = null }) { Text("মুছুন") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("বাতিল") } },
        )
    }
}

@Composable
private fun TxnRow(t: Txn, onDelete: () -> Unit) {
    val credit = t.type == "credit"
    val color = if (credit) GREEN else RED
    ListItem(
        leadingContent = {
            Icon(
                if (credit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = color,
            )
        },
        headlineContent = { Text(t.category) },
        supportingContent = {
            val note = if (t.note.isNotBlank()) " • ${t.note}" else ""
            Text("${t.account} • ${t.date.take(10)}$note")
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    (if (credit) "+" else "-") + tk(t.amount),
                    color = color,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "মুছুন")
                }
            }
        },
    )
}

@Composable
private fun AddTxnDialog(
    accounts: List<Account>,
    expenseCats: List<String>,
    incomeCats: List<String>,
    onDismiss: () -> Unit,
    onSave: (type: String, amount: Double, accountId: Long?, category: String, note: String, baki: Boolean) -> Unit,
) {
    val ctx = LocalContext.current
    var type by remember { mutableStateOf("debit") }
    var amount by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("") }
    var acc by remember { mutableStateOf<Account?>(null) }
    var note by remember { mutableStateOf("") }
    var baki by remember { mutableStateOf(false) }

    FormDialog(
        title = "নতুন লেনদেন",
        confirmText = "সংরক্ষণ",
        onDismiss = onDismiss,
        onConfirm = {
            val a = amount.toDoubleOrNull()
            val c = cat.trim()
            val ac = acc
            val isBaki = type == "debit" && baki
            if (a == null || a <= 0 || c.isEmpty() || (!isBaki && ac == null)) {
                ctx.toast(if (isBaki) "পরিমাণ ও খাত দিন" else "পরিমাণ, খাত ও অ্যাকাউন্ট দিন")
            } else {
                onSave(type, a, ac?.id, c, note.trim(), isBaki)
            }
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = type == "debit",
                onClick = { type = "debit"; cat = "" },
                label = { Text("খরচ") },
            )
            FilterChip(
                selected = type == "credit",
                onClick = { type = "credit"; cat = "" },
                label = { Text("আয়") },
            )
        }
        MoneyField(amount) { amount = it }
        CategoryField(if (type == "debit") expenseCats else incomeCats, cat) { cat = it }
        if (type == "debit") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("বাকি (এখনো টাকা দিইনি)")
                Switch(checked = baki, onCheckedChange = { baki = it })
            }
        }
        if (!(type == "debit" && baki)) {
            Picker(
                label = if (type == "debit") "কোন অ্যাকাউন্ট থেকে কাটবে" else "কোন অ্যাকাউন্টে জমা হবে",
                options = accounts,
                selected = acc,
                text = { "${it.name} (${tk(it.balance)})" },
            ) { acc = it }
        }
        TextInput(note, { note = it }, "নোট (ঐচ্ছিক)")
    }
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
        Picker("যে অ্যাকাউন্ট থেকে", accounts, from, label) { from = it }
        Picker("যে অ্যাকাউন্টে", accounts, to, label) { to = it }
        MoneyField(amount) { amount = it }
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)

package com.hisab.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hisab.app.HisabViewModel
import com.hisab.app.data.Account
import com.hisab.app.data.Debt
import com.hisab.app.data.Kind

/** ধার দেওয়া / ধার নেওয়া: প্রতিটির আলাদা স্ক্রিন, ডানে ভাসমান (+) বাটন */
@Composable
fun DebtsScreen(vm: HisabViewModel, kind: String) {
    var showAdd by remember { mutableStateOf(false) }
    var payTarget by remember { mutableStateOf<Debt?>(null) }
    val list = vm.debts[kind].orEmpty()

    Scaffold(
        // উপরের অংশের জায়গা বাইরের কাঠামোই ধরে, তাই এখানে আলাদা ইনসেট নেই
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "নতুন")
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("কোনো হিসাব নেই") }
            } else {
                LazyColumn {
                    items(list, key = { it.id }) { d ->
                        val done = d.remaining <= 0.0
                        ListItem(
                            modifier = Modifier.clickable(enabled = !done) { payTarget = d },
                            headlineContent = { Text(d.person) },
                            supportingContent = { Text("মোট ${tk(d.total)} • ${d.date.take(10)}") },
                            trailingContent = {
                                Text(
                                    if (done) "পরিশোধিত ✓" else tk(d.remaining),
                                    fontWeight = FontWeight.Bold,
                                    color = if (done) Color(0xFF2E7D32) else Color(0xFFC62828),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddDebtDialog(
            kind = kind,
            // ধার দিলে টাকা বেরিয়ে যায়, তাই তখন ০ ব্যালেন্সের অ্যাকাউন্ট বাদ
            accounts = if (kind == Kind.LENT) vm.accounts.withMoney() else vm.accounts,
            // আগে সেভ করা সব নাম (সাম্প্রতিক আগে), লিখতে শুরু করলে সাজেশন হিসেবে দেখাবে
            suggestions = vm.debts.values.flatten().sortedByDescending { it.id }.map { it.person }.distinct(),
            onDismiss = { showAdd = false },
            onSave = { person, total, note, accId ->
                vm.addDebt(kind, person, total, note, accId)
                showAdd = false
            },
        )
    }
    payTarget?.let { d ->
        PayDialog(
            debt = d,
            // ধার পরিশোধে টাকা বেরিয়ে যায়, ধার ফেরত পেলে টাকা ঢোকে
            accounts = if (d.kind == Kind.LOAN) vm.accounts.withMoney() else vm.accounts,
            onDismiss = { payTarget = null },
            onSave = { amount, accId ->
                vm.pay(d, amount, accId)
                payTarget = null
            },
        )
    }
}

@Composable
private fun AddDebtDialog(
    kind: String,
    accounts: List<Account>,
    suggestions: List<String>,
    onDismiss: () -> Unit,
    onSave: (person: String, total: Double, note: String, accountId: Long?) -> Unit,
) {
    val ctx = LocalContext.current
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var acc by remember { mutableStateOf<Account?>(null) }

    val personLabel = when (kind) {
        Kind.LOAN -> "কার কাছ থেকে নিলেন"
        Kind.LENT -> "কাকে দিলেন"
        else -> "দোকানের নাম"
    }

    FormDialog(
        title = "নতুন হিসাব",
        confirmText = "সংরক্ষণ",
        onDismiss = onDismiss,
        onConfirm = {
            val a = amount.toDoubleOrNull()
            if (person.isBlank() || a == null || a <= 0 || (kind != Kind.SHOP && acc == null)) {
                ctx.toast("সব তথ্য ও অ্যাকাউন্ট সিলেক্ট করুন")
            } else {
                onSave(person.trim(), a, note.trim(), acc?.id)
            }
        },
    ) {
        TextInput(person, { person = it }, personLabel)
        // নামের অক্ষর লিখলেই মিলে যাওয়া আগের নামগুলো দেখায় (শুরুতে মিললে আগে), ট্যাপ করলে নাম বসে যায়
        val q = person.trim()
        val shown = if (q.isEmpty()) {
            emptyList()
        } else {
            suggestions
                .filter { it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true) }
                .sortedByDescending { it.startsWith(q, ignoreCase = true) }
                .take(5)
        }
        if (shown.isNotEmpty()) {
            Surface(
                tonalElevation = 3.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    shown.forEach { n ->
                        Text(
                            n,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { person = n }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
        MoneyField(amount) { amount = it }
        if (kind != Kind.SHOP) {
            Picker(
                label = if (kind == Kind.LOAN) "টাকা কোন অ্যাকাউন্টে জমা হলো" else "টাকা কোন অ্যাকাউন্ট থেকে গেল",
                options = accounts,
                selected = acc,
                text = { "${it.name} (${tk(it.balance)})" },
            ) { acc = it }
        }
        TextInput(note, { note = it }, "নোট")
    }
}

@Composable
private fun PayDialog(
    debt: Debt,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (amount: Double, accountId: Long) -> Unit,
) {
    val ctx = LocalContext.current
    val lent = debt.kind == Kind.LENT
    var amount by remember { mutableStateOf("") }
    var acc by remember { mutableStateOf<Account?>(null) }

    FormDialog(
        title = "${debt.person} — বাকি ${tk(debt.remaining)}",
        confirmText = "নিশ্চিত করুন",
        onDismiss = onDismiss,
        onConfirm = {
            val a = amount.toDoubleOrNull()
            val ac = acc
            when {
                a == null || a <= 0 || ac == null -> ctx.toast("পরিমাণ ও অ্যাকাউন্ট সিলেক্ট করুন")
                a > debt.remaining -> ctx.toast("বাকির চেয়ে বেশি দেওয়া যাবে না")
                else -> onSave(a, ac.id)
            }
        },
    ) {
        MoneyField(amount, { amount = it }, if (lent) "কত ফেরত পেলেন" else "কত পরিশোধ করলেন")
        Picker(
            label = if (lent) "কোন অ্যাকাউন্টে জমা হলো" else "কোন অ্যাকাউন্ট থেকে কাটবে",
            options = accounts,
            selected = acc,
            text = { "${it.name} (${tk(it.balance)})" },
        ) { acc = it }
    }
}

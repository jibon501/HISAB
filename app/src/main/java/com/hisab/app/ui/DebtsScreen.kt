@file:OptIn(ExperimentalMaterial3Api::class)

package com.hisab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import com.hisab.app.HisabViewModel
import com.hisab.app.data.Account
import com.hisab.app.data.Debt
import com.hisab.app.data.Kind
import com.hisab.app.data.HistoryItem
import com.hisab.app.data.fmtDate

/** ধার দেওয়া / ধার নেওয়া: প্রতিটির আলাদা তালিকা। নতুন ধার যোগ হয় লেনদেনের নতুন ফর্ম থেকে */
@Composable
fun DebtsScreen(vm: HisabViewModel, kind: String) {
    var editTarget by remember { mutableStateOf<Debt?>(null) }
    val list = vm.debts[kind].orEmpty()

    Scaffold(
        // উপরের অংশের জায়গা বাইরের কাঠামোই ধরে, তাই এখানে আলাদা ইনসেট নেই
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("কোনো হিসাব নেই") }
            } else {
                // নিচে একটু ফাঁকা রাখা হয়েছে, যাতে শেষ সারি (+) বাটন ও মোটের নিচে ঢাকা না পড়ে
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.id }) { d ->
                        val done = d.remaining <= 0.0
                        // প্রতিটি হিসাব আলাদা হালকা গাঢ় বক্সে
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { editTarget = d },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            ),
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(d.person) },
                                supportingContent = { Text("মোট ${tk(d.total)} • ${fmtDate(d.date)}") },
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
            // স্ক্রিনে যা দেখা যাচ্ছে তার মোট বাকি: নিচে বামে, ছোট ও ধূসর
            Text(
                "মোট: ${tk(list.sumOf { it.remaining })}",
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp),
                fontSize = 12.sp,
                color = Color.Gray,
            )
        }
    }

    editTarget?.let { d ->
        DebtEditDialog(
            debt = d,
            entries = vm.debtEntries(d.id),
            // ধার পরিশোধে টাকা বেরিয়ে যায়, ধার ফেরত পেলে টাকা ঢোকে
            accounts = if (d.kind == Kind.LOAN) vm.accounts.withMoney() else vm.accounts,
            onDismiss = { editTarget = null },
            onSave = { name, splits ->
                vm.saveDebtEdit(d, name, splits)
                editTarget = null
            },
            onDelete = {
                vm.deleteDebt(d.id)
                editTarget = null
            },
        )
    }
}

/**
 * হিসাবে চাপলে প্রথমে শুধু দেখার বক্স: নাম, প্রতিটি এন্ট্রি, বাকি।
 * নিচের বাটন: সম্পাদনা, সংরক্ষণ, মুছুন। "সম্পাদনা" চাপলে নাম বদল ও ফেরত/পরিশোধের লাইন আসে,
 * তখন "সংরক্ষণ" চালু হয়।
 */
@Composable
private fun DebtEditDialog(
    debt: Debt,
    entries: List<HistoryItem>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (name: String, splits: List<Pair<Long, Double>>) -> Unit,
    onDelete: () -> Unit,
) {
    val ctx = LocalContext.current
    val lent = debt.kind == Kind.LENT
    val done = debt.remaining <= 0.0
    var editing by remember(debt.id) { mutableStateOf(false) }
    var name by remember(debt.id) { mutableStateOf(debt.person) }
    var lines by remember(debt.id) { mutableStateOf(listOf(SplitLine())) }
    var confirmDelete by remember(debt.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!editing) {
                    // শুধু দেখার অবস্থা: নাম মাঝখানে, তিন কলাম, নিচে মোট
                    DebtTable(debt.person, entries)
                } else {
                    TextInput(name, { name = it }, "নাম")
                    if (!done) {
                        Text(
                            if (lent) "ফেরত পেলে" else "পরিশোধ করলে",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        SplitLines(
                            accounts = accounts,
                            lines = lines,
                            onChange = { lines = it },
                            accountLabel = if (lent) "অ্যাকাউন্টে জমা হবে" else "অ্যাকাউন্ট থেকে কাটবে",
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { editing = true }, enabled = !editing) { Text("সম্পাদনা") }
                TextButton(
                    enabled = editing,
                    onClick = {
                        val n = name.trim()
                        val splits = lines.splits()
                        val total = splits.sumOf { it.second }
                        when {
                            n.isEmpty() -> ctx.toast("নাম খালি রাখা যাবে না")
                            splits.any { it.first == null } -> ctx.toast("প্রতিটি টাকার জন্য অ্যাকাউন্ট সিলেক্ট করুন")
                            total > debt.remaining + 0.0001 -> ctx.toast("বাকির চেয়ে বেশি দেওয়া যাবে না")
                            else -> onSave(n, splits.map { (it.first as Account).id to it.second })
                        }
                    },
                ) { Text("সংরক্ষণ") }
                TextButton(onClick = { confirmDelete = true }) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
            }
        },
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("\"${debt.person}\" মুছবেন?") },
            text = { Text("এই হিসাবের সব লেনদেন মুছে যাবে এবং অ্যাকাউন্টের ব্যালেন্স আগের অবস্থায় ফিরবে।") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("বাতিল") } },
        )
    }
}

package com.hisab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisab.app.HisabViewModel
import com.hisab.app.data.Kind

/** কোন কার্ডে ট্যাপ করা হয়েছে */
private enum class Detail {
    CASH, LENT, LOAN, SHOP, EXPENSE, DPS, ASSET
}

/** তালিকার একটি সারি। kind থাকলে (দেনা-পাওনা) নামে ট্যাপ করে এডিট করা যায় */
private data class DRow(
    val name: String,
    val value: Double,
    val sub: String? = null,
    val kind: String? = null,
)

/** হোম স্ক্রিন: সারসংক্ষেপ কার্ড। কার্ডে ট্যাপ করলে বিস্তারিত তালিকা */
@Composable
fun DashboardScreen(vm: HisabViewModel) {
    val s = vm.summary
    var detail by remember { mutableStateOf<Detail?>(null) }
    var editing by remember { mutableStateOf<Pair<String, String>?>(null) } // (ধরন, নাম)

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SummaryCard("ক্যাশ", s.cash, Color(0xFF00897B), Icons.Default.Payments, big = true) { detail = Detail.CASH } }
        item { SummaryCard("মোট পাওনা", s.lent, Color(0xFF43A047), Icons.Default.TrendingUp) { detail = Detail.LENT } }
        item { SummaryCard("মোট লোন", s.loan, Color(0xFFE53935), Icons.Default.TrendingDown) { detail = Detail.LOAN } }
        item { SummaryCard("দোকান বাকি", s.shop, Color(0xFFFB8C00), Icons.Default.Store) { detail = Detail.SHOP } }
        item { SummaryCard("এই মাসের খরচ", s.monthExpense, Color(0xFF8E24AA), Icons.Default.ShoppingCart) { detail = Detail.EXPENSE } }
        item { SummaryCard("ডিপিএস মোট", s.dps, Color(0xFF3949AB), Icons.Default.Savings) { detail = Detail.DPS } }
        item { SummaryCard("সম্পদ", s.asset, Color(0xFF8D6E63), Icons.Default.Landscape) { detail = Detail.ASSET } }
    }

    detail?.let { d ->
        DetailDialog(
            rows = rowsFor(d, vm),
            // মাসের খরচ কার্ডে ট্যাপ করলে বাকির অংশ আলাদা দেখায়
            baki = if (d == Detail.EXPENSE) s.monthBaki else null,
            onEdit = { kind, person -> editing = kind to person },
            onDismiss = { detail = null },
        )
    }

    editing?.let { (kind, person) ->
        EditDebtDialog(vm, kind, person) { editing = null }
    }
}

/** কার্ড অনুযায়ী তালিকার সারি */
private fun rowsFor(d: Detail, vm: HisabViewModel): List<DRow> {
    fun accounts(vararg types: String) =
        vm.accounts.filter { it.type in types }.map { DRow(it.name, it.balance) }

    // একই নামের একাধিক হিসাব যোগ করে মোট দেখায়
    fun owed(kind: String) = vm.debts[kind].orEmpty()
        .filter { it.remaining > 0 }
        .groupBy { it.person }
        .map { (person, list) -> DRow(person, list.sumOf { it.remaining }, kind = kind) }
        .sortedByDescending { it.value }

    return when (d) {
        Detail.CASH -> accounts("cash", "mobile", "bank")
        Detail.LOAN -> owed(Kind.LOAN)
        Detail.LENT -> owed(Kind.LENT)
        Detail.SHOP -> owed(Kind.SHOP)
        Detail.DPS -> accounts("dps")
        Detail.ASSET -> accounts("asset", "other")
        Detail.EXPENSE -> vm.monthExpenses.map {
            DRow(it.title, it.amount, sub = if (it.remaining > 0) "বাকি ${tk(it.remaining)}" else null)
        }
    }
}

/** তালিকা। কার্ডের নাম এখানে দেখানো হয় না; নিচে "বন্ধ করুন" বাটন */
@Composable
private fun DetailDialog(
    rows: List<DRow>,
    baki: Double?,
    onEdit: (kind: String, person: String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                Text(
                    "মোট: ${tk(rows.sumOf { it.value })}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (baki != null) {
                    Text(
                        "বাকি: ${tk(baki)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935),
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                if (rows.isEmpty()) {
                    Text("কোনো তথ্য নেই")
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(rows) { row ->
                            val tap = row.kind
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .then(if (tap != null) Modifier.clickable { onEdit(tap, row.name) } else Modifier)
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        row.name,
                                        color = if (tap != null) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                    )
                                    if (row.sub != null) {
                                        Text(row.sub, fontSize = 12.sp, color = Color(0xFFE53935))
                                    }
                                }
                                Text(tk(row.value), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("বন্ধ করুন") } },
    )
}

/** দেনা-পাওনার নামে ট্যাপ করলে: নাম বদল এবং (একটি মাত্র হিসাব থাকলে) বাকির পরিমাণ বদল */
@Composable
private fun EditDebtDialog(vm: HisabViewModel, kind: String, person: String, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val mine = vm.debts[kind].orEmpty().filter { it.person == person && it.remaining > 0 }
    val single = mine.singleOrNull()
    var name by remember(kind, person) { mutableStateOf(person) }
    var remaining by remember(kind, person) {
        mutableStateOf(single?.remaining?.let { r -> if (r % 1.0 == 0.0) r.toLong().toString() else r.toString() } ?: "")
    }

    FormDialog(
        title = "সম্পাদনা",
        confirmText = "সংরক্ষণ",
        onDismiss = onDone,
        onConfirm = {
            val n = name.trim()
            val r = remaining.toDoubleOrNull()
            when {
                n.isEmpty() -> ctx.toast("নাম খালি রাখা যাবে না")
                single != null && (r == null || r <= 0) -> ctx.toast("সঠিক পরিমাণ দিন")
                else -> {
                    vm.editDebt(kind, person, n, single?.id, if (single != null) r else null)
                    onDone()
                }
            }
        },
    ) {
        TextInput(name, { name = it }, "নাম")
        if (single != null) {
            MoneyField(remaining, { remaining = it }, "বাকির পরিমাণ")
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: Double,
    color: Color,
    icon: ImageVector,
    big: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(if (big) 40.dp else 30.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 14.sp)
                Text(
                    tk(value),
                    fontSize = if (big) 28.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
        }
    }
}

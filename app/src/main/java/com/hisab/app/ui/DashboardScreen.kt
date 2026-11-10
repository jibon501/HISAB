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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisab.app.HisabViewModel
import com.hisab.app.data.Kind
import com.hisab.app.data.OTHER_ACCOUNT
import com.hisab.app.data.fmtDate

/** কোন কার্ডে ট্যাপ করা হয়েছে */
private enum class Detail {
    CASH, LENT, LOAN, EXPENSE, DPS, ASSET, OTHER
}

/** তালিকার একটি সারি। kind থাকলে (দেনা-পাওনা) নামে ট্যাপ করে এডিট করা যায় */
private data class DRow(
    val name: String,
    val value: Double,
    val sub: String? = null,
    val kind: String? = null,
    val payDebtId: Long? = null, // বাকি খরচ: ট্যাপ করলে পরিশোধ
)

/** হোম স্ক্রিন: সারসংক্ষেপ কার্ড। কার্ডে ট্যাপ করলে বিস্তারিত তালিকা */
@Composable
fun DashboardScreen(vm: HisabViewModel) {
    val s = vm.summary
    var detail by remember { mutableStateOf<Detail?>(null) }
    var history by remember { mutableStateOf<Pair<String, String>?>(null) } // (ধরন, নাম)
    var paying by remember { mutableStateOf<Long?>(null) } // বাকি খরচের debt id

    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SummaryCard("ক্যাশ", s.cash, Color(0xFF00897B), Icons.Default.Payments, big = true) { detail = Detail.CASH } }
        item { SummaryCard("ধার দেওয়া", s.lent, Color(0xFF43A047), Icons.Default.TrendingUp) { detail = Detail.LENT } }
        item { SummaryCard("ধার নেওয়া", s.loan, Color(0xFFE53935), Icons.Default.TrendingDown) { detail = Detail.LOAN } }
        item { SummaryCard("এই মাসের খরচ", s.monthExpense, Color(0xFF8E24AA), Icons.Default.ShoppingCart) { detail = Detail.EXPENSE } }
        item { SummaryCard("ডিপিএস মোট", s.dps, Color(0xFF3949AB), Icons.Default.Savings) { detail = Detail.DPS } }
        item { SummaryCard("সম্পদ", s.asset, Color(0xFF8D6E63), Icons.Default.Landscape) { detail = Detail.ASSET } }
        // "অন্যান্য" অ্যাকাউন্টের ব্যালেন্স ০ হলে কার্ডটি দেখানো হয় না
        if (s.other != 0.0) {
            item { SummaryCard("অন্যান্য", s.other, Color(0xFF546E7A), Icons.Default.MoreHoriz) { detail = Detail.OTHER } }
        }
    }

    detail?.let { d ->
        DetailDialog(
            rows = rowsFor(d, vm),
            // মাসের খরচ কার্ডে ট্যাপ করলে বাকির অংশ আলাদা দেখায়
            baki = if (d == Detail.EXPENSE) s.monthBaki else null,
            onEdit = { kind, person -> history = kind to person },
            onPay = { id -> paying = id },
            onDismiss = { detail = null },
        )
    }

    history?.let { (kind, person) ->
        DebtHistoryDialog(vm, kind, person) { history = null }
    }

    paying?.let { id ->
        PayBakiDialog(vm, id) { paying = null }
    }
}

/** কার্ড অনুযায়ী তালিকার সারি */
private fun rowsFor(d: Detail, vm: HisabViewModel): List<DRow> {
    fun accounts(vararg types: String) =
        // ব্যালেন্স ০ হলে দেখানো হয় না
        vm.accounts.filter { it.type in types && it.balance != 0.0 }.map { DRow(it.name, it.balance) }

    // একই নামের একাধিক হিসাব যোগ করে মোট দেখায়
    fun owed(kind: String) = vm.debts[kind].orEmpty()
        .groupBy { it.person }
        .map { (person, list) -> DRow(person, list.sumOf { it.remaining }, kind = kind) }
        .filter { it.value > 0 } // ফেরত বেশি হলে (অতিরিক্ত) এই কার্ডে ধরা হয় না
        .sortedByDescending { it.value }

    return when (d) {
        Detail.CASH -> accounts("cash", "mobile", "bank")
        Detail.LOAN -> owed(Kind.LOAN)
        Detail.LENT -> owed(Kind.LENT)
        Detail.DPS -> accounts("dps")
        Detail.ASSET -> vm.accounts
            .filter { it.type !in ASSET_EXCLUDED_TYPES && it.name != OTHER_ACCOUNT && it.balance != 0.0 }
            .map { DRow(it.name, it.balance) }
        Detail.OTHER -> vm.accounts
            .filter { it.name == OTHER_ACCOUNT && it.balance != 0.0 }
            .map { DRow(it.name, it.balance) }
        Detail.EXPENSE -> vm.monthExpenses.map {
            DRow(
                it.title, it.amount,
                sub = if (it.remaining > 0) "বাকি ${tk(it.remaining)} • চাপলে পরিশোধ" else null,
                payDebtId = if (it.remaining > 0 && it.debtId > 0) it.debtId else null,
            )
        }
    }
}

/** তালিকা। কার্ডের নাম এখানে দেখানো হয় না; নিচে "বন্ধ করুন" বাটন */
@Composable
private fun DetailDialog(
    rows: List<DRow>,
    baki: Double?,
    onEdit: (kind: String, person: String) -> Unit,
    onPay: (debtId: Long) -> Unit,
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
                if (rows.isNotEmpty()) {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(rows) { row ->
                            val tap = row.kind
                            val pay = row.payDebtId
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .then(
                                        when {
                                            tap != null -> Modifier.clickable { onEdit(tap, row.name) }
                                            pay != null -> Modifier.clickable { onPay(pay) }
                                            else -> Modifier
                                        }
                                    )
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
        // বক্সের বাইরে চাপলে বা ব্যাক দিলে বন্ধ হয়, আলাদা বাটন নেই
        confirmButton = {},
    )
}

/** একজনের ধার-হিসাব: কবে কত ধার নেওয়া/দেওয়া, কবে কত ফেরত (শুধু দেখার জন্য, এডিট বা শেয়ার নেই) */
@Composable
private fun DebtHistoryDialog(vm: HisabViewModel, kind: String, person: String, onDismiss: () -> Unit) {
    val items = remember(kind, person, vm.debts) { vm.debtHistory(kind, person) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DebtTable(person, kind, items)
            }
        },
        // বক্সের বাইরে চাপলে বা ব্যাক দিলে বন্ধ হয়, আলাদা বাটন নেই
        confirmButton = {},
    )
}

/** বাকি খরচ পরিশোধ: অ্যাকাউন্ট বেছে টাকা দিলে সেই অ্যাকাউন্ট থেকে কাটবে, বাকি কমবে */
@Composable
private fun PayBakiDialog(vm: HisabViewModel, debtId: Long, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val debt = vm.debts[Kind.SHOP].orEmpty().firstOrNull { it.id == debtId }
    if (debt == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    var amount by remember(debtId) {
        mutableStateOf(debt.remaining.let { r -> if (r % 1.0 == 0.0) r.toLong().toString() else r.toString() })
    }
    var acc by remember(debtId) { mutableStateOf<com.hisab.app.data.Account?>(null) }

    var confirmDelete by remember(debtId) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("বাকি পরিশোধ") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("${debt.person} • বাকি ${tk(debt.remaining)}")
                MoneyField(amount, { amount = it }, "কত টাকা দিচ্ছেন")
                Picker(
                    label = "অ্যাকাউন্ট থেকে কাটবে",
                    options = vm.accounts.withMoney(),
                    selected = acc,
                    text = { "${it.name} (${tk(it.balance)})" },
                ) { acc = it }
            }
        },
        // বাটন: পরিশোধ, বাতিল, মুছুন (ভুল বা পুরনো বাকি খরচ মুছে ফেলার জন্য)
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    val a = amount.toDoubleOrNull()
                    val ac = acc
                    when {
                        a == null || a <= 0 -> ctx.toast("সঠিক পরিমাণ দিন")
                        a > debt.remaining -> ctx.toast("বাকির চেয়ে বেশি দেওয়া যাবে না")
                        ac == null -> ctx.toast("অ্যাকাউন্ট সিলেক্ট করুন")
                        else -> {
                            vm.pay(debt, a, ac.id)
                            onDone()
                        }
                    }
                }) { Text("পরিশোধ") }
                TextButton(onClick = onDone) { Text("বাতিল") }
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
            text = { Text("এই বাকি খরচ মাসের খরচ থেকে বাদ যাবে এবং এর আগের পরিশোধের লেনদেনও মুছে অ্যাকাউন্টের ব্যালেন্স আগের অবস্থায় ফিরবে।") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteDebt(debt.id)
                    onDone()
                }) { Text("মুছুন", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("বাতিল") } },
        )
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
        // আইকন ও লেখা কার্ডের মাঝখানে
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(if (big) 40.dp else 30.dp))
            Spacer(Modifier.width(14.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontSize = 14.sp, textAlign = TextAlign.Center)
                Text(
                    tk(value),
                    fontSize = if (big) 28.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

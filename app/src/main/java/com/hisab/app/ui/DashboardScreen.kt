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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisab.app.HisabViewModel
import com.hisab.app.data.Kind

/** কোন কার্ডে ট্যাপ করা হয়েছে */
private enum class Detail(val title: String) {
    CASH("ক্যাশ"),
    LOAN("মোট লোন"),
    LENT("মোট পাওনা"),
    SHOP("দোকান বাকি"),
    DPS("ডিপিএস মোট"),
    ASSET("জমি/সম্পদ"),
    EXPENSE("এই মাসের খরচ"),
}

/** হোম স্ক্রিন: সারসংক্ষেপ কার্ড। কার্ডে ট্যাপ করলে বিস্তারিত তালিকা (শুধু দেখা যায়) */
@Composable
fun DashboardScreen(vm: HisabViewModel) {
    val s = vm.summary
    var detail by remember { mutableStateOf<Detail?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SummaryCard("ক্যাশ", s.cash, Color(0xFF00897B), Icons.Default.Payments, big = true) { detail = Detail.CASH } }
        item { SummaryCard("মোট লোন", s.loan, Color(0xFFE53935), Icons.Default.TrendingDown) { detail = Detail.LOAN } }
        item { SummaryCard("মোট পাওনা", s.lent, Color(0xFF43A047), Icons.Default.TrendingUp) { detail = Detail.LENT } }
        item { SummaryCard("দোকান বাকি", s.shop, Color(0xFFFB8C00), Icons.Default.Store) { detail = Detail.SHOP } }
        item { SummaryCard("ডিপিএস মোট", s.dps, Color(0xFF3949AB), Icons.Default.Savings) { detail = Detail.DPS } }
        item { SummaryCard("জমি/সম্পদ", s.asset, Color(0xFF8D6E63), Icons.Default.Landscape) { detail = Detail.ASSET } }
        item { SummaryCard("এই মাসের খরচ", s.monthExpense, Color(0xFF8E24AA), Icons.Default.ShoppingCart) { detail = Detail.EXPENSE } }
    }

    detail?.let { d ->
        DetailDialog(d.title, rowsFor(d, vm)) { detail = null }
    }
}

/** কার্ড অনুযায়ী তালিকার সারি (নাম, পরিমাণ) */
private fun rowsFor(d: Detail, vm: HisabViewModel): List<Pair<String, Double>> {
    fun accounts(vararg types: String) =
        vm.accounts.filter { it.type in types }.map { it.name to it.balance }

    // একই নামের একাধিক হিসাব যোগ করে মোট দেখায়
    fun owed(kind: String) = vm.debts[kind].orEmpty()
        .filter { it.remaining > 0 }
        .groupBy { it.person }
        .map { (person, list) -> person to list.sumOf { it.remaining } }
        .sortedByDescending { it.second }

    return when (d) {
        Detail.CASH -> accounts("cash", "mobile", "bank")
        Detail.LOAN -> owed(Kind.LOAN)
        Detail.LENT -> owed(Kind.LENT)
        Detail.SHOP -> owed(Kind.SHOP)
        Detail.DPS -> accounts("dps")
        Detail.ASSET -> accounts("asset")
        Detail.EXPENSE -> vm.monthExpenses.map { "${it.category} • ${it.date.take(10)}" to it.amount }
    }
}

/** শুধু দেখার জন্য তালিকা: কোনো এডিট নেই */
@Composable
private fun DetailDialog(title: String, rows: List<Pair<String, Double>>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    "মোট: ${tk(rows.sumOf { it.second })}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                if (rows.isEmpty()) {
                    Text("কোনো তথ্য নেই")
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(rows) { (name, value) ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(name, modifier = Modifier.weight(1f))
                                Text(tk(value), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("বন্ধ করুন") } },
    )
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

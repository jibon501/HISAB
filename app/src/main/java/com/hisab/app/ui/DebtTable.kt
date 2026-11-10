package com.hisab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisab.app.data.HistoryItem
import com.hisab.app.data.debtTotal
import com.hisab.app.data.fmtDate

/**
 * একজনের ধার-হিসাব: নাম মাঝখানে, তিন কলাম (তারিখ | ধরন: দিছি/নিছি/ফেরত | পরিমাণ),
 * আর পরিমাণ কলামের নিচে "মোট=..."।
 */
@Composable
fun DebtTable(person: String, kind: String, entries: List<HistoryItem>) {
    val total = entries.sumOf { if (it.payment) -it.amount else it.amount }
    val dateW = 1.05f
    val typeW = 1.1f
    val amountW = 1f
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            person,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        HorizontalDivider()
        Row(Modifier.fillMaxWidth()) {
            Text("তারিখ", Modifier.weight(dateW), fontSize = 12.sp, color = Color.Gray)
            Text("ধরন", Modifier.weight(typeW), fontSize = 12.sp, color = Color.Gray)
            Text("পরিমাণ", Modifier.weight(amountW), fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.End)
        }
        entries.forEach { h ->
            // ফেরত/পরিশোধের সারিতে তারিখ, ধরন ও পরিমাণ একই (সবুজ) রঙে
            val c = if (h.payment) Color(0xFF2E7D32) else Color.Unspecified
            Row(Modifier.fillMaxWidth()) {
                Text(fmtDate(h.date), Modifier.weight(dateW), fontSize = 13.sp, color = c)
                Text(h.label, Modifier.weight(typeW), fontSize = 13.sp, color = c)
                Text(
                    tk(h.amount),
                    Modifier.weight(amountW),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    color = c,
                )
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            // ফেরত বেশি হলে অতিরিক্তটা উল্টো দিকের লেবেলে (যেমন "আমার কাছে পাবে")
            val (label, amount) = debtTotal(kind, total)
            Text("$label=${tk(amount)}", fontWeight = FontWeight.Bold)
        }
    }
}

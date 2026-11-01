package com.hisab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisab.app.HisabViewModel

/** হোম স্ক্রিন: সব হিসাবের সারসংক্ষেপ */
@Composable
fun DashboardScreen(vm: HisabViewModel) {
    val s = vm.summary
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "আমার হিসাব",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp),
            )
        }
        item { SummaryCard("ক্যাশ ইন হ্যান্ড (ক্যাশ + বিকাশ/নগদ + ব্যাংক)", s.cash, Color(0xFF00897B), Icons.Default.Payments, big = true) }
        item { SummaryCard("মোট লোন (আমি দেনাদার)", s.loan, Color(0xFFE53935), Icons.Default.TrendingDown) }
        item { SummaryCard("মোট পাওনা (মানুষের কাছে)", s.lent, Color(0xFF43A047), Icons.Default.TrendingUp) }
        item { SummaryCard("দোকান বাকি", s.shop, Color(0xFFFB8C00), Icons.Default.Store) }
        item { SummaryCard("ডিপিএস মোট", s.dps, Color(0xFF3949AB), Icons.Default.Savings) }
        item { SummaryCard("জমি/সম্পদ", s.asset, Color(0xFF8D6E63), Icons.Default.Landscape) }
        item { SummaryCard("এই মাসের খরচ", s.monthExpense, Color(0xFF8E24AA), Icons.Default.ShoppingCart) }
    }
}

@Composable
private fun SummaryCard(title: String, value: Double, color: Color, icon: ImageVector, big: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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

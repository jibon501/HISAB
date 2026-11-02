package com.hisab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hisab.app.data.Account

/** অ্যাকাউন্টের আইকন: প্রতিটির নিজস্ব ধরন ও রঙ আছে */
data class AccIcon(val key: String, val vector: ImageVector, val color: Color)

val ACC_ICONS = listOf(
    AccIcon("cash", Icons.Default.Payments, Color(0xFF2E7D32)),
    AccIcon("phone", Icons.Default.PhoneAndroid, Color(0xFFD81B60)),
    AccIcon("phone2", Icons.Default.PhoneIphone, Color(0xFFEF6C00)),
    AccIcon("bank", Icons.Default.AccountBalance, Color(0xFF1565C0)),
    AccIcon("bank2", Icons.Default.AccountBalance, Color(0xFF00838F)),
    AccIcon("card", Icons.Default.CreditCard, Color(0xFF6A1B9A)),
    AccIcon("wallet", Icons.Default.AccountBalanceWallet, Color(0xFF546E7A)),
    AccIcon("savings", Icons.Default.Savings, Color(0xFF3949AB)),
    AccIcon("savings2", Icons.Default.Savings, Color(0xFF00897B)),
    AccIcon("land", Icons.Default.Landscape, Color(0xFF6D4C41)),
    AccIcon("home", Icons.Default.Home, Color(0xFFF9A825)),
)

/** ধরন অনুযায়ী ডিফল্ট আইকনের কী */
fun defaultIconKey(type: String): String = when (type) {
    "cash" -> "cash"
    "mobile" -> "phone"
    "bank" -> "bank"
    "dps" -> "savings"
    "asset" -> "land"
    else -> "wallet"
}

fun iconByKey(key: String, type: String): AccIcon =
    ACC_ICONS.firstOrNull { it.key == key }
        ?: ACC_ICONS.first { it.key == defaultIconKey(type) }

fun iconFor(a: Account): AccIcon = iconByKey(a.icon, a.type)

/** রঙিন গোল আইকন ব্যাজ */
@Composable
fun AccountBadge(ic: AccIcon, size: Dp = 40.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(ic.color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(ic.vector, contentDescription = null, tint = ic.color)
    }
}

/** আইকন বাছাই করার সারি (আড়াআড়ি স্ক্রল হয়) */
@Composable
fun IconPicker(selectedKey: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ACC_ICONS.forEach { ic ->
            val selected = ic.key == selectedKey
            val ring = if (selected) Modifier.border(2.dp, ic.color, CircleShape) else Modifier
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ic.color.copy(alpha = if (selected) 0.30f else 0.12f))
                    .then(ring)
                    .clickable { onSelect(ic.key) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(ic.vector, contentDescription = null, tint = ic.color)
            }
        }
    }
}

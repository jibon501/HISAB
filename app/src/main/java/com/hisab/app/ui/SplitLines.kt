package com.hisab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hisab.app.data.Account

/** একটি লাইন: কোন অ্যাকাউন্ট (খালি থাকতে পারে) ও কত টাকা */
data class SplitLine(val acc: Account? = null, val amount: String = "")

/** টাকা দেওয়া লাইনগুলো (অ্যাকাউন্ট, পরিমাণ); খালি বা ০ টাকার লাইন বাদ */
fun List<SplitLine>.splits(): List<Pair<Account?, Double>> =
    mapNotNull { l -> l.amount.toDoubleOrNull()?.takeIf { it > 0 }?.let { l.acc to it } }

/**
 * একাধিক অ্যাকাউন্টে টাকা ভাগ করার ঘর: প্রতিটি লাইনে অ্যাকাউন্ট ও টাকা।
 * "+ আরেকটি অ্যাকাউন্ট" চাপলে নতুন লাইন আসে; একটির বেশি থাকলে মোট দেখায়।
 */
@Composable
fun SplitLines(
    accounts: List<Account>,
    lines: List<SplitLine>,
    onChange: (List<SplitLine>) -> Unit,
    accountLabel: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        lines.forEachIndexed { i, line ->
            // একই অ্যাকাউন্ট আরেক লাইনে বাছা থাকলে এখানে দেখানো হয় না
            val options = accounts.filter { a -> a.id == line.acc?.id || lines.none { it.acc?.id == a.id } }
            Picker(
                label = if (lines.size > 1) "$accountLabel (${i + 1})" else accountLabel,
                options = options,
                selected = line.acc,
                text = { "${it.name} (${tk(it.balance)})" },
            ) { sel -> onChange(lines.toMutableList().also { it[i] = line.copy(acc = sel) }) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    MoneyField(
                        value = line.amount,
                        onChange = { v -> onChange(lines.toMutableList().also { it[i] = line.copy(amount = v) }) },
                        label = "টাকার পরিমাণ",
                    )
                }
                if (lines.size > 1) {
                    IconButton(onClick = { onChange(lines.toMutableList().also { it.removeAt(i) }) }) {
                        Icon(Icons.Default.Close, contentDescription = "বাদ দিন")
                    }
                }
            }
        }
        TextButton(onClick = { onChange(lines + SplitLine()) }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("আরেকটি অ্যাকাউন্ট")
        }
        if (lines.size > 1) {
            Text(
                "মোট: ${tk(lines.sumOf { it.amount.toDoubleOrNull() ?: 0.0 })}",
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

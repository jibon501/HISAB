package com.hisab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * খাতের ঘর: লিখলে সাজেশন আসে, ট্যাপ করলে বসে যায়।
 * তালিকায় না থাকলে লেখাটাই নতুন খাত হিসেবে সেভ হবে।
 */
@Composable
fun CategoryField(options: List<String>, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextInput(value, onChange, "খাত")
        val q = value.trim()
        val shown = options
            .filter { q.isEmpty() || it.contains(q, ignoreCase = true) }
            .filter { !it.equals(q, ignoreCase = true) }
            .sortedByDescending { it.startsWith(q, ignoreCase = true) }
            .take(if (q.isEmpty()) 9 else 5)
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
                                .clickable { onChange(n) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
        if (q.isNotEmpty() && options.none { it.equals(q, ignoreCase = true) }) {
            Text(
                "নতুন খাত হিসেবে সেভ হবে",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

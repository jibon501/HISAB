package com.hisab.app.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

// ---------- ধ্রুবক ----------
val SYSTEM_CATS = listOf("ট্রান্সফার", "লোন গ্রহণ", "ধার প্রদান", "লোন পরিশোধ", "ধার ফেরত", "দোকান বাকি পরিশোধ")

/** অ্যাকাউন্টের ধরন (তালিকার ক্রম: উপর থেকে নিচে)। "other" শুধু আগের অ্যাকাউন্টের জন্য, নতুন করে বেছে নেওয়া যায় না */
val TYPE_LABELS = linkedMapOf(
    "asset" to "সম্পদ",
    "dps" to "ডিপিএস",
    "bank" to "ব্যাংক",
    "mobile" to "মোবাইল",
    "cash" to "নগদ",
    "other" to "অন্যান্য",
)

/** টাকা বেরিয়ে যাওয়ার কাজে শুধু যে অ্যাকাউন্টে টাকা আছে (ব্যালেন্স ০ বা কম হলে বাদ) */
fun List<com.hisab.app.data.Account>.withMoney() = filter { it.balance > 0.0 }

/** নিজে যোগ করা ধরনের নামই ধরনের মান; বাকি ধরনের জন্য উপরের তালিকার নাম */
fun typeLabel(type: String): String = TYPE_LABELS[type] ?: type

/** ড্যাশবোর্ডে ক্যাশ ও ডিপিএস কার্ডের বাইরের সব ধরন "সম্পদ" কার্ডে যায় */
val ASSET_EXCLUDED_TYPES = setOf("cash", "mobile", "bank", "dps")

private val BN: Locale = Locale.forLanguageTag("bn-BD")

/** টাকার ফরম্যাট, যেমন ৳ ১,২৫০ */
fun tk(v: Double): String = "৳ " + String.format(BN, "%,.0f", v)

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

// ---------- কমন UI ----------

/** ফর্ম ডায়ালগ: ভেতরের কনটেন্ট স্ক্রল করা যায় */
@Composable
fun FormDialog(
    title: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}

/** ড্রপডাউন সিলেক্টর */
@Composable
fun <T> Picker(
    label: String,
    options: List<T>,
    selected: T?,
    text: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected?.let(text) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // টেক্সটফিল্ডের উপর অদৃশ্য ক্লিক এলাকা
        Box(Modifier.matchParentSize().clickable { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(text(o)) },
                    onClick = {
                        onSelect(o)
                        open = false
                    },
                )
            }
        }
    }
}

/** ডিফল্ট লেবেলসহ: MoneyField(x) { x = it } */
@Composable
fun MoneyField(value: String, onChange: (String) -> Unit) {
    MoneyField(value, onChange, "টাকার পরিমাণ")
}

/** কাস্টম লেবেলসহ: MoneyField(x, { x = it }, "লেবেল") */
@Composable
fun MoneyField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { s -> if (s.all { it.isDigit() || it == '.' }) onChange(s) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun TextInput(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

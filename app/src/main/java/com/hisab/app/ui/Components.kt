package com.hisab.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
val SYSTEM_CATS = listOf("ট্রান্সফার", "ধার নিছি", "ধার দিছি", "লোন পরিশোধ", "ধার ফেরত", "দোকান বাকি পরিশোধ")

/** অ্যাকাউন্টের ধরন (তালিকার ক্রম: উপর থেকে নিচে)। "other" শুধু আগের অ্যাকাউন্টের জন্য, নতুন করে বেছে নেওয়া যায় না */
val TYPE_LABELS = linkedMapOf(
    "asset" to "সম্পদ",
    "dps" to "ডিপিএস",
    "bank" to "ব্যাংক",
    "mobile" to "মোবাইল",
    "cash" to "নগদ",
    "other" to "অন্যান্য",
)

/** টাকা বেরিয়ে যাওয়ার কাজে শুধু যে অ্যাকাউন্টে টাকা আছে (ব্যালেন্স ০ বা কম হলে বাদ; "অন্যান্য" ছাড়া) */
fun List<com.hisab.app.data.Account>.withMoney() =
    filter { it.balance > 0.0 || it.name == com.hisab.app.data.OTHER_ACCOUNT } // "অন্যান্য" সবসময় বাছা যায়

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
    centerTitle: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            if (centerTitle) Text(title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            else Text(title)
        },
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

/**
 * বক্স: লেখা (label) বক্সের উপরের দাগের ওপর বসে, ভেতরটা ফাঁকা থাকে (লেখা সবসময় দাগের ওপরেই থাকে)।
 * ফোকাস থাকলে দাগ ও লেখা প্রাইমারি রঙের হয়।
 */
@Composable
private fun BorderLabelBox(
    label: String,
    focused: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(modifier.fillMaxWidth().padding(top = 8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(if (focused) 2.dp else 1.dp, color, RoundedCornerShape(4.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
            content = content,
        )
        Text(
            label,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 12.dp, end = 12.dp)
                .offset(y = (-8).dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 4.dp),
        )
    }
}

/** ড্রপডাউন সিলেক্টর */
@Composable
fun <T> Picker(
    label: String,
    options: List<T>,
    selected: T?,
    text: (T) -> String,
    fieldText: (T) -> String = text, // বন্ধ ঘরে যা দেখাবে (তালিকায় text দেখায়)
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        BorderLabelBox(label = label, focused = open) {
            Row(
                // এন্টার চাপলে কার্সার এই ঘর বাদ দিয়ে পরের লেখার ঘরে যায়
                Modifier.fillMaxWidth().focusProperties { canFocus = false }.clickable { open = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    selected?.let(fieldText) ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
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
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    BorderLabelBox(label = label, focused = focused) {
        BasicTextField(
            value = value,
            onValueChange = { s -> if (s.all { it.isDigit() || it == '.' }) onChange(s) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { moveNext(focus) }),
            modifier = Modifier.fillMaxSize().onFocusChanged { focused = it.isFocused },
            // বক্সের যেকোনো জায়গায় চাপলে লেখার ঘরে কার্সার আসে
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() }
            },
        )
    }
}

/** এন্টার চাপলে পরের ঘরে যায়; পরের ঘর না থাকলে কিবোর্ড বন্ধ হয় */
private fun moveNext(focus: FocusManager) {
    if (!focus.moveFocus(FocusDirection.Down)) focus.clearFocus()
}

@Composable
fun TextInput(value: String, onChange: (String) -> Unit, label: String) {
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    BorderLabelBox(label = label, focused = focused) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { moveNext(focus) }),
            modifier = Modifier.fillMaxSize().onFocusChanged { focused = it.isFocused },
            // বক্সের যেকোনো জায়গায় চাপলে লেখার ঘরে কার্সার আসে
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() }
            },
        )
    }
}

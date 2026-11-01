package com.hisab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hisab.app.data.Category

/** নাম লেখার ছোট ডায়ালগের অবস্থা: id == null হলে নতুন খাত */
private data class NameInput(val id: Long?, val kind: String, val text: String)

/** খাত যোগ, নাম বদল ও মোছার ডায়ালগ */
@Composable
fun CategoriesDialog(
    categories: List<Category>,
    onAdd: (name: String, kind: String) -> Unit,
    onRename: (id: Long, name: String) -> Unit,
    onDelete: (id: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf<NameInput?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("খাত") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                listOf("expense" to "খরচের খাত", "income" to "আয়ের খাত").forEach { (kind, label) ->
                    Text(
                        label,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    categories.filter { it.kind == kind }.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(c.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { input = NameInput(c.id, kind, c.name) }) {
                                Icon(Icons.Default.Edit, contentDescription = "এডিট")
                            }
                            IconButton(onClick = { deleting = c }) {
                                Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    TextButton(onClick = { input = NameInput(null, kind, "") }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(if (kind == "expense") "নতুন খরচের খাত" else "নতুন আয়ের খাত")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("বন্ধ করুন") } },
    )

    input?.let { inp ->
        FormDialog(
            title = if (inp.id == null) "নতুন খাত" else "খাতের নাম বদল",
            confirmText = "সংরক্ষণ",
            onDismiss = { input = null },
            onConfirm = {
                val n = inp.text.trim()
                when {
                    n.isEmpty() -> ctx.toast("নাম দিন")
                    categories.any { it.kind == inp.kind && it.name == n && it.id != inp.id } ->
                        ctx.toast("এই নামের খাত আগেই আছে")
                    else -> {
                        if (inp.id == null) onAdd(n, inp.kind) else onRename(inp.id, n)
                        input = null
                    }
                }
            },
        ) {
            TextInput(inp.text, { input = inp.copy(text = it) }, "খাতের নাম")
        }
    }

    deleting?.let { c ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("\"${c.name}\" মুছবেন?") },
            text = { Text("আগের লেনদেন যেমন আছে তেমনই থাকবে; শুধু নতুন লেনদেনে এই খাত আর আসবে না।") },
            confirmButton = {
                TextButton(onClick = { onDelete(c.id); deleting = null }) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("বাতিল") } },
        )
    }
}

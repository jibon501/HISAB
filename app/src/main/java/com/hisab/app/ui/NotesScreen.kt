@file:OptIn(ExperimentalMaterial3Api::class)

package com.hisab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisab.app.HisabViewModel
import com.hisab.app.data.Note
import com.hisab.app.data.fmtDate

/** নোট ট্যাব: লেখা সংরক্ষণ, এডিট ও ডিলিট। ভাসমান "+" নতুন নোট, নোটে চাপলে এডিট */
@Composable
fun NotesScreen(vm: HisabViewModel) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Default.Add, contentDescription = "নতুন নোট")
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (vm.notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("কোনো নোট নেই") }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    items(vm.notes, key = { it.id }) { n ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { editing = n },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            ),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(n.text, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                Text(fmtDate(n.date), fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        NoteDialog(
            initial = "",
            isNew = true,
            onDismiss = { adding = false },
            onSave = { vm.addNote(it); adding = false },
            onDelete = {},
        )
    }
    editing?.let { n ->
        NoteDialog(
            initial = n.text,
            isNew = false,
            onDismiss = { editing = null },
            onSave = { vm.updateNote(n.id, it); editing = null },
            onDelete = { vm.deleteNote(n.id); editing = null },
        )
    }
}

/** নোট লেখা/এডিটের ডায়ালগ: সংরক্ষণ, (পুরনো নোটে) মুছুন, বাতিল */
@Composable
private fun NoteDialog(
    initial: String,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf(initial) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "নতুন নোট" else "নোট") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 10,
                label = { Text("লেখা") },
            )
        },
        confirmButton = {
            androidx.compose.foundation.layout.Row {
                TextButton(onClick = {
                    val t = text.trim()
                    if (t.isEmpty()) ctx.toast("কিছু লিখুন") else onSave(t)
                }) { Text("সংরক্ষণ") }
                if (!isNew) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("মুছুন", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("নোট মুছবেন?") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("বাতিল") } },
        )
    }
}

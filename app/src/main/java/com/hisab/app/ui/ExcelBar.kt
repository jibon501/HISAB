package com.hisab.app.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisab.app.HisabViewModel

/**
 * অ্যাকাউন্ট স্ক্রিনের উপরের দুটি বাটন: সব ডেটা xlsx-এ এক্সপোর্ট ও ইম্পোর্ট।
 * এক্সপোর্টে ফাইল সরাসরি Documents/আমার হিসাব ফোল্ডারে সেভ হয় (ফাইল ম্যানেজার খোলে না);
 * ইম্পোর্টে ওই ফোল্ডারের ব্যাকআপের তালিকা অ্যাপেই দেখায়।
 */
@Composable
fun ExcelBar(vm: HisabViewModel, onTransfer: () -> Unit) {
    val ctx = LocalContext.current
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var picking by remember { mutableStateOf(false) } // ব্যাকআপের তালিকা খোলা আছে কি না
    var backups by remember { mutableStateOf<List<BackupStore.Item>>(emptyList()) }

    fun doExport() {
        val out = BackupStore.openForWrite(ctx)
        if (out == null) {
            ctx.toast("ব্যাকআপ ফোল্ডারে ফাইল বানানো গেল না")
        } else {
            vm.exportXlsx(out) { ok ->
                ctx.toast(if (ok) "Documents/${BackupStore.FOLDER} ফোল্ডারে সেভ হয়েছে" else "এক্সপোর্ট করা গেল না")
            }
        }
    }
    fun openList() {
        backups = BackupStore.list(ctx)
        picking = true
    }

    // অ্যান্ড্রয়েড ৯ ও পুরনো ফোনে অনুমতি চাওয়া হয়; নতুন ফোনে লাগে না
    var afterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val next = afterPermission
        afterPermission = null
        if (granted) next?.invoke() else ctx.toast("স্টোরেজ অনুমতি ছাড়া ব্যাকআপ ফোল্ডার ব্যবহার করা যাবে না")
    }
    fun withPermission(action: () -> Unit) {
        if (BackupStore.needsPermission(ctx)) {
            afterPermission = action
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            action()
        }
    }

    // ফোল্ডারে না পেলে হাতে বেছে নেওয়ার বিকল্প (যেমন অ্যাপ নতুন করে ইনস্টলের পর পুরনো ফাইল না দেখালে)
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> if (uri != null) pendingImport = uri }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = { withPermission { doExport() } },
            modifier = Modifier.weight(1f),
        ) { Text("এক্সপোর্ট") }
        // এক্সপোর্ট ও ইম্পোর্টের মাঝখানে ট্রান্সফার বাটন
        FilledTonalIconButton(onClick = onTransfer) {
            Icon(Icons.Default.CompareArrows, contentDescription = "ট্রান্সফার")
        }
        OutlinedButton(
            onClick = { withPermission { openList() } },
            modifier = Modifier.weight(1f),
        ) { Text("ইম্পোর্ট") }
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("ব্যাকআপ বেছে নিন") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    Text("Documents/${BackupStore.FOLDER}", fontSize = 12.sp, color = Color.Gray)
                    if (backups.isEmpty()) {
                        Text("এই ফোল্ডারে কোনো ব্যাকআপ পাওয়া যায়নি", Modifier.padding(vertical = 16.dp))
                    }
                    backups.forEach { b ->
                        HorizontalDivider()
                        Text(
                            b.name,
                            Modifier.fillMaxWidth()
                                .clickable { picking = false; pendingImport = b.uri }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { picking = false; importLauncher.launch(arrayOf("*/*")) }) { Text("অন্য ফাইল বেছে নিন") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("বাতিল") } },
        )
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("ইম্পোর্ট করবেন?") },
            text = {
                Text(
                    "বর্তমান সব ডেটা (অ্যাকাউন্ট, লেনদেন, ধার-হিসাব, খাত) মুছে ফাইলের ডেটা বসবে। " +
                        "আগে এক্সপোর্ট করে রাখা ভালো।",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    val input = ctx.contentResolver.openInputStream(uri)
                    if (input == null) {
                        ctx.toast("ফাইল খোলা গেল না")
                    } else {
                        vm.importXlsx(input) { ok ->
                            ctx.toast(if (ok) "ইম্পোর্ট হয়েছে" else "ইম্পোর্ট হয়নি, ফাইলটি সঠিক নয়")
                        }
                    }
                }) { Text("ইম্পোর্ট") }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("বাতিল") } },
        )
    }
}

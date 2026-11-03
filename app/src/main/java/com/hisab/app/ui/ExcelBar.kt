package com.hisab.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.hisab.app.HisabViewModel
import java.time.LocalDate

private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/** অ্যাকাউন্ট স্ক্রিনের উপরের দুটি বাটন: সব ডেটা xlsx-এ এক্সপোর্ট ও ইম্পোর্ট */
@Composable
fun ExcelBar(vm: HisabViewModel) {
    val ctx = LocalContext.current
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(XLSX_MIME),
    ) { uri: Uri? ->
        if (uri != null) {
            val out = ctx.contentResolver.openOutputStream(uri)
            if (out == null) {
                ctx.toast("ফাইল খোলা গেল না")
            } else {
                vm.exportXlsx(out) { ok -> ctx.toast(if (ok) "এক্সপোর্ট হয়েছে" else "এক্সপোর্ট করা গেল না") }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> if (uri != null) pendingImport = uri }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = { exportLauncher.launch("hisab-backup-${LocalDate.now()}.xlsx") },
            modifier = Modifier.weight(1f),
        ) { Text("এক্সপোর্ট") }
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.weight(1f),
        ) { Text("ইম্পোর্ট") }
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

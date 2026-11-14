@file:OptIn(ExperimentalMaterial3Api::class)

package com.hisab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import com.hisab.app.data.fmtDate
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hisab.app.HisabViewModel
import com.hisab.app.data.Kind
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.PictureAsPdf
import com.hisab.app.data.Account
import com.hisab.app.data.Txn

private val GREEN = Color(0xFF2E7D32)
private val RED = Color(0xFFC62828)

/** আয়-ব্যয়ের তালিকা, খাত অনুযায়ী ফিল্টার ও ট্রান্সফার */
@Composable
fun TransactionsScreen(vm: HisabViewModel, addTick: Int = 0) {
    var showAdd by remember { mutableStateOf(false) }
    // উপরের "নতুন লেনদেন" বাটন চাপলে ফর্ম খোলে
    LaunchedEffect(addTick) { if (addTick > 0) showAdd = true }
    var menu by remember { mutableStateOf(false) }
    var showCats by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Txn?>(null) }
    var showPdf by remember { mutableStateOf(false) }
    // PDF-এর জন্য বাছা খাত ও মাস (ফাইল বাছাইয়ের পর কাজে লাগে)
    var pdfOpts by remember { mutableStateOf<Pair<String?, String?>>(null to null) }
    val ctx = LocalContext.current
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri: Uri? ->
        if (uri != null) {
            val (cat, month) = pdfOpts
            val list = vm.txnsFor(cat).filter { month == null || it.date.startsWith(month) }
            val sub = "খাত: ${cat ?: "সব"} • মাস: ${month?.let { monthLabel(it) } ?: "সব"}"
            val ok = runCatching {
                ctx.contentResolver.openOutputStream(uri)!!.use { TxnPdf.write(it, "লগবুক", sub, list) }
            }.isSuccess
            ctx.toast(if (ok) "PDF এক্সপোর্ট হয়েছে" else "PDF করা গেল না")
        }
    }

    Scaffold(
        // উপরের স্ট্যাটাস বারের জায়গা আলাদা করে যোগ করা হয় না (নইলে দ্বিগুণ ফাঁকা হয়)
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(vm.filter ?: "") },
                actions = {
                    // PDF: স্ক্রিনে যেভাবে দেখায় সেভাবে; খাত ও মাস অনুযায়ী
                    IconButton(onClick = { showPdf = true }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF এক্সপোর্ট")
                    }
                    IconButton(onClick = { showCats = true }) {
                        Icon(Icons.Default.Category, contentDescription = "খাত")
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "খাত ফিল্টার",
                                tint = if (vm.filter != null) Color(0xFFFB8C00) else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("সব খাত") },
                                onClick = { vm.applyFilter(null); menu = false },
                            )
                            vm.usedCategories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c) },
                                    onClick = { vm.applyFilter(c); menu = false },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "নতুন লেনদেন")
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (vm.txns.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কোনো লেনদেন নেই")
                }
            } else {
                // নিচে একটু ফাঁকা রাখা হয়েছে, যাতে শেষ সারি (+) বাটন ও মোটের নিচে ঢাকা না পড়ে
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    itemsIndexed(vm.txns, key = { _, t -> t.id }) { i, t -> TxnRow(t, i) { toDelete = t } }
                }
            }
            // স্ক্রিনে যা দেখা যাচ্ছে তার নিট মোট (জমা − খরচ): নিচে বামে, ছোট ও ধূসর
            val net = vm.txns.sumOf { if (it.type == "credit") it.amount else -it.amount }
            Text(
                "মোট: ${tk(net)}",
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp),
                fontSize = 12.sp,
                color = Color.Gray,
            )
        }
    }

    if (showAdd) {
        AddTxnDialog(
            accounts = vm.accounts,
            expenseCats = vm.categories.filter { it.kind == "expense" }.map { it.name },
            incomeCats = vm.categories.filter { it.kind == "income" }.map { it.name },
            // আগে সেভ করা ধারের নাম (সাম্প্রতিক আগে), সাজেশনের জন্য
            personSuggestions = vm.debts.values.flatten().sortedByDescending { it.id }.map { it.person }.distinct(),
            onDismiss = { showAdd = false },
            onSave = { n ->
                when (n.type) {
                    "lent" -> vm.addDebt(Kind.LENT, n.person, n.note, n.splits, n.date)
                    "loan" -> vm.addDebt(Kind.LOAN, n.person, n.note, n.splits, n.date)
                    else -> {
                        // তালিকায় না থাকা নতুন খাত লিখলে নিজে থেকে সেভ হয়ে যায়
                        vm.addCategory(n.category, if (n.type == "debit") "expense" else "income")
                        if (n.baki) vm.addBakiExpense(n.splits.sumOf { it.second }, n.category, n.note, n.date)
                        else vm.addTxns(n.type, n.splits, n.category, n.note, n.date)
                    }
                }
                showAdd = false
            },
        )
    }
    if (showPdf) {
        PdfDialog(
            vm = vm,
            onDismiss = { showPdf = false },
            onExport = { cat, month ->
                pdfOpts = cat to month
                showPdf = false
                pdfLauncher.launch("logbook-${LocalDate.now()}.pdf")
            },
        )
    }
    if (showCats) {
        CategoriesDialog(
            categories = vm.categories,
            onAdd = { name, kind -> vm.addCategory(name, kind) },
            onRename = { id, name -> vm.renameCategory(id, name) },
            onDelete = { id -> vm.deleteCategory(id) },
            onDismiss = { showCats = false },
        )
    }
    toDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("লেনদেন মুছবেন?") },
            text = { Text("${t.category} • ${tk(t.amount)}\nঅ্যাকাউন্টের ব্যালেন্স সেই অনুযায়ী ঠিক হয়ে যাবে।") },
            confirmButton = {
                TextButton(onClick = { vm.deleteTxn(t.id); toDelete = null }) { Text("মুছুন") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("বাতিল") } },
        )
    }
}

/** যে খাতের সারিতে ব্যক্তির নাম (কার থেকে নেওয়া / কাকে দেওয়া) ছোট করে দেখানো হয় */
internal val DEBT_CATS = setOf("ধার নিছি", "ধার দিছি", "লোন পরিশোধ", "ধার ফেরত", "দোকান বাকি পরিশোধ")

/** ছোট, কাছাকাছি সারি; একটা পরপর হালকা ধূসর ব্যাকগ্রাউন্ড। অ্যাকাউন্টের নাম দেখানো হয় না */
@Composable
private fun TxnRow(t: Txn, index: Int, onDelete: () -> Unit) {
    val credit = t.type == "credit"
    // ক্লিয়ার (শোধ হওয়া) লেনদেন: ধূসর ও কাটা দাগ
    val color = if (t.cleared) Color.Gray else if (credit) GREEN else RED
    val deco = if (t.cleared) TextDecoration.LineThrough else null
    val base = if (t.cleared) Color.Gray else Color.Unspecified
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (index % 2 == 1) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f) else Color.Transparent)
            .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (credit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.category, fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = base, textDecoration = deco)
                if (t.baki) {
                    Text("  বাকি", fontSize = 11.sp, lineHeight = 16.sp, color = if (t.cleared) Color.Gray else Color(0xFFFB8C00), fontWeight = FontWeight.Bold, textDecoration = deco)
                } else if (t.category in DEBT_CATS && t.note.isNotBlank()) {
                    Text("  ${t.note}", fontSize = 11.sp, lineHeight = 16.sp, color = Color.Gray, textDecoration = deco)
                }
            }
            val note = if (t.note.isNotBlank() && !t.baki && t.category !in DEBT_CATS) " • ${t.note}" else ""
            val bakiNote = if (t.baki && t.note.isNotBlank()) " • ${t.note}" else ""
            // খাতের নামের ঠিক নিচে, কোনো ফাঁকা ছাড়া
            Text("${fmtDate(t.date)}$note$bakiNote", fontSize = 11.sp, lineHeight = 12.sp, color = Color.Gray, textDecoration = deco)
        }
        Text(
            (if (credit) "+" else "-") + tk(t.amount),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textDecoration = deco,
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "মুছুন", modifier = Modifier.size(18.dp))
        }
    }
}

/** নতুন লেনদেনের ফর্ম থেকে যা সেভ হবে। type: debit (খরচ) / credit (আয়) / lent (ধার দেওয়া) / loan (ধার নেওয়া) */
private class NewTxn(
    val type: String,
    val splits: List<Pair<Long?, Double>>,
    val category: String,
    val note: String,
    val baki: Boolean,
    val date: LocalDate,
    val person: String,
)

/** চারটি ধরনের একটি বাটন: বাছা থাকলে #191970 রঙের বক্স */
@Composable
private fun TypeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val navy = Color(0xFF191970)
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .height(42.dp)
            .clip(shape)
            .background(if (selected) navy else Color.Transparent)
            .border(1.dp, if (selected) navy else MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 12.sp,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AddTxnDialog(
    accounts: List<Account>,
    expenseCats: List<String>,
    incomeCats: List<String>,
    personSuggestions: List<String>,
    onDismiss: () -> Unit,
    onSave: (NewTxn) -> Unit,
) {
    val ctx = LocalContext.current
    var type by remember { mutableStateOf("debit") }
    var lines by remember { mutableStateOf(listOf(SplitLine())) }
    var cat by remember { mutableStateOf("") }
    var person by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var baki by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var pickDate by remember { mutableStateOf(false) }

    val isDebt = type == "lent" || type == "loan"

    // ধরন বদলালে অ্যাকাউন্ট বাছাই ও খাত আবার শুরু থেকে (টাকার পরিমাণ থাকে)
    fun pick(t: String) {
        type = t
        cat = ""
        if (t != "debit") baki = false
        lines = lines.map { it.copy(acc = null) }
    }

    FormDialog(
        title = "নতুন লেনদেন",
        centerTitle = true,
        confirmText = "সংরক্ষণ",
        onDismiss = onDismiss,
        onConfirm = {
            val splits = lines.splits()
            val c = cat.trim()
            val isBaki = type == "debit" && baki
            when {
                isDebt && person.isBlank() -> ctx.toast("নাম দিন")
                splits.isEmpty() -> ctx.toast("টাকার পরিমাণ দিন")
                isDebt && splits.any { it.first == null } -> ctx.toast("প্রতিটি টাকার জন্য অ্যাকাউন্ট সিলেক্ট করুন")
                !isDebt && c.isEmpty() -> ctx.toast("খাত দিন")
                // অ্যাকাউন্ট না বাছলে খরচ/আয় "অন্যান্য" অ্যাকাউন্টে যায়
                else -> onSave(
                    NewTxn(type, splits.map { it.first?.id to it.second }, c, note.trim(), isBaki, date, person.trim()),
                )
            }
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TypeButton("খরচ", type == "debit", Modifier.weight(1f)) { pick("debit") }
            TypeButton("আয়", type == "credit", Modifier.weight(1f)) { pick("credit") }
        }

        if (isDebt) {
            TextInput(person, { person = it }, if (type == "loan") "কার কাছ থেকে নিলেন" else "কাকে দিলেন")
            // নামের অক্ষর লিখলেই মিলে যাওয়া আগের নাম দেখায় (শুরুতে মিললে আগে), ট্যাপ করলে নাম বসে যায়
            val q = person.trim()
            val shown = if (q.isEmpty()) {
                emptyList()
            } else {
                personSuggestions
                    .filter { it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true) }
                    .sortedByDescending { it.startsWith(q, ignoreCase = true) }
                    .take(5)
            }
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
                                    .clickable { person = n }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }

        if (type == "debit" && baki) {
            // বাকি খরচে অ্যাকাউন্ট থেকে টাকা কাটে না, তাই শুধু পরিমাণ
            MoneyField(lines.first().amount) { v -> lines = listOf(lines.first().copy(acc = null, amount = v)) }
        } else {
            SplitLines(
                // টাকা বেরিয়ে গেলে (খরচ / ধার দেওয়া) ০ ব্যালেন্সের অ্যাকাউন্ট দেখানো হয় না
                accounts = if (type == "debit" || type == "lent") accounts.withMoney() else accounts,
                lines = lines,
                onChange = { lines = it },
                accountLabel = when (type) {
                    "debit", "lent" -> "অ্যাকাউন্ট থেকে কাটবে"
                    else -> "অ্যাকাউন্টে জমা হবে"
                },
            )
        }

        if (!isDebt) {
            CategoryField(if (type == "debit") expenseCats else incomeCats, cat) { cat = it }
        }
        if (type == "debit") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("বাকি (এখনো টাকা দিইনি)")
                Switch(checked = baki, onCheckedChange = { baki = it })
            }
        }
        TextInput(note, { note = it }, "নোট")
        // সবার নিচে: আজকের তারিখ দেখায়, চাপলে আগের তারিখ বাছা যায়
        OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
            Text("তারিখ: ${fmtDate(date.toString())}")
        }
    }

    if (pickDate) {
        // আজকের পরের তারিখ বাছা যায় না
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtc
                override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    pickDate = false
                }) { Text("ঠিক আছে") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("বাতিল") } },
        ) {
            DatePicker(state = state)
        }
    }
}

/** একটি লাইনের তারিখ ও টাকার পরিমাণ এডিট (আজকের পরের তারিখ বাছা যায় না); ধার-খাতা থেকে ব্যবহার হয় */
@Composable
internal fun EditEntryDialog(
    title: String,
    initialAmount: Double,
    initialDate: String,
    onDismiss: () -> Unit,
    accounts: List<Account> = emptyList(),
    initialAccountId: Long? = null, // null হলে (একাধিক ভাগ) অ্যাকাউন্ট বদলানো যায় না
    accountLabel: String = "অ্যাকাউন্ট",
    onSave: (amount: Double, date: LocalDate, accountId: Long?) -> Unit,
) {
    val ctx = LocalContext.current
    var account by remember(initialAccountId) { mutableStateOf(accounts.firstOrNull { it.id == initialAccountId }) }
    var amount by remember(initialAmount, initialDate) {
        mutableStateOf(initialAmount.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() })
    }
    var date by remember(initialAmount, initialDate) {
        mutableStateOf(runCatching { LocalDate.parse(initialDate.take(10)) }.getOrDefault(LocalDate.now()))
    }
    var pickDate by remember { mutableStateOf(false) }

    FormDialog(
        title = title,
        centerTitle = true,
        confirmText = "সংরক্ষণ",
        onDismiss = onDismiss,
        onConfirm = {
            val a = amount.toDoubleOrNull()
            if (a == null || a <= 0) ctx.toast("সঠিক পরিমাণ দিন") else onSave(a, date, account?.id)
        },
    ) {
        MoneyField(amount) { amount = it }
        if (account != null) {
            Picker(
                label = accountLabel,
                options = accounts,
                selected = account,
                text = { "${it.name} (${tk(it.balance)})" },
                fieldText = { it.name },
            ) { account = it }
        }
        OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
            Text("তারিখ: ${fmtDate(date.toString())}")
        }
    }

    if (pickDate) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtc
                override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    pickDate = false
                }) { Text("ঠিক আছে") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("বাতিল") } },
        ) {
            DatePicker(state = state)
        }
    }
}

private val MONTHS_BN = listOf(
    "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
    "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর",
)

/** "2026-11" → "নভেম্বর ২০২৬" */
private fun monthLabel(ym: String): String {
    val p = ym.split("-")
    val m = p.getOrNull(1)?.toIntOrNull() ?: return ym
    val year = p[0].map { if (it in '0'..'9') '\u09E6' + (it - '0') else it }.joinToString("")
    return "${MONTHS_BN[(m - 1).coerceIn(0, 11)]} $year"
}

/** PDF এক্সপোর্টের অপশন: খাত ও মাস (দুটোই "সব" রাখা যায়) */
@Composable
private fun PdfDialog(vm: HisabViewModel, onDismiss: () -> Unit, onExport: (category: String?, month: String?) -> Unit) {
    val allCat = "সব খাত"
    val allMonth = "সব মাস"
    val months = remember { vm.txnsFor(null).map { it.date.take(7) }.distinct().sortedDescending() }
    var cat by remember { mutableStateOf(vm.filter ?: allCat) }
    var month by remember { mutableStateOf(allMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PDF এক্সপোর্ট") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Picker(
                    label = "খাত",
                    options = listOf(allCat) + vm.usedCategories,
                    selected = cat,
                    text = { it },
                ) { cat = it }
                Picker(
                    label = "মাস",
                    options = listOf(allMonth) + months.map { monthLabel(it) },
                    selected = month,
                    text = { it },
                ) { month = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val key = months.firstOrNull { monthLabel(it) == month }
                onExport(if (cat == allCat) null else cat, key)
            }) { Text("এক্সপোর্ট") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}

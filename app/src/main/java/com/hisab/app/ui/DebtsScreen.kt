@file:OptIn(ExperimentalMaterial3Api::class)

package com.hisab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import com.hisab.app.data.debtLabel
import com.hisab.app.data.debtTotal
import com.hisab.app.data.repayLabel
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisab.app.HisabViewModel
import com.hisab.app.data.Account
import com.hisab.app.data.Debt
import com.hisab.app.data.Kind
import com.hisab.app.data.HistoryItem
import com.hisab.app.data.fmtDate

/** একই ব্যক্তির সব ধার/দেওয়া এক সারিতে (লেনদেন ট্যাবে এগুলো আলাদাই থাকে) */
private class DebtGroup(val person: String, val kind: String, val debts: List<Debt>) {
    val remaining get() = debts.sumOf { it.remaining }
    val date get() = debts.maxOf { it.date } // সবচেয়ে নতুন তারিখ

    /** ডায়ালগের জন্য একটি মিলিত হিসাব */
    fun merged() = Debt(debts.maxOf { it.id }, kind, person, debts.sumOf { it.total }, remaining, date)
}

/** একটি "ধার" ট্যাব: উপরে দিছি | নিছি বাছাই, নিচে সেটার তালিকা। ভাসমান "+" বাছা ধরনে নতুন ধার যোগ করে */
@Composable
fun DebtsScreen(vm: HisabViewModel) {
    var kind by rememberSaveable { mutableStateOf(Kind.LENT) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = kind == Kind.LENT,
                onClick = { kind = Kind.LENT },
                label = { Text("দিছি", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = kind == Kind.LOAN,
                onClick = { kind = Kind.LOAN },
                label = { Text("নিছি", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                modifier = Modifier.weight(1f),
            )
        }
        Box(Modifier.weight(1f)) { DebtsPane(vm, kind) }
    }
}

/** বাছা ধরনের (দিছি / নিছি) তালিকা। নতুন ধার যোগ হয় ভাসমান "+" থেকে */
@Composable
private fun DebtsPane(vm: HisabViewModel, kind: String) {
    var editPerson by remember(kind) { mutableStateOf<String?>(null) }
    var showAdd by remember(kind) { mutableStateOf(false) }
    val list = vm.debts[kind].orEmpty()
    // তালিকা নতুন আগে সাজানো; groupBy প্রথম আসার ক্রম রাখে
    val groups = list.groupBy { it.person }.map { (p, ds) -> DebtGroup(p, kind, ds) }

    Scaffold(
        // উপরের অংশের জায়গা বাইরের কাঠামোই ধরে, তাই এখানে আলাদা ইনসেট নেই
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // ভাসমান "+": নতুন ধার (দিছি / নিছি) যোগ
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "নতুন যোগ")
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("কোনো হিসাব নেই") }
            } else {
                // নিচে একটু ফাঁকা রাখা হয়েছে, যাতে শেষ সারি মোটের নিচে ঢাকা না পড়ে
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    itemsIndexed(groups, key = { _, g -> g.person }) { i, g ->
                        val done = g.remaining <= 0.0001
                        // এক লাইন পর পর হালকা ধূসর বক্স; বক্স লেখার চেয়ে সামান্য বড়
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    if (i % 2 == 1) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .clickable { editPerson = g.person }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // "তারিখ নাম": তারিখ ছোট ও ধূসর
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(fontSize = 11.sp, color = Color.Gray)) { append(fmtDate(g.date)) }
                                    append("  ")
                                    append(g.person)
                                },
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                            )
                            // ফেরত বেশি হলে অতিরিক্তটা উল্টো লেবেলে (কমলা রঙে)
                            val over = g.remaining < -0.0001
                            Text(
                                when {
                                    over -> debtTotal(kind, g.remaining).let { (l, a) -> "$l ${tk(a)}" }
                                    done -> "পরিশোধিত ✓"
                                    else -> tk(g.remaining)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = when {
                                    over -> Color(0xFFEF6C00)
                                    done -> Color(0xFF2E7D32)
                                    else -> Color(0xFFC62828)
                                },
                            )
                        }
                    }
                }
            }
            // স্ক্রিনে যা দেখা যাচ্ছে তার মোট বাকি: নিচে বামে, ছোট ও ধূসর
            // দিছি-তে "মোট দিয়েছি", নিছি-তে "মোট নিয়েছি"; ফেরত বেশি হওয়া ব্যক্তিদের অতিরিক্ত আলাদা লাইনে
            val owedTotal = groups.sumOf { it.remaining.coerceAtLeast(0.0) }
            val extraTotal = groups.sumOf { (-it.remaining).coerceAtLeast(0.0) }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp)) {
                Text("${debtTotal(kind, 0.0).first}: ${tk(owedTotal)}", fontSize = 12.sp, color = Color.Gray)
                if (extraTotal > 0.0001) {
                    Text("${debtTotal(kind, -1.0).first}: ${tk(extraTotal)}", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }

    if (showAdd) {
        AddDebtDialog(
            kind = kind,
            accounts = if (kind == Kind.LENT) vm.accounts.withMoney() else vm.accounts,
            personSuggestions = vm.debts.values.flatten().sortedByDescending { it.id }.map { it.person }.distinct(),
            onDismiss = { showAdd = false },
            onSave = { person, note, splits, date ->
                vm.addDebt(kind, person, note, splits, date)
                showAdd = false
            },
        )
    }

    groups.firstOrNull { it.person == editPerson }?.let { g ->
        DebtEditDialog(
            debt = g.merged(),
            entries = vm.debtHistory(g.kind, g.person),
            // ধার পরিশোধে টাকা বেরিয়ে যায়, ধার ফেরত পেলে টাকা ঢোকে
            accounts = if (g.kind == Kind.LOAN) vm.accounts.withMoney() else vm.accounts,
            onDismiss = { editPerson = null },
            onSave = { name, splits ->
                vm.saveGroupEdit(g.kind, g.person, name, g.debts, splits)
                editPerson = null
            },
            onDelete = {
                vm.deleteDebts(g.debts.map { it.id })
                editPerson = null
            },
        )
    }
}

/**
 * হিসাবে চাপলে শুধু দেখার বক্স: নাম, প্রতিটি এন্ট্রি, বাকি।
 * নিচের বাটন: "+", সেভ গ্যালারি, শেয়ার, মুছুন। "+" চাপলে ছোট ফর্ম খোলে (নাম বদল ও ফেরতের লাইন),
 * সেখানে নিচে "সংরক্ষণ" ও "বাতিল"।
 */
@Composable
private fun DebtEditDialog(
    debt: Debt,
    entries: List<HistoryItem>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (name: String, splits: List<Pair<Long, Double>>) -> Unit,
    onDelete: () -> Unit,
) {
    val ctx = LocalContext.current
    var adding by remember(debt.id) { mutableStateOf(false) }
    var confirmDelete by remember(debt.id) { mutableStateOf(false) }
    // একবার সেভ হলে শেয়ারে একই ছবি ব্যবহার হয়, দুটো কপি হয় না
    var savedUri by remember(debt.id) { mutableStateOf<android.net.Uri?>(null) }

    fun ensureSaved(): android.net.Uri? {
        savedUri?.let { return it }
        return saveToGallery(
            ctx,
            buildHistoryBitmap(debt.person, debt.kind, entries),
            "hisab_${debt.person}_${System.currentTimeMillis()}".replace(" ", "_"),
        ).also { savedUri = it }
    }

    fun saveOnly() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            ctx.toast("এই ফোনে গ্যালারিতে সেভ চলে না, শেয়ার ব্যবহার করুন")
        } else if (ensureSaved() != null) {
            ctx.toast("গ্যালারিতে সেভ হয়েছে")
        } else {
            ctx.toast("সেভ করা গেল না")
        }
    }

    fun share() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            shareText(ctx, debt.person, debt.kind, entries)
            return
        }
        val uri = ensureSaved()
        if (uri != null) shareImage(ctx, uri) else ctx.toast("শেয়ার করা গেল না")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // শুধু দেখার অবস্থা: নাম মাঝখানে, তিন কলাম, নিচে মোট
                DebtTable(debt.person, debt.kind, entries)
            }
        },
        // বাটন: + | সেভ গ্যালারি | শেয়ার | মুছুন (চারটি এক লাইনে, তাই ফাঁকা কম)
        confirmButton = {
            val pad = PaddingValues(horizontal = 8.dp)
            Row {
                TextButton(onClick = { adding = true }, contentPadding = pad) {
                    Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { saveOnly() }, contentPadding = pad) { Text("সেভ গ্যালারি", fontSize = 13.sp) }
                TextButton(onClick = { share() }, contentPadding = pad) { Text("শেয়ার", fontSize = 13.sp) }
                TextButton(onClick = { confirmDelete = true }, contentPadding = pad) {
                    Text("মুছুন", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
    )

    if (adding) {
        DebtAddScreen(
            debt = debt,
            accounts = accounts,
            onCancel = { adding = false },
            onSave = onSave,
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("\"${debt.person}\" মুছবেন?") },
            text = { Text("এই হিসাবের সব লেনদেন মুছে যাবে এবং অ্যাকাউন্টের ব্যালেন্স আগের অবস্থায় ফিরবে।") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("বাতিল") } },
        )
    }
}

/** "+" চাপলে ছোট ফর্ম ডায়ালগ: নাম বদল ও ফেরতের লাইন; নিচে "সংরক্ষণ" ও "বাতিল" */
@Composable
private fun DebtAddScreen(
    debt: Debt,
    accounts: List<Account>,
    onCancel: () -> Unit,
    onSave: (name: String, splits: List<Pair<Long, Double>>) -> Unit,
) {
    val ctx = LocalContext.current
    val lent = debt.kind == Kind.LENT
    val done = debt.remaining <= 0.0
    var name by remember(debt.id) { mutableStateOf(debt.person) }
    var lines by remember(debt.id) { mutableStateOf(listOf(SplitLine())) }

    FormDialog(
        title = repayLabel(debt.kind),
        centerTitle = true,
        confirmText = "সংরক্ষণ",
        onDismiss = onCancel,
        onConfirm = {
            val n = name.trim()
            val splits = lines.splits()
            val total = splits.sumOf { it.second }
            when {
                n.isEmpty() -> ctx.toast("নাম খালি রাখা যাবে না")
                splits.any { it.first == null } -> ctx.toast("প্রতিটি টাকার জন্য অ্যাকাউন্ট সিলেক্ট করুন")
                else -> onSave(n, splits.map { (it.first as Account).id to it.second })
            }
        },
    ) {
        TextInput(name, { name = it }, "নাম")
        if (!done) {
            SplitLines(
                accounts = accounts,
                lines = lines,
                onChange = { lines = it },
                accountLabel = if (lent) "অ্যাকাউন্টে জমা হবে" else "অ্যাকাউন্ট থেকে কাটবে",
            )
        }
    }
}


/** ভাসমান "+" চাপলে নতুন ধারের ফর্ম: নাম, টাকা (একাধিক অ্যাকাউন্টে ভাগ করা যায়), নোট, তারিখ */
@Composable
private fun AddDebtDialog(
    kind: String,
    accounts: List<Account>,
    personSuggestions: List<String>,
    onDismiss: () -> Unit,
    onSave: (person: String, note: String, splits: List<Pair<Long?, Double>>, date: LocalDate) -> Unit,
) {
    val ctx = LocalContext.current
    val lent = kind == Kind.LENT
    var person by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf(listOf(SplitLine())) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var pickDate by remember { mutableStateOf(false) }

    FormDialog(
        title = debtLabel(kind),
        centerTitle = true,
        confirmText = "সংরক্ষণ",
        onDismiss = onDismiss,
        onConfirm = {
            val splits = lines.splits()
            when {
                person.isBlank() -> ctx.toast("নাম দিন")
                splits.isEmpty() -> ctx.toast("টাকার পরিমাণ দিন")
                splits.any { it.first == null } -> ctx.toast("প্রতিটি টাকার জন্য অ্যাকাউন্ট সিলেক্ট করুন")
                else -> onSave(person.trim(), note.trim(), splits.map { it.first?.id to it.second }, date)
            }
        },
    ) {
        TextInput(person, { person = it }, if (lent) "কাকে দিলেন" else "কার কাছ থেকে নিলেন")
        // নামের অক্ষর লিখলেই মিলে যাওয়া আগের নাম দেখায়, ট্যাপ করলে নাম বসে যায়
        val q = person.trim()
        val shown = if (q.isEmpty()) emptyList() else personSuggestions
            .filter { it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true) }
            .sortedByDescending { it.startsWith(q, ignoreCase = true) }
            .take(5)
        if (shown.isNotEmpty()) {
            Surface(tonalElevation = 3.dp, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Column {
                    shown.forEach { n ->
                        Text(
                            n,
                            modifier = Modifier.fillMaxWidth().clickable { person = n }.padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
        SplitLines(
            accounts = accounts,
            lines = lines,
            onChange = { lines = it },
            accountLabel = if (lent) "অ্যাকাউন্ট থেকে কাটবে" else "অ্যাকাউন্টে জমা হবে",
        )
        TextInput(note, { note = it }, "নোট")
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

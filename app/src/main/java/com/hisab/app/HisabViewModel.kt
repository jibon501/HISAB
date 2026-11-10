package com.hisab.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisab.app.data.Account
import com.hisab.app.data.Category
import com.hisab.app.data.HistoryItem
import com.hisab.app.data.MonthItem
import com.hisab.app.data.Note
import com.hisab.app.data.debtNets
import com.hisab.app.data.Debt
import com.hisab.app.data.HisabDb
import com.hisab.app.data.Kind
import com.hisab.app.data.OTHER_ACCOUNT
import com.hisab.app.data.Txn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ড্যাশবোর্ডের সারসংক্ষেপ */
data class Summary(
    val cash: Double = 0.0,
    val loan: Double = 0.0,
    val lent: Double = 0.0,
    val shop: Double = 0.0,
    val dps: Double = 0.0,
    val asset: Double = 0.0,
    val monthExpense: Double = 0.0,
    val monthBaki: Double = 0.0,
    val other: Double = 0.0,
)

/**
 * সব ডেটা এখানে থাকে। ডাটাবেসের কাজ ব্যাকগ্রাউন্ড থ্রেডে (IO) হয়,
 * তারপর UI স্টেট আপডেট হয়।
 */
class HisabViewModel(app: Application) : AndroidViewModel(app) {

    private val db = HisabDb(app)

    var accounts by mutableStateOf(emptyList<Account>())
        private set
    var txns by mutableStateOf(emptyList<Txn>())
        private set
    var monthExpenses by mutableStateOf(emptyList<MonthItem>())
        private set
    var categories by mutableStateOf(emptyList<Category>())
        private set
    var usedCategories by mutableStateOf(emptyList<String>())
        private set
    var debts by mutableStateOf(emptyMap<String, List<Debt>>())
        private set
    var notes by mutableStateOf(emptyList<Note>())
        private set
    var summary by mutableStateOf(Summary())
        private set
    var filter by mutableStateOf<String?>(null)
        private set

    private class Snapshot(
        val accounts: List<Account>,
        val txns: List<Txn>,
        val monthExpenses: List<MonthItem>,
        val categories: List<Category>,
        val usedCategories: List<String>,
        val debts: Map<String, List<Debt>>,
        val notes: List<Note>,
        val summary: Summary,
    )

    init {
        exec { }
    }

    private fun load(filter: String?): Snapshot {
        val accs = db.accounts()
        val debtMap = listOf(Kind.LOAN, Kind.LENT, Kind.SHOP).associateWith { db.debts(it) }
        val month = db.monthItems()
        fun sum(vararg types: String) = accs.filter { it.type in types }.sumOf { it.balance }
        // দিছি + নিছি মিলিয়ে প্রতি ব্যক্তির নিট: ধনাত্মক = সে দেবে (ধার দেওয়া), ঋণাত্মক = আমি দেব (ধার নেওয়া)
        val nets = debtNets(debtMap).values
        fun owed(kind: String) = debtMap[kind].orEmpty().sumOf { it.remaining } // শুধু দোকান বাকির জন্য
        return Snapshot(
            accounts = accs,
            txns = db.txns(filter),
            monthExpenses = month,
            categories = db.categories(),
            usedCategories = db.usedCategories(),
            debts = debtMap,
            notes = db.notes(),
            summary = Summary(
                cash = sum("cash", "mobile", "bank"),
                loan = nets.filter { it < 0 }.sumOf { -it },
                lent = nets.filter { it > 0 }.sum(),
                shop = owed(Kind.SHOP),
                dps = sum("dps"),
                asset = accs
                    .filter { it.type !in setOf("cash", "mobile", "bank", "dps") && it.name != OTHER_ACCOUNT }
                    .sumOf { it.balance },
                other = accs.filter { it.name == OTHER_ACCOUNT }.sumOf { it.balance },
                monthExpense = month.sumOf { it.amount },
                monthBaki = month.sumOf { it.remaining },
            ),
        )
    }

    private fun publish(s: Snapshot) {
        accounts = s.accounts
        txns = s.txns
        monthExpenses = s.monthExpenses
        categories = s.categories
        usedCategories = s.usedCategories
        debts = s.debts
        notes = s.notes
        summary = s.summary
    }

    /** ডাটাবেসে কাজ করে সব স্টেট রিফ্রেশ করে */
    private fun exec(block: () -> Unit) {
        val f = filter
        viewModelScope.launch {
            val s = withContext(Dispatchers.IO) {
                block()
                load(f)
            }
            publish(s)
        }
    }

    // নোট: `setFilter` নাম দেওয়া যাবে না, কারণ `filter` প্রপার্টির সেটারের সাথে মিলে যায়
    fun applyFilter(category: String?) {
        filter = category
        exec { }
    }

    fun addTxn(type: String, amount: Double, accountId: Long, category: String, note: String) =
        exec { db.addTxn(type, amount, accountId, category, note) }

    /** একজনের দেওয়া-নেওয়া-ফেরতের একটানা খাতা (দিছি ও নিছি মিলিয়ে) */
    fun personHistory(person: String): List<HistoryItem> = db.personHistory(person)

    /** একটি অ্যাকাউন্টের সব লেনদেন */
    fun accountTxns(accountId: Long): List<Txn> = db.accountTxns(accountId)

    fun addNote(text: String) = exec { db.addNote(text) }
    fun updateNote(id: Long, text: String) = exec { db.updateNote(id, text) }
    fun deleteNote(id: Long) = exec { db.deleteNote(id) }

    fun addBakiExpense(amount: Double, category: String, note: String, date: java.time.LocalDate) =
        exec { db.addBakiExpense(amount, category, note, date) }

    fun addCategory(name: String, kind: String) = exec { db.addCategory(name, kind) }

    fun renameCategory(id: Long, newName: String) = exec { db.renameCategory(id, newName) }

    fun deleteCategory(id: Long) = exec { db.deleteCategory(id) }

    /** দেনা-পাওনা এডিট: নাম বদল এবং (একটি মাত্র হিসাব থাকলে) বাকির পরিমাণ বদল */
    fun editDebt(kind: String, oldName: String, newName: String, debtId: Long?, newRemaining: Double?) =
        exec {
            db.renameDebtPerson(kind, oldName, newName)
            if (debtId != null && newRemaining != null) db.setDebtRemaining(debtId, newRemaining)
        }

    /** id ঋণাত্মক হলে এটা বাকি খরচ: পুরো বাকি-হিসাবটাই মোছা হয় */
    fun deleteTxn(id: Long) = exec { if (id < 0) db.deleteDebt(-id) else db.deleteTxn(id) }

    /** লেনদেনের পরিমাণ ও তারিখ এডিট (id ঋণাত্মক হলে বাকি খরচ) */
    fun editTxn(id: Long, amount: Double, date: java.time.LocalDate) = exec { db.updateTxn(id, amount, date) }

    fun transfer(fromId: Long, toId: Long, amount: Double) =
        exec { db.transfer(fromId, toId, amount) }

    fun addAccount(name: String, type: String, opening: Double, icon: String) =
        exec { db.addAccount(name, type, opening, icon) }

    fun updateAccount(id: Long, name: String, icon: String) =
        exec { db.updateAccount(id, name, icon) }

    /** ডিলিট হলে true; অ্যাকাউন্টে লেনদেন থাকলে ডিলিট না হয়ে false */
    fun deleteAccount(id: Long, onDone: (Boolean) -> Unit) {
        val f = filter
        viewModelScope.launch {
            val (ok, s) = withContext(Dispatchers.IO) {
                val ok = db.deleteAccount(id)
                ok to load(f)
            }
            publish(s)
            onDone(ok)
        }
    }

    fun addDebt(
        kind: String,
        person: String,
        note: String,
        splits: List<Pair<Long?, Double>>,
        date: java.time.LocalDate,
    ) = exec { db.addDebt(kind, person, note, splits, date) }

    /** একাধিক অ্যাকাউন্টে ভাগ করা লেনদেন */
    fun addTxns(
        type: String,
        splits: List<Pair<Long?, Double>>,
        category: String,
        note: String,
        date: java.time.LocalDate,
    ) = exec { db.addTxns(type, splits, category, note, date) }

    /** ধার-হিসাব এডিট: নাম বদল + (দিলে) ফেরত/পরিশোধ, একাধিক অ্যাকাউন্ট থেকেও */
    fun saveDebtEdit(debt: Debt, newName: String, splits: List<Pair<Long, Double>>) = exec {
        if (newName != debt.person) db.renameDebt(debt.id, newName)
        splits.forEach { (accId, amount) -> db.pay(debt.copy(person = newName), amount, accId) }
    }

    fun deleteDebt(id: Long) = exec { db.deleteDebt(id) }

    /** একজনের সব হিসাব (দিছি ও নিছি) মোছে */
    fun deletePerson(person: String) = exec {
        listOf(Kind.LENT, Kind.LOAN).forEach { k -> db.debts(k).filter { it.person == person }.forEach { db.deleteDebt(it.id) } }
    }

    /**
     * একজনের এডিট: নাম বদল (দুই ধরনেই) + ফেরত/পরিশোধ। payKind: যে ধরনের হিসাবে টাকা মেলানো হবে
     * (সে দেবে → lent, আমি দেব → loan)। টাকা পুরনো হিসাব থেকে আগে বাদ যায়;
     * সব শোধের পরেও বাড়তি থাকলে সেটা সবচেয়ে নতুন হিসাবে যায়, ফলে নিট উল্টে গিয়ে অন্য ট্যাবে চলে যায়।
     */
    fun savePersonEdit(oldName: String, newName: String, payKind: String, splits: List<Pair<Long, Double>>) = exec {
        if (newName != oldName) {
            db.renameDebtPerson(Kind.LENT, oldName, newName)
            db.renameDebtPerson(Kind.LOAN, oldName, newName)
        }
        val left = db.debts(payKind).filter { it.person == newName }.sortedBy { it.id }
            .map { it to it.remaining }.toMutableList()
        splits.forEach { (accId, amount) ->
            var rest = amount
            for (i in left.indices) {
                if (rest <= 0.0001) break
                val (d, rem) = left[i]
                if (rem <= 0.0001) continue
                val part = minOf(rest, rem)
                db.pay(d, part, accId)
                left[i] = d to (rem - part)
                rest -= part
            }
            if (rest > 0.0001 && left.isNotEmpty()) db.pay(left.last().first, rest, accId)
        }
    }

    /** সব ডেটা xlsx আকারে লেখে। শেষে done(সফল হয়েছে কি না) */
    fun exportXlsx(out: java.io.OutputStream, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    com.hisab.app.data.ExcelIO.write(out, db.exportSheets())
                    true
                } catch (e: Exception) {
                    false
                } finally {
                    runCatching { out.close() }
                }
            }
            done(ok)
        }
    }

    /** xlsx থেকে সব ডেটা ফিরিয়ে আনে (বর্তমান ডেটা মুছে যায়)। শেষে done(সফল হয়েছে কি না) */
    fun importXlsx(input: java.io.InputStream, done: (Boolean) -> Unit) {
        val f = filter
        viewModelScope.launch {
            var ok = false
            val s = withContext(Dispatchers.IO) {
                try {
                    db.replaceAll(com.hisab.app.data.ExcelIO.read(input))
                    ok = true
                } catch (e: Exception) {
                    // ভুল ফাইল হলে আগের ডেটাই থাকে
                } finally {
                    runCatching { input.close() }
                }
                load(f)
            }
            publish(s)
            done(ok)
        }
    }

    fun pay(debt: Debt, amount: Double, accountId: Long) =
        exec { db.pay(debt, amount, accountId) }
}

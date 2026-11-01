package com.hisab.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisab.app.data.Account
import com.hisab.app.data.Debt
import com.hisab.app.data.HisabDb
import com.hisab.app.data.Kind
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
    var debts by mutableStateOf(emptyMap<String, List<Debt>>())
        private set
    var summary by mutableStateOf(Summary())
        private set
    var filter by mutableStateOf<String?>(null)
        private set

    private class Snapshot(
        val accounts: List<Account>,
        val txns: List<Txn>,
        val debts: Map<String, List<Debt>>,
        val summary: Summary,
    )

    init {
        exec { }
    }

    private fun load(filter: String?): Snapshot {
        val accs = db.accounts()
        val debtMap = listOf(Kind.LOAN, Kind.LENT, Kind.SHOP).associateWith { db.debts(it) }
        fun sum(vararg types: String) = accs.filter { it.type in types }.sumOf { it.balance }
        fun owed(kind: String) = debtMap[kind].orEmpty().sumOf { it.remaining }
        return Snapshot(
            accounts = accs,
            txns = db.txns(filter),
            debts = debtMap,
            summary = Summary(
                cash = sum("cash", "mobile", "bank"),
                loan = owed(Kind.LOAN),
                lent = owed(Kind.LENT),
                shop = owed(Kind.SHOP),
                dps = sum("dps"),
                asset = sum("asset"),
                monthExpense = db.monthExpense(),
            ),
        )
    }

    /** ডাটাবেসে কাজ করে সব স্টেট রিফ্রেশ করে */
    private fun exec(block: () -> Unit) {
        val f = filter
        viewModelScope.launch {
            val s = withContext(Dispatchers.IO) {
                block()
                load(f)
            }
            accounts = s.accounts
            txns = s.txns
            debts = s.debts
            summary = s.summary
        }
    }

    fun applyFilter(category: String?) {
        filter = category
        exec { }
    }

    fun addTxn(type: String, amount: Double, accountId: Long, category: String, note: String) =
        exec { db.addTxn(type, amount, accountId, category, note) }

    fun deleteTxn(id: Long) = exec { db.deleteTxn(id) }

    fun transfer(fromId: Long, toId: Long, amount: Double) =
        exec { db.transfer(fromId, toId, amount) }

    fun addAccount(name: String, type: String, opening: Double) =
        exec { db.addAccount(name, type, opening) }

    fun addDebt(kind: String, person: String, total: Double, note: String, accountId: Long?) =
        exec { db.addDebt(kind, person, total, note, accountId) }

    fun pay(debt: Debt, amount: Double, accountId: Long) =
        exec { db.pay(debt, amount, accountId) }
}

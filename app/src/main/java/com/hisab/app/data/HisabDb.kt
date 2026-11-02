package com.hisab.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// ---------- মডেল ----------
data class Account(
    val id: Long,
    val name: String,
    val type: String,
    val balance: Double,
    val icon: String, // আইকনের কী (খালি থাকলে ধরন অনুযায়ী ডিফল্ট)
)

data class Txn(
    val id: Long,
    val type: String, // credit (জমা) / debit (খরচ)
    val amount: Double,
    val account: String,
    val category: String,
    val note: String,
    val date: String,
)

data class Debt(
    val id: Long,
    val kind: String,
    val person: String,
    val total: Double,
    val remaining: Double,
    val date: String,
)

/** খাত (kind: expense = খরচ, income = আয়) */
data class Category(val id: Long, val name: String, val kind: String)

/** তারিখ দিন-মাস-বছর, বাংলা সংখ্যায়: "2026-11-02T..." → "০২-১১-২০২৬" */
fun fmtDate(iso: String): String {
    val p = iso.take(10).split("-")
    val text = if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}" else iso.take(10)
    return text.map { if (it in '0'..'9') '\u09E6' + (it - '0') else it }.joinToString("")
}

/** একজনের ধার-হিসাবের একটি এন্ট্রি (payment = true হলে ফেরত/পরিশোধ) */
data class HistoryItem(val date: String, val label: String, val amount: Double, val payment: Boolean)

/** চলতি মাসের খরচের একটি সারি। remaining > 0 হলে সেটা বাকি */
data class MonthItem(val title: String, val amount: Double, val remaining: Double, val debtId: Long = 0L)

val DEFAULT_EXPENSE_CATS = listOf("খাবার", "বাজার", "বাসা ভাড়া", "যাতায়াত", "বিল", "চিকিৎসা", "শিক্ষা", "বিনোদন", "অন্যান্য")
val DEFAULT_INCOME_CATS = listOf("বেতন", "ব্যবসা", "উপহার", "অন্যান্য আয়")

/** দেনা-পাওনার ধরন */
object Kind {
    const val LOAN = "loan" // লোন নেওয়া
    const val LENT = "lent" // ধার দেওয়া
    const val SHOP = "shop" // দোকান বাকি
}

/**
 * SQLite ডাটাবেস (ফোনেই থাকে, ইন্টারনেট লাগে না)।
 * সব লেনদেনে অ্যাকাউন্ট বাধ্যতামূলক, তাই ব্যালেন্স সবসময় মিলে যায়।
 *
 * ভার্সন ২: অ্যাকাউন্টে আইকন কলাম যোগ হয়েছে।
 * ভার্সন ৩: খাত টেবিল, বাকি-খরচ (debts.is_expense) এবং IFIC BANK নাম।
 */
class HisabDb(context: Context) : SQLiteOpenHelper(context, "hisab.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE accounts(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, type TEXT NOT NULL, opening REAL NOT NULL DEFAULT 0, " +
                "icon TEXT NOT NULL DEFAULT '')"
        )
        db.execSQL(
            "CREATE TABLE debts(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, " +
                "person TEXT NOT NULL, total REAL NOT NULL, note TEXT NOT NULL DEFAULT '', date TEXT NOT NULL, " +
                "is_expense INTEGER NOT NULL DEFAULT 0, category TEXT NOT NULL DEFAULT '')"
        )
        db.execSQL(
            "CREATE TABLE txns(id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, " +
                "amount REAL NOT NULL, account_id INTEGER NOT NULL, category TEXT NOT NULL, " +
                "note TEXT NOT NULL DEFAULT '', date TEXT NOT NULL, debt_id INTEGER, " +
                "is_payment INTEGER NOT NULL DEFAULT 0, ref TEXT)"
        )
        // শুরুর অ্যাকাউন্ট (নাম, ধরন, আইকন)
        val seed = listOf(
            Triple("ক্যাশ", "cash", "cash"),
            Triple("বিকাশ", "mobile", "phone"),
            Triple("নগদ", "mobile", "phone2"),
            Triple("AB Bank", "bank", "bank"),
            Triple("IFIC BANK", "bank", "bank2"),
            Triple("DPS ১", "dps", "savings"),
            Triple("DPS ২", "dps", "savings2"),
            Triple("জমি (ইনভেস্ট)", "asset", "land"),
        )
        createCategories(db)
        seed.forEach { (name, type, icon) ->
            db.insert("accounts", null, ContentValues().apply {
                put("name", name)
                put("type", type)
                put("opening", 0.0)
                put("icon", icon)
            })
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN icon TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE debts ADD COLUMN is_expense INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE debts ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            createCategories(db)
            db.execSQL("UPDATE accounts SET name = 'IFIC BANK' WHERE name = 'TFIC Bank'")
        }
    }

    private fun createCategories(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE categories(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, kind TEXT NOT NULL)"
        )
        DEFAULT_EXPENSE_CATS.forEach { insertCategory(db, it, "expense") }
        DEFAULT_INCOME_CATS.forEach { insertCategory(db, it, "income") }
    }

    private fun insertCategory(db: SQLiteDatabase, name: String, kind: String) {
        db.insert("categories", null, ContentValues().apply {
            put("name", name)
            put("kind", kind)
        })
    }

    // ---------- খাত ----------
    fun categories(): List<Category> =
        readableDatabase.rawQuery("SELECT id, name, kind FROM categories ORDER BY id", null).use { c ->
            buildList {
                while (c.moveToNext()) add(Category(c.getLong(0), c.getString(1), c.getString(2)))
            }
        }

    fun addCategory(name: String, kind: String) {
        val db = writableDatabase
        val exists = db.rawQuery(
            "SELECT COUNT(*) FROM categories WHERE name = ? AND kind = ?",
            arrayOf(name, kind),
        ).use { c -> c.moveToFirst() && c.getInt(0) > 0 }
        if (!exists) insertCategory(db, name, kind)
    }

    /** নাম বদলালে পুরনো লেনদেনের খাতের নামও বদলে যায় */
    fun renameCategory(id: Long, newName: String) {
        val db = writableDatabase
        val old = db.rawQuery("SELECT name FROM categories WHERE id = ?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: return
        db.transaction {
            update("categories", ContentValues().apply { put("name", newName) }, "id = ?", arrayOf(id.toString()))
            update("txns", ContentValues().apply { put("category", newName) }, "category = ?", arrayOf(old))
            update("debts", ContentValues().apply { put("category", newName) }, "category = ?", arrayOf(old))
        }
    }

    /** খাত মুছলেও পুরনো লেনদেন থাকে (শুধু নতুন লেনদেনে খাতটা আর আসবে না) */
    fun deleteCategory(id: Long) {
        writableDatabase.delete("categories", "id = ?", arrayOf(id.toString()))
    }

    // ---------- অ্যাকাউন্ট ----------
    fun accounts(): List<Account> =
        readableDatabase.rawQuery(
            """
            SELECT a.id, a.name, a.type,
                   a.opening + IFNULL((SELECT SUM(CASE WHEN t.type='credit' THEN t.amount ELSE -t.amount END)
                                       FROM txns t WHERE t.account_id = a.id), 0),
                   a.icon
            FROM accounts a ORDER BY a.id
            """.trimIndent(),
            null,
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(Account(c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3), c.getString(4)))
                }
            }
        }

    fun addAccount(name: String, type: String, opening: Double, icon: String) {
        writableDatabase.insertOrThrow("accounts", null, ContentValues().apply {
            put("name", name)
            put("type", type)
            put("opening", opening)
            put("icon", icon)
        })
    }

    /** নাম ও আইকন বদলানো */
    fun updateAccount(id: Long, name: String, icon: String) {
        writableDatabase.update(
            "accounts",
            ContentValues().apply {
                put("name", name)
                put("icon", icon)
            },
            "id = ?",
            arrayOf(id.toString()),
        )
    }

    /**
     * অ্যাকাউন্ট ডিলিট। লেনদেন থাকলে ডিলিট হয় না (false ফেরত দেয়),
     * কারণ তাহলে অন্য হিসাব (লোন/ডিপিএস ইত্যাদি) এলোমেলো হয়ে যাবে।
     */
    fun deleteAccount(id: Long): Boolean {
        val db = writableDatabase
        val used = db.rawQuery(
            "SELECT COUNT(*) FROM txns WHERE account_id = ?",
            arrayOf(id.toString()),
        ).use { c -> c.moveToFirst() && c.getInt(0) > 0 }
        if (used) return false
        db.delete("accounts", "id = ?", arrayOf(id.toString()))
        return true
    }

    // ---------- লেনদেন ----------
    private fun insertTxn(
        db: SQLiteDatabase,
        type: String,
        amount: Double,
        accountId: Long,
        category: String,
        note: String,
        debtId: Long? = null,
        isPayment: Boolean = false,
        ref: String? = null,
    ) {
        val v = ContentValues().apply {
            put("type", type)
            put("amount", amount)
            put("account_id", accountId)
            put("category", category)
            put("note", note)
            put("date", LocalDateTime.now().toString())
            if (debtId != null) put("debt_id", debtId) else putNull("debt_id")
            put("is_payment", if (isPayment) 1 else 0)
            if (ref != null) put("ref", ref) else putNull("ref")
        }
        db.insertOrThrow("txns", null, v)
    }

    fun addTxn(type: String, amount: Double, accountId: Long, category: String, note: String) {
        insertTxn(writableDatabase, type, amount, accountId, category, note)
    }

    private fun Cursor.toTxns(): List<Txn> = buildList {
        while (moveToNext()) {
            add(Txn(getLong(0), getString(1), getDouble(2), getString(3), getString(4), getString(5), getString(6)))
        }
    }

    fun txns(category: String?): List<Txn> {
        val where = if (category == null) "" else "WHERE t.category = ?"
        val args = if (category == null) null else arrayOf(category)
        return readableDatabase.rawQuery(
            """
            SELECT t.id, t.type, t.amount, IFNULL(a.name, ''), t.category, t.note, t.date
            FROM txns t LEFT JOIN accounts a ON a.id = t.account_id
            $where ORDER BY t.id DESC
            """.trimIndent(),
            args,
        ).use { c -> c.toTxns() }
    }

    /** লেনদেনে যেসব খাত সত্যিই আছে (ফিল্টার মেনুর জন্য) */
    fun usedCategories(): List<String> =
        readableDatabase.rawQuery("SELECT DISTINCT category FROM txns ORDER BY category", null).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }

    /** এক ব্যক্তির ধার নেওয়া/দেওয়া ও ফেরতের সব এন্ট্রি, তারিখ অনুযায়ী পুরনো আগে */
    fun debtHistory(kind: String, person: String): List<HistoryItem> {
        val db = readableDatabase
        val out = mutableListOf<HistoryItem>()
        val firstLabel = if (kind == Kind.LENT) "ধার দেওয়া" else "ধার নেওয়া"
        db.rawQuery(
            "SELECT date, total FROM debts WHERE kind = ? AND person = ?",
            arrayOf(kind, person),
        ).use { c ->
            while (c.moveToNext()) out += HistoryItem(c.getString(0), firstLabel, c.getDouble(1), false)
        }
        db.rawQuery(
            """
            SELECT t.date, t.amount FROM txns t JOIN debts d ON d.id = t.debt_id
            WHERE d.kind = ? AND d.person = ? AND t.is_payment = 1
            """.trimIndent(),
            arrayOf(kind, person),
        ).use { c ->
            while (c.moveToNext()) out += HistoryItem(c.getString(0), "ফেরত", c.getDouble(1), true)
        }
        return out.sortedBy { it.date }
    }

    /** ট্রান্সফারের দুই দিকই একসাথে মুছে যায় (ref একই) */
    fun deleteTxn(id: Long) {
        val db = writableDatabase
        val ref = db.rawQuery("SELECT ref FROM txns WHERE id = ?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
        }
        if (ref != null) db.delete("txns", "ref = ?", arrayOf(ref))
        else db.delete("txns", "id = ?", arrayOf(id.toString()))
    }

    fun transfer(fromId: Long, toId: Long, amount: Double) {
        val ref = UUID.randomUUID().toString()
        writableDatabase.transaction {
            insertTxn(this, "debit", amount, fromId, "ট্রান্সফার", "", ref = ref)
            insertTxn(this, "credit", amount, toId, "ট্রান্সফার", "", ref = ref)
        }
    }

    /**
     * চলতি মাসের খরচ: পরিশোধিত খরচ + বাকি খরচ (ট্রান্সফার ও দেনা-পাওনার লেনদেন বাদে)।
     * বাকি খরচ পরে পরিশোধ করলে আবার খরচে ধরা হয় না।
     */
    fun monthItems(): List<MonthItem> {
        val start = LocalDate.now().withDayOfMonth(1).atStartOfDay().toString()
        val db = readableDatabase
        val items = mutableListOf<Pair<String, MonthItem>>() // (সাজানোর তারিখ, সারি)
        db.rawQuery(
            """
            SELECT t.category, t.amount, t.date
            FROM txns t
            WHERE t.type = 'debit' AND t.debt_id IS NULL AND t.category != 'ট্রান্সফার' AND t.date >= ?
            """.trimIndent(),
            arrayOf(start),
        ).use { c ->
            while (c.moveToNext()) {
                items += c.getString(2) to MonthItem("${c.getString(0)} • ${fmtDate(c.getString(2))}", c.getDouble(1), 0.0)
            }
        }
        db.rawQuery(
            """
            SELECT d.category, d.total,
                   d.total - IFNULL((SELECT SUM(amount) FROM txns WHERE debt_id = d.id AND is_payment = 1), 0),
                   d.date, d.id
            FROM debts d WHERE d.is_expense = 1 AND d.date >= ?
            """.trimIndent(),
            arrayOf(start),
        ).use { c ->
            while (c.moveToNext()) {
                items += c.getString(3) to MonthItem("${c.getString(0)} • ${fmtDate(c.getString(3))}", c.getDouble(1), c.getDouble(2), c.getLong(4))
            }
        }
        return items.sortedByDescending { it.first }.map { it.second }
    }

    // ---------- দেনা-পাওনা ----------
    fun debts(kind: String): List<Debt> =
        readableDatabase.rawQuery(
            """
            SELECT d.id, d.kind, d.person, d.total,
                   d.total - IFNULL((SELECT SUM(amount) FROM txns WHERE debt_id = d.id AND is_payment = 1), 0),
                   d.date
            FROM debts d WHERE d.kind = ? ORDER BY d.id DESC
            """.trimIndent(),
            arrayOf(kind),
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Debt(
                            c.getLong(0), c.getString(1), c.getString(2),
                            c.getDouble(3), c.getDouble(4), c.getString(5),
                        )
                    )
                }
            }
        }

    /**
     * loan: টাকা অ্যাকাউন্টে জমা হয় | lent: অ্যাকাউন্ট থেকে কাটে | shop: অ্যাকাউন্ট লাগে না
     */
    fun addDebt(kind: String, person: String, total: Double, note: String, accountId: Long?) {
        writableDatabase.transaction {
            val debtId = insertOrThrow("debts", null, ContentValues().apply {
                put("kind", kind)
                put("person", person)
                put("total", total)
                put("note", note)
                put("date", LocalDateTime.now().toString())
            })
            if (accountId != null && kind != Kind.SHOP) {
                insertTxn(
                    this,
                    type = if (kind == Kind.LOAN) "credit" else "debit",
                    amount = total,
                    accountId = accountId,
                    category = if (kind == Kind.LOAN) "লোন গ্রহণ" else "ধার প্রদান",
                    note = person,
                    debtId = debtId,
                )
            }
        }
    }

    /** বাকিতে খরচ: অ্যাকাউন্ট থেকে টাকা কাটে না, দোকান বাকিতে জমা হয় এবং মাসের খরচে ধরা হয় */
    fun addBakiExpense(amount: Double, category: String, note: String) {
        writableDatabase.insertOrThrow("debts", null, ContentValues().apply {
            put("kind", Kind.SHOP)
            put("person", note.ifBlank { category })
            put("total", amount)
            put("note", note)
            put("date", LocalDateTime.now().toString())
            put("is_expense", 1)
            put("category", category)
        })
    }

    /** নাম বদল: এই ধরনের একই নামের সব হিসাবে প্রযোজ্য */
    fun renameDebtPerson(kind: String, oldName: String, newName: String) {
        if (oldName == newName) return
        writableDatabase.transaction {
            update(
                "debts", ContentValues().apply { put("person", newName) },
                "kind = ? AND person = ?", arrayOf(kind, oldName),
            )
            execSQL(
                "UPDATE txns SET note = ? WHERE debt_id IN (SELECT id FROM debts WHERE kind = ? AND person = ?)",
                arrayOf(newName, kind, newName),
            )
        }
    }

    /** বাকির পরিমাণ বদল: মোট = এ পর্যন্ত পরিশোধ + নতুন বাকি */
    fun setDebtRemaining(id: Long, newRemaining: Double) {
        writableDatabase.transaction {
            val paid = rawQuery(
                "SELECT IFNULL(SUM(amount), 0) FROM txns WHERE debt_id = ? AND is_payment = 1",
                arrayOf(id.toString()),
            ).use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
            val newTotal = paid + newRemaining
            update("debts", ContentValues().apply { put("total", newTotal) }, "id = ?", arrayOf(id.toString()))
            // লোন/ধার নেওয়া-দেওয়ার মূল লেনদেনের পরিমাণও মিলিয়ে দেওয়া হয়
            update(
                "txns", ContentValues().apply { put("amount", newTotal) },
                "debt_id = ? AND is_payment = 0", arrayOf(id.toString()),
            )
        }
    }

    /** loan/shop পরিশোধ = debit | lent ফেরত পাওয়া = credit */
    fun pay(debt: Debt, amount: Double, accountId: Long) {
        insertTxn(
            writableDatabase,
            type = if (debt.kind == Kind.LENT) "credit" else "debit",
            amount = amount,
            accountId = accountId,
            category = when (debt.kind) {
                Kind.LENT -> "ধার ফেরত"
                Kind.LOAN -> "লোন পরিশোধ"
                else -> "দোকান বাকি পরিশোধ"
            },
            note = debt.person,
            debtId = debt.id,
            isPayment = true,
        )
    }
}

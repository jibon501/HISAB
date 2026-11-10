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
    val baki: Boolean = false, // বাকি খরচ (পরে পরিশোধ হবে); id = −(হিসাবের আইডি)
)

data class Debt(
    val id: Long,
    val kind: String,
    val person: String,
    val total: Double,
    val remaining: Double,
    val date: String,
)

/** ডিফল্ট অ্যাকাউন্টের নাম (সম্পদের অধীনে)। অ্যাকাউন্ট ছাড়া লেনদেন এতে জমা হয় */
const val OTHER_ACCOUNT = "অন্যান্য"

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

/** ধার-হিসাবের প্রথম এন্ট্রির লেবেল */
fun debtLabel(kind: String): String = if (kind == Kind.LENT) "ধার দিয়েছি" else "ধার নিয়েছি"

/**
 * ধার-হিসাবের নিচের মোটের লেবেল ও পরিমাণ। net = ধার − ফেরত।
 * ফেরত বেশি হলে (net < 0) অতিরিক্তটা উল্টো দিকের হিসাব: দিছি-তে "আমার কাছে পাবে", নিছি-তে "আমি পাবো"।
 */
fun debtTotal(kind: String, net: Double): Pair<String, Double> =
    if (net >= 0) (if (kind == Kind.LENT) "মোট দিয়েছি" else "মোট নিয়েছি") to net
    else (if (kind == Kind.LENT) "আমার কাছে পাবে" else "আমি পাবো") to -net

/** ফেরত/পরিশোধ এন্ট্রির লেবেল */
fun repayLabel(kind: String): String = if (kind == Kind.LENT) "ফেরত নিছি" else "ফেরত দিয়েছি"

/**
 * SQLite ডাটাবেস (ফোনেই থাকে, ইন্টারনেট লাগে না)।
 * সব লেনদেনে অ্যাকাউন্ট বাধ্যতামূলক, তাই ব্যালেন্স সবসময় মিলে যায়।
 *
 * ভার্সন ২: অ্যাকাউন্টে আইকন কলাম যোগ হয়েছে।
 * ভার্সন ৩: খাত টেবিল, বাকি-খরচ (debts.is_expense) এবং IFIC BANK নাম।
 * ভার্সন ৪: সম্পদের অধীনে ডিফল্ট "অন্যান্য" অ্যাকাউন্ট; অ্যাকাউন্ট ছাড়া লেনদেন এতেই জমা হয়।
 * ভার্সন ৫: খাতের নাম "ধার প্রদান" → "ধার দিছি", "লোন গ্রহণ" → "ধার নিছি"।
 */
class HisabDb(context: Context) : SQLiteOpenHelper(context, "hisab.db", null, 5) {

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
            Triple(OTHER_ACCOUNT, "asset", ""),
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
        if (oldVersion < 4) {
            // আগের সংস্করণে অ্যাকাউন্ট ছাড়া সেভ হওয়া লেনদেন (আইডি ০) এখন "অন্যান্য" অ্যাকাউন্টে আসে
            val id = otherAccountId(db)
            db.execSQL("UPDATE txns SET account_id = ? WHERE account_id = 0", arrayOf<Any>(id))
        }
        if (oldVersion < 5) renameDebtCategories(db)
    }

    /** পুরনো খাতের নাম নতুন নামে বদলায় (আগের লেনদেন ও পুরনো এক্সেল ব্যাকআপ, দুইয়ের জন্যই) */
    private fun renameDebtCategories(db: SQLiteDatabase) {
        db.execSQL("UPDATE txns SET category = 'ধার দিছি' WHERE category = 'ধার প্রদান'")
        db.execSQL("UPDATE txns SET category = 'ধার নিছি' WHERE category = 'লোন গ্রহণ'")
    }

    /** ডিফল্ট "অন্যান্য" অ্যাকাউন্টের আইডি; না থাকলে (মোছা হলে) নতুন করে বানায় */
    private fun otherAccountId(db: SQLiteDatabase): Long {
        db.rawQuery("SELECT id FROM accounts WHERE name = ? LIMIT 1", arrayOf(OTHER_ACCOUNT)).use { c ->
            if (c.moveToFirst()) return c.getLong(0)
        }
        return db.insertOrThrow("accounts", null, ContentValues().apply {
            put("name", OTHER_ACCOUNT)
            put("type", "asset")
            put("opening", 0.0)
            put("icon", "")
        })
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
        date: LocalDateTime? = null,
    ) {
        val v = ContentValues().apply {
            put("type", type)
            put("amount", amount)
            put("account_id", accountId)
            put("category", category)
            put("note", note)
            put("date", (date ?: LocalDateTime.now()).toString())
            if (debtId != null) put("debt_id", debtId) else putNull("debt_id")
            put("is_payment", if (isPayment) 1 else 0)
            if (ref != null) put("ref", ref) else putNull("ref")
        }
        db.insertOrThrow("txns", null, v)
    }

    fun addTxn(type: String, amount: Double, accountId: Long, category: String, note: String) {
        insertTxn(writableDatabase, type, amount, accountId, category, note)
    }

    /**
     * একাধিক অ্যাকাউন্টে ভাগ করা লেনদেন। অ্যাকাউন্ট না থাকলে "অন্যান্য" অ্যাকাউন্টে যায়।
     * তারিখ আগের হতে পারে; সময় এখনকার সময় ধরা হয় (সাজানোর ক্রম ঠিক রাখতে)।
     */
    fun addTxns(
        type: String,
        splits: List<Pair<Long?, Double>>,
        category: String,
        note: String,
        date: LocalDate = LocalDate.now(),
    ) {
        val at = LocalDateTime.of(date, java.time.LocalTime.now())
        writableDatabase.transaction {
            splits.forEach { (accountId, amount) ->
                insertTxn(this, type, amount, accountId ?: otherAccountId(this), category, note, date = at)
            }
        }
    }

    private fun Cursor.toTxns(): List<Txn> = buildList {
        while (moveToNext()) {
            add(Txn(getLong(0), getString(1), getDouble(2), getString(3), getString(4), getString(5), getString(6)))
        }
    }

    /** লেনদেনের তালিকা (বাকি খরচসহ), তারিখ অনুযায়ী নতুন আগে: আজকের তারিখ সবার উপরে */
    fun txns(category: String?): List<Txn> {
        val where = if (category == null) "" else "WHERE t.category = ?"
        val args = if (category == null) null else arrayOf(category)
        val db = readableDatabase
        val list = db.rawQuery(
            """
            SELECT t.id, t.type, t.amount, IFNULL(a.name, ''), t.category, t.note, t.date
            FROM txns t LEFT JOIN accounts a ON a.id = t.account_id
            $where
            """.trimIndent(),
            args,
        ).use { c -> c.toTxns() }.toMutableList()
        // বাকি খরচ: অ্যাকাউন্ট থেকে টাকা কাটেনি, তবু তালিকায় "বাকি" চিহ্নসহ থাকে
        val bakiWhere = if (category == null) "" else "AND category = ?"
        db.rawQuery(
            "SELECT id, total, category, note, date FROM debts WHERE is_expense = 1 $bakiWhere",
            args,
        ).use { c ->
            while (c.moveToNext()) {
                list += Txn(-c.getLong(0), "debit", c.getDouble(1), "", c.getString(2), c.getString(3), c.getString(4), baki = true)
            }
        }
        return list.sortedWith(compareByDescending<Txn> { it.date }.thenByDescending { it.id })
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
        val firstLabel = debtLabel(kind)
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
            while (c.moveToNext()) out += HistoryItem(c.getString(0), repayLabel(kind), c.getDouble(1), true)
        }
        return out.sortedBy { it.date }
    }

    /** আগের তারিখ-সময়ের সময়টা রেখে শুধু দিনটা বদলায় (সাজানোর ক্রম ঠিক থাকে) */
    private fun withDate(old: String?, date: LocalDate): String {
        val time = runCatching { LocalDateTime.parse(old).toLocalTime() }.getOrDefault(java.time.LocalTime.now())
        return LocalDateTime.of(date, time).toString()
    }

    /**
     * লেনদেনের পরিমাণ ও তারিখ বদল। id ঋণাত্মক হলে বাকি খরচ।
     * ট্রান্সফারের দুই দিকই বদলায়; ধারের মূল লেনদেন বদলালে ধারের মোট ও তারিখও মিলিয়ে দেওয়া হয়।
     */
    fun updateTxn(id: Long, amount: Double, date: LocalDate) {
        writableDatabase.transaction<Unit> {
            if (id < 0) {
                val debtId = (-id).toString()
                val old = rawQuery("SELECT date FROM debts WHERE id = ?", arrayOf(debtId)).use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
                update(
                    "debts",
                    ContentValues().apply { put("total", amount); put("date", withDate(old, date)) },
                    "id = ?", arrayOf(debtId),
                )
                return@transaction
            }
            val row = rawQuery(
                "SELECT ref, debt_id, is_payment, date FROM txns WHERE id = ?", arrayOf(id.toString()),
            ).use { c ->
                if (c.moveToFirst()) arrayOf(c.getString(0), if (c.isNull(1)) null else c.getString(1), c.getString(2), c.getString(3)) else null
            } ?: return@transaction
            val ref = row[0]
            val debtId = row[1]
            val isPayment = row[2] == "1"
            val v = ContentValues().apply { put("amount", amount); put("date", withDate(row[3], date)) }
            if (ref != null) update("txns", v, "ref = ?", arrayOf(ref))
            else update("txns", v, "id = ?", arrayOf(id.toString()))
            if (debtId != null && !isPayment) {
                // একাধিক অ্যাকাউন্টে ভাগ থাকলে ধারের মোট = সব ভাগের যোগফল
                val total = rawQuery(
                    "SELECT IFNULL(SUM(amount), 0) FROM txns WHERE debt_id = ? AND is_payment = 0", arrayOf(debtId),
                ).use { c -> if (c.moveToFirst()) c.getDouble(0) else amount }
                update(
                    "debts",
                    ContentValues().apply { put("total", total); put("date", withDate(row[3], date)) },
                    "id = ?", arrayOf(debtId),
                )
            }
        }
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
    /** একজনের নামে ধার নেওয়া/দেওয়া। একাধিক অ্যাকাউন্টে ভাগ করা যায়; মোট = সব ভাগের যোগফল */
    fun addDebt(
        kind: String,
        person: String,
        note: String,
        splits: List<Pair<Long?, Double>>,
        date: LocalDate = LocalDate.now(),
    ) {
        val total = splits.sumOf { it.second }
        val at = LocalDateTime.of(date, java.time.LocalTime.now())
        writableDatabase.transaction {
            val debtId = insertOrThrow("debts", null, ContentValues().apply {
                put("kind", kind)
                put("person", person)
                put("total", total)
                put("note", note)
                put("date", at.toString())
            })
            if (kind != Kind.SHOP) {
                splits.forEach { (accountId, amount) ->
                    if (accountId != null) {
                        insertTxn(
                            this,
                            type = if (kind == Kind.LOAN) "credit" else "debit",
                            amount = amount,
                            accountId = accountId,
                            category = if (kind == Kind.LOAN) "ধার নিছি" else "ধার দিছি",
                            note = person,
                            debtId = debtId,
                            date = at,
                        )
                    }
                }
            }
        }
    }

    /** একটি ধার-হিসাবের সব এন্ট্রি (প্রথম ধার + প্রতিটি ফেরত/পরিশোধ), পুরনো আগে */
    fun debtEntries(id: Long): List<HistoryItem> {
        val db = readableDatabase
        val out = mutableListOf<HistoryItem>()
        var kind = Kind.LOAN
        db.rawQuery("SELECT kind, date, total FROM debts WHERE id = ?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst()) {
                kind = c.getString(0)
                out += HistoryItem(c.getString(1), debtLabel(kind), c.getDouble(2), false)
            }
        }
        db.rawQuery(
            "SELECT date, amount FROM txns WHERE debt_id = ? AND is_payment = 1",
            arrayOf(id.toString()),
        ).use { c ->
            while (c.moveToNext()) out += HistoryItem(c.getString(0), repayLabel(kind), c.getDouble(1), true)
        }
        return out.sortedBy { it.date }
    }

    /** শুধু এই হিসাবের নাম বদল (এর লেনদেনের নোটও বদলায়) */
    fun renameDebt(id: Long, newName: String) {
        writableDatabase.transaction {
            update("debts", ContentValues().apply { put("person", newName) }, "id = ?", arrayOf(id.toString()))
            update("txns", ContentValues().apply { put("note", newName) }, "debt_id = ?", arrayOf(id.toString()))
        }
    }

    /** হিসাব ও তার সব লেনদেন মোছে; ফলে অ্যাকাউন্টের ব্যালেন্স আগের অবস্থায় ফেরে */
    fun deleteDebt(id: Long) {
        writableDatabase.transaction {
            delete("txns", "debt_id = ?", arrayOf(id.toString()))
            delete("debts", "id = ?", arrayOf(id.toString()))
        }
    }

    /** বাকিতে খরচ: অ্যাকাউন্ট থেকে টাকা কাটে না, দোকান বাকিতে জমা হয় এবং মাসের খরচে ধরা হয় */
    fun addBakiExpense(amount: Double, category: String, note: String, date: LocalDate = LocalDate.now()) {
        writableDatabase.insertOrThrow("debts", null, ContentValues().apply {
            put("kind", Kind.SHOP)
            put("person", note.ifBlank { category })
            put("total", amount)
            put("note", note)
            put("date", LocalDateTime.of(date, java.time.LocalTime.now()).toString())
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

    // ---------- xlsx ইম্পোর্ট / এক্সপোর্ট ----------

    /** টেবিল → (শিটের নাম, কলাম ও ধরন: L = পূর্ণসংখ্যা, D = দশমিক, S = লেখা) */
    private val sheetSpec = listOf(
        Triple("accounts", "Accounts", linkedMapOf("id" to 'L', "name" to 'S', "type" to 'S', "opening" to 'D', "icon" to 'S')),
        Triple(
            "txns", "Transactions",
            linkedMapOf(
                "id" to 'L', "type" to 'S', "amount" to 'D', "account_id" to 'L', "category" to 'S',
                "note" to 'S', "date" to 'S', "debt_id" to 'L', "is_payment" to 'L', "ref" to 'S',
            ),
        ),
        Triple(
            "debts", "Debts",
            linkedMapOf(
                "id" to 'L', "kind" to 'S', "person" to 'S', "total" to 'D', "note" to 'S',
                "date" to 'S', "is_expense" to 'L', "category" to 'S',
            ),
        ),
        Triple("categories", "Categories", linkedMapOf("id" to 'L', "name" to 'S', "kind" to 'S')),
    )

    private val nullableCols = setOf("debt_id", "ref")

    /** সব ডেটা শিট আকারে */
    fun exportSheets(): List<SheetData> = sheetSpec.map { (table, sheet, cols) ->
        val names = cols.keys.toList()
        val rows = readableDatabase.rawQuery("SELECT ${names.joinToString(",")} FROM $table ORDER BY id", null).use { c ->
            buildList<List<Any?>> {
                while (c.moveToNext()) {
                    add(names.indices.map { i ->
                        when (c.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> null
                            Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                            Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                            else -> c.getString(i)
                        }
                    })
                }
            }
        }
        SheetData(sheet, names, rows)
    }

    /** বর্তমান সব ডেটা মুছে ফাইলের ডেটা বসায় (কোনো ভুল হলে আগের ডেটাই থাকে) */
    fun replaceAll(data: Map<String, List<Map<String, String>>>) {
        require(data.containsKey("Accounts") && data.containsKey("Transactions") && data.containsKey("Debts")) {
            "সঠিক ফাইল নয়"
        }
        writableDatabase.transaction {
            sheetSpec.forEach { (table, sheet, _) ->
                if (data.containsKey(sheet)) delete(table, null, null)
            }
            sheetSpec.forEach { (table, sheet, cols) ->
                data[sheet].orEmpty().forEach { row ->
                    val v = ContentValues()
                    cols.forEach { (col, kind) ->
                        val raw = row[col]?.trim()
                        if (raw.isNullOrEmpty()) {
                            when {
                                col == "id" -> Unit
                                col in nullableCols -> v.putNull(col)
                                kind == 'S' -> v.put(col, "")
                                else -> v.put(col, 0)
                            }
                        } else {
                            when (kind) {
                                'L' -> v.put(col, raw.toDouble().toLong())
                                'D' -> v.put(col, raw.toDouble())
                                else -> v.put(col, raw)
                            }
                        }
                    }
                    insertOrThrow(table, null, v)
                }
            }
            renameDebtCategories(this)
        }
    }
}

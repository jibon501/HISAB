package com.hisab.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// ---------- মডেল ----------
data class Account(val id: Long, val name: String, val type: String, val balance: Double)

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

/** দেনা-পাওনার ধরন */
object Kind {
    const val LOAN = "loan" // লোন নেওয়া
    const val LENT = "lent" // ধার দেওয়া
    const val SHOP = "shop" // দোকান বাকি
}

/**
 * SQLite ডাটাবেস (ফোনেই থাকে, ইন্টারনেট লাগে না)।
 * সব লেনদেনে অ্যাকাউন্ট বাধ্যতামূলক, তাই ব্যালেন্স সবসময় মিলে যায়।
 */
class HisabDb(context: Context) : SQLiteOpenHelper(context, "hisab.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE accounts(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, type TEXT NOT NULL, opening REAL NOT NULL DEFAULT 0)"
        )
        db.execSQL(
            "CREATE TABLE debts(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, " +
                "person TEXT NOT NULL, total REAL NOT NULL, note TEXT NOT NULL DEFAULT '', date TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE txns(id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, " +
                "amount REAL NOT NULL, account_id INTEGER NOT NULL, category TEXT NOT NULL, " +
                "note TEXT NOT NULL DEFAULT '', date TEXT NOT NULL, debt_id INTEGER, " +
                "is_payment INTEGER NOT NULL DEFAULT 0, ref TEXT)"
        )
        // শুরুর অ্যাকাউন্ট
        val seed = listOf(
            "ক্যাশ" to "cash",
            "বিকাশ" to "mobile",
            "নগদ" to "mobile",
            "AB Bank" to "bank",
            "TFIC Bank" to "bank",
            "DPS ১" to "dps",
            "DPS ২" to "dps",
            "জমি (ইনভেস্ট)" to "asset",
        )
        seed.forEach { (name, type) ->
            db.insert("accounts", null, ContentValues().apply {
                put("name", name)
                put("type", type)
                put("opening", 0.0)
            })
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // ভবিষ্যতে স্কিমা বদলালে এখানে মাইগ্রেশন লিখুন
    }

    // ---------- অ্যাকাউন্ট ----------
    fun accounts(): List<Account> =
        readableDatabase.rawQuery(
            """
            SELECT a.id, a.name, a.type,
                   a.opening + IFNULL((SELECT SUM(CASE WHEN t.type='credit' THEN t.amount ELSE -t.amount END)
                                       FROM txns t WHERE t.account_id = a.id), 0)
            FROM accounts a ORDER BY a.id
            """.trimIndent(),
            null,
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(Account(c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3)))
            }
        }

    fun addAccount(name: String, type: String, opening: Double) {
        writableDatabase.insertOrThrow("accounts", null, ContentValues().apply {
            put("name", name)
            put("type", type)
            put("opening", opening)
        })
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
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Txn(
                            c.getLong(0), c.getString(1), c.getDouble(2), c.getString(3),
                            c.getString(4), c.getString(5), c.getString(6),
                        )
                    )
                }
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

    /** চলতি মাসের খরচ (ট্রান্সফার ও দেনা-পাওনা বাদে) */
    fun monthExpense(): Double {
        val start = LocalDate.now().withDayOfMonth(1).atStartOfDay().toString()
        return readableDatabase.rawQuery(
            "SELECT IFNULL(SUM(amount), 0) FROM txns WHERE type='debit' AND debt_id IS NULL " +
                "AND category != 'ট্রান্সফার' AND date >= ?",
            arrayOf(start),
        ).use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
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

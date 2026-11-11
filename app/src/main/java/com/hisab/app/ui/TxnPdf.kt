package com.hisab.app.ui

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.hisab.app.data.Txn
import com.hisab.app.data.fmtDate
import java.io.OutputStream

/**
 * লগবুকের তালিকা PDF করে, স্ক্রিনে যেভাবে দেখায় সেভাবেই:
 * খাত (ও নোট/বাকি), নিচে তারিখ, ডানে জমা (সবুজ +) বা খরচ (লাল -); ক্লিয়ার লেনদেন ধূসর ও কাটা দাগে।
 * একটা পরপর হালকা ধূসর ব্যাকগ্রাউন্ড। বাংলা লেখা ফোনের সিস্টেম ফন্টে আঁকা হয়।
 */
object TxnPdf {
    private const val W = 595 // A4 (পয়েন্ট)
    private const val H = 842
    private const val M = 32f
    private const val ROW = 34f
    private val GREEN = Color.rgb(0x2E, 0x7D, 0x32)
    private val RED = Color.rgb(0xC6, 0x28, 0x28)
    private val ORANGE = Color.rgb(0xFB, 0x8C, 0x00)

    private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK, right: Boolean = false) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            if (right) textAlign = Paint.Align.RIGHT
        }

    fun write(out: OutputStream, title: String, subtitle: String, txns: List<Txn>) {
        val doc = PdfDocument()
        try {
            var pageNo = 1
            var page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            var c = page.canvas

            // শিরোনাম
            c.drawText(title, M, M + 16f, paint(18f, bold = true))
            c.drawText(subtitle, M, M + 34f, paint(10f, color = Color.GRAY))
            var y = M + 46f
            c.drawLine(M, y, W - M, y, paint(1f, color = Color.LTGRAY))
            y += 6f

            if (txns.isEmpty()) {
                c.drawText("কোনো লেনদেন নেই", M, y + 24f, paint(12f))
            }

            txns.forEachIndexed { i, t ->
                if (y + ROW > H - M - 24f) {
                    doc.finishPage(page)
                    pageNo++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
                    c = page.canvas
                    y = M
                }
                val credit = t.type == "credit"
                val tint = if (t.cleared) Color.GRAY else if (credit) GREEN else RED
                val strike = t.cleared

                if (i % 2 == 1) c.drawRect(M - 6f, y, W - M + 6f, y + ROW, Paint().apply { color = Color.argb(18, 0, 0, 0) })

                val cat = paint(12f, bold = true, color = if (strike) Color.GRAY else Color.BLACK).apply { isStrikeThruText = strike }
                c.drawText(t.category, M, y + 14f, cat)
                var x = M + cat.measureText(t.category)
                if (t.baki) {
                    val p = paint(10f, bold = true, color = if (strike) Color.GRAY else ORANGE).apply { isStrikeThruText = strike }
                    c.drawText("  বাকি", x, y + 14f, p)
                } else if (t.category in DEBT_CATS && t.note.isNotBlank()) {
                    val p = paint(10f, color = Color.GRAY).apply { isStrikeThruText = strike }
                    c.drawText("  ${t.note}", x, y + 14f, p)
                }
                val note = if (t.note.isNotBlank() && !t.baki && t.category !in DEBT_CATS) " • ${t.note}" else ""
                val bakiNote = if (t.baki && t.note.isNotBlank()) " • ${t.note}" else ""
                c.drawText(
                    "${fmtDate(t.date)}$note$bakiNote",
                    M, y + 27f,
                    paint(9.5f, color = Color.GRAY).apply { isStrikeThruText = strike },
                )
                c.drawText(
                    (if (credit) "+" else "-") + tk(t.amount),
                    W - M, y + 20f,
                    paint(12f, bold = true, color = tint, right = true).apply { isStrikeThruText = strike },
                )
                y += ROW
            }

            // মোট (জমা − খরচ), স্ক্রিনের নিচের "মোট" এর মতো
            if (txns.isNotEmpty()) {
                if (y + 30f > H - M) {
                    doc.finishPage(page)
                    pageNo++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
                    c = page.canvas
                    y = M
                }
                c.drawLine(M, y + 4f, W - M, y + 4f, paint(1f, color = Color.LTGRAY))
                val net = txns.sumOf { if (it.type == "credit") it.amount else -it.amount }
                c.drawText("মোট: ${tk(net)}", W - M, y + 22f, paint(12f, bold = true, right = true))
            }
            doc.finishPage(page)
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }
}

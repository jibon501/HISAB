package com.hisab.app.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.hisab.app.data.HistoryItem
import com.hisab.app.data.debtTotal
import com.hisab.app.data.fmtDate

/** একজনের ধার-হিসাবের ছবি বানায়: নাম, প্রতিটি এন্ট্রি ও নিচে বাকি */
fun buildHistoryBitmap(person: String, items: List<HistoryItem>): Bitmap {
    val width = 720
    val pad = 40f
    val rowH = 72f
    val headH = 150f
    val footH = 110f
    val height = (headH + rowH * items.size + footH).toInt()

    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(Color.WHITE)

    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 46f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK
    }
    val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 28f; color = Color.GRAY }
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 32f; color = Color.BLACK }
    val bold = Paint(text).apply { typeface = Typeface.DEFAULT_BOLD }
    val line = Paint().apply { color = Color.LTGRAY; strokeWidth = 2f }

    c.drawText(person, (width - title.measureText(person)) / 2f, 85f, title) // নাম মাঝখানে
    c.drawLine(pad, headH - 12f, width - pad, headH - 12f, line)

    var y = headH + 40f
    var remaining = 0.0
    items.forEach { h ->
        remaining += h.delta
        // ফেরত/পরিশোধের সারিতে তারিখ, ধরন ও পরিমাণ একই (সবুজ) রঙে
        val green = Color.rgb(0x2E, 0x7D, 0x32)
        c.drawText(fmtDate(h.date), pad, y, if (h.payment) Paint(sub).apply { color = green } else sub)
        c.drawText(h.label, pad + 190f, y, if (h.payment) Paint(text).apply { color = green } else text)
        val amountPaint = Paint(bold).apply { if (h.payment) color = green }
        val amt = tk(h.amount)
        c.drawText(amt, width - pad - amountPaint.measureText(amt), y, amountPaint)
        y += rowH
    }

    c.drawLine(pad, y - rowH + 28f, width - pad, y - rowH + 28f, line)
    val (totalLabel, totalAmount) = debtTotal(remaining)
    val rest = "$totalLabel=${tk(totalAmount)}"
    c.drawText(rest, width - pad - bold.measureText(rest), y + 20f, bold)
    return bmp
}

/**
 * ছবি গ্যালারির Pictures/Hisab ফোল্ডারে সেভ করে। সফল হলে ছবির Uri ফেরত দেয়।
 * অ্যান্ড্রয়েড ৯ ও পুরনো ফোনে (অনুমতি লাগে বলে) সমর্থিত নয়, তখন null।
 */
fun saveToGallery(ctx: Context, bmp: Bitmap, baseName: String): Uri? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$baseName.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Hisab")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val resolver = ctx.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    return try {
        val ok = resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: false
        if (!ok) throw IllegalStateException("সেভ হয়নি")
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        uri
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        null
    }
}

/** ছবি শেয়ার (শেয়ার অ্যাপের তালিকা খোলে) */
fun shareImage(ctx: Context, uri: Uri) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(send, "শেয়ার").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** পুরনো ফোনের জন্য: ছবির বদলে লেখা আকারে শেয়ার */
fun shareText(ctx: Context, person: String, items: List<HistoryItem>) {
    var remaining = 0.0
    val body = buildString {
        appendLine(person)
        items.forEach { h ->
            remaining += h.delta
            appendLine("${fmtDate(h.date)}  ${h.label}  ${tk(h.amount)}")
        }
        val (totalLabel, totalAmount) = debtTotal(remaining)
        append("$totalLabel=${tk(totalAmount)}")
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
    }
    ctx.startActivity(Intent.createChooser(send, "শেয়ার").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

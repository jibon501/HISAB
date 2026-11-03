package com.hisab.app.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** একটি শিট: নাম, কলামের নাম (প্রথম সারি) ও ডেটার সারি (Number / String / null) */
class SheetData(val name: String, val columns: List<String>, val rows: List<List<Any?>>)

/** বাইরের লাইব্রেরি ছাড়া সরাসরি xlsx (জিপ + XML) লেখা ও পড়া */
object ExcelIO {

    private const val NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val NS_PKG_REL = "http://schemas.openxmlformats.org/package/2006/relationships"

    private fun esc(s: String): String = s
        .filter { it == '\n' || it == '\t' || it == '\r' || it >= ' ' }
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun colName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        do {
            sb.insert(0, 'A' + n % 26)
            n = n / 26 - 1
        } while (n >= 0)
        return sb.toString()
    }

    private fun colIndex(ref: String): Int {
        var n = 0
        for (ch in ref) {
            if (ch !in 'A'..'Z') break
            n = n * 26 + (ch - 'A' + 1)
        }
        return n - 1
    }

    // ---------------- লেখা ----------------

    fun write(out: OutputStream, sheets: List<SheetData>) {
        ZipOutputStream(out).use { z ->
            fun put(name: String, text: String) {
                z.putNextEntry(ZipEntry(name))
                z.write(text.toByteArray(Charsets.UTF_8))
                z.closeEntry()
            }
            val head = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"

            val overrides = sheets.indices.joinToString("") {
                "<Override PartName=\"/xl/worksheets/sheet${it + 1}.xml\" " +
                    "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            }
            put(
                "[Content_Types].xml",
                head + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                    overrides +
                    "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
                    "</Types>",
            )
            put(
                "_rels/.rels",
                head + "<Relationships xmlns=\"$NS_PKG_REL\"><Relationship Id=\"rId1\" " +
                    "Type=\"$NS_REL/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>",
            )
            val sheetTags = sheets.mapIndexed { i, s ->
                "<sheet name=\"${esc(s.name)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>"
            }.joinToString("")
            put(
                "xl/workbook.xml",
                head + "<workbook xmlns=\"$NS_MAIN\" xmlns:r=\"$NS_REL\"><sheets>$sheetTags</sheets></workbook>",
            )
            val rels = sheets.indices.joinToString("") {
                "<Relationship Id=\"rId${it + 1}\" Type=\"$NS_REL/worksheet\" Target=\"worksheets/sheet${it + 1}.xml\"/>"
            } + "<Relationship Id=\"rId${sheets.size + 1}\" Type=\"$NS_REL/styles\" Target=\"styles.xml\"/>"
            put("xl/_rels/workbook.xml.rels", head + "<Relationships xmlns=\"$NS_PKG_REL\">$rels</Relationships>")
            put(
                "xl/styles.xml",
                head + "<styleSheet xmlns=\"$NS_MAIN\"><fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
                    "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>" +
                    "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
                    "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
                    "<cellXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/></cellXfs></styleSheet>",
            )
            sheets.forEachIndexed { i, s -> put("xl/worksheets/sheet${i + 1}.xml", head + sheetXml(s)) }
        }
    }

    private fun cell(ref: String, value: Any?): String = when (value) {
        null -> ""
        is Number -> "<c r=\"$ref\"><v>${BigDecimal(value.toString()).toPlainString()}</v></c>"
        else -> "<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(value.toString())}</t></is></c>"
    }

    private fun sheetXml(s: SheetData): String {
        val sb = StringBuilder("<worksheet xmlns=\"$NS_MAIN\"><sheetData>")
        sb.append("<row r=\"1\">")
        s.columns.forEachIndexed { j, name -> sb.append(cell("${colName(j)}1", name)) }
        sb.append("</row>")
        s.rows.forEachIndexed { i, row ->
            val r = i + 2
            sb.append("<row r=\"$r\">")
            row.forEachIndexed { j, v -> sb.append(cell("${colName(j)}$r", v)) }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    // ---------------- পড়া ----------------

    /** শিটের নাম → সারির তালিকা (প্রতিটি সারি: কলামের নাম → মান) */
    fun read(input: InputStream): Map<String, List<Map<String, String>>> {
        val files = HashMap<String, ByteArray>()
        ZipInputStream(input).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                if (!e.isDirectory) files[e.name] = z.readBytes()
            }
        }
        val workbook = files["xl/workbook.xml"] ?: throw IllegalArgumentException("সঠিক xlsx ফাইল নয়")
        val shared = files["xl/sharedStrings.xml"]?.let { parseShared(it) } ?: emptyList()

        // rId → ফাইলের পথ
        val targets = HashMap<String, String>()
        files["xl/_rels/workbook.xml.rels"]?.let { bytes ->
            val p = parser(bytes)
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                if (p.eventType == XmlPullParser.START_TAG && p.name == "Relationship") {
                    val id = p.getAttributeValue(null, "Id") ?: continue
                    val t = p.getAttributeValue(null, "Target") ?: continue
                    targets[id] = if (t.startsWith("/")) t.removePrefix("/") else "xl/$t"
                }
            }
        }

        val result = LinkedHashMap<String, List<Map<String, String>>>()
        val p = parser(workbook)
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType == XmlPullParser.START_TAG && p.name == "sheet") {
                val name = p.getAttributeValue(null, "name") ?: continue
                var rid: String? = null
                for (i in 0 until p.attributeCount) {
                    val an = p.getAttributeName(i)
                    if (an == "r:id" || an.endsWith(":id") || an == "id") rid = p.getAttributeValue(i)
                }
                val path = targets[rid] ?: continue
                val bytes = files[path] ?: continue
                result[name] = toRecords(parseSheet(bytes, shared))
            }
        }
        return result
    }

    private fun parser(bytes: ByteArray): XmlPullParser =
        Xml.newPullParser().apply { setInput(ByteArrayInputStream(bytes), "UTF-8") }

    private fun parseShared(bytes: ByteArray): List<String> {
        val out = mutableListOf<String>()
        val p = parser(bytes)
        var sb: StringBuilder? = null
        var inT = false
        var skip = 0 // রাবি/ফোনেটিক অংশের লেখা বাদ
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> sb = StringBuilder()
                    "rPh" -> skip++
                    "t" -> inT = true
                }
                XmlPullParser.TEXT -> if (inT && skip == 0) sb?.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "t" -> inT = false
                    "rPh" -> skip--
                    "si" -> {
                        out += sb?.toString().orEmpty()
                        sb = null
                    }
                }
            }
        }
        return out
    }

    /** সারি → (কলাম নম্বর → মান) */
    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<Map<Int, String>> {
        val rows = mutableListOf<Map<Int, String>>()
        val p = parser(bytes)
        var row: MutableMap<Int, String>? = null
        var col = 0
        var type: String? = null
        var value: StringBuilder? = null
        var inline: StringBuilder? = null
        var inV = false
        var inT = false
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> row = mutableMapOf()
                    "c" -> {
                        col = colIndex(p.getAttributeValue(null, "r") ?: "A1")
                        type = p.getAttributeValue(null, "t")
                        value = null
                        inline = null
                    }
                    "v" -> { inV = true; value = StringBuilder() }
                    "is" -> inline = StringBuilder()
                    "t" -> inT = true
                }
                XmlPullParser.TEXT -> {
                    if (inV) value?.append(p.text)
                    else if (inT) inline?.append(p.text)
                }
                XmlPullParser.END_TAG -> when (p.name) {
                    "v" -> inV = false
                    "t" -> inT = false
                    "c" -> {
                        val text = when (type) {
                            "s" -> value?.toString()?.trim()?.toIntOrNull()?.let { shared.getOrNull(it) }
                            "inlineStr" -> inline?.toString()
                            else -> value?.toString()
                        }
                        if (text != null) row?.put(col, text)
                    }
                    "row" -> {
                        row?.let { rows += it }
                        row = null
                    }
                }
            }
        }
        return rows
    }

    /** প্রথম সারি = কলামের নাম, বাকিগুলো ডেটা */
    private fun toRecords(rows: List<Map<Int, String>>): List<Map<String, String>> {
        val header = rows.firstOrNull() ?: return emptyList()
        val names = header.mapValues { it.value.trim() }
        return rows.drop(1).map { r ->
            val rec = LinkedHashMap<String, String>()
            r.forEach { (c, v) -> names[c]?.let { rec[it] = v } }
            rec
        }.filter { it.isNotEmpty() }
    }
}

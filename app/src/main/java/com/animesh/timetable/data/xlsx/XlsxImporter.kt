package com.animesh.timetable.data.xlsx

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Minimal .xlsx reader + timetable extractor. It unzips the workbook, reads shared
 * strings and the first worksheet into a grid, then applies the same block logic used
 * to build the bundled Module 5 data, so a spreadsheet in the same format yields a new
 * module's class offerings. No third-party dependencies are used.
 */
object XlsxImporter {

    data class Draft(
        val day: String,
        val start: String,
        val end: String,
        val slot: String,
        val section: String,
        val subject: String,
        val faculty: String,
        val room: String
    )

    private val DAYS = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
    // Disambiguation for faculty who teach more than one course (resolved by faculty, not column order).
    private val OVERRIDE = mapOf(
        "Prof. Milind M. Akarte" to "Operations Strategy",
        "Prof. Anju Singh" to "Sustainable Development for Business"
    )

    class ParseException(message: String) : Exception(message)

    fun parse(input: InputStream): List<Draft> {
        val entries = HashMap<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                val name = e.name
                if (name == "xl/sharedStrings.xml" || name.startsWith("xl/worksheets/sheet")) {
                    entries[name] = zip.readBytes()
                }
                e = zip.nextEntry
            }
        }
        val sheetName = entries.keys
            .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            .minOrNull() ?: throw ParseException("No worksheet found in the file.")

        val shared = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val grid = parseSheet(entries[sheetName]!!, shared)
        if (grid.size < 3) throw ParseException("The sheet looks empty or has an unexpected format.")
        return extract(grid)
    }

    // ---- XML parsing ----

    private fun newParser(bytes: ByteArray): XmlPullParser {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        return parser
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val list = ArrayList<String>()
        val p = newParser(bytes)
        var sb: StringBuilder? = null
        var inT = false
        var event = p.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> sb = StringBuilder()
                    "t" -> inT = true
                }
                XmlPullParser.TEXT -> if (inT) sb?.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "t" -> inT = false
                    "si" -> { list.add(sb?.toString() ?: ""); sb = null }
                }
            }
            event = p.next()
        }
        return list
    }

    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val rows = HashMap<Int, HashMap<Int, String>>()
        var maxRow = 0
        var maxCol = 0
        val p = newParser(bytes)
        var curRow = -1
        var colRef = ""
        var cellType = ""
        var vText: StringBuilder? = null
        var inlineText: StringBuilder? = null
        var inV = false
        var inInlineT = false
        var event = p.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> curRow = (p.getAttributeValue(null, "r")?.toIntOrNull() ?: (curRow + 2)) - 1
                    "c" -> {
                        colRef = p.getAttributeValue(null, "r") ?: ""
                        cellType = p.getAttributeValue(null, "t") ?: ""
                        vText = null
                        inlineText = null
                    }
                    "v" -> { inV = true; vText = StringBuilder() }
                    "t" -> if (cellType == "inlineStr") { inInlineT = true; inlineText = StringBuilder() }
                }
                XmlPullParser.TEXT -> {
                    if (inV) vText?.append(p.text)
                    if (inInlineT) inlineText?.append(p.text)
                }
                XmlPullParser.END_TAG -> when (p.name) {
                    "v" -> inV = false
                    "t" -> inInlineT = false
                    "c" -> {
                        val raw = vText?.toString()?.trim() ?: ""
                        val value = when (cellType) {
                            "s" -> raw.toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                            "inlineStr" -> inlineText?.toString() ?: ""
                            else -> raw
                        }
                        if (value.isNotEmpty() && curRow >= 0) {
                            val col = colIndex(colRef)
                            if (col >= 0) {
                                rows.getOrPut(curRow) { HashMap() }[col] = value
                                if (curRow > maxRow) maxRow = curRow
                                if (col > maxCol) maxCol = col
                            }
                        }
                    }
                }
            }
            event = p.next()
        }
        return (0..maxRow).map { r ->
            val row = rows[r]
            (0..maxCol).map { c -> row?.get(c) ?: "" }
        }
    }

    private fun colIndex(ref: String): Int {
        var col = 0
        var i = 0
        while (i < ref.length && ref[i].isLetter()) {
            col = col * 26 + (ref[i].uppercaseChar() - 'A' + 1)
            i++
        }
        return col - 1
    }

    // ---- Timetable extraction (mirrors the bundled-data generator) ----

    private fun extract(grid: List<List<String>>): List<Draft> {
        fun cell(r: Int, c: Int): String = grid.getOrNull(r)?.getOrNull(c)?.trim() ?: ""

        val headerRow = grid.indexOfFirst { it.isNotEmpty() && it[0].trim().equals("DAYS", true) }
        val ranges = buildRanges(grid, if (headerRow >= 0) headerRow else 0)
        if (ranges.isEmpty()) throw ParseException("Could not find the time-slot header row.")

        val dayRow = HashMap<String, Int>()
        for (r in grid.indices) {
            val d = cell(r, 0).uppercase()
            if (d in DAYS && d !in dayRow) dayRow[d] = r
        }
        if (dayRow.isEmpty()) throw ParseException("Could not find day rows (MONDAY..SATURDAY).")

        fun subRow(ds: Int, label: String): Int {
            for (r in ds until minOf(ds + 7, grid.size)) {
                if (cell(r, 1).equals(label, true)) return r
            }
            return -1
        }

        // Pass 1: learn faculty -> subjects from blocks where faculty count == subject count.
        val facToSubjects = HashMap<String, MutableSet<String>>()
        for (day in DAYS) {
            val ds = dayRow[day] ?: continue
            val slotsR = subRow(ds, "SLOTS"); val courseR = subRow(ds, "Course Name"); val facultyR = subRow(ds, "Faculty")
            if (courseR < 0) continue
            for ((a, b, _, _) in ranges) {
                val slotRaw = (a until b).firstNotNullOfOrNull { c -> cell(slotsR, c).ifEmpty { null } } ?: ""
                val course = (a until b).firstNotNullOfOrNull { c -> cell(courseR, c).ifEmpty { null } } ?: ""
                if (course.isEmpty() || course.lowercase().startsWith("lunch") || slotRaw.contains("surprise", true)) continue
                val subs = splitSubjects(course)
                val facs = (a until b).map { cell(facultyR, it).replace("\n", " ").trim() }.filter { it.isNotEmpty() }
                if (facs.size == subs.size) facs.forEachIndexed { i, f -> facToSubjects.getOrPut(f) { mutableSetOf() }.add(subs[i]) }
            }
        }

        fun resolveSectioned(faculty: String, subjects: List<String>, k: Int): String {
            OVERRIDE[faculty]?.let { if (it in subjects) return it }
            val candidates = subjects.filter { it in (facToSubjects[faculty] ?: emptySet()) }
            return when {
                candidates.size == 1 -> candidates[0]
                subjects.size == 1 -> subjects[0]
                else -> subjects.getOrNull(k) ?: subjects.last()
            }
        }

        val drafts = ArrayList<Draft>()
        for (day in DAYS) {
            val ds = dayRow[day] ?: continue
            val slotsR = subRow(ds, "SLOTS")
            val courseR = subRow(ds, "Course Name")
            val sectionR = subRow(ds, "Section")
            val facultyR = subRow(ds, "Faculty")
            val roomR = subRow(ds, "Class Room")
            if (courseR < 0) continue

            for ((a, b, st, et) in ranges) {
                val slotRaw = (a until b).firstNotNullOfOrNull { c -> cell(slotsR, c).ifEmpty { null } } ?: ""
                val course = (a until b).firstNotNullOfOrNull { c -> cell(courseR, c).ifEmpty { null } } ?: ""
                if (course.isEmpty() || course.lowercase().startsWith("lunch")) continue
                if (slotRaw.contains("surprise", true)) continue
                val subjects = splitSubjects(course)
                if (subjects.isEmpty()) continue
                val slot = slotRaw.replace(Regex("\\s*-\\s*[A-Z]\\s*$"), "").trim()

                fun fac(c: Int) = cell(facultyR, c).replace("\n", " ").trim()
                fun room(c: Int) = cell(roomR, c)
                val sectionPresent = sectionR >= 0 && (a until b).any { cell(sectionR, it).isNotEmpty() }

                if (!sectionPresent) {
                    val cols = (a until b).filter { fac(it).isNotEmpty() }
                    subjects.forEachIndexed { i, su ->
                        val c = cols.getOrNull(i) ?: cols.firstOrNull()
                        drafts.add(
                            Draft(day.lowercase().replaceFirstChar { it.uppercase() }, st, et, slot, "",
                                su, c?.let { fac(it) } ?: "", c?.let { room(it) } ?: "")
                        )
                    }
                } else {
                    val cols = (a until b).filter { fac(it).isNotEmpty() }
                    cols.forEachIndexed { k, c ->
                        val sec = cell(sectionR, c)
                        val su = resolveSectioned(fac(c), subjects, k)
                        drafts.add(
                            Draft(day.lowercase().replaceFirstChar { it.uppercase() }, st, et, slot, sec,
                                su, fac(c), room(c))
                        )
                    }
                }
            }
        }
        if (drafts.isEmpty()) throw ParseException("No classes were found. Is this the same timetable format?")
        return drafts
    }

    private data class Range(val a: Int, val b: Int, val start: String, val end: String)

    private fun buildRanges(grid: List<List<String>>, headerRow: Int): List<Range> {
        val header = grid.getOrNull(headerRow) ?: return emptyList()
        val starts = ArrayList<Int>()
        for (c in 2 until header.size) {
            val v = header[c].trim()
            if (v.isNotEmpty() && (v.contains("am", true) || v.contains("pm", true) ||
                        Regex("\\d{1,2}[:.]\\d{2}").containsMatchIn(v))
            ) starts.add(c)
        }
        return starts.mapIndexed { i, start ->
            val end = if (i + 1 < starts.size) starts[i + 1] else header.size
            val (s, e) = splitTimes(header[start].trim())
            Range(start, end, s, e)
        }
    }

    private fun splitTimes(label: String): Pair<String, String> {
        val parts = when {
            label.contains(" to ", true) -> label.split(Regex("(?i)\\s+to\\s+"), 2)
            label.contains("-") -> label.split("-", limit = 2)
            else -> listOf(label, "")
        }
        return normalizeTime(parts.getOrElse(0) { "" }) to normalizeTime(parts.getOrElse(1) { "" })
    }

    private fun normalizeTime(token: String): String {
        var t = token.trim().lowercase().replace(".", ":")
        val pm = t.contains("pm")
        val am = t.contains("am")
        t = t.replace("am", "").replace("pm", "").trim()
        val m = Regex("(\\d{1,2}):?(\\d{2})?").find(t) ?: return token.trim()
        var hh = m.groupValues[1].toIntOrNull() ?: return token.trim()
        val mm = m.groupValues[2].toIntOrNull() ?: 0
        if (pm && hh < 12) hh += 12
        if (am && hh == 12) hh = 0
        return "%02d:%02d".format(hh, mm)
    }

    private fun splitSubjects(s: String): List<String> {
        val cleaned = s.replace("\n", " ").trim()
        if (cleaned.isEmpty()) return emptyList()
        val parts = if (cleaned.contains("/")) cleaned.split("/") else cleaned.split(",")
        return parts.map { it.trim(' ', '.') }.filter { it.isNotEmpty() }
    }
}

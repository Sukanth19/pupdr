package dev.suks.pupdr.domain

/** One parsed fragment of a line. name may be null when the line only carries sets/reps. */
internal data class Parsed(val name: String?, val sets: Int?, val reps: String?) {
    fun toItem(): RoutineItemDto? {
        val n = name ?: return null
        val timed = reps != null && (reps.endsWith("s") || reps.endsWith("min") || reps.endsWith("m"))
        return RoutineItemDto(
            name = n,
            sets = (sets ?: if (timed) 1 else 3).coerceIn(1, 50),
            reps = reps ?: "10",
        )
    }
}

/** Parses a single exercise line such as "B2) DB incline press – 3 sets x 8-10 reps @ 25kg (rest 90s)". */
internal object LineParser {
    private val I = RegexOption.IGNORE_CASE
    private const val RANGE = """\d{1,3}(?:\s*(?:-|–|—|to)\s*\d{1,3})?"""
    private const val UNIT = """(reps?|repetitions|s|secs?|seconds|mins?|minutes|m)?"""

    private val bullet = Regex("""^\s*(?:[-*•·▪►>+]+|\[[ xX✓]?]|[A-Za-z]?\d{1,2}[a-z]?\s*[.):\]]|[A-Za-z]\s*[.)])\s*""")
    private val leadingSymbols = Regex("""^[^\p{L}\p{N}(]+""")

    private val noise = listOf(
        Regex("""\brest\b\s*[:=]?\s*\d+(?:\.\d+)?\s*(?:s|secs?|seconds|mins?|minutes|m|')?(?:\s*(?:-|to)\s*\d+\s*(?:s|secs?|seconds|mins?|minutes|m)?)?""", I),
        Regex("""\b(?:rpe|rir)\s*[:=@]?\s*\d+(?:\.\d+)?(?:\s*-\s*\d+(?:\.\d+)?)?""", I),
        Regex("""\btempo\s*[:=]?\s*[\dxX-]+""", I),
        Regex("""@\s*\d+(?:\.\d+)?\s*(?:kgs?|lbs?|pounds|%)?""", I),
        Regex("""(?<![\w.])\d+(?:\.\d+)?\s*(?:kgs?|lbs?|pounds|%\s*1rm|%)(?![\w])""", I),
        Regex("""\b(?:each|per)\s+(?:side|arm|leg)\b|/\s*(?:side|arm|leg)\b""", I),
    )

    private val setsOfReps = Regex("""(?<![\w.])(\d{1,2})\s*(?:sets?|rounds?)\s*(?:of|x|×|,|:|-|–|for)?\s*($RANGE)\s*$UNIT(?![\w])""", I)
    private val repsForSets = Regex("""(?<![\w.])($RANGE)\s*(?:reps?|repetitions)\s*(?:x|×|,|for|of|-|–|\*)?\s*(\d{1,2})\s*(?:sets?|rounds?)(?![\w])""", I)
    private val nByM = Regex("""(?<![\w.])(\d{1,2})\s*[x×*]\s*($RANGE|amrap|max|failure)\s*$UNIT(?![\w])""", I)
    private val scheme = Regex("""(?<![\w./])(\d{1,3}(?:\s*/\s*\d{1,3}){1,9}|\d{1,3}(?:\s*[-–]\s*\d{1,3}){2,9})\s*(?:reps?)?(?![\w/])""", I)
    private val setsOnly = Regex("""(?<![\w.])(\d{1,2})\s*(?:sets?|rounds?)(?![\w])|\b(?:sets?|rounds)\s*[:=\-–]\s*(\d{1,2})""", I)
    private val repsOnly = Regex("""(?<![\w.])($RANGE)\s*(?:reps?|repetitions)(?![\w])|\b(?:reps?|repetitions)\s*[:=\-–]\s*($RANGE)""", I)
    private val timeOnly = Regex("""(?<![\w.])(\d{1,3}(?:\.\d)?)\s*(s|secs?|seconds|mins?|minutes)(?![\w])""", I)
    private val amrap = Regex("""\b(amrap|to failure|until failure|max reps)\b""", I)
    private val columns = Regex("""^(.*?\p{L}.*?)[\s,;|\t]+(\d{1,2})[\s,;|\t/x×]+($RANGE)\s*$""", I)
    private val trailingNumber = Regex("""^(.*?\p{L}.*?)[\s,;:|\t\-–]+(\d{1,3})\s*$""")
    private val parens = Regex("""[(\[]([^)\]]*)[)\]]""")
    private val parenNoise = Regex("""\d|rest|rpe|superset|optional|each|side|warm|drop|tempo|failure|amrap|alt""", I)

    fun hasSetInfo(s: String): Boolean = listOf(setsOfReps, repsForSets, nByM, setsOnly, repsOnly).any { it.containsMatchIn(s) }

    fun normalizeReps(r: String, unit: String? = null): String {
        val base = r.lowercase().replace(Regex("""\s*(?:-|–|—|to)\s*"""), "-").replace(Regex("""\s*/\s*"""), "/").replace(" ", "")
        if (base == "amrap" || base == "max") return "AMRAP"
        if (base == "failure") return "failure"
        val u = when (unit?.lowercase()) {
            "s", "sec", "secs", "seconds" -> "s"
            "min", "mins", "minutes" -> "min"
            "m" -> "m"
            else -> ""
        }
        // a reps field like "30 sec" or "1 min"
        Regex("""^(\d+(?:\.\d)?)(s|secs?|seconds|mins?|minutes)$""").matchEntire(base)?.let { return it.groupValues[1] + if (it.groupValues[2].startsWith("m")) "min" else "s" }
        return base + u
    }

    fun parse(segment: String): Parsed? {
        var s = segment.replace(Regex("""\*\*|__|`"""), "").trim()
        s = s.replace(bullet, "").replace(leadingSymbols, "")
        for (r in noise) s = s.replace(r, " ")
        var sets: Int? = null
        var reps: String? = null

        fun cut(m: MatchResult) { s = s.removeRange(m.range).let { " $it " } }

        setsOfReps.find(s)?.let { m -> sets = m.groupValues[1].toInt(); reps = normalizeReps(m.groupValues[2], m.groupValues[3]); cut(m) }
        if (sets == null) repsForSets.find(s)?.let { m -> reps = normalizeReps(m.groupValues[1]); sets = m.groupValues[2].toInt(); cut(m) }
        if (sets == null) nByM.find(s)?.let { m -> sets = m.groupValues[1].toInt(); reps = normalizeReps(m.groupValues[2], m.groupValues[3]); cut(m) }
        if (sets == null && reps == null) scheme.find(s)?.let { m ->
            val nums = Regex("""\d+""").findAll(m.groupValues[1]).map { it.value }.toList()
            sets = nums.size
            reps = if (nums.distinct().size == 1) nums.first() else nums.joinToString("/")
            cut(m)
        }
        if (sets == null) setsOnly.find(s)?.let { m -> sets = (m.groupValues[1].ifEmpty { m.groupValues[2] }).toInt(); cut(m) }
        if (reps == null) repsOnly.find(s)?.let { m -> reps = normalizeReps(m.groupValues[1].ifEmpty { m.groupValues[2] }); cut(m) }
        if (reps == null) timeOnly.find(s)?.let { m -> reps = normalizeReps(m.groupValues[1], m.groupValues[2]); cut(m) }
        if (reps == null) amrap.find(s)?.let { m -> reps = if (m.value.lowercase() == "amrap" || m.value.lowercase() == "max reps") "AMRAP" else "failure"; cut(m) }
        if (sets == null && reps == null) {
            columns.matchEntire(s.trim())?.let { m -> s = m.groupValues[1]; sets = m.groupValues[2].toInt(); reps = normalizeReps(m.groupValues[3]) }
                ?: trailingNumber.matchEntire(s.trim())?.let { m -> s = m.groupValues[1]; reps = m.groupValues[2] }
        }

        s = s.replace(parens) { m -> if (parenNoise.containsMatchIn(m.groupValues[1])) " " else " ${m.groupValues[1]} " }
        s = s.replace(Regex("""(?<![\w])[x×@](?![\w])""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\s+"""), " ").trim()
            .trim { it.isWhitespace() || it in "-–—:,;|/.·•*+=" }
            .trim()
        val name = s.takeIf { it.any(Char::isLetter) && it.length <= 70 }?.let(::prettify)
        if (name == null && sets == null && reps == null) return null
        return Parsed(name, sets?.coerceIn(1, 50), reps)
    }

    fun prettify(n: String): String =
        if (n == n.lowercase() || (n == n.uppercase() && n.length > 4)) n.lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } } else n
}

/** Line-by-line text importer: headers start routines, everything else becomes exercises. */
internal class TextImport(private val isKnown: (String) -> Boolean) {
    private val I = RegexOption.IGNORE_CASE
    private val dayHeader = Regex("""^(?:(?:day|workout|session|routine|template|phase)\s*(?:#?\d+|[a-e])\b|(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tues?|wed|thu|thurs?|fri|sat|sun)\b)""", I)
    private val groups = "chest|back|shoulders?|arms?|core|abs|biceps|triceps|legs?|glutes?|cardio|calves|hamstrings|quads|delts|traps|forearms|lats"
    private val keywordHeader = Regex(
        """^(?:push|pull|legs?|leg day|upper(?: body)?|lower(?: body)?|full[- ]?body|posterior chain|hiit|conditioning|$groups)""" +
            """(?:\s+(?:day|workout|session|[a-d1-9]))?(?:\s*(?:&|and|\+|/|,)\s*(?:$groups))*(?:\s+(?:day|workout|session))?(?:\s+[-–—]\s+.*)?$""",
        I,
    )
    private val skipHeader = Regex("""^(?:warm[- ]?ups?|cool[- ]?downs?|accessor(?:y|ies)|finishers?|main(?: lifts?)?|stretch(?:ing)?|mobility)$""", I)
    private val ignore = Regex("""^(?:week\s*\d+.*|rest(?:\s.*)?|off(?: day)?|notes?|tips?|instructions?|(?:a|b|c)\s*block)\s*:?$""", I)
    private val attribute = Regex("""^(sets?|reps?|repetitions|rounds|weight|load|rest|rpe|rir|tempo|notes?|time|duration)\s*[:=\-–]\s*(.+)$""", I)

    private val out = mutableListOf<RoutineDto>()
    private var name: String? = null
    private var items = mutableListOf<RoutineItemDto>()

    private fun flush() {
        if (items.isNotEmpty()) out += RoutineDto(name ?: "Imported routine", exercises = items)
        items = mutableListOf()
    }

    private fun start(n: String) {
        flush()
        val clean = n.trim().trim { it in "-–—:#*_ " }
        name = (if (clean == clean.uppercase()) clean.lowercase().replaceFirstChar { it.uppercase() } else clean).ifEmpty { null }
    }

    fun parse(text: String): List<RoutineDto> {
        var table: Table? = null
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.contains("http", ignoreCase = true)) continue

            // tables (markdown, tab or csv with a header row)
            val cells = Table.cells(line)
            if (cells != null) {
                val header = Table.header(cells)
                if (header != null) { table = header; continue }
                val t = table
                if (t != null) {
                    if (cells.all { it.matches(Regex("""[:\-\s=]*""")) }) continue
                    t.row(cells)?.let { (routine, item) ->
                        if (routine != null && routine != name) start(routine)
                        items += item
                    }
                    continue
                }
            } else if (table != null && !line.startsWith("#")) {
                table = null
            }
            if (line.startsWith("#")) table = null

            val attr = attribute.matchEntire(line)
            if (attr != null) { applyAttribute(attr.groupValues[1].lowercase(), attr.groupValues[2]); continue }
            if (ignore.matches(line.replace("*", ""))) continue

            val header = header(line)
            if (header != null) {
                val (h, rest) = header
                if (!skipHeader.matches(h)) start(h)
                if (rest.isNotBlank()) addItems(rest)
                continue
            }
            addItems(line)
        }
        flush()
        return out
    }

    /** "Sets: 4", "Reps - 8-10", "Time: 45s" lines that follow an exercise name. */
    private fun applyAttribute(key: String, value: String) {
        val last = items.lastOrNull() ?: return
        if (!(key.startsWith("set") || key.startsWith("rep") || key == "rounds" || key == "time" || key == "duration")) return
        val p = LineParser.parse("$key: $value")
        val sets = p?.sets ?: if (key.startsWith("set") || key == "rounds") Regex("""\d+""").find(value)?.value?.toIntOrNull() else null
        items[items.lastIndex] = last.copy(sets = (sets ?: last.sets).coerceIn(1, 50), reps = p?.reps ?: last.reps)
    }

    private fun header(line: String): Pair<String, String>? {
        val markdown = line.startsWith("#")
        val bold = Regex("""^\*\*(.+?)\*\*:?$|^__(.+?)__:?$""").matchEntire(line)
        val l = line.trimStart('#').replace("**", "").replace("__", "").trim()
        if (l.isEmpty()) return null
        val colon = l.indexOf(':')
        val head = if (colon >= 0) l.substring(0, colon).trim() else l
        val rest = if (colon >= 0) l.substring(colon + 1).trim() else ""
        val headHasSets = LineParser.hasSetInfo(head)
        return when {
            markdown -> head to rest
            headHasSets -> null
            bold != null -> head to rest
            colon >= 0 && rest.isEmpty() -> head to ""
            dayHeader.containsMatchIn(head) && head.length <= 50 -> head to rest
            keywordHeader.matches(head) -> head to rest
            rest.isEmpty() && isAllCapsTitle(head) && !isKnown(head) -> head to ""
            else -> null
        }
    }

    private fun isAllCapsTitle(s: String) =
        s.any(Char::isLetter) && s == s.uppercase() && s.length >= 3 && s.split(' ').size <= 5 && s.none(Char::isDigit)

    private fun addItems(line: String) {
        for (seg in split(line)) {
            val p = LineParser.parse(seg) ?: continue
            val item = p.toItem()
            if (item != null) items += item
            else if (items.isNotEmpty()) {
                // a line like "4 x 8" right under an exercise name
                val last = items.last()
                items[items.lastIndex] = last.copy(sets = p.sets ?: last.sets, reps = p.reps ?: last.reps)
            }
        }
    }

    private fun split(line: String): List<String> {
        fun good(parts: List<String>) = parts.size >= 2 && parts.all { part ->
            part.any(Char::isLetter) && (LineParser.hasSetInfo(part) || isKnown(LineParser.parse(part)?.name ?: part))
        }
        val hard = line.split(Regex("""\s*(?:;|\s\+\s|•|\|)\s*""")).filter { it.isNotBlank() }
        val base = if (hard.size > 1 && hard.all { p -> p.any(Char::isLetter) }) hard else listOf(line)
        return base.flatMap { part ->
            val commas = part.split(Regex("""\s*,\s*""")).filter { it.isNotBlank() }
            if (good(commas)) commas else listOf(part)
        }
    }
}

/** Column layout of a table whose header row names the columns. */
internal class Table(private val ex: Int, private val sets: Int?, private val reps: Int?, private val scheme: Int?, private val routine: Int?) {
    fun row(cells: List<String>): Pair<String?, RoutineItemDto>? {
        val name = cells.getOrNull(ex)?.trim().orEmpty()
        if (name.isEmpty() || !name.any(Char::isLetter)) return null
        val schemeText = listOfNotNull(
            sets?.let { cells.getOrNull(it) }?.let { v -> Regex("""\d+""").find(v)?.value?.let { "$it sets" } },
            reps?.let { cells.getOrNull(it) }?.takeIf { it.isNotBlank() }?.let { v -> if (v.any(Char::isLetter)) v else "$v reps" },
            scheme?.let { cells.getOrNull(it) },
        ).joinToString(" ")
        val item = LineParser.parse("$name $schemeText")?.toItem() ?: return null
        return routine?.let { cells.getOrNull(it)?.trim()?.ifEmpty { null } } to item
    }

    companion object {
        private val exH = Regex("""^(exercises?|exercise name|name|movements?|lifts?)$""", RegexOption.IGNORE_CASE)
        private val setsH = Regex("""^(sets?|#\s*sets|set count|no\.? of sets)$""", RegexOption.IGNORE_CASE)
        private val repsH = Regex("""^(reps?|repetitions|rep range|reps/time|target|time|duration)$""", RegexOption.IGNORE_CASE)
        private val schemeH = Regex("""^(sets\s*[x×]\s*reps|scheme|prescription|volume|sets/reps)$""", RegexOption.IGNORE_CASE)
        private val routineH = Regex("""^(day|routine|workout|session|split|template)$""", RegexOption.IGNORE_CASE)

        fun cells(line: String): List<String>? {
            val parts = when {
                line.contains('|') -> line.trim().trim('|').split('|')
                line.contains('\t') -> line.split('\t')
                line.contains(',') || line.contains(';') -> csv(line, if (line.count { it == ';' } > line.count { it == ',' }) ';' else ',')
                else -> return null
            }.map { it.trim().trim('"').trim() }
            return parts.takeIf { it.size >= 2 }
        }

        private fun csv(line: String, sep: Char): List<String> {
            val out = mutableListOf<String>()
            val cur = StringBuilder()
            var quoted = false
            for (c in line) when {
                c == '"' -> quoted = !quoted
                c == sep && !quoted -> { out += cur.toString(); cur.clear() }
                else -> cur.append(c)
            }
            out += cur.toString()
            return out
        }

        fun header(cells: List<String>): Table? {
            val ex = cells.indexOfFirst { exH.matches(it.trim()) }
            if (ex < 0) return null
            fun idx(r: Regex) = cells.indexOfFirst { r.matches(it.trim()) }.takeIf { it >= 0 }
            val t = Table(ex, idx(setsH), idx(repsH), idx(schemeH), idx(routineH))
            return if (t.sets != null || t.reps != null || t.scheme != null || t.routine != null) t else null
        }
    }
}

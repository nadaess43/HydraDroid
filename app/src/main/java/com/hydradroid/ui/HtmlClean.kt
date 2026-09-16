package com.hydradroid.ui

// Чистка Steam/HTML-описаний для телефона: теги, сущности, мусорные пробелы.
// Было: «разметка как в коде» (Cyberpunk и др.) — сырой detailed_description.
private val TagRegex = Regex("<[^>]*>")
private val ScriptRegex = Regex("<(script|style)[^>]*>.*?</\\1>", RegexOption.DOT_MATCHES_ALL)
private val BrRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val BlockEndRegex = Regex("</(p|div|li|ul|ol|h\\d|tr)>", RegexOption.IGNORE_CASE)
private val MultiSpaceRegex = Regex("[ \\t\\x0B\\f\\r]+")
private val MultiNlRegex = Regex("\\n{3,}")
private val NumericEntityRegex = Regex("&#(\\d+);")
private val HexEntityRegex = Regex("&#x([0-9a-fA-F]+);")

private val NamedEntities = mapOf(
    "&nbsp;" to " ", "&amp;" to "&", "&lt;" to "<",
    "&gt;" to ">", "&quot;" to "\"", "&#39;" to "'",
    "&apos;" to "'", "&laquo;" to "«", "&raquo;" to "»",
    "&hellip;" to "…", "&mdash;" to "—", "&ndash;" to "–",
    "&copy;" to "©", "&reg;" to "®", "&trade;" to "™"
)

fun cleanHtml(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    var s = raw.replace(ScriptRegex, " ")
    s = s.replace(BrRegex, "\n").replace(BlockEndRegex, "\n")
    s = s.replace(TagRegex, "")
    NamedEntities.forEach { (k, v) -> s = s.replace(k, v) }
    s = NumericEntityRegex.replace(s) {
        it.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: ""
    }
    s = HexEntityRegex.replace(s) {
        it.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: ""
    }
    s = s.replace(MultiSpaceRegex, " ")
    s = s.replace(MultiNlRegex, "\n\n")
    return s.trim()
}

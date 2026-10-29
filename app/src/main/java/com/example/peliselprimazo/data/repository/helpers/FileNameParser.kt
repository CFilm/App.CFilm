package com.example.peliselprimazo.data.repository.helpers

import java.util.regex.Pattern

data class InternalFileInfo(
    val cleanTitle: String,
    val year: String?,
    val season: Int?,
    val episode: Int?,
    val isTv: Boolean,
    val isAnime: Boolean,
    val language: String = "Latino"
)

object FileNameParser {
    fun extractShowNameFromPath(path: String): String? {
        try {
            val parts = path.replace("\\", "/").split("/").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val parent = parts[parts.size - 2]
                if (!parent.equals("anime", true) && !parent.equals("series", true) && !parent.equals("tv", true) && !parent.equals("peliculas", true) && !parent.startsWith("season", true) && !parent.startsWith("temporada", true)) {
                    return parent
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun parseFileName(fileName: String): InternalFileInfo {
        val nameWithoutExt = fileName.replace(Regex("\\.(mp4|mkv|avi|mov|wmv|flv|webm|m3u8)$", RegexOption.IGNORE_CASE), "")
        var name = nameWithoutExt.replace(Regex("[._\\-/]"), " ")

        val yearPattern = Pattern.compile("\\b(19|20)\\d{2}\\b")
        val yearMatcher = yearPattern.matcher(name)
        val year = if (yearMatcher.find()) yearMatcher.group() else null

        var season: Int? = null
        var episode: Int? = null
        var seMatch: String? = null

        // Pattern 1: s1e2, S01E02, s1.e2, s1 e2 (case-insensitive)
        val p1 = Pattern.compile("(?i)s(\\d{1,2})[\\s._-]*e(\\d{1,3})")
        val m1 = p1.matcher(name)
        if (m1.find()) {
            seMatch = m1.group()
            season = m1.group(1)?.toIntOrNull()
            episode = m1.group(2)?.toIntOrNull()
        }

        // Pattern 2: 1x02
        if (season == null || episode == null) {
            val p2 = Pattern.compile("(?i)\\b(\\d{1,2})x(\\d{1,3})\\b")
            val m2 = p2.matcher(name)
            if (m2.find()) {
                seMatch = m2.group()
                season = m2.group(1)?.toIntOrNull()
                episode = m2.group(2)?.toIntOrNull()
            }
        }

        // Pattern 3: Season 1 Episode 2 / Temp 1 Cap 2
        if (season == null || episode == null) {
            val p3 = Pattern.compile("(?i)(?:season|temp|temporada)\\s*(\\d{1,2})[\\s._-]*(?:ep|episodio|cap|capitulo|e)?\\s*(\\d{1,3})")
            val m3 = p3.matcher(name)
            if (m3.find()) {
                seMatch = m3.group()
                season = m3.group(1)?.toIntOrNull()
                episode = m3.group(2)?.toIntOrNull()
            }
        }

        // Pattern 4: Episode / Cap / Ep number standalone (e.g. "ep 4", "cap 4", "e04")
        if (season == null || episode == null) {
            val p4 = Pattern.compile("(?i)\\b(?:ep|episodio|cap|capitulo|e)\\s*(\\d{1,3})\\b")
            val m4 = p4.matcher(name)
            if (m4.find()) {
                seMatch = m4.group()
                season = 1
                episode = m4.group(1)?.toIntOrNull()
            }
        }

        // Pattern 5: Dash followed by number (e.g. " - 04")
        if (season == null || episode == null) {
            val p5 = Pattern.compile("\\b-\\s*(\\d{1,3})\\b")
            val m5 = p5.matcher(name)
            if (m5.find()) {
                val num = m5.group(1)?.toIntOrNull()
                if (num != null && num in 1..999 && num != year?.toIntOrNull()) {
                    season = 1
                    episode = num
                    seMatch = m5.group()
                }
            }
        }

        val isAnime = name.lowercase().contains("anime") || name.lowercase().contains("keroro") || name.lowercase().contains("tokyo")
        val isTv = season != null || name.lowercase().contains("tv") || isAnime

        if (isTv || isAnime) {
            if (season == null) season = 1
            if (episode == null) episode = 1
        }

        var cleanTitle = name
        
        if (seMatch != null) {
            val index = cleanTitle.indexOf(seMatch)
            if (index != -1) {
                cleanTitle = cleanTitle.substring(0, index)
            }
        }
        
        if (year != null) {
            cleanTitle = cleanTitle.replace(year, "")
        }

        val lower = name.lowercase()
        val language = when {
            lower.contains("ingles") || lower.contains("english") -> "Inglés"
            lower.contains("aleman") || lower.contains("german") -> "Alemán"
            lower.contains("frances") || lower.contains("french") -> "Francés"
            lower.contains("japones") || lower.contains("japanese") -> "Japonés"
            lower.contains("castellano") -> "Castellano"
            lower.contains("sub") || lower.contains("subtitulado") -> "Subtitulado"
            lower.contains("latino") || lower.contains("lat") -> "Latino"
            else -> "Latino"
        }

        val noiseTags = listOf(
            "1080p", "720p", "480p", "h264", "h265", "x264", "x265", 
            "bluray", "webrip", "latino", "español", "castellano", "dual", 
            "sub", "subs", "subtitulado", "multi", "hdrip", "dvdrip", "remux", "streamtape",
            "ingles", "english", "aleman", "german", "frances", "french", "japones", "japanese"
        )
        noiseTags.forEach { tag ->
            cleanTitle = cleanTitle.replace(Regex("(?i)\\b$tag\\b"), "")
        }

        cleanTitle = cleanTitle.replace(Regex("[()\\[\\]{}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            
        if (cleanTitle.isEmpty()) cleanTitle = nameWithoutExt.trim()

        return InternalFileInfo(cleanTitle, year, season, episode, isTv, isAnime, language)
    }
}

package com.example.peliselprimazo.ui.screens.home

import com.example.peliselprimazo.domain.model.Movie
import java.util.Random

data class HomeSection(
    val title: String,
    val items: List<Movie>,
    val isWatchingSection: Boolean = false
)

object HomeSectionBuilder {

    private val dailySeed: Int
        get() = (System.currentTimeMillis() / (24 * 60 * 60 * 1000L)).toInt()

    fun createDailyRotatedSelection(list: List<Movie>, limit: Int = 12, seedOffset: Int = 0): List<Movie> {
        if (list.isEmpty()) return emptyList()
        val random = Random((dailySeed + seedOffset).toLong())
        return list.shuffled(random).take(limit)
    }

    private fun pickDeduplicated(
        candidates: List<Movie>,
        limit: Int,
        usedIds: MutableSet<Int>
    ): List<Movie> {
        val unused = candidates.filter { it.id !in usedIds }.take(limit)
        if (unused.isNotEmpty()) {
            usedIds.addAll(unused.map { it.id })
            return unused
        }
        val fallback = candidates.take(limit)
        usedIds.addAll(fallback.map { it.id })
        return fallback
    }

    fun createDiverseSelection(list: List<Movie>): List<Movie> {
        if (list.isEmpty()) return emptyList()
        val groupedByGenre = mutableMapOf<String, MutableList<Movie>>()
        list.forEach { movie ->
            val primaryGenre = movie.genres.firstOrNull() ?: "General"
            groupedByGenre.getOrPut(primaryGenre) { mutableListOf<Movie>() }.add(movie)
        }
        
        groupedByGenre.values.forEach { group -> group.sortByDescending { it.rating } }
        
        val result = mutableListOf<Movie>()
        var addedAny = true
        var index = 0
        while (result.size < 15 && addedAny) {
            addedAny = false
            for (group in groupedByGenre.values) {
                if (index < group.size && result.none { it.id == group[index].id }) {
                    result.add(group[index])
                    addedAny = true
                    if (result.size >= 15) break
                }
            }
            index++
        }
        return if (result.isNotEmpty()) result else createDailyRotatedSelection(list, 15)
    }

    fun createSections(list: List<Movie>, watching: List<Movie>, trends: List<Movie>): List<HomeSection> {
        if (list.isEmpty()) return emptyList()
        val usedIds = mutableSetOf<Int>()

        // 1. Continuar Viendo
        val watchingItems = watching.take(12)

        // 2. Recién agregados
        val newlyAdded = pickDeduplicated(list, 12, usedIds)

        // 3. Recomendados del Día
        val userLikedOrWatched = list.filter { it.isLiked || it.isSaved || it.isWatching }
        val favoriteGenres = userLikedOrWatched.flatMap { it.genres }.groupingBy { it }.eachCount()
        val personalizedCandidates = if (favoriteGenres.isNotEmpty()) {
            list.filter { !it.isFinished && !it.isWatching }
                .sortedByDescending { movie ->
                    var score = movie.rating
                    movie.genres.forEach { genre ->
                        val count = favoriteGenres[genre] ?: 0
                        score += count * 2.0
                    }
                    if (movie.isWatchLater) score += 3.0
                    score
                }
        } else {
            createDailyRotatedSelection(list, list.size, seedOffset = 10)
        }
        val personalizedForYou = pickDeduplicated(personalizedCandidates, 12, usedIds)

        // 4. Tendencias globales
        val trendCandidates = if (trends.isNotEmpty()) trends else createDailyRotatedSelection(list, list.size, seedOffset = 20)
        val globalTrends = pickDeduplicated(trendCandidates, 12, usedIds)

        // 5. Estrenos
        val sortedByRating = list.sortedByDescending { it.rating }
        val exclusives = pickDeduplicated(list.sortedByDescending { it.releaseDate }, 12, usedIds)
        val topRated = pickDeduplicated(sortedByRating, 12, usedIds)
        
        val watchLaterCandidates = list.filter { it.isWatchLater }
        val watchLaterItems = pickDeduplicated(watchLaterCandidates, 12, usedIds)

        val dailySpecials = pickDeduplicated(createDailyRotatedSelection(sortedByRating, sortedByRating.size, seedOffset = 30), 12, usedIds)
        val masterWorks = pickDeduplicated(sortedByRating.filter { it.rating >= 7.5 }.let { if (it.isNotEmpty()) it else sortedByRating }, 12, usedIds)

        return buildList {
            if (watchingItems.isNotEmpty()) add(HomeSection("🍿 Continuar Viendo", watchingItems, isWatchingSection = true))
            if (newlyAdded.isNotEmpty()) add(HomeSection("🆕 Recién Agregados en CFilm", newlyAdded))
            if (personalizedForYou.isNotEmpty()) add(HomeSection("✨ Recomendados del Día", personalizedForYou))
            if (globalTrends.isNotEmpty()) add(HomeSection("🔥 Tendencias Globales", globalTrends))
            if (exclusives.isNotEmpty()) add(HomeSection("🚀 Estrenos & Novedades", exclusives))
            if (topRated.isNotEmpty()) add(HomeSection("⭐ Los Más Valorados", topRated))
            if (watchLaterItems.isNotEmpty()) add(HomeSection("🕒 Guardados Para Ver Después", watchLaterItems))
            if (dailySpecials.isNotEmpty()) add(HomeSection("❤️ Selección Especial para Hoy", dailySpecials))
            if (masterWorks.isNotEmpty()) add(HomeSection("🏆 Obras Maestras De La Pantalla", masterWorks))

            val genreSections = listOf(
                "💥 Acción & Adrenalina" to listOf("Acción"),
                "🛸 Ciencia Ficción & Futuro" to listOf("Ciencia Ficción", "Sci-Fi"),
                "😱 Terror & Horror Nocturno" to listOf("Terror", "Horror"),
                "😂 Comedia & Risas" to listOf("Comedia"),
                "🎭 Dramas Intensos" to listOf("Drama"),
                "🦄 Fantasía & Mundos Mágicos" to listOf("Fantasía", "Fantasy"),
                "🔍 Misterio & Policías" to listOf("Misterio", "Crimen"),
                "💘 Romances & Amores" to listOf("Romance"),
                "🌴 Aventuras & Exploración" to listOf("Aventura"),
                "👾 Animación & Dibujos" to listOf("Animación"),
                "🦸‍♂️ Superhéroes & Cómics" to listOf("Acción", "Fantasía"),
                "🔪 Suspenso & Thrillers" to listOf("Suspenso"),
                "📜 Hechos Reales & Historia" to listOf("Historia", "Biografía"),
                "👪 Cine & Entretenimiento Familiar" to listOf("Familia"),
                "🌌 Viajes En El Tiempo & Espacio" to listOf("Ciencia Ficción"),
                "🥋 Artes Marciales & Combate" to listOf("Acción"),
                "💎 Joyas Ocultas De La Pantalla" to listOf("Drama"),
                "🎶 Música & Espectáculo" to listOf("Música"),
                "🔥 Éxitos De Cartelera" to listOf("Acción", "Aventura"),
                "🧠 Enigmas & Mente" to listOf("Misterio", "Suspenso"),
                "🏹 Leyendas & Reinos" to listOf("Fantasía", "Aventura"),
                "🌊 Odiseas & Naturaleza" to listOf("Aventura"),
                "🏙️ Historias Urbanas" to listOf("Drama", "Crimen"),
                "💣 Tensión Al Límite" to listOf("Suspenso", "Acción"),
                "🌟 Favoritas De La Crítica" to listOf("Drama"),
                "🐉 Universos Fantásticos" to listOf("Fantasía"),
                "🎬 Grandes Producciones" to listOf("Acción", "Ciencia Ficción")
            )

            genreSections.forEachIndexed { idx, (title, genres) ->
                val filtered = list.filter { m -> genres.any { g -> m.genres.any { mg -> mg.contains(g, ignoreCase = true) } } }
                val candidateList = if (filtered.isNotEmpty()) {
                    createDailyRotatedSelection(filtered, filtered.size, seedOffset = 100 + idx)
                } else {
                    createDailyRotatedSelection(sortedByRating, sortedByRating.size, seedOffset = 200 + idx)
                }
                val content = pickDeduplicated(candidateList, 12, usedIds)
                if (content.isNotEmpty()) {
                    add(HomeSection(title, content))
                }
            }
        }.take(35)
    }

    fun createMovieSections(list: List<Movie>): List<HomeSection> {
        if (list.isEmpty()) return emptyList()
        val usedIds = mutableSetOf<Int>()

        val newlyAdded = pickDeduplicated(list, 15, usedIds)
        val topMovies = list.sortedByDescending { it.rating }
        val recentMovies = list.sortedByDescending { it.releaseDate }
        val diverseRecommended = pickDeduplicated(createDailyRotatedSelection(list, list.size, seedOffset = 50), 15, usedIds)
        val highRatedMovies = pickDeduplicated(topMovies.drop(3).let { if (it.isNotEmpty()) it else topMovies }, 15, usedIds)
        val dailyBlockbusters = pickDeduplicated(createDailyRotatedSelection(topMovies, topMovies.size, seedOffset = 60), 15, usedIds)
        val popularMovies = pickDeduplicated(topMovies, 15, usedIds)
        val recentEstrenos = pickDeduplicated(recentMovies, 15, usedIds)

        return buildList {
            if (newlyAdded.isNotEmpty()) add(HomeSection("🆕 Últimas Películas Agregadas", newlyAdded))
            if (diverseRecommended.isNotEmpty()) add(HomeSection("🎬 Recomendadas del Día", diverseRecommended))
            if (popularMovies.isNotEmpty()) add(HomeSection("🔥 Películas Más Populares", popularMovies))
            if (recentEstrenos.isNotEmpty()) add(HomeSection("🚀 Recientes & Estrenos", recentEstrenos))
            if (highRatedMovies.isNotEmpty()) add(HomeSection("⭐ Las Mejor Valoradas", highRatedMovies))
            if (dailyBlockbusters.isNotEmpty()) add(HomeSection("🍿 Selección Recomendada de Cine", dailyBlockbusters))

            val categoryMap = listOf(
                "💥 Pura Acción & Explosiones" to listOf("Acción"),
                "🛸 Ciencia Ficción & Universos" to listOf("Ciencia Ficción", "Sci-Fi"),
                "👻 Espantosas & Terror" to listOf("Terror", "Horror"),
                "😂 Para Morirse De Risa" to listOf("Comedia"),
                "🎭 Historias Dramáticas" to listOf("Drama"),
                "🔪 Suspenso & Thrillers" to listOf("Suspenso"),
                "🏰 Fantasía & Magia" to listOf("Fantasía", "Fantasy"),
                "🕵️‍♂️ Crimen & Investigaciones" to listOf("Crimen", "Misterio"),
                "💘 Historias De Amor" to listOf("Romance"),
                "🌴 Aventuras Inolvidables" to listOf("Aventura"),
                "👾 Animación & Dibujos" to listOf("Animación"),
                "👪 Para Toda La Familia" to listOf("Familia"),
                "📜 Hechos Reales & Basado En Historia" to listOf("Historia", "Biografía"),
                "🌌 Odiseas Espaciales & Futuro" to listOf("Ciencia Ficción"),
                "🗡️ Espada & Brujería" to listOf("Fantasía"),
                "🧠 Thrillers Psicológicos" to listOf("Suspenso"),
                "🥋 Artes Marciales & Combate" to listOf("Acción"),
                "💎 Selección Imprescindible" to listOf("Drama"),
                "🔥 Películas Más Vistas De La Semana" to listOf("Acción", "Aventura"),
                "🎶 Musicales & Ritmo" to listOf("Música"),
                "🏎️ Velocidad & Persecuciones" to listOf("Acción"),
                "🧟‍♂️ Invasión & Apocalipsis" to listOf("Terror", "Ciencia Ficción"),
                "🕵️ Chispas & Comedia Romántica" to listOf("Comedia", "Romance"),
                "🏴‍☠️ Piratas & Misiones" to listOf("Aventura"),
                "🏛️ Épicas De Época" to listOf("Historia", "Drama"),
                "🔫 Agentes & Espionaje" to listOf("Acción", "Suspenso"),
                "🌌 Cyberpunk & Robots" to listOf("Ciencia Ficción"),
                "🧩 Rompecabezas & Giros" to listOf("Misterio"),
                "🎬 Selección Estelar" to listOf("Drama", "Acción")
            )

            categoryMap.forEachIndexed { idx, (title, genres) ->
                val filtered = list.filter { m -> genres.any { g -> m.genres.any { mg -> mg.contains(g, ignoreCase = true) } } }
                val candidateList = if (filtered.isNotEmpty()) {
                    createDailyRotatedSelection(filtered, filtered.size, seedOffset = 300 + idx)
                } else {
                    createDailyRotatedSelection(topMovies, topMovies.size, seedOffset = 400 + idx)
                }
                val content = pickDeduplicated(candidateList, 15, usedIds)
                if (content.isNotEmpty()) {
                    add(HomeSection(title, content))
                }
            }
        }.take(35)
    }

    fun createSeriesSections(list: List<Movie>): List<HomeSection> {
        if (list.isEmpty()) return emptyList()
        val usedIds = mutableSetOf<Int>()

        val newlyAdded = pickDeduplicated(list, 15, usedIds)
        val topSeries = list.sortedByDescending { it.rating }
        val recentSeries = list.sortedByDescending { it.releaseDate }
        val diverseSeries = pickDeduplicated(createDailyRotatedSelection(list, list.size, seedOffset = 70), 15, usedIds)
        val highRatedSeries = pickDeduplicated(topSeries.drop(3).let { if (it.isNotEmpty()) it else topSeries }, 15, usedIds)
        val popularSeries = pickDeduplicated(topSeries, 15, usedIds)
        val recentEstrenosSeries = pickDeduplicated(recentSeries, 15, usedIds)

        return buildList {
            if (newlyAdded.isNotEmpty()) add(HomeSection("🆕 Últimas Series Agregadas", newlyAdded))
            if (diverseSeries.isNotEmpty()) add(HomeSection("📺 Recomendadas del Día", diverseSeries))
            if (popularSeries.isNotEmpty()) add(HomeSection("🔥 Series Más Populares", popularSeries))
            if (recentEstrenosSeries.isNotEmpty()) add(HomeSection("🚀 Recientes & Estrenos", recentEstrenosSeries))
            if (highRatedSeries.isNotEmpty()) add(HomeSection("🏆 Series Aclamadas", highRatedSeries))

            val categoryMap = listOf(
                "💥 Series De Acción" to listOf("Acción"),
                "🛸 Universos & Ciencia Ficción" to listOf("Ciencia Ficción", "Sci-Fi"),
                "😱 Suspenso & Tensión" to listOf("Suspenso"),
                "😂 Comedias Divertidas" to listOf("Comedia"),
                "🎭 Dramas & Pasiones" to listOf("Drama"),
                "🔍 Casos & Misterios" to listOf("Misterio"),
                "🐉 Fantasía Épica" to listOf("Fantasía", "Fantasy"),
                "🚓 Crimen & Mafia" to listOf("Crimen"),
                "💘 Romances De Serie" to listOf("Romance"),
                "🤠 Aventuras & Fronteras" to listOf("Aventura"),
                "🏫 Sobrenatural & Paranormal" to listOf("Terror"),
                "👾 Animación & Anime" to listOf("Animación"),
                "👑 Intrigas De Poder" to listOf("Drama"),
                "🕵️‍♂️ Detectives & Policiales" to listOf("Misterio", "Crimen"),
                "🩸 Terror & Criaturas" to listOf("Terror"),
                "🚀 Ciencia Ficción Espacial" to listOf("Ciencia Ficción"),
                "🏆 Grandes Sagas De Series" to listOf("Fantasía"),
                "⭐ Éxitos De Temporada" to listOf("Acción"),
                "🌟 Imperdibles En Pantalla" to listOf("Drama"),
                "⌛ Miniseries & Ediciones Especiales" to listOf("Drama", "Misterio"),
                "🤖 Inteligencia & Futuro" to listOf("Ciencia Ficción"),
                "🏙️ Dramas De Ciudad" to listOf("Drama"),
                "🎮 Adaptaciones & Universos" to listOf("Acción", "Fantasía"),
                "🔥 Series Más Comentadas" to listOf("Acción", "Drama"),
                "🌌 Dimensiones & Magia" to listOf("Fantasía"),
                "🗡️ Venganza & Honor" to listOf("Acción", "Drama"),
                "🧩 Misterios Desconcertantes" to listOf("Misterio"),
                "🎉 Comedias Ligeras" to listOf("Comedia"),
                "📺 Grandes Estrenos De La Televisión" to listOf("Drama", "Acción")
            )

            categoryMap.forEachIndexed { idx, (title, genres) ->
                val filtered = list.filter { m -> genres.any { g -> m.genres.any { mg -> mg.contains(g, ignoreCase = true) } } }
                val candidateList = if (filtered.isNotEmpty()) {
                    createDailyRotatedSelection(filtered, filtered.size, seedOffset = 500 + idx)
                } else {
                    createDailyRotatedSelection(topSeries, topSeries.size, seedOffset = 600 + idx)
                }
                val content = pickDeduplicated(candidateList, 15, usedIds)
                if (content.isNotEmpty()) {
                    add(HomeSection(title, content))
                }
            }
        }.take(35)
    }
}

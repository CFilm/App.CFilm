package com.example.peliselprimazo.ui.screens.home

import com.example.peliselprimazo.domain.model.ContentType
import com.example.peliselprimazo.domain.model.Movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSectionBuilderTest {

    @Test
    fun `createSections returns empty list when input is empty`() {
        val sections = HomeSectionBuilder.createSections(emptyList<Movie>(), emptyList<Movie>(), emptyList<Movie>())
        assertTrue(sections.isEmpty())
    }

    @Test
    fun `createSections returns expected sections when movies are present`() {
        val movies = listOf(
            Movie(
                id = 1,
                title = "Test Movie",
                overview = "Overview",
                rating = 8.5,
                serverLinks = emptyList(),
                contentType = ContentType.MOVIE,
                genres = listOf("Acción", "Drama")
            )
        )
        val sections = HomeSectionBuilder.createSections(movies, emptyList(), emptyList())
        assertTrue(sections.isNotEmpty())
        assertEquals("🆕 Recién Agregados en CFilm", sections[0].title)
    }

    @Test
    fun `createMovieSections filters and groups movie sections correctly`() {
        val movies = listOf(
            Movie(
                id = 1,
                title = "Movie 1",
                rating = 8.0,
                serverLinks = emptyList(),
                contentType = ContentType.MOVIE,
                genres = listOf("Acción")
            )
        )
        val sections = HomeSectionBuilder.createMovieSections(movies)
        assertTrue(sections.isNotEmpty())
        assertEquals("🆕 Últimas Películas Agregadas", sections[0].title)
    }

    @Test
    fun `createSeriesSections groups series sections correctly`() {
        val series = listOf(
            Movie(
                id = 2,
                title = "Series 1",
                rating = 9.0,
                serverLinks = emptyList(),
                contentType = ContentType.TV,
                genres = listOf("Drama")
            )
        )
        val sections = HomeSectionBuilder.createSeriesSections(series)
        assertTrue(sections.isNotEmpty())
        assertEquals("🆕 Últimas Series Agregadas", sections[0].title)
    }
}

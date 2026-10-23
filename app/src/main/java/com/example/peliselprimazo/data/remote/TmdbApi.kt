package com.example.peliselprimazo.data.remote

import com.example.peliselprimazo.data.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("search/movie")
    suspend fun searchMovie(
        @Query("query") query: String,
        @Query("year") year: String? = null,
        @Query("language") language: String = "es-MX"
    ): TmdbSearchResponse<TmdbMovieResult>

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("first_air_date_year") year: String? = null,
        @Query("language") language: String = "es-MX"
    ): TmdbSearchResponse<TmdbTvResult>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "es-MX",
        @Query("append_to_response") append: String = "videos,credits,alternative_titles"
    ): TmdbMovieDetail

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "es-MX",
        @Query("append_to_response") append: String = "videos,credits,alternative_titles"
    ): TmdbTvDetail

    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getTvSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String = "es-MX"
    ): TmdbSeasonDetail

    companion object {
        const val BASE_URL = NetworkConfig.TMDB_BASE_URL
        const val IMAGE_BASE_URL = NetworkConfig.TMDB_IMAGE_BASE_URL
    }
}

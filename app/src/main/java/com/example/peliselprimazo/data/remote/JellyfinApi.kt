package com.example.peliselprimazo.data.remote

import com.example.peliselprimazo.BuildConfig
import com.example.peliselprimazo.data.model.JellyfinItemsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface JellyfinApi {

    @GET("Items")
    suspend fun getItems(
        @Query("Recursive") recursive: Boolean = true,
        @Query("IncludeItemTypes") includeItemTypes: String = "Movie,Series,Episode",
        @Query("Fields") fields: String = "Path,Genres,Overview",
        @Query("EnableImages") enableImages: Boolean = false,
        @Query("EnableUserData") enableUserData: Boolean = false
    ): JellyfinItemsResponse

    companion object {
        val BASE_URL: String
            get() = BuildConfig.JELLYFIN_BASE_URL
        val API_KEY: String
            get() = BuildConfig.JELLYFIN_API_KEY
    }
}

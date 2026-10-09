package com.example.peliselprimazo.data.remote

import com.google.gson.JsonArray
import retrofit2.http.GET
import retrofit2.http.Query

interface TranslationApi {

    @GET("translate_a/single")
    suspend fun translateText(
        @Query("client") client: String = "gtx",
        @Query("sl") sourceLang: String = "auto",
        @Query("tl") targetLang: String = "es",
        @Query("dt") dataType: String = "t",
        @Query("q") text: String
    ): JsonArray

    companion object {
        const val BASE_URL = "https://translate.googleapis.com/"
    }
}

package com.example.peliselprimazo.core.network.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.JsonElement

class TranslationApi(private val httpClient: HttpClient) {

    suspend fun translateText(
        client: String = "gtx",
        sourceLang: String = "auto",
        targetLang: String = "es",
        dataType: String = "t",
        text: String
    ): JsonElement {
        return httpClient.get("${BASE_URL}translate_a/single") {
            parameter("client", client)
            parameter("sl", sourceLang)
            parameter("tl", targetLang)
            parameter("dt", dataType)
            parameter("q", text)
        }.body()
    }

    companion object {
        const val BASE_URL = "https://translate.googleapis.com/"
    }
}

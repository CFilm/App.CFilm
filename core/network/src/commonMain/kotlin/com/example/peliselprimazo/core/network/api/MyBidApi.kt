package com.example.peliselprimazo.core.network.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

class MyBidApi(private val httpClient: HttpClient) {

    suspend fun getUserSpots(
        token: String? = null,
        authToken: String? = null
    ): MyBidResponse<List<MyBidSpot>> {
        return httpClient.get("${BASE_URL}user-spots") {
            header(HttpHeaders.Accept, "application/json")
            token?.let { parameter("token", it) }
            authToken?.let { header("X-AUTH-TOKEN", it) }
        }.body()
    }

    suspend fun getStats(
        token: String,
        date1: String,
        date2: String,
        fields: String,
        authToken: String = token,
        orderBy: String = "-date",
        limit: Int = 500,
        offset: Int = 0,
        filters: String? = null
    ): MyBidResponse<List<MyBidStat>> {
        return httpClient.get("${BASE_URL}stats") {
            header(HttpHeaders.Accept, "application/json")
            header("X-AUTH-TOKEN", authToken)
            parameter("token", token)
            parameter("date1", date1)
            parameter("date2", date2)
            parameter("fields", fields)
            parameter("orderBy", orderBy)
            parameter("limit", limit)
            parameter("offset", offset)
            filters?.let { parameter("filters", it) }
        }.body()
    }

    companion object {
        const val BASE_URL = "https://publishers.mybid.io/backend/api/public/"
    }
}

@Serializable
data class MyBidResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: String? = null,
    val message: String? = null
)

@Serializable
data class MyBidSpot(
    val id: String,
    val name: String,
    val adformat: String,
    val status: String,
    val tag_url: String? = null
)

@Serializable
data class MyBidStat(
    val date: String?,
    val impressions: String?,
    val clicks: String?,
    val money: String?,
    val adformat: String?
)

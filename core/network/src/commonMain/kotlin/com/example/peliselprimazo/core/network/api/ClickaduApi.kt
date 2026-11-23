package com.example.peliselprimazo.core.network.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

class ClickaduApi(private val httpClient: HttpClient) {

    suspend fun getStats(
        token: String,
        dateFrom: String,
        dateTo: String,
        groupBy: String = "geo",
        format: String = "json",
        zoneId: String? = null,
        adFormat: String? = null
    ): ClickaduResponse {
        return httpClient.get("${BASE_URL}stats") {
            parameter("token", token)
            parameter("dateFrom", dateFrom)
            parameter("dateTo", dateTo)
            parameter("groupBy", groupBy)
            parameter("format", format)
            zoneId?.let { parameter("zoneId", it) }
            adFormat?.let { parameter("adFormat", it) }
        }.body()
    }

    suspend fun getRequestVarStats(
        token: String,
        dateFrom: String,
        dateTo: String,
        format: String = "json",
        page: Int = 1,
        limit: Int = 30000
    ): ClickaduRequestVarResponse {
        return httpClient.get("${BASE_URL}requestVarStats") {
            parameter("token", token)
            parameter("dateFrom", dateFrom)
            parameter("dateTo", dateTo)
            parameter("format", format)
            parameter("page", page)
            parameter("limit", limit)
        }.body()
    }

    companion object {
        const val BASE_URL = "http://v2.api.clickadu.com/partner/"
    }
}

@Serializable
data class ClickaduResponse(
    val stats: List<ClickaduStat>?,
    val total: ClickaduTotal?,
    val filter: ClickaduFilter?
)

@Serializable
data class ClickaduStat(
    val geo: String?,
    val day: String?,
    val hour: String?,
    val zone: String?,
    val impressions: Long?,
    val money: Double?
)

@Serializable
data class ClickaduTotal(
    val impressions: Long?,
    val money: Double?
)

@Serializable
data class ClickaduFilter(
    val dateFrom: String?,
    val dateTo: String?,
    val groupBy: String?
)

@Serializable
data class ClickaduRequestVarResponse(
    val stats: List<ClickaduRequestVarItem>?,
    val total: ClickaduRequestVarTotal?
)

@Serializable
data class ClickaduRequestVarItem(
    val request_var: String?,
    val money: Double?
)

@Serializable
data class ClickaduRequestVarTotal(
    val money: Double?
)

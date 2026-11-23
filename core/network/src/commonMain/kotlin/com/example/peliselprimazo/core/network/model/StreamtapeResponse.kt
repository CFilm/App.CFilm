package com.example.peliselprimazo.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreamtapeResponse<T>(
    @SerialName("status") val status: Int,
    @SerialName("msg") val msg: String,
    @SerialName("result") val result: T?
)

@Serializable
data class StreamtapeFolderList(
    @SerialName("folders") val folders: List<StreamtapeFolder>? = emptyList(),
    @SerialName("files") val files: List<StreamtapeFile>? = emptyList()
)

@Serializable
data class StreamtapeFolder(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String
)

@Serializable
data class StreamtapeFile(
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long,
    @SerialName("link") val link: String,
    @SerialName("linkid") val linkid: String,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("downloads") val downloads: Int? = null,
    @SerialName("convert") val convert: String? = null
)

@Serializable
data class StreamtapeTicket(
    @SerialName("ticket") val ticket: String,
    @SerialName("wait_time") val waitTime: Int,
    @SerialName("valid_until") val validUntil: String
)

@Serializable
data class StreamtapeDownloadLink(
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long,
    @SerialName("url") val url: String
)

package com.example.peliselprimazo.data.model

import com.google.gson.annotations.SerializedName

data class JellyfinItemsResponse(
    @SerializedName("Items") val items: List<JellyfinItem>? = emptyList(),
    @SerializedName("TotalRecordCount") val totalRecordCount: Int?
)

data class JellyfinItem(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("Type") val type: String?,
    @SerializedName("ProductionYear") val productionYear: Int?,
    @SerializedName("Overview") val overview: String?,
    @SerializedName("RunTimeTicks") val runTimeTicks: Long?,
    @SerializedName("CommunityRating") val communityRating: Float?,
    @SerializedName("SeriesName") val seriesName: String?,
    @SerializedName("IndexNumber") val indexNumber: Int?,
    @SerializedName("ParentIndexNumber") val parentIndexNumber: Int?
)

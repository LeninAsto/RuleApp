package com.leninasto.ruleapp.data

import com.google.gson.annotations.SerializedName

data class ShimmiePost(
    val id: Int,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("thumb_url") val thumbUrl: String,
    val tags: String,
    val ext: String,
    val width: Int,
    val height: Int
)

data class ShimmieResponse(
    val posts: List<ShimmiePost>? = null
)

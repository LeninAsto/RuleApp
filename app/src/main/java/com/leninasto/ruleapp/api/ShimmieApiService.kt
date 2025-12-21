package com.leninasto.ruleapp.api

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ShimmieApiService {
    @GET("post/list")
    suspend fun getPostsHtml(
        @Query("page") page: Int = 1,
        @Query("search") search: String? = null
    ): String

    companion object {
        private const val BASE_URL = "https://shimmie.shishnet.org/"

        fun create(): ShimmieApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(ShimmieApiService::class.java)
        }
    }
}

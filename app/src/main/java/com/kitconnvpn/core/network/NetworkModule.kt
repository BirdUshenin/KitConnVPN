package com.kitconnvpn.core.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

private const val BASE_URL = "https://kitconn-api.ilyaushenin.ru/"
private const val API_TOKEN = ""

private val authInterceptor = Interceptor { chain ->
    val request = chain.request()
        .newBuilder()
        .addHeader("Authorization", "Bearer $API_TOKEN")
        .build()

    chain.proceed(request)
}

private val json = Json {
    ignoreUnknownKeys = true
}

private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(authInterceptor)
    .build()

val kitConnApi: KitConnApi = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(okHttpClient)
    .addConverterFactory(
        json.asConverterFactory("application/json".toMediaType())
    )
    .build()
    .create(KitConnApi::class.java)
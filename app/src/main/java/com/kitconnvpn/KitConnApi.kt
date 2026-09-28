package com.kitconnvpn

import retrofit2.http.GET
import retrofit2.http.Header

interface KitConnApi {
    @GET("api/v1/configs")
    suspend fun getConfigs(
        @Header("X-App-Version") appVersion: Int
    ): VpnConfigsResponse
}
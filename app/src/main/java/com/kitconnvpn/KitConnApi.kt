package com.kitconnvpn

import retrofit2.http.GET

interface KitConnApi {

    @GET("api/v1/configs")
    suspend fun getConfigs(): VpnConfigsResponse
}
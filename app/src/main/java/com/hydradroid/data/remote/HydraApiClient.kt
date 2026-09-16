package com.hydradroid.data.remote

import com.hydradroid.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

// Порт src/main/services/hydra-api.ts: единая обёртка, refresh, User-Agent, needsAuth
object HydraApiClient {
    var accessToken: String? = null
    var onUnauthorized: (() -> Unit)? = null

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val authInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("User-Agent", "HydraDroid v${BuildConfig.VERSION_NAME}")
            .apply { accessToken?.let { header("Authorization", "Bearer $it") } }
            .build()
        val resp = chain.proceed(req)
        if (resp.code == 401) onUnauthorized?.invoke() // handleUnauthorizedError: signout
        resp
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        // Логи тел запросов только в дебаге: в релизе это CPU + IO на каждый вызов.
        .addInterceptor(
            HttpLoggingInterceptor().setLevel(
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            )
        )
        .build()

    val service: HydraService = Retrofit.Builder()
        .baseUrl(BuildConfig.HYDRA_API_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(HydraService::class.java)
}

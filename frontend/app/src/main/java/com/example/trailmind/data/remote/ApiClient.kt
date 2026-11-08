package com.example.trailmind.data.remote

import android.content.Context
import com.example.trailmind.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // 10.0.2.2 = the host machine as seen from the Android emulator.
    // Physical phone over USB: run `adb reverse tcp:5000 tcp:5000` and use http://127.0.0.1:5000/
    private const val BASE_URL = "http://10.0.2.2:5000/"

    private var tokenStore: TokenStore? = null

    fun init(context: Context) {
        tokenStore = TokenStore(context.applicationContext)
    }

    // adds "Authorization: Bearer <token>" to every request that doesn't set its own
    private val authInterceptor = Interceptor { chain ->
        val token = tokenStore?.getToken()
        val request = chain.request()
        chain.proceed(
            if (token != null && request.header("Authorization") == null)
                request.newBuilder().header("Authorization", "Bearer $token").build()
            else request
        )
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS) // itinerary generation can be slow
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val gatewayApi: GatewayApi = retrofit.create(GatewayApi::class.java)
}
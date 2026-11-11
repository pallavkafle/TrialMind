package com.example.trailmind.data.remote

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.example.trailmind.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // To test on a real phone over Wi-Fi, put your PC's address here, for example "http://192.168.1.20:5000/".
    // Leave it empty to use the automatic choice below:
    //   emulator    -> http://10.0.2.2:5000/   (the host PC as the emulator sees it)
    //   real phone  -> http://127.0.0.1:5000/  (USB: run `adb reverse tcp:5000 tcp:5000` first)
    private const val LAN_BASE_URL = ""

    private var appContext: Context? = null
    private var tokenStore: TokenStore? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        tokenStore = TokenStore(context.applicationContext)
    }

    private fun isDebuggable(): Boolean {
        val ctx = appContext ?: return false
        return (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.PRODUCT.contains("sdk_gphone") ||
            Build.PRODUCT.contains("emulator")

    private fun baseUrl(): String {
        val lan = LAN_BASE_URL.trim()
        return when {
            lan.isNotEmpty() -> if (lan.endsWith("/")) lan else "$lan/"
            isEmulator() -> "http://10.0.2.2:5000/"
            else -> "http://127.0.0.1:5000/"
        }
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

    // a 401 on a request that carried a token means the saved login expired: forget it and tell the UI.
    // /api/auth/ calls are skipped, because a wrong password on login also answers 401.
    private val sessionInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        val sentToken = request.header("Authorization") != null
        val isAuthCall = request.url.encodedPath.startsWith("/api/auth/")
        if (response.code == 401 && sentToken && !isAuthCall) {
            tokenStore?.clear()
            SessionEvents.notifyUnauthorized()
        }
        response
    }

    private val okHttp: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // request line and status only: bodies never reach Logcat, so passwords and tokens stay out
            level = if (isDebuggable()) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(sessionInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(130, TimeUnit.SECONDS) // itinerary generation can take up to about 2 minutes
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val gatewayApi: GatewayApi by lazy { retrofit.create(GatewayApi::class.java) }
}
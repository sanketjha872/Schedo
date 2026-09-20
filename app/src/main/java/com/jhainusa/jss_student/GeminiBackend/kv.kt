package com.jhainusa.jss_student.GeminiBackend

import com.jhainusa.jss_student.BuildConfig
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

interface SupabaseApiService {
    @Multipart
    @POST("bright-responder")
    fun sendTimetable(
        @Part("user_id") userId: String, // Changed from RequestBody to String
        @Part("username") username: String?,
        @Part image: MultipartBody.Part
    ): Call<String>
}

object SupabaseClient {
    private val BASE_URL = "${BuildConfig.SUPABASE_URL}/functions/v1/"
    private val API_KEY = BuildConfig.SUPABASE_ANON_KEY

    val api: SupabaseApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseApiService::class.java)
    }
}
// ... rest of your data classes

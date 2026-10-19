package com.example.data.remote

import com.example.data.model.BaseApiResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface GasApiService {

    @GET
    suspend fun getSettings(
        @Url url: String,
        @Query("action") action: String = "get_settings"
    ): Response<BaseApiResponse>

    @GET
    suspend fun getAttendanceHistory(
        @Url url: String,
        @Query("action") action: String = "get_attendance_history",
        @Query("nisn") nisn: String,
        @Query("token") token: String
    ): Response<BaseApiResponse>

    @GET
    suspend fun getAdminRecap(
        @Url url: String,
        @Query("action") action: String = "get_admin_recap",
        @Query("adminToken") adminToken: String,
        @Query("date") date: String? = null
    ): Response<BaseApiResponse>

    @POST
    suspend fun postAction(
        @Url url: String,
        @Body payload: Map<String, Any>
    ): Response<BaseApiResponse>

    companion object {
        fun create(): GasApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl("https://script.google.com/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GasApiService::class.java)
        }
    }
}

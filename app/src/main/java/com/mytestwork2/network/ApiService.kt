package com.mytestwork2.network

import com.mytestwork2.models.AdminDashboardResponse
import com.mytestwork2.models.Child
import com.mytestwork2.models.GameData
import com.mytestwork2.models.LoginRequest
import com.mytestwork2.models.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/admins/{adminId}/children/all")
    suspend fun getAllChildren(@Path("adminId") adminId: Long): List<Child>

    @GET("api/admins/{adminId}")
    suspend fun getAdminDashboard(@Path("adminId") adminId: Long): AdminDashboardResponse

    @GET("api/admins/{adminId}/children/{childId}/games")
    suspend fun getGame(
        @Path("adminId") adminId: Long,
        @Path("childId") childId: Long,
        @Query("gameType") gameType: String
    ): GameData

    // Ping method to wake up the server:
    @GET("api/ping")
    suspend fun ping(): Response<String>

}
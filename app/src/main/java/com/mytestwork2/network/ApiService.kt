package com.mytestwork2.network

import com.mytestwork2.models.Admin
import com.mytestwork2.models.AdminDashboardResponse
import com.mytestwork2.models.Child
import com.mytestwork2.models.ChildPointsResponse
import com.mytestwork2.models.GameData
import com.mytestwork2.models.GameSessionResponse
import com.mytestwork2.models.LatestSessionsResponse
import com.mytestwork2.models.LoginRequest
import com.mytestwork2.models.LoginResponse
import com.mytestwork2.models.QuestionDataResponse
import com.mytestwork2.models.SessionUpdateResponse
import com.mytestwork2.models.SupervisorDashboardResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/admins/{adminId}/children/all")
    suspend fun getAllChildren(@Path("adminId") adminId: Long): List<Child>

    // add
    // /api/admins/{adminId}/children
    @POST("api/admins/{adminId}/children")
    suspend fun addChildToGroup(@Path("adminId") adminId: Long, @Query("childId") childId: Long)

    // New endpoint: Clear all children from Admin Group
    @DELETE("api/admins/{adminId}/children")
    suspend fun clearGroup(@Path("adminId") adminId: Long): Response<Unit>

    // New endpoint: Get unmanaged children for Admin Group
    @GET("api/admins/{adminId}/children/unmanaged")
    suspend fun getUnmanagedChildren(@Path("adminId") adminId: Long): List<Child>

    @GET("api/admins/{adminId}")
    suspend fun getAdminDashboard(@Path("adminId") adminId: Long): AdminDashboardResponse

    @GET("api/admins/{adminId}/children/{childId}/games")
    suspend fun getGame(
        @Path("adminId") adminId: Long,
        @Path("childId") childId: Long,
        @Query("gameType") gameType: Int
    ): GameData

    // Ping method to wake up the server:
    @GET("api/ping")
    suspend fun ping(): Response<String>

    @GET("api/supervisor/{adminId}/dashboard")
    suspend fun getSupervisorDashboard(@Path("adminId") adminId: Long): SupervisorDashboardResponse

    @POST("api/supervisor/child/create")
    suspend fun createChild(@Query("adminId") adminId: Long, @Body child: Child): Child


    @DELETE("api/supervisor/child/{id}")
    suspend fun deleteChild(@Path("id") childId: Long, @Query("adminId") adminId: Long): Response<Unit>

    @POST("api/supervisor/admin/create")
    suspend fun createAdmin(@Query("adminId") adminId: Long, @Body admin: Admin): Admin

    @DELETE("api/supervisor/admin/{id}")
    suspend fun deleteAdmin(@Path("id") targetAdminId: Long ,@Query("adminId") adminId: Long): Response<Unit>

    @POST("api/supervisor/admin/change-password")
    suspend fun changeAdminPassword(
        @Query("adminId") adminId: Long,
        @Query("id") targetAdminId: Long,
        @Query("newPassword") newPassword: String
    ): retrofit2.Response<String>

    @POST("api/admins/{adminId}/children/{childId}/games/{gameId}/sessions/start")
    suspend fun startSession(
        @Path("adminId") adminId: Long,
        @Path("childId") childId: Long,
        @Path("gameId") gameId: Int
    ): GameSessionResponse

    @POST("api/admins/{adminId}/children/{childId}/games/{gameId}/sessions/{sessionId}/answer")
    suspend fun recordAnswer(
        @Path("adminId") adminId: Long,
        @Path("childId") childId: Long,
        @Path("gameId") gameId: Int,
        @Path("sessionId") sessionId: Long,
        @Query("questionId") questionId: Int,
        @Query("optionChosen") optionChosen: Int,
        @Query("correctOption") correctOption: Int,
        @Query("isCorrect") isCorrect: Boolean
    ): SessionUpdateResponse

    @POST("api/admins/{adminId}/children/{childId}/games/{gameId}/sessions/{sessionId}/end")
    suspend fun endSession(
        @Path("adminId") adminId: Long,
        @Path("childId") childId: Long,
        @Path("gameId") gameId: Int,
        @Path("sessionId") sessionId: Long
    ): Response<Unit>

    /**
     * Get points for a child by game type.
     */
    @GET("api/points/children/{childId}/games/{gameType}")
    suspend fun getChildPointsByGameType(
        @Path("childId") childId: Long,
        @Path("gameType") gameType: Int,
        @Header("Cache-Control") cacheControl: String = "public, max-age=60"
    ): ChildPointsResponse

    /**
     * Get all points for a child across all categories.
     */
    @GET("api/points/children/{childId}")
    suspend fun getAllChildPoints(
        @Path("childId") childId: Long
    ): Map<String, Int>


    @GET("api/admins/{adminId}/children/{childId}/games/{gameId}/sessions/latest")
    suspend fun getLatestSessions(
        @Path("adminId") adminId: Long,
        @Path("childId") childId: Long,
        @Path("gameId") gameId: Int
    ): LatestSessionsResponse
}
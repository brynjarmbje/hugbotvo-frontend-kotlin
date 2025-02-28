package com.mytestwork2.network

import com.mytestwork2.models.Admin
import com.mytestwork2.models.AdminDashboardResponse
import com.mytestwork2.models.Child
import com.mytestwork2.models.GameData
import com.mytestwork2.models.LoginRequest
import com.mytestwork2.models.LoginResponse
import com.mytestwork2.models.SupervisorDashboardResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/admins/{adminId}/children/all")
    suspend fun getAllChildren(@Path("adminId") adminId: Long): List<Child>

    // get children in group
    // /api/admins/{adminId}/children
    @GET("api/admins/{adminId}/children")
    suspend fun getChildrenInGroup(@Path("adminId") adminId: Long): List<Child>

    // delete
    // /api/admins/{adminId}/children/{childId}
    @DELETE("api/admins/{adminId}/children/{childId}")
    suspend fun deleteChildFromGroup(@Path("adminId") adminId: Long, @Path("childId") childId: Long)

    // add
    // /api/admins/{adminId}/children
    @POST("api/admins/{adminId}/children")
    suspend fun addChildToGroup(@Path("adminId") adminId: Long, @Query("childId") childId: Long)

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

    @GET("api/supervisor/{adminId}/dashboard")
    suspend fun getSupervisorDashboard(@Path("adminId") adminId: Long): SupervisorDashboardResponse

    //@POST("api/admins/{adminId}/children")
    //suspend fun createChild(@Path("adminId") adminId: Long, @Body child: Child): Child

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

}
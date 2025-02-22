package com.mytestwork2.models

data class SupervisorDashboardResponse(
    val schoolName: String?,
    val children: List<Child>?,
    val admins: List<Admin>?
)

package com.mytestwork2.models

class LoginResponse(
    val adminId: Long,
    val token: String?, // Include if your backend returns a token
    val isSupervisor: Boolean,
)

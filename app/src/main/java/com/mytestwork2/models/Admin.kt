package com.mytestwork2.models

data class Admin(
    val id: Long? = null,          // Nullable, auto-generated
    val username: String,
    val password: String,
    val school: School? = null,
    val supervisor: Boolean = false

)

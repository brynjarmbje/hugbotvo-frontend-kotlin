package com.mytestwork2.models

class GameOption(
    val id: Int,
    val name: String,
    var points: Int = 0,
    val enabled: Boolean = true
)


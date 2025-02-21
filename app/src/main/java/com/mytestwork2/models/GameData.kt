package com.mytestwork2.models

data class GameData(
    val adminId: Int,
    val childId: Int,
    val gameType: Int,  // can be used if needed
    val correctId: Int,
    val optionIds: List<Int>,
    val message: String
)

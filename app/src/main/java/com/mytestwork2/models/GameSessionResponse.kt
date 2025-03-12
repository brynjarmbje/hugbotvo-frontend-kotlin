package com.mytestwork2.models

data class GameSessionResponse(
    val sessionId: Long,
    val currentLevel: Int,      // This represents the child's current level (or points used as level)
    val totalGamePoints: Int,   // The overall points for this game category
    val message: String
)
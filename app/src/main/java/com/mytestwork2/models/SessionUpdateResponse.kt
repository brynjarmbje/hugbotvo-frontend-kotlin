package com.mytestwork2.models

data class SessionUpdateResponse(
    val currentSessionPoints: Int,  // Updated session points (level)
    val pointsEarned: Int,
    val totalGamePoints: Int,       // Updated total points for the game category
    val isCorrect: Boolean,
    val message: String
)
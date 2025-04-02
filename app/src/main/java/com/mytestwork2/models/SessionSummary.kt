package com.mytestwork2.models

import com.google.gson.annotations.SerializedName

data class SessionSummary(
    val sessionId: Long,
    val gameId: Int, // Include this field to know which game the session is for.
    val points: Int,
    val correctAnswers: Int,
    val totalQuestions: Int,
    @SerializedName("accuracyPercentage")
    val accuracy: Double,  // Use Double to match the JSON value
    val isActive: Boolean,
    val startTime: String,
    val endTime: String?
)


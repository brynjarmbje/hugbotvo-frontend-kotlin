package com.mytestwork2.models

data class SessionSummary(
    val sessionId: Long,
    val gameId: Int, // Include this field to know which game the session is for.
    val points: Int,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val accuracy: Int,
    val isActive: Boolean,
    val startTime: String,
    val endTime: String?
)


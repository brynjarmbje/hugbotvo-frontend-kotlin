package com.mytestwork2.models

data class QuestionData(
    val id: Long,
    val type: Int,
    val points: Int,
    val hasImage: Boolean,
    val hasAudio: Boolean,
    val imageData: String?,
    val audioData: String?
)

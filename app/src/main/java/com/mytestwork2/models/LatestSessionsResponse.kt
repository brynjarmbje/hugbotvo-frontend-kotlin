package com.mytestwork2.models

import com.google.gson.annotations.SerializedName

data class LatestSessionsResponse(
    @SerializedName("latestSessions")
    val sessions: List<SessionSummary>
)
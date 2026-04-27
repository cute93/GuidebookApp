package com.example.guidebook.models

data class UserNote(
    val id: String = "",
    val problemId: String = "",
    val userId: String = "",
    val userName: String = "",
    val role: String = "student",   // "teacher" or "student"
    val noteImageUrl: String = "",  // uploaded drawing image URL
    val localPath: String = "",     // temp saved path on device
    val updatedAt: Long = 0L
)

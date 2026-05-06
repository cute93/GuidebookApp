package com.example.guidebook.models

data class UserNote(
    val id: String = "",
    val problemId: String = "",
    val userId: String = "",
    val userName: String = "",
    val role: String = "student",   // "teacher" or "student"
    val noteImageUrl: String = "",
    val updatedAt: Long = 0L
)

package com.example.guidebook.models

data class Problem(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val subject: String = "",
    val createdAt: Long = 0L,
    val teacherId: String = ""
)

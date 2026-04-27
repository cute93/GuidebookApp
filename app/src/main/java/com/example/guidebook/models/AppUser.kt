package com.example.guidebook.models

import java.io.Serializable

data class AppUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "student"    // "teacher" or "student"
) : Serializable

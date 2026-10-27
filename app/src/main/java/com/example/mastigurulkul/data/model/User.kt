package com.example.mastigurulkul.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "student", // "student" or "admin"
    val xp: Int = 0,
    val streak: Int = 0
)

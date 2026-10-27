package com.example.mastigurulkul.data.model

import android.net.Uri

data class ChatMessage(
    val role: String = "user", // "user" or "model"
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: Uri? = null,
    val sender: String = ""
)

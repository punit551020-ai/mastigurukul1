package com.example.mastigurulkul.ui.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuizScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Quiz Screen - Mission 2027", style = MaterialTheme.typography.headlineMedium)
    }
}

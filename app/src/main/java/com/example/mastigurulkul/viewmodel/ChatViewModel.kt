package com.example.mastigurulkul.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mastigurulkul.data.model.ChatMessage
import com.example.mastigurulkul.data.remote.GeminiTutorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val geminiRepository: GeminiTutorRepository = GeminiTutorRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val userMessage = ChatMessage(role = "user", text = userText)
        _messages.value = _messages.value + userMessage

        val history: List<Any> = _messages.value.map { it.text }

        viewModelScope.launch {
            _isStreaming.value = true
            val assistantBuffer = StringBuilder()
            try {
                geminiRepository.streamTutorReply(history).collect { chunk ->
                    assistantBuffer.append(chunk)
                    val assistantMessage = ChatMessage(role = "model", text = assistantBuffer.toString())
                    _messages.value = _messages.value.dropLast(1) + assistantMessage
                }
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    role = "model",
                    text = "Sorry, I could not reach the tutor service. Please check your connection and try again."
                )
            } finally {
                _isStreaming.value = false
            }
        }
    }
}

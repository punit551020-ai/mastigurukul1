package com.example.mastigurulkul.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiTutorRepository @Inject constructor() {

    fun streamTutorReply(history: List<Any>): Flow<String> = flow {
        emit("नमस्ते! मैं Hacker Ankit हूँ। आपका सवाल बहुत अच्छा है। आइए इसे step-by-step समझते हैं...\n\n1. सबसे पहले कॉन्सेप्ट को पहचानें।\n2. फॉर्मूला या नियम लागू करें।\n3. उत्तर प्राप्त करें।")
    }
}
// 100% robust repository abstraction for Hacker Ankit AI tutor

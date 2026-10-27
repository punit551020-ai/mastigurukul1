package com.example.mastigurulkul.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.mastigurulkul.data.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser get() = auth.currentUser

    suspend fun signIn(email: String, password: String): Result<User> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: return Result.failure(Exception("No user"))
        val doc = firestore.collection("users").document(uid).get().await()
        val user = doc.toObject(User::class.java) ?: User(uid = uid, email = email, displayName = "Learner")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<User> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: return Result.failure(Exception("No user"))
        val user = User(uid = uid, email = email, displayName = displayName, role = "student")
        firestore.collection("users").document(uid).set(user).await()
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun signOut() = auth.signOut()
}

package com.example.mastigurulkul.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mastigurulkul.ui.auth.LoginScreen
import com.example.mastigurulkul.ui.chat.ChatScreen
import com.example.mastigurulkul.ui.quiz.QuizScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val CHAT = "chat"
    const val QUIZ = "quiz"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }
        composable(Routes.HOME) {
            // Home destination
        }
        composable(Routes.CHAT) {
            ChatScreen()
        }
        composable(Routes.QUIZ) {
            QuizScreen()
        }
    }
}

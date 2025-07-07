package software.lunchtable.recky

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun ReckyNavGraph(
    navController: NavHostController,
    auth: FirebaseAuth,
    isLoading: Boolean,
    onGoogleSignIn: () -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    NavHost(
        navController = navController,
        startDestination = if (auth.currentUser != null) "home" else "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGoogleSignIn = onGoogleSignIn
            )
        }
        composable("home") {
            HomeScreen(
                onProfileClick = {
                    navController.navigate("profile")
                },
                onRecommendationClick = { recId ->
                    if (recId.isNotBlank()) {
                        navController.navigate("recommend_detail/$recId")
                    } else {
                        Log.e("Navigation", "Tried to navigate with blank recId!")
                    }
                },
                onRecommendSomething = { /* TODO: implement navigation to send screen */ }
            )
        }
        composable("profile") {
            ProfileScreen(
                userEmail = auth.currentUser?.email ?: "Unknown",
                onSignOut = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onManageFriends = {
                    navController.navigate("friends")
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("friends") {
            FriendsScreen(
                onBack = { navController.popBackStack() },
                onRequestsClick = {
                    navController.navigate("friend_requests")
                },
                onAddFriendClick = {
                    navController.navigate("addFriend")
                }
            )
        }
        composable("friend_requests") {
            FriendRequestsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("addFriend") {
            AddFriendScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "recommend_detail/{recId}",
            arguments = listOf(
                navArgument("recId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val recId = backStackEntry.arguments?.getString("recId") ?: ""
            val currentUserUID = auth.currentUser?.uid ?: ""

            RecommendationDetailScreen(
                recId = recId,
                currentUserUID = currentUserUID,
                onVoteSubmit = { vote, note ->
                    Firebase.firestore.collection("recommendations")
                        .document(recId)
                        .update(
                            mapOf(
                                "vote" to vote,
                                "voteNote" to note
                            )
                        )
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

package software.lunchtable.recky

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.auth.FirebaseAuth

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
                }
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}

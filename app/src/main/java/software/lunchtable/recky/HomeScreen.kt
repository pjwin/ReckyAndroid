package software.lunchtable.recky

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    onRecommendationClick: (String) -> Unit,
    onRecommendSomething: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email ?: "Unknown user"

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(WindowInsets.systemBars.asPaddingValues())
                .padding(bottom = 80.dp)
        ) {
            HomeHeader(
                userEmail = email,
                onProfileClick = onProfileClick
            )

            Divider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            RecommendationList(
                onRecommendationClick = onRecommendationClick
            )
        }

        // Always-visible bottom button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = onRecommendSomething,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Recommend Something")
            }
        }
    }
}

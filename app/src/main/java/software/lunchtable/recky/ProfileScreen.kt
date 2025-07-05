package software.lunchtable.recky

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(
    userEmail: String,
    onSignOut: () -> Unit,
    onManageFriends: () -> Unit,
    onBack: () -> Unit
) {
    var pendingRequestsCount by remember { mutableStateOf(0) }

    val auth = Firebase.auth
    val firestore = Firebase.firestore
    val currentUID = auth.currentUser?.uid

    var sentStats by remember { mutableStateOf(RecommendationStats()) }
    var receivedStats by remember { mutableStateOf(RecommendationStats()) }

    LaunchedEffect(currentUID) {
        if (currentUID == null) return@LaunchedEffect

        // Get pending friend requests
        pendingRequestsCount = getPendingFriendRequestsCount()

        // Load sent stats
        val sentRecs = firestore.collection("recommendations")
            .whereEqualTo("fromUID", currentUID)
            .get()
            .await()

        val sentThumbsUp = sentRecs.count { it.getBoolean("vote") == true }
        val sentThumbsDown = sentRecs.count { it.getBoolean("vote") == false }
        val sentNoVote = sentRecs.count { it.get("vote") == null }

        sentStats = RecommendationStats(
            thumbsUp = sentThumbsUp,
            thumbsDown = sentThumbsDown,
            noVote = sentNoVote
        )

        // Load received stats
        val receivedRecs = firestore.collection("recommendations")
            .whereEqualTo("toUID", currentUID)
            .get()
            .await()

        val receivedThumbsUp = receivedRecs.count { it.getBoolean("vote") == true }
        val receivedThumbsDown = receivedRecs.count { it.getBoolean("vote") == false }
        val receivedNoVote = receivedRecs.count { it.get("vote") == null }

        receivedStats = RecommendationStats(
            thumbsUp = receivedThumbsUp,
            thumbsDown = receivedThumbsDown,
            noVote = receivedNoVote
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile Icon",
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Signed in as: $userEmail", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSignOut) {
            Text(
                text = "Sign Out",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onManageFriends) {
            Box {
                Text(
                    text = "Manage Friends",
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                if (pendingRequestsCount > 0) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 18.dp, y = (-2).dp)
                    ) {
                        Text("$pendingRequestsCount")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        UserStatsSection(sent = sentStats, received = receivedStats)

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(onClick = onBack) {
            Text("Back to Home")
        }
    }
}

@Composable
fun UserStatsSection(
    sent: RecommendationStats,
    received: RecommendationStats
) {
    RecommendationCategory(title = "Recommendations Sent", stats = sent, totalLabel = "📤 Total Sent")
    Spacer(modifier = Modifier.height(16.dp))
    RecommendationCategory(title = "Recommendations Received", stats = received, totalLabel = "📥 Total Received")
}

@Composable
fun RecommendationCategory(title: String, stats: RecommendationStats, totalLabel: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))

        StatRow(label = "👍 Thumbs Up", value = stats.thumbsUp, percent = stats.percent(stats.thumbsUp))
        StatRow(label = "👎 Thumbs Down", value = stats.thumbsDown, percent = stats.percent(stats.thumbsDown))
        StatRow(label = "🤷 No Vote", value = stats.noVote, percent = stats.percent(stats.noVote))
        StatRow(label = totalLabel, value = stats.total)
    }
}

@Composable
fun StatRow(label: String, value: Int, percent: Int? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(
            text = if (percent != null) "$value (${percent}%)" else "$value",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

data class RecommendationStats(
    val thumbsUp: Int = 0,
    val thumbsDown: Int = 0,
    val noVote: Int = 0
) {
    val total: Int get() = thumbsUp + thumbsDown + noVote

    fun percent(part: Int): Int =
        if (total > 0) (part * 100 / total) else 0
}

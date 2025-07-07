package software.lunchtable.recky

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun RecommendationList(
    onRecommendationClick: (String) -> Unit,
) {
    val auth = FirebaseAuth.getInstance()
    val currentUID = auth.currentUser?.uid ?: return

    var recommendations by remember { mutableStateOf<List<Recommendation>>(emptyList()) }
    var usernameMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    // Function to load data
    suspend fun loadData() {
        isLoading = true
        val recs = loadRecommendations(currentUID)
        val usernames = loadUsernames(recs)
        recommendations = recs.sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
        usernameMap = usernames
        isLoading = false
    }

    // Initial load
    LaunchedEffect(Unit) {
        loadData()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = {
                coroutineScope.launch { loadData() }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            recommendations.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recommendations yet", style = MaterialTheme.typography.bodyMedium)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(recommendations) { rec ->
                        val from = usernameMap[rec.fromUID] ?: "unknown"
                        val to = usernameMap[rec.toUID] ?: "unknown"
                        RecommendationCard(
                            recommendation = rec,
                            currentUserId = currentUID,
                            fromUsername = from,
                            toUsername = to,
                            onClick = { onRecommendationClick(rec.id) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

suspend fun loadRecommendations(currentUID: String): List<Recommendation> {
    val db = FirebaseFirestore.getInstance()

    val fromRecs = db.collection("recommendations")
        .whereEqualTo("fromUID", currentUID)
        .get()
        .await()

    val toRecs = db.collection("recommendations")
        .whereEqualTo("toUID", currentUID)
        .get()
        .await()

    return (fromRecs.documents + toRecs.documents)
        .mapNotNull { it.toObject(Recommendation::class.java)?.copy(id = it.id) }
        .distinct() // Optional: avoid duplicates
        .sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
}

suspend fun loadUsernames(recommendations: List<Recommendation>): Map<String, String> {
    val db = FirebaseFirestore.getInstance()
    val userIds = recommendations.flatMap { listOf(it.fromUID, it.toUID) }.toSet()
    val usernameMap = mutableMapOf<String, String>()

    userIds.chunked(10).forEach { chunk ->
        val docs = db.collection("users")
            .whereIn(FieldPath.documentId(), chunk)
            .get()
            .await()
        docs.forEach {
            val username = it.getString("username")
            if (username != null) {
                usernameMap[it.id] = username
            }
        }
    }

    return usernameMap
}

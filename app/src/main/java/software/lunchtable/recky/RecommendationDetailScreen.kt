package software.lunchtable.recky

import android.graphics.drawable.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationDetailScreen(
    recId: String,
    currentUserUID: String,
    onBack: () -> Unit,
    onVoteSubmit: (Boolean, String) -> Unit
) {
    val firestore = Firebase.firestore
    var recommendation by remember { mutableStateOf<Recommendation?>(null) }
    var usernameMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(recId) {
        val doc = firestore.collection("recommendations").document(recId).get().await()
        val rec = doc.toObject(Recommendation::class.java)?.copy(id = doc.id)

        rec?.let {
            val userIds = listOf(it.fromUID, it.toUID)
            val users = firestore.collection("users")
                .whereIn(FieldPath.documentId(), userIds)
                .get()
                .await()

            usernameMap = users.documents.associate { doc ->
                doc.id to (doc.getString("username") ?: "unknown")
            }

            recommendation = it
        }

        isLoading = false
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    recommendation?.let { rec ->
        RecommendationDetailContent(
            recommendation = rec,
            currentUserUID = currentUserUID,
            usernameMap = usernameMap,
            onBack = onBack,
            onVoteSubmit = onVoteSubmit
        )
    } ?: Text("Recommendation not found.")
}

@Composable
fun RecommendationDetailContent(
    recommendation: Recommendation,
    currentUserUID: String,
    usernameMap: Map<String, String>,
    onBack: () -> Unit,
    onVoteSubmit: (Boolean, String) -> Unit
) {
    val isReceived = recommendation.toUID == currentUserUID
    val isSent = recommendation.fromUID == currentUserUID
    val fromName = usernameMap[recommendation.fromUID] ?: recommendation.fromUID
    val toName = usernameMap[recommendation.toUID] ?: recommendation.toUID

    val dateFormatted = remember(recommendation.timestamp) {
        recommendation.timestamp?.toDate()?.let {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it)
        } ?: "Unknown date"
    }

    var selectedVote by remember { mutableStateOf(recommendation.vote) }
    var voteNote by remember { mutableStateOf(recommendation.voteNote ?: "") }
    val originalNote = recommendation.voteNote ?: ""

    val showSave = isReceived && (voteNote != originalNote && voteNote.isNotBlank())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.weight(1f))
        }

        // Header
        val direction = when {
            isReceived -> "From: @$fromName"
            isSent -> "To: @$toName"
            else -> ""
        }

        Text(text = direction, style = MaterialTheme.typography.bodyLarge)
        Text(text = "Sent: $dateFormatted", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        Spacer(Modifier.height(16.dp))

        // Content
        Text(
            text = getEmojiForType(recommendation.type),
            fontSize = 36.sp
        )
        Text(
            text = recommendation.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        recommendation.notes?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = "\"$it\"",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (isReceived) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconToggleButton(
                    checked = selectedVote == true,
                    onCheckedChange = { checked ->
                        selectedVote = if (checked) true else null
                    }
                ) {
                    Icon(
                        imageVector = if (selectedVote == true) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Thumbs Up"
                    )
                }
                IconToggleButton(
                    checked = selectedVote == false,
                    onCheckedChange = { checked ->
                        selectedVote = if (checked) false else null
                    }
                ) {
                    Icon(
                        imageVector = if (selectedVote == false) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Thumb Down",
                        modifier = Modifier.graphicsLayer(rotationZ = 180f)
                    )
                }
            }

            OutlinedTextField(
                value = voteNote,
                onValueChange = { voteNote = it },
                label = { Text("Optional note") },
                modifier = Modifier.fillMaxWidth()
            )

            if (showSave && selectedVote != null) {
                Button(
                    onClick = { onVoteSubmit(selectedVote!!, voteNote) },
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                ) {
                    Text("Save")
                }
            }
        } else {
            Text(
                text = when (recommendation.vote) {
                    true -> "Voted: 👍"
                    false -> "Voted: 👎"
                    null -> "Not yet voted"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "Vote note: ${recommendation.voteNote ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun VoteButton(selected: Boolean, emoji: String, label: String, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray
    val textColor = if (selected) Color.White else Color.Black

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = background),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text("$emoji $label", color = textColor)
    }
}
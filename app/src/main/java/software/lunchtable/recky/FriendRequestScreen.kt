package software.lunchtable.recky

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class FriendRequest(
    val uid: String,
    val username: String
)

@Composable
fun FriendRequestsScreen(onBack: () -> Unit) {
    val firestore = Firebase.firestore
    val auth = Firebase.auth
    val currentUID = auth.currentUser?.uid ?: return
    val coroutineScope = rememberCoroutineScope()

    var friendRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val userDoc = firestore.collection("users").document(currentUID).get().await()
        val requestUIDs = userDoc.get("friendRequests") as? List<String> ?: emptyList()

        val requestDocs = requestUIDs.mapNotNull { uid ->
            val doc = firestore.collection("users").document(uid).get().await()
            val username = doc.getString("username")
            if (username != null) FriendRequest(uid, username) else null
        }

        friendRequests = requestDocs
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        FriendRequestsTopBar(onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            friendRequests.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No friend requests")
                }
            }

            else -> {
                LazyColumn {
                    items(friendRequests) { request ->
                        FriendRequestCard(
                            username = request.username,
                            onAccept = {
                                coroutineScope.launch {
                                    acceptFriendRequest(currentUID, request.uid)
                                    friendRequests = friendRequests.filter { it.uid != request.uid }
                                }
                            },
                            onIgnore = {
                                coroutineScope.launch {
                                    ignoreFriendRequest(currentUID, request.uid)
                                    friendRequests = friendRequests.filter { it.uid != request.uid }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text("Friend Requests", textAlign = TextAlign.Center)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

@Composable
fun FriendRequestCard(
    username: String,
    onAccept: () -> Unit,
    onIgnore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(username, style = MaterialTheme.typography.titleMedium)

            Row {
                TextButton(onClick = onIgnore) {
                    Text("Ignore")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onAccept) {
                    Text("Accept")
                }
            }
        }
    }
}

suspend fun acceptFriendRequest(
    currentUID: String,
    senderUID: String
) {
    val db = Firebase.firestore

    val currentUserRef = db.collection("users").document(currentUID)
    val senderUserRef = db.collection("users").document(senderUID)

    db.runBatch { batch ->
        batch.update(currentUserRef, "friends", FieldValue.arrayUnion(senderUID))
        batch.update(senderUserRef, "friends", FieldValue.arrayUnion(currentUID))
        batch.update(currentUserRef, "friendRequests", FieldValue.arrayRemove(senderUID))
    }.await()
}

suspend fun ignoreFriendRequest(
    currentUID: String,
    senderUID: String
) {
    val db = Firebase.firestore
    val currentUserRef = db.collection("users").document(currentUID)
    currentUserRef.update("friendRequests", FieldValue.arrayRemove(senderUID)).await()
}

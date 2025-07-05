package software.lunchtable.recky

import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import software.lunchtable.recky.model.Recommendation
import androidx.compose.material3.Badge // Add this import
import androidx.compose.material3.BadgedBox // Add this import

@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    onRequestsClick: () -> Unit = {},
    onAddFriendClick: () -> Unit = {}
) {
    val firestore = Firebase.firestore
    val auth = Firebase.auth

    var isEditMode by remember { mutableStateOf(false) }
    val friends = remember { mutableStateListOf<FriendStats>() }
    var pendingRequestsCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser ?: return@LaunchedEffect
        val currentUID = currentUser.uid

        val currentUserDoc = firestore.collection("users").document(currentUID).get().await()
        val friendUIDs = currentUserDoc.get("friends") as? List<String> ?: emptyList()

        // Load friend documents
        val friendDocs = friendUIDs.map { uid ->
            firestore.collection("users").document(uid).get().await()
        }

        // Load recommendations where fromUID is current user or any friend
        val fromRecs = if (friendUIDs.isNotEmpty()) {
            firestore.collection("recommendations")
                .whereIn("fromUID", friendUIDs)
                .get()
                .await()
                .toObjects(Recommendation::class.java)
        } else {
            emptyList()
        }

        val toRecs = if (friendUIDs.isNotEmpty()) {
            firestore.collection("recommendations")
                .whereIn("toUID", friendUIDs)
                .get()
                .await()
                .toObjects(Recommendation::class.java)
        } else {
            emptyList()
        }

        val recommendations = fromRecs + toRecs

        // Combine user info and recommendation stats
        val friendStatsList = friendDocs.mapNotNull { doc ->
            val friendUID = doc.id
            val username = doc.getString("username") ?: return@mapNotNull null

            val sentRecs = recommendations.filter { it.fromUID == friendUID && it.vote != null }
            val sentTotal = sentRecs.size
            val sentThumbsUp = sentRecs.count { it.vote == true }

            val receivedRecs = recommendations.filter { it.toUID == friendUID && it.vote != null }
            val receivedTotal = receivedRecs.size
            val receivedThumbsUp = receivedRecs.count { it.vote == true }

            FriendStats(
                uid = friendUID,
                username = username,
                sentThumbsUp = sentThumbsUp,
                sentTotal = sentTotal,
                receivedThumbsUp = receivedThumbsUp,
                receivedTotal = receivedTotal
            )
        }

        // Replace the list contents with new data
        friends.clear()
        friends.addAll(friendStatsList)

        pendingRequestsCount = getPendingFriendRequestsCount()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        FriendsTopBar(
            onBack = onBack,
            isEditMode = isEditMode,
            onEdit = { isEditMode = !isEditMode }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onRequestsClick) {
                Box {
                    Text(
                        text = "Requests",
                        modifier = Modifier.align(Alignment.Center)
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

            Button(onClick = onAddFriendClick) {
                Text("Add Friend")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            friends.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No friends yet")
                }
            }

            else -> {
                LazyColumn {
                    items(friends) { friend ->
                        FriendCard(
                            friend = friend,
                            isEditMode = isEditMode,
                            onDelete = {
                                val currentUser = Firebase.auth.currentUser
                                val uid = currentUser?.uid
                                if (uid != null) {
                                    val db = Firebase.firestore
                                    val batch = db.batch()

                                    val currentUserRef = db.collection("users").document(uid)
                                    val friendRef = db.collection("users").document(friend.uid)

                                    batch.update(
                                        currentUserRef,
                                        "friends",
                                        FieldValue.arrayRemove(friend.uid)
                                    )
                                    batch.update(friendRef, "friends", FieldValue.arrayRemove(uid))

                                    batch.commit().addOnSuccessListener {
                                        friends.remove(friend)
                                    }.addOnFailureListener {
                                        Log.w("RemoveFriend", "Failed to remove friend", it)
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}


@Composable
fun FriendCard(
    friend: FriendStats,
    isEditMode: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(friend.username, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Sent: ${friend.sentThumbsUp}/${friend.sentTotal} (${friend.sentPercentage}%)")
                    Text("Received: ${friend.receivedThumbsUp}/${friend.receivedTotal} (${friend.receivedPercentage}%)")
                }
            }

            if (isEditMode) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Friend"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun FriendsTopBar(
    onBack: () -> Unit,
    isEditMode: Boolean,
    onEdit: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Friends",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            TextButton(onClick = onEdit) {
                Text(
                    if (isEditMode) "Done" else "Edit",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
            }
        }
    )
}

data class FriendStats(
    val uid: String = "",
    val username: String = "",
    val sentThumbsUp: Int = 0,
    val sentTotal: Int = 0,
    val receivedThumbsUp: Int = 0,
    val receivedTotal: Int = 0
) {
    val sentPercentage: Int
        get() = if (sentTotal > 0) (sentThumbsUp * 100 / sentTotal) else 0

    val receivedPercentage: Int
        get() = if (receivedTotal > 0) (receivedThumbsUp * 100 / receivedTotal) else 0
}

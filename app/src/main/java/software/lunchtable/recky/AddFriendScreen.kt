package software.lunchtable.recky

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(onBack: () -> Unit) {
    val firestore = Firebase.firestore
    val auth = Firebase.auth
    val currentUID = auth.currentUser?.uid ?: return

    var emailInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (showToast) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Friend request sent", Toast.LENGTH_SHORT).show()
            showToast = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text("Add Friend") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Enter friend's email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val emailTrimmed = emailInput.trim().lowercase()
                if (emailTrimmed.isNotBlank()) {
                    isSending = true
                    coroutineScope.launch {
                        try {
                            val query = firestore.collection("users")
                                .whereEqualTo("emailLowercase", emailTrimmed)
                                .get()
                                .await()

                            val friendDoc = query.documents.firstOrNull()
                            val friendUID = friendDoc?.id

                            if (friendUID != null && friendUID != currentUID) {
                                firestore.collection("users").document(friendUID)
                                    .update("friendRequests", FieldValue.arrayUnion(currentUID))
                                firestore.collection("users").document(currentUID)
                                    .update("sentRequests", FieldValue.arrayUnion(friendUID))
                            }

                            // ✅ Always show success message
                            showToast = true
                        } catch (e: Exception) {
                            Log.w("AddFriend", "Error sending friend request", e)
                            showToast = true // still show generic success
                        } finally {
                            emailInput = ""
                            isSending = false
                        }
                    }
                }
            },
            enabled = !isSending,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Send Request")
        }
    }
}


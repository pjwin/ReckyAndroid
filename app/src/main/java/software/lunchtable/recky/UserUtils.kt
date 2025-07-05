package software.lunchtable.recky // Or your common utility package

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

const val PENDING_REQUESTS_FIELD = "friendRequests" // Define field name as a constant

suspend fun getPendingFriendRequestsCount(): Int {
    val currentUser = Firebase.auth.currentUser
    val firestore = Firebase.firestore

    if (currentUser == null) {
        Log.w("UserUtils", "Cannot get pending requests count: User not logged in.")
        return 0
    }
    val currentUID = currentUser.uid

    return try {
        val currentUserDocRef = firestore.collection("users").document(currentUID)
        val currentUserSnapshot = currentUserDocRef.get().await()

        if (currentUserSnapshot.exists()) {
            val requestSenderUIDs = currentUserSnapshot.get(PENDING_REQUESTS_FIELD) as? List<String>
            requestSenderUIDs?.size ?: 0
        } else {
            Log.w("UserUtils", "User document not found for UID: $currentUID while fetching request count.")
            0
        }
    } catch (e: Exception) {
        Log.e("UserUtils", "Error fetching pending friend requests count for UID: $currentUID", e)
        0 // Return 0 on error
    }
}

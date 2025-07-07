package software.lunchtable.recky

import com.google.firebase.Timestamp

data class Recommendation(
    val id: String = "",
    val fromUID: String = "",
    val toUID: String = "",
    val title: String = "",
    val type: String = "",
    val vote: Boolean? = null,
    val voteNote: String? = null,
    val notes: String? = null,
    val timestamp: Timestamp? = null,
    val hasBeenViewedByRecipient: Boolean = false
)
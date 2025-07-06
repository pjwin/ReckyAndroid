package software.lunchtable.recky

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import software.lunchtable.recky.model.Recommendation

@Composable
fun RecommendationCard(
    recommendation: Recommendation,
    currentUserId: String,
    fromUsername: String,
    toUsername: String,
    onClick: () -> Unit
) {
    val voteIcon = when (recommendation.vote) {
        true -> "👍"
        false -> "👎"
        null -> ""
    }

    val isSentByUser = recommendation.fromUID == currentUserId
    val direction = if (isSentByUser) "to @$toUsername" else "from @$fromUsername"

    val typeEmoji = getEmojiForType(recommendation.type)
    val timeAgo = remember(recommendation.timestamp) {
        getRelativeTimeString(recommendation.timestamp?.toDate()?.time ?: 0L)
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (voteIcon.isNotEmpty()) {
                Text(
                    voteIcon,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$typeEmoji ${recommendation.title}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = timeAgo,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Text(
                    text = direction,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

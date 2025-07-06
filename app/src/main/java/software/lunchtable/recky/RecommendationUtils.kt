package software.lunchtable.recky

import java.util.concurrent.TimeUnit

fun getRelativeTimeString(timestampMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMillis

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${diff / TimeUnit.MINUTES.toMillis(1)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${diff / TimeUnit.HOURS.toMillis(1)}h ago"
        diff < TimeUnit.DAYS.toMillis(7) -> "${diff / TimeUnit.DAYS.toMillis(1)}d ago"
        else -> "${diff / TimeUnit.DAYS.toMillis(7)}w ago"
    }
}

fun getEmojiForType(type: String): String {
    return when (type.lowercase()) {
        "movie" -> "🎬"
        "book" -> "📚"
        "tv" -> "📺"
        "album", "music" -> "🎵"
        "game" -> "🎮"
        else -> "❓"
    }
}

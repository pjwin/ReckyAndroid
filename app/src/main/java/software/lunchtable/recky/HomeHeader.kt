package software.lunchtable.recky

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun HomeHeader(userEmail: String, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo (left)
        Image(
            painter = painterResource(id = R.drawable.recky_logo),
            contentDescription = "Recky Logo",
            modifier = Modifier.size(40.dp)
        )

        // Spacer between logo and text
        Spacer(modifier = Modifier.weight(1f))

        // Welcome text (center)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Welcome back,", style = MaterialTheme.typography.bodyMedium)
            Text(text = userEmail, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }

        // Spacer between text and icon
        Spacer(modifier = Modifier.weight(1f))

        // Profile icon (right)
        IconButton(onClick = onProfileClick) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, Color.Gray, CircleShape)
                    .padding(4.dp)
            )
        }

    }
}

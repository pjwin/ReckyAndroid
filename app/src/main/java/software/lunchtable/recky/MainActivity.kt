package software.lunchtable.recky

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import software.lunchtable.recky.ui.theme.ReckyTheme

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private var isLoggingIn = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        var onGoogleLoginSuccess: (() -> Unit)? = null

        googleSignInLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    isLoggingIn.value = true
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener { firebaseResult ->
                            isLoggingIn.value = false
                            if (firebaseResult.isSuccessful) {
                                onGoogleLoginSuccess?.invoke()
                            } else {
                                Log.e(
                                    "GoogleLogin",
                                    "Firebase auth failed",
                                    firebaseResult.exception
                                )
                            }
                        }
                } catch (e: ApiException) {
                    Log.e("GoogleLogin", "Google sign-in failed", e)
                }
            }

        // Compose UI
        setContent {
            ReckyTheme {
                val navController = rememberNavController()
                onGoogleLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }

                ReckyNavGraph(
                    navController = navController,
                    auth = FirebaseAuth.getInstance(),
                    isLoading = isLoggingIn.value,
                    onGoogleSignIn = {
                        val intent = googleSignInClient.signInIntent
                        googleSignInLauncher.launch(intent)
                    }
                )
            }
        }
    }
}

package software.lunchtable.recky

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import software.lunchtable.recky.ui.theme.ReckyTheme

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private var setLoggedIn: ((Boolean) -> Unit)? = null // 👈 will set this later from Compose

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Set up Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ✅ Register launcher
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { firebaseResult ->
                        if (firebaseResult.isSuccessful) {
                            Log.d("GoogleLogin", "Firebase auth successful")
                            setLoggedIn?.invoke(true) // ✅ update Compose state
                        } else {
                            Log.e("GoogleLogin", "Firebase auth failed", firebaseResult.exception)
                        }
                    }
            } catch (e: ApiException) {
                Log.e("GoogleLogin", "Google sign-in failed", e)
            }
        }

        // ✅ Compose UI
        setContent {
            ReckyTheme {
                var loggedIn by remember { mutableStateOf(auth.currentUser != null) }
                setLoggedIn = { loggedIn = it } // 👈 expose state update to callback

                if (loggedIn) {
                    HomeScreen(
                        onLogout = {
                            auth.signOut()
                            googleSignInClient.signOut().addOnCompleteListener {
                                loggedIn = false
                            }
                        }
                    )
                } else {
                    LoginScreen(
                        onLoginSuccess = { loggedIn = true },
                        onGoogleSignIn = {
                            val intent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(intent)
                        }
                    )
                }
            }
        }
    }
}

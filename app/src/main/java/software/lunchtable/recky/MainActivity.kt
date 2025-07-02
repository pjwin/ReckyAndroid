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

    enum class Screen { Login, Home, Profile }

    private var setScreen: ((Screen) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { firebaseResult ->
                        if (firebaseResult.isSuccessful) {
                            Log.d("GoogleLogin", "Firebase auth successful")
                            setScreen?.invoke(Screen.Home)
                        } else {
                            Log.e("GoogleLogin", "Firebase auth failed", firebaseResult.exception)
                        }
                    }
            } catch (e: ApiException) {
                Log.e("GoogleLogin", "Google sign-in failed", e)
            }
        }

        setContent {
            ReckyTheme {
                var currentScreen by remember {
                    mutableStateOf(
                        if (auth.currentUser != null) Screen.Home else Screen.Login
                    )
                }
                setScreen = { screen -> currentScreen = screen }

                when (currentScreen) {
                    Screen.Login -> LoginScreen(
                        onLoginSuccess = { currentScreen = Screen.Home },
                        onGoogleSignIn = {
                            val intent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(intent)
                        }
                    )

                    Screen.Home -> HomeScreen(
                        onProfileClick = {
                            currentScreen = Screen.Profile
                        }
                    )

                    Screen.Profile -> ProfileScreen(
                        onSignOut = {
                            auth.signOut()
                            googleSignInClient.signOut().addOnCompleteListener {
                                currentScreen = Screen.Login
                            }
                        },
                        onBack = {
                            currentScreen = Screen.Home
                        }
                    )
                }
            }
        }
    }
}

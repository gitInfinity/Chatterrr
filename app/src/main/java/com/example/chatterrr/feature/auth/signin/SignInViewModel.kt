package com.example.chatterrr.feature.auth.signin

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database // Import database if needed for token logic here
import com.google.firebase.ktx.Firebase // Import Firebase if not already
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class SignInViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow<SignInState>(SignInState.Nothing)
    val state = _state.asStateFlow()

    // Get Firebase Auth instance
    private val auth = FirebaseAuth.getInstance()

    fun signIn(email: String, password: String) {
        _state.value = SignInState.Loading

        // --- SINGLE ACCOUNT LOGIN LOGIC ---
        if (auth.currentUser != null) {
            Log.w("SignInViewModel", "Existing user found (${auth.currentUser?.uid}). Signing out before new login attempt.")
            // Sign out the currently logged-in user
            auth.signOut()
            // Optional: Clear local cache or data related to the old user if necessary
        }
        // --- END SINGLE ACCOUNT LOGIN LOGIC ---

        // Proceed with the new sign-in attempt
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // User successfully signed in
                    Log.d("SignInViewModel", "Sign in successful for ${auth.currentUser?.uid}") //debugging
                    // Get FCM token and send to server for the *new* user
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val token = tokenTask.result
                            if (token != null) {
                                // Use the updated sendTokenToServer function
                                sendTokenToServerRTDB(token)
                            } else {
                                Log.e("SignInViewModel", "FCM token was null.")
                            }
                            // Whether token sending succeeds or fails, login was successful
                            _state.value = SignInState.Success
                        } else {
                            Log.e(
                                "SignInViewModel",
                                "Failed to get FCM token after login: ${tokenTask.exception?.message}"
                            )
                            // Login succeeded, but token retrieval failed. Still proceed to Success state.
                            _state.value = SignInState.Success
                        }
                    }
                } else {
                    // Sign in failed
                    Log.e("SignInViewModel", "Sign in failed: ${task.exception?.message}")
                    _state.value = SignInState.Error
                }
            }
    }

    // Updated function to store token in Realtime Database (like in MyFirebaseMessagingService)
    // This avoids needing the Cloud Function call here for consistency
    private fun sendTokenToServerRTDB(token: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            Firebase.database.reference.child("active-tokens").child(userId).setValue(token)
                .addOnSuccessListener {
                    Log.d("SignInViewModel", "Active FCM token stored in RTDB for user $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("SignInViewModel", "Error storing active FCM token in RTDB: ${e.message}", e)
                }
        } else {
            Log.w("SignInViewModel", "User not logged in after successful sign-in? Cannot store FCM token.")
        }
    }

    // Keep the original function if you still need to call the Cloud Function for other reasons,
    // but using RTDB directly like the service is simpler here.
    /*
    private fun sendTokenToServer(token: String) {
        //  Call the Firebase Function to store the token
        val functions = Firebase.functions
        val data = hashMapOf("token" to token)

        functions
            .getHttpsCallable("storeUserFCMToken")
            .call(data)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SignInViewModel", "FCM token sent to function successfully")
                } else {
                    Log.e(
                        "SignInViewModel",
                        "Error sending FCM token via function: ${task.exception?.message}"
                    )
                }
            }
    }
    */
}

// SignInState remains the same
sealed class SignInState {
    object Nothing: SignInState()
    object Loading: SignInState()
    object Success: SignInState()
    object Error: SignInState()
}
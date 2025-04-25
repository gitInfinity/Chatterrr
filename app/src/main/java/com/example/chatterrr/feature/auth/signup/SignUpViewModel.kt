package com.example.chatterrr.feature.auth.signup
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.functions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow<SignUpState>(SignUpState.Nothing)
    val state = _state.asStateFlow()

    fun signUp(name: String, email: String, password: String) {
        _state.value = SignUpState.Loading
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result.user?.let { user ->
                        // Get FCM token and send to server
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                val token = tokenTask.result
                                if (token != null) {
                                    sendTokenToServer(token, user.uid) // Pass the UID
                                }
                                _state.value = SignUpState.Success
                            } else {
                                Log.e(
                                    "SignUpViewModel",
                                    "Failed to get FCM token: ${tokenTask.exception?.message}"
                                )
                                _state.value = SignUpState.Success  // Still navigate, but log error
                            }
                        }
                        return@addOnCompleteListener
                    }
                    _state.value = SignUpState.Error
                } else {
                    _state.value = SignUpState.Error
                }
            }
    }

    private fun sendTokenToServer(token: String, userId: String) { // Add userId parameter
        // Call the Firebase Function to store the token
        val functions = Firebase.functions
        val data = hashMapOf("token" to token)

        functions
            .getHttpsCallable("storeUserFCMToken")
            .call(data)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SignUpViewModel", "FCM token sent to server successfully")
                } else {
                    Log.e(
                        "SignUpViewModel",
                        "Error sending FCM token: ${task.exception?.message}"
                    )
                }
            }
    }
}

sealed class SignUpState {
    object Nothing: SignUpState()
    object Loading: SignUpState()
    object Success: SignUpState()
    object Error: SignUpState()
}
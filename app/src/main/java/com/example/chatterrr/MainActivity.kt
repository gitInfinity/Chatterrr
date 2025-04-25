package com.example.chatterrr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val channelId = intent.getStringExtra("channelId")
        val channelName = intent.getStringExtra("channelName")
        val userSignedIn = FirebaseAuth.getInstance().currentUser != null

        setContent {
            MainApp(
                startChatId = if (userSignedIn) channelId else null,
                startChatName = if (userSignedIn) channelName else null
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset the unread count and clear the notification when the main activity resumes
    }
}

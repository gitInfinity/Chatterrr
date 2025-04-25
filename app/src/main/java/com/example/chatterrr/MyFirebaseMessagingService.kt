package com.example.chatterrr

import android.R.id.message
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("MyFirebaseMessagingService", "Message received: ${message.data}")
        val currentUid = Firebase.auth.currentUser?.uid
        val senderId  = message.data["senderId"]
        if (senderId != null && senderId == currentUid) {
            Log.d("MyFirebaseMessagingService", "Skipping self‐notification for $senderId")
            return
        }
        message.notification?.let {
            showNotification(it.title, it.body)
        }
    }

    fun showNotification(title: String?, message: String?){
        Firebase.auth.currentUser?.let {
            if (title?.contains(it.displayName.toString()) == true || message?.contains(it.displayName.toString()) == true){}
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("messages","Messages",NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
        val notificationID = Random().nextInt(1000)
        val notification = NotificationCompat.Builder(this,"messages").setContentTitle(title)
            .setContentText(message).setSmallIcon(android.R.drawable.ic_dialog_info).build()
        notificationManager.notify(notificationID,notification)
        }
    }

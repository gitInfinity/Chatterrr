package com.example.chatterrr.feature.chat

// ... other imports ...
import android.content.Context
import android.net.Uri
import android.util.Base64 // Use android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.volley.Request.Method.POST
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.chatterrr.R
import com.example.chatterrr.model.Message
import com.google.android.gms.common.api.Response
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.storage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.lang.reflect.Method
import java.util.UUID
import javax.inject.Inject
import com.google.auth.oauth2.GoogleCredentials

@HiltViewModel
class ChatViewModel @Inject constructor(@ApplicationContext private val context: Context) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val message = _messages.asStateFlow()
    private val db = Firebase.database
    private val auth = Firebase.auth

    private val _participants = MutableStateFlow<List<String>>(emptyList())
    val participants = _participants.asStateFlow()

    fun loadParticipants(channelID: String) {
        val partRef = db.getReference("channel-participants").child(channelID)
        partRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // snapshot.children are userId nodes; key = UID
                val uids = snapshot.children.mapNotNull { it.key }
                _participants.value = uids
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("ChatViewModel", "loadParticipants:onCancelled", error.toException())
            }
        })
    }

    fun sendMessage(channelID: String, messageText: String?, image: String? = null) {
        val message = Message(
            id = db.reference.push().key ?: UUID.randomUUID().toString(),
            senderId = Firebase.auth.currentUser?.uid ?: "",
            message = messageText, // No plain text for image messages
            createdAt = System.currentTimeMillis(),
            senderName = Firebase.auth.currentUser?.displayName ?: "",
            senderPhotoUrl = null, // Add if you have sender photo
            imageUrl = image
        )
        db.reference.child("messages").child(channelID).push().setValue(message).addOnCompleteListener {
            if (it.isSuccessful) {
                postNotification(channelID, message.senderName, messageText?:" ")
            }
        }
    }

    fun markAllRead(channelID: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val messagesRef = db.getReference("messages")
            .child(channelID)
            .orderByChild("createdAt")

        // Listen once for messages and update those not yet read
        messagesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { msgSnap ->
                    val msg = msgSnap.getValue(Message::class.java) ?: return@forEach
                    if (currentUid !in msg.readBy) {
                        // Append current user to readBy
                        msgSnap.ref.child("readBy")
                            .setValue(msg.readBy + currentUid)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) { /* handle error */ }
        })
    }

    fun sendImage(uri: Uri, channelID: String) {
        val imageRef = Firebase.storage.reference.child("images/${UUID.randomUUID()}")
        imageRef.putFile(uri).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            // val currentUser = Firebase.auth.currentUser // Already available via auth member
            if (task.isSuccessful) {
                val downloadUri = task.result.toString()
                // Call sendMessage with null text and the image URL
                sendMessage(channelID, " ", downloadUri) // Pass blank message text, use image param
            } else {
                Log.e("ChatViewModel", "Image upload failed", task.exception)
            }
        }
    }

    fun sendVideo(uri: Uri, channelID: String) {
        val videoRef = Firebase.storage.reference.child("videos/${UUID.randomUUID()}")
        videoRef.putFile(uri).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            videoRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result.toString()
                // Call sendMessage with null text and the video URL
                sendMessage(channelID, "", downloadUri) // Pass blank message text, use image param
            } else {
                Log.e("ChatViewModel", "Video upload failed", task.exception)
                // Handle upload failure
            }
        }
    }

    fun listenForMessages(channelID: String) {
        // 1. Reference the “messages/{channelID}” node, ordered by createdAt
        val messagesRef = db.getReference("messages")
            .child(channelID)
            .orderByChild("createdAt")

        // 2. Attach a ValueEventListener
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 3. Map each snapshot to your Message class, then sort
                val list = snapshot.children
                    .mapNotNull { it.getValue(Message::class.java) }
                    .sortedBy { it.createdAt }     // oldest first
                _messages.value = list           // update your StateFlow
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("ChatViewModel", "listenForMessages:onCancelled", error.toException())
            }
        })
        subscribeForNotifications(channelID)
        loadParticipants(channelID)
    }

    private fun subscribeForNotifications(channelId: String){
        FirebaseMessaging.getInstance().subscribeToTopic("group_$channelId")
            .addOnSuccessListener { Log.d("FCM", "Subscribed to topic $channelId") }
            .addOnFailureListener { Log.w("FCM", "Failed to subscribe to topic $channelId", it) }
    }

    private fun postNotification(channelId: String, senderName: String, message:String) {
        val fcmURL = "https://fcm.googleapis.com/v1/projects/${Firebase.app.options.projectId}/messages:send"
        val jsonBody = JSONObject().apply {
            put("message", JSONObject().apply {
                put("topic", "group_$channelId")
                put("notification", JSONObject().apply {
                    put("title", "New Message in $channelId")
                    put("body", "$senderName: $message")
                }
                )
                put("data", JSONObject().apply {
                    put("senderId", auth.currentUser?.uid)
                })
            }
            )
        }
        val requestBody = jsonBody.toString()
        val request = object : StringRequest(
            POST, fcmURL,
            com.android.volley.Response.Listener{
                Log.d(
                    "FCM",
                    "Notification sent successfully"
                )
            },
            com.android.volley.Response.ErrorListener {
                Log.e(
                    "FCM",
                    "Error sending notification",
                    it
                )
            }) {

            override fun getBody(): ByteArray {
                return requestBody.toByteArray()
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${getAccessToken()}"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        val queue = Volley.newRequestQueue(context)
        queue.add(request)
    }

    private fun getAccessToken():String{
        val inputStream = context.resources.openRawResource(R.raw.chatapp_key)
        val googleCreds = GoogleCredentials.fromStream(inputStream).
        createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        return googleCreds.refreshAccessToken().tokenValue
    }

    fun leaveChannel(channelId: String, onComplete: (isSuccessful: Boolean) -> Unit = {}) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.w("ChatViewModel", "User not logged in, cannot leave channel.")
            onComplete(false)
            return
        }
        // Unsubscribe from FCM topic when leaving
        FirebaseMessaging.getInstance().unsubscribeFromTopic(channelId)
            .addOnSuccessListener { Log.d("FCM", "Unsubscribed from topic $channelId on leave") }
            .addOnFailureListener { Log.w("FCM", "Failed to unsubscribe from topic $channelId on leave", it) }

        val updates = hashMapOf<String, Any?>(
            "channel-participants/$channelId/$currentUserId" to null,
            "user-channels/$currentUserId/$channelId" to null
        )

        db.getReference().updateChildren(updates)
            .addOnSuccessListener {
                Log.d("ChatViewModel", "User $currentUserId successfully left channel $channelId.")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Failed for user $currentUserId to leave channel $channelId", e)
                onComplete(false)
            }
    }
}
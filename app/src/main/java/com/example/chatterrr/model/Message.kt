package com.example.chatterrr.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties // Important for Firebase serialization
data class Message(
    val id: String = "", // Default value needed for Firebase
    val senderId: String = "",
    var message: String? = null, // Will hold DECRYPTED text, or null for image/video/failed decrypt
    val createdAt: Long = System.currentTimeMillis(),
    val senderName: String = "", // Or display name
    val senderPhotoUrl: String? = null, // If you store user avatars
    val imageUrl: String? = null, // For images/videos
    val readBy: List<String> = emptyList()
)
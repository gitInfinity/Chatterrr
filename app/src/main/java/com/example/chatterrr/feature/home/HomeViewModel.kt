package com.example.chatterrr.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.chatterrr.model.Channel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(): ViewModel() {
    private val firebaseDatabase = Firebase.database
    private val firebaseAuth = FirebaseAuth.getInstance() // <-- Get FirebaseAuth instance

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init{
        getChannels()
    }

    private fun getChannels() {
        // Keep existing logic for getting channel list
        firebaseDatabase.getReference("channel").get().addOnSuccessListener {
            val list = mutableListOf<Channel>()
            it.children.forEach{data ->
                // Assuming the structure is /channel/{channelId}: "ChannelName"
                val channelId = data.key
                val channelName = data.value.toString()
                if (channelId != null) {
                    // Create Channel object, potentially add other fields if needed later
                    val channel = Channel(id = channelId, name = channelName)
                    list.add(channel)
                }
            }
            _channels.value = list
            Log.d("HomeViewModel", "Fetched ${list.size} channels")
        }.addOnFailureListener {
            Log.e("HomeViewModel", "Error fetching channels", it)
        }
    }

    fun addChannel(name: String){
        val channelRef = firebaseDatabase.getReference("channel")
        val participantsRef = firebaseDatabase.getReference("channel-participants")
        val userChannelsRef = firebaseDatabase.getReference("user-channels") // <-- Reference for participants

        val newChannelKey = channelRef.push().key // Generate a unique ID for the new channel
        val currentUserId = firebaseAuth.currentUser?.uid // Get the ID of the user creating the channel

        if (newChannelKey != null && currentUserId != null) {
            // Use a Map to set multiple values if needed, but for now, just setting the name
            // In a real app, Channel might store more data like creation time, creatorId etc.
            // val newChannelData = mapOf(
            //     "name" to name,
            //     "createdAt" to System.currentTimeMillis(),
            //     "creatorId" to currentUserId
            // )

            // 1. Set the channel name under the generated key
            channelRef.child(newChannelKey).setValue(name).addOnSuccessListener {
                Log.d("HomeViewModel", "Channel '$name' added successfully with ID: $newChannelKey")

                // 2. Add the current user as a participant for this new channel
                // Set the user's ID to true under the channel's participants node
                participantsRef.child(newChannelKey).child(currentUserId).setValue(true)
                    .addOnSuccessListener {
                        Log.d("HomeViewModel", "User $currentUserId added as participant to channel $newChannelKey")
                        // Refresh the channel list after the process is complete
                        getChannels()
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeViewModel", "Failed to add user $currentUserId as participant to channel $newChannelKey", e)
                        // Handle failure: maybe the channel shouldn't have been created if participant couldn't be added?
                        getChannels() // Still refresh even if participant add failed
                    }
                // 3. Add the channel to the user's list of channels
                userChannelsRef.child(currentUserId).child(newChannelKey).setValue(true)
                    .addOnSuccessListener {
                        Log.d("HomeViewModel", "Channel $newChannelKey added to user $currentUserId's channels")
                        // Refresh the channel list after the process is complete
                        getChannels()
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeViewModel", "Failed to add channel $newChannelKey to user $currentUserId's channels", e)
                        // Handle failure: maybe the channel shouldn't have been created if participant couldn't be added?
                        getChannels() // Still refresh even if participant add failed
                    }
            }
                .addOnFailureListener { e ->
                    Log.e("HomeViewModel", "Failed to add channel '$name'", e)
                    getChannels() // Still refresh even if channel add failed
                }
        } else {
            Log.e("HomeViewModel", "Failed to add channel: New key or current user ID is null.")
            // Consider showing an error to the user
        }
    }

    fun onChannelClicked(channelId: String, channelName: String, navigateToChat: (channelId: String, channelName: String) -> Unit) {
        // Reset error state when a new action is initiated
        _error.value = null

        val currentUserId = firebaseAuth.currentUser?.uid

        if (currentUserId == null) {
            Log.w("HomeViewModel", "User not logged in, cannot join channel or navigate.")
            _error.value = "You must be logged in to join a channel." // Set error state
            return
        }

        // First, check the current participants for this channel
        firebaseDatabase.getReference("channel-participants").child(channelId).get().addOnSuccessListener { snapshot ->
            val participantsData = snapshot.value as? Map<*, *> // Get participants as a map
            val currentParticipantCount = participantsData?.size ?: 0 // Count existing participants
            val isAlreadyParticipant = participantsData?.containsKey(currentUserId) ?: false // Check if current user is already in

            if (isAlreadyParticipant) {
                Log.d("HomeViewModel", "User $currentUserId is already a participant in channel $channelId. Navigating directly.")
                // User is already a member, just navigate
                navigateToChat(channelId, channelName)

            } else {
                // User is NOT already a participant
                if (currentParticipantCount == 2) {
                    // Channel is full (2 or more participants), prevent joining
                    Log.w("HomeViewModel", "Channel $channelId is full ($currentParticipantCount participants). Cannot join.")
                    _error.value = "This channel is full. Cannot join." // Set error state

                } else {
                    // Channel has less than 2 participants, allow joining (this user will be the 2nd)
                    Log.d("HomeViewModel", "User $currentUserId joining channel $channelId. Current count: $currentParticipantCount")

                    // Prepare a batched update to add the current user as a participant
                    // in channel-participants and add the channel to the user's list in user-channels.
                    val updates = hashMapOf<String, Any?>(
                        "channel-participants/$channelId/$currentUserId" to true, // Add user to channel's participants list
                        "user-channels/$currentUserId/$channelId" to true // Add channel to user's list of channels
                    )

                    // Perform the batched update
                    firebaseDatabase.getReference().updateChildren(updates)
                        .addOnSuccessListener {
                            Log.d("HomeViewModel", "User $currentUserId successfully added to participants and user-channels for channel $channelId.")
                            // After the database update is complete, perform the navigation
                            navigateToChat(channelId, channelName)
                        }
                        .addOnFailureListener { e ->
                            Log.e("HomeViewModel", "Failed to add user $currentUserId to participants/user-channels for channel $channelId upon clicking", e)
                            _error.value = "Failed to join channel. Please try again." // Set error state
                            // You might still navigate here if you want the user to see history even if join failed
                            // navigateToChat(channelId, channelName)
                        }
                }
            }

        }.addOnFailureListener { e ->
            Log.e("HomeViewModel", "Failed to read participants for channel $channelId", e)
            _error.value = "Failed to check channel status. Please try again." // Set error state
            // Decide whether to navigate or not on read failure
            // navigateToChat(channelId, channelName)
        }
    }

}
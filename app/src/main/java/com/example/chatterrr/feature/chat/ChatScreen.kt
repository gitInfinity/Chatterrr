package com.example.chatterrr.feature.chat

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons // Import Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExitToApp // Import ExitToApp icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi // Still using this for ContentSelectionDialogue
import androidx.compose.material3.Icon // Import Icon
import androidx.compose.material3.IconButton // Import IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar // Import TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chatterrr.R
// Removed import for ChannelItem as it will be replaced by TopAppBar title
// import com.example.chatterrr.feature.home.ChannelItem
import com.example.chatterrr.model.Message
import com.example.chatterrr.ui.theme.DarkGrey
import com.example.chatterrr.ui.theme.LightBlue
import com.example.chatterrr.ui.theme.LightGrey
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessaging
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class) // Add ExperimentalMaterial3Api to the composable annotation
@Composable
fun ChatScreen(navController: NavController, channelId: String, channelName: String) {
    val viewModel: ChatViewModel = hiltViewModel()

    val showLeaveDialog = remember { mutableStateOf(false) }
    val participants = viewModel.participants.collectAsState()

    LaunchedEffect(channelId) {
        viewModel.listenForMessages(channelId)
        viewModel.markAllRead(channelId)
    }

    FirebaseMessaging.getInstance()
        .subscribeToTopic(channelId)
        .addOnSuccessListener { Log.d("FCM", "Subscribed to topic $channelId") }
        .addOnFailureListener { Log.w("FCM", "Subscribe to topic failed", it) }


    val cameraImageURI = remember { mutableStateOf<Uri?>(null) }
    fun createImageURI(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = navController.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile(imageFileName, ".jpg", storageDir).apply {
            cameraImageURI.value = Uri.fromFile(this)
        }
        return FileProvider.getUriForFile(
            navController.context,
            "${navController.context.packageName}.provider",
            file
        )
    }

    val cameraImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageURI.value?.let {
                viewModel.sendImage(it, channelId)
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraImageLauncher.launch(createImageURI())
        }
    }
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendImage(it, channelId)
        }
    }

    val videoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendVideo(it, channelId)
        }
    }
    // End of media picker logic


    Scaffold(
        containerColor = Color.DarkGray,
        // --- Add a TopAppBar ---

        // --- End of TopAppBar ---
    ) { paddingValues ->

        val chooseDialogue = remember { mutableStateOf(false) } // Still needed for media picker

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from the Scaffold
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = channelName,
                    color = Color.White,
                    modifier = Modifier.weight(1f) // Take up remaining space
                )
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Leave Channel",
                        tint = Color.White
                    )
                }
            }
            val messages = viewModel.message.collectAsState()
            ChatMessages(
                // channelName is no longer passed here as it's in the TopAppBar
                messages = messages.value,
                onSendMessage = { message ->
                    viewModel.sendMessage(channelId, message)
                },
                participants = participants.value,
                onImageClicked = {
                    chooseDialogue.value = true
                }
            )
        }

        // --- Add Leave Channel Confirmation Dialog ---
        if (showLeaveDialog.value) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog.value = false }, // Dismiss when clicking outside
                title = { Text(text = "Leave Channel") },
                text = { Text(text = "Are you sure you want to leave \"$channelName\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        showLeaveDialog.value = false // Dismiss dialog
                        // Call the ViewModel function to leave the channel
                        viewModel.leaveChannel(channelId) { isSuccessful ->
                            if (isSuccessful) {
                                // If leaving was successful, navigate back
                                navController.popBackStack()
                                // Optional: Also unsubscribe from the FCM topic if you only subscribe when in the channel
                                FirebaseMessaging.getInstance().unsubscribeFromTopic(channelId)
                                    .addOnSuccessListener { Log.d("FCM", "Unsubscribed from topic $channelId") }
                                    .addOnFailureListener { Log.w("FCM", "Unsubscribe from topic failed", it) }
                            } else {
                                // Handle failure (e.g., show a Toast message)
                                Log.e("ChatScreen", "Failed to leave channel $channelId")
                                // You might choose to still navigate back or stay depending on desired UX on failure
                                // navController.popBackStack() // Optional: navigate back even on failure
                            }
                        }
                    }) {
                        Text("Leave")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveDialog.value = false }) { // Dismiss dialog
                        Text("Cancel")
                    }
                }
            )
        }
        // --- End of Leave Channel Confirmation Dialog ---


        // Media selection dialog (remains unchanged)
        if (chooseDialogue.value) {
            ContentSelectionDialogue(
                onDismissRequest = { chooseDialogue.value = false },
                onCameraSelected = {
                    chooseDialogue.value = false
                    if (navController.context.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraImageLauncher.launch(createImageURI())
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                },
                onGallerySelected = {
                    chooseDialogue.value = false
                    imageLauncher.launch("image/*")
                },
                onVideoGallerySelected = {
                    chooseDialogue.value = false
                    videoGalleryLauncher.launch("video/*")
                }
            )
        }
    }
}

// ContentSelectionDialogue remains unchanged
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContentSelectionDialogue(
    onDismissRequest: () -> Unit,
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit,
    onVideoGallerySelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Select attachment source") },
        text = { // Placing selection options within the text area
            Column {
                Text(text = "Choose where to get your attachment from.") // Keep the introductory text
                Spacer(modifier = Modifier.size(16.dp)) // Add some spacing for separation

                TextButton(onClick = onCameraSelected) {
                    Text(text = "Camera")
                }
                TextButton(onClick = onGallerySelected) {
                    Text(text = "Image Gallery")
                }
                TextButton(onClick = onVideoGallerySelected) {
                    Text(text = "Video Gallery")
                }
            }
        },
        confirmButton = {
            // Confirm button slot is typically for the main positive action, not a list of options
            // Leaving it empty or adding a neutral/informative button is common here.
            // For selection dialogues, the action happens when a source is chosen from the list above.
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        }
    )
}


// ChatMessages composable (remove channelName parameter)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessages(
    messages: List<Message>,
    onSendMessage: (String) -> Unit,
    participants: List<String>,
    onImageClicked: () -> Unit
) {
    val hideKeyboardController = LocalSoftwareKeyboardController.current
    val msg = remember {
        mutableStateOf("")
    }
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp) // Add some horizontal padding to the messages
        ) {
            // Removed the ChannelItem display here as it's now in the TopAppBar
            // item {
            //     ChannelItem(channelname = channelName, modifier = Modifier) { }
            // }
            items(messages) { message ->
                ChatBubble(message = message, participants = participants)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = DarkGrey)
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        )
        {
            IconButton(onClick = {
                onImageClicked()
            })
            {
                Image(painter = painterResource(R.drawable.attach_document), contentDescription = "Attach")
            }
            TextField(
                value = msg.value,
                onValueChange = { msg.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(text = "Type") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (msg.value.isNotBlank()) { // Only send if message is not blank
                            onSendMessage(msg.value)
                            msg.value = "" // Clear input after sending
                            hideKeyboardController?.hide() // Hide keyboard
                        } else {
                            hideKeyboardController?.hide() // Just hide keyboard if blank
                        }
                    }
                ),
                colors = TextFieldDefaults.colors().copy(
                    focusedContainerColor = DarkGrey,
                    unfocusedContainerColor = DarkGrey,
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedPlaceholderColor = White,
                    unfocusedPlaceholderColor = White,
                    cursorColor = White // Set cursor color
                ),
                shape = RoundedCornerShape(24.dp) // Optional: Add rounded corners to TextField
            )
            Spacer(modifier = Modifier.width(8.dp)) // Space between TextField and Send button
            IconButton(onClick = {
                if (msg.value.isNotBlank()) { // Only send if message is not blank
                    onSendMessage(msg.value)
                    msg.value = "" // Clear input after sending
                    hideKeyboardController?.hide() // Hide keyboard
                }
            }) {
                Image(painter = painterResource(R.drawable.baseline_send_24), contentDescription = "Send")
            }
        }
    }
}

// ChatBubble remains unchanged
@Composable
fun ChatBubble(message: Message, participants: List<String>) {
    val currentUid = Firebase.auth.currentUser?.uid
    val isCurrentUser = message.senderId == Firebase.auth.currentUser?.uid
    val otherRecipients = participants.filter { it != message.senderId }
    val allRead = otherRecipients.all { it in message.readBy }

    val bubbleColor = if (isCurrentUser) {
        LightBlue
    } else {
        LightGrey
    }
    // Use Alignment within the Box itself
    val bubbleAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = bubbleAlignment // Apply alignment to the Box content
    ) {
        Row(
            // Remove redundant alignment here
            verticalAlignment = Alignment.CenterVertically
        )
        {
            // Conditionally show sender indicator (if not current user) - assuming R.drawable.chat is a placeholder avatar
            if (!isCurrentUser) {
                Image(
                    painter = painterResource(id = R.drawable.chat),
                    contentDescription = "Sender Avatar",
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterVertically) // Align avatar vertically in the Row
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (isCurrentUser) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (allRead) Icons.Default.DoneAll else Icons.Default.Done,
                    contentDescription = if (allRead) "Read" else "Sent",
                    modifier = Modifier.size(16.dp)
                )
            }

            // Message bubble background and padding
            Box(
                modifier = Modifier
                    .background(
                        color = bubbleColor,
                        shape = RoundedCornerShape(8.dp) // Consistent shape
                    )
                    .padding(8.dp)
            ) {
                if (message.imageUrl != null) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Image message", // Better content description
                        modifier = Modifier
                            .size(200.dp) // Adjust size as needed
                            .clip(RoundedCornerShape(4.dp)), // Clip image corners
                        contentScale = ContentScale.Crop // Use Crop or FillBounds as appropriate
                        // alignment is applied by the parent Box now
                    )
                    // No Spacer needed inside this Box
                } else if (message.message != null && message.message!!.isNotBlank()) { // Check for blank message too
                    Text(text = message.message!!.trim(), color = White)
                } else {
                    // Handle empty message case if necessary, maybe show nothing or an indicator
                    Text(text = "Empty message", color = Color.Gray) // Placeholder
                }
            }
        }
    }
}
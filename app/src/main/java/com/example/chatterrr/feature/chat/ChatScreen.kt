package com.example.chatterrr.feature.chat

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.chatterrr.feature.home.ChannelItem
import com.example.chatterrr.model.Message
import com.example.chatterrr.ui.theme.DarkGrey
import com.example.chatterrr.ui.theme.LightBlue
import com.example.chatterrr.ui.theme.LightGrey
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(navController: NavController, channelId: String, channelName: String) {
    val viewModel: ChatViewModel = hiltViewModel()
    Scaffold(
        containerColor = Color.DarkGray
    ) { paddingValues ->
        val chooseDialogue = remember { mutableStateOf(false) }
        val cameraImageURI = remember { mutableStateOf<Uri?>(null) }
        fun createImageURI(
        ): Uri {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir =
                navController.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
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
        val permissionLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    cameraImageLauncher.launch(createImageURI())
                }
            }
        val imageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri:Uri? ->
            uri?.let{
                viewModel.sendImage(it, channelId)
            }
        }

        val videoGalleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri:Uri? ->
            uri?.let{
                viewModel.sendVideo(it, channelId)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LaunchedEffect(key1 = true) {
                viewModel.listenforMessages(channelId)
            }
            val messages = viewModel.message.collectAsState()
            ChatMessages(
                channelName = channelName,
                messages = messages.value,
                onSendMessage = { message ->
                    viewModel.sendMessage(channelId, message)
                },
                onImageClicked = {
                    chooseDialogue.value = true
                }
            )
        }
        if (chooseDialogue.value) {
            ContentSelectionDialogue(
                onDismissRequest = { chooseDialogue.value = false },
                onCameraSelected = {
                    chooseDialogue.value = false
                    if(navController.context.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                       cameraImageLauncher.launch(createImageURI())
                   }else{
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContentSelectionDialogue(
onDismissRequest: () -> Unit,
onCameraSelected: () -> Unit,
onGallerySelected: () -> Unit,
onVideoGallerySelected: () -> Unit
){
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {Text(text = "Select attachment source")},
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessages(
    channelName: String,
    messages: List<Message>,
    onSendMessage: (String) -> Unit,
    onImageClicked:()->Unit
){
    val hideKeyboardController = LocalSoftwareKeyboardController.current
    val msg = remember{
        mutableStateOf("")
    }
    Column(modifier = Modifier.fillMaxSize()){
        LazyColumn(
            modifier = Modifier.weight(1f)
        ){
            item{
                ChannelItem(channelname = channelName, modifier = Modifier) { }
            }
            items(messages){message ->
                ChatBubble(message = message)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()
            .background(color= DarkGrey)
            .padding(8.dp),
            verticalAlignment = Alignment.Bottom)
        {
            IconButton(onClick = {
                msg.value = ""
                onImageClicked()
            })
            {
                Image(painter = painterResource(R.drawable.attach_document), contentDescription = "Attach")
            }
            TextField(value = msg.value, onValueChange = {msg.value=it},
                modifier = Modifier.weight(1f),
                placeholder = {Text(text = "Type")},
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        hideKeyboardController?.hide()
                    }
                ),
                colors = TextFieldDefaults.colors().copy(
                    focusedContainerColor = DarkGrey,
                    unfocusedContainerColor = DarkGrey,
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedPlaceholderColor = White,
                    unfocusedPlaceholderColor = White,
                )
            )
            IconButton(onClick = {onSendMessage(msg.value)
            msg.value = ""}) {
                Image(painter = painterResource(R.drawable.baseline_send_24), contentDescription = "Send")
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val isCurrentUser = message.senderId == Firebase.auth.currentUser?.uid
    val bubbleColor = if (isCurrentUser) {
        LightBlue
    } else {
        LightGrey
    }
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp))
    {
        val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
        Row(
            modifier = Modifier.padding(8.dp).align(alignment),
            verticalAlignment = Alignment.CenterVertically
        )
        {
            if (!isCurrentUser) {
                Image(
                    painter = painterResource(id = R.drawable.chat),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp).align(Alignment.CenterVertically),
                    alignment = alignment
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier.background(
                    color = bubbleColor,
                    shape = RoundedCornerShape(8.dp)
                ).padding(8.dp)
            ) {
                if (message.imageUrl != null) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(200.dp).align(Alignment.Center),
                        alignment = alignment,
                        contentScale = ContentScale.FillBounds
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Text(text = message.message?.trim() ?: "", color = White)
                }
            }
        }
    }
}
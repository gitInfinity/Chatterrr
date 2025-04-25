package com.example.chatterrr.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.chatterrr.feature.auth.signout.AuthViewModel
import com.example.chatterrr.ui.theme.DarkGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, loginRoute: String = "login") {
    val authViewModel: AuthViewModel = hiltViewModel()

    // Collect state
    val _signedOut = authViewModel.signedOut.collectAsState()
    val signedOut = _signedOut.value

    // Navigate to SignIn if signed out
    LaunchedEffect(signedOut) {
        if (signedOut) {
            navController.popBackStack(route = "home", inclusive = true)
            navController.navigate(loginRoute)
        }
    }
    val viewModel: HomeViewModel = hiltViewModel()
    val channels = viewModel.channels.collectAsState()
    val addChannel = remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val searchQuery = remember { mutableStateOf("") }
    val filteredChannels = channels.value.filter { channel ->
        channel.name.contains(searchQuery.value, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Blue)
                    .clickable { addChannel.value = true }
            ) {
                Text(
                    text = "Add Channel",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White
                )
            }
        },
        containerColor = Color.Cyan
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            LazyColumn {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Search Channels",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                        IconButton(
                            onClick = {
                                if (!signedOut) authViewModel.signOut()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out"
                            )
                        }
                    }
                }
                item {
                    TextField(
                        value = searchQuery.value,
                        onValueChange = { searchQuery.value = it },
                        placeholder = { Text(text = "Search") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(DarkGrey, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp)),
                        textStyle = TextStyle(color = Color.White),
                        colors = TextFieldDefaults.colors().copy(
                            focusedContainerColor = DarkGrey,
                            unfocusedContainerColor = DarkGrey,
                            focusedTextColor = Color.Gray,
                            unfocusedTextColor = Color.Gray,
                            focusedPlaceholderColor = Color.Gray,
                            unfocusedPlaceholderColor = Color.Gray,
                            cursorColor = Color.Gray
                        ),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null
                            )
                        }
                    )
                }
                items(filteredChannels) { channel ->
                    Column {
                        ChannelItem(
                            channelname = channel.name,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            // Navigate to the chat screen for this channel
                            viewModel.onChannelClicked(
                                channelId = channel.id,
                                channelName = channel.name,
                                navigateToChat = { id, name ->
                                    // This lambda is called by the ViewModel after it handles the join logic
                                    // It performs the actual navigation using the NavController from the Composable
                                    navController.navigate("chat/$id&$name")
                                }
                            )
                            // Note: If you were using per-channel counters,
                            // you would reset that specific counter when entering the chat screen.
                            // Since this is a global counter reset, doing it on the Home screen is appropriate.
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                }
            }
        }

        if (addChannel.value) {
            ModalBottomSheet(
                onDismissRequest = { addChannel.value = false },
                sheetState = sheetState
            ) {
                AddChannelDialog {
                    viewModel.addChannel(it)
                    addChannel.value = false
                }
            }
        }
    }
}



@Composable
fun ChannelItem(
    channelname: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkGrey)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.Blue.copy(alpha = 0.3f))
        ) {
            Text(
                text = channelname[0].uppercase(),
                color = Color.White,
                style = TextStyle(fontSize = 35.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Text(
            text = channelname,
            modifier = Modifier.padding(8.dp),
            color = Color.White
        )
    }
}

@Composable
fun AddChannelDialog(onAddNewChannel: (String) -> Unit) {
    val channelName = remember { mutableStateOf("") }
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Add Channel")
        Spacer(modifier = Modifier.size(8.dp))
        TextField(
            value = channelName.value,
            onValueChange = { channelName.value = it },
            label = { Text(text = "Channel Name") },
            singleLine = true
        )
        Spacer(modifier = Modifier.size(8.dp))
        Button(
            onClick = { onAddNewChannel(channelName.value) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Add")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController(), loginRoute = "login")
}
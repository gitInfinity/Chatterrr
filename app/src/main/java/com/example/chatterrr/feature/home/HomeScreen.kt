package com.example.chatterrr.feature.home

import android.R.attr.onClick
import android.R.attr.text
import android.R.attr.textStyle
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.chatterrr.ui.theme.DarkGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController){
    val viewModel: HomeViewModel = hiltViewModel<HomeViewModel>()
    val channels = viewModel.channels.collectAsState()
    val addChannel = remember{
        mutableStateOf(false)
    }
    val sheetState = rememberModalBottomSheetState()
    val searchQuery = remember{mutableStateOf("")}
    val filteredChannels = channels.value.filter {channel->
        channel.name.contains(searchQuery.value, ignoreCase = true)
    }
    Scaffold(
        floatingActionButton = {
            Box(
                modifier = Modifier.padding(16.dp).
                clip(RoundedCornerShape(16.dp)).
                background(Color.Blue).
                clickable {
                    addChannel.value = true
                }
            ){
                Text(
                    text = "Add Channel",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White
                )
            }
        }
    , containerColor = Color.Cyan)
    {
        Box(
            modifier = Modifier.padding(it).
            fillMaxSize()
        ) {
            LazyColumn {
                item{
                    Text(text = "Messages", color = Color.Blue, style = TextStyle(fontSize = 20.sp,
                        fontWeight = FontWeight.Black), modifier = Modifier.padding(16.dp))
                }
                item{
                    TextField(value = searchQuery.value, onValueChange = {searchQuery.value = it}, placeholder = {Text(text = "Search")},
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).
                        background(DarkGrey, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)),
                        textStyle = TextStyle(color = Color.White),
                        colors = TextFieldDefaults.colors().copy(focusedContainerColor = DarkGrey, unfocusedContainerColor = DarkGrey,
                            focusedTextColor = Color.Gray, unfocusedTextColor = Color.Gray, focusedPlaceholderColor = Color.Gray,
                            unfocusedPlaceholderColor = Color.Gray, cursorColor = Color.Gray),
                        trailingIcon = {Icon(imageVector = Icons.Filled.Search, contentDescription = null)}
                    )
                }
               items(filteredChannels){channel ->
                   Column {
                       ChannelItem(channel.name,
                           modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                       ){
                           navController.navigate("chat/${channel.id}&${channel.name}")
                       }
                       Spacer(modifier = Modifier.padding(8.dp))
                   }
               }
            }
        }
    }
    if(addChannel.value){
        ModalBottomSheet(onDismissRequest = {addChannel.value = false},
        sheetState = sheetState ){
            AddChannelDialog {
                viewModel.addChannel(it)
                addChannel.value = false
            }
        }
    }
}
@Composable
fun ChannelItem(channelname: String, modifier: Modifier, onClick: () -> Unit){
    Row(modifier = Modifier.fillMaxWidth().
        clip(RoundedCornerShape(16.dp)).background(DarkGrey).
        clickable{onClick()},
        verticalAlignment = Alignment.CenterVertically
    ){
        Box(modifier = Modifier
            .padding(8.dp)
            .size(70.dp)
            .clip(CircleShape).
        background(Color.Blue.copy(alpha = 0.3f))
        )
        {
            Text(text = channelname[0].uppercase(),
                color = Color.White,
                style = TextStyle(fontSize = 35.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Text(text = channelname, modifier = Modifier.padding(8.dp),
            color = Color.White)
    }
}

@Composable
fun AddChannelDialog(onAddNewChannel: (String) -> Unit){
    val channelName = remember{
        mutableStateOf("")
    }
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally)
    {
        Text(text = "Add Channel")
        Spacer(modifier = Modifier.padding(8.dp))
        TextField(value = channelName.value, onValueChange = {
            channelName.value = it
        },
        label = {Text(text = "Channel Name")},
        singleLine = true
        )
        Spacer(modifier = Modifier.padding(8.dp))
        Button(onClick = {onAddNewChannel(channelName.value)},
            modifier = Modifier.fillMaxWidth())
        {
            Text(text = "Add")
        }
    }
}
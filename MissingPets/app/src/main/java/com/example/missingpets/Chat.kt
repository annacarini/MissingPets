package com.example.missingpets


import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.missingpets.ui.theme.green2
import com.example.missingpets.ui.theme.superDarkGreen
import com.example.missingpets.ui.theme.superLightGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun createChatID(uid1: String, uid2: String): Int {
    val sortedUids = listOf(uid1, uid2).sorted()
    val combinedString = "${sortedUids[0]}${sortedUids[1]}"
    return combinedString.hashCode()
}

fun formatDateTime(timestamp: Date): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(timestamp)
}

//--------------------------------------------------------------------------------------------------

@Composable
fun ChatMessageItem(message: ChatMessage, sending: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (sending) Arrangement.End else Arrangement.Start
    ) {
        Column(
            //Different colour between chat bubbles (sender VS receiver)
            if (sending){
                Modifier
                    .background(green2, shape = RoundedCornerShape(16.dp))
                    .padding(12.dp)
                    .clip(MaterialTheme.shapes.medium)}
            else {
                Modifier
                    .background(superLightGreen, shape = RoundedCornerShape(16.dp))
                    .padding(12.dp)
                    .clip(MaterialTheme.shapes.medium)}
        ) {
            MyText(text = message.message)

            // Display timestamp
            Text(
                text = formatTime(message.timestamp),
                color = superDarkGreen,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

//--------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTextInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    onSendClick()
                }
            ),
            placeholder = {
                MyText(text = "Type a message...")
            }
        )

        IconButton(
            onClick = {
                onSendClick()
            },
            modifier = Modifier
                .background(superDarkGreen, shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = Color.White
            )
        }
    }
}
//--------------------------------------------------------------------------------------------------

fun refreshChat(chatMessages: MutableState<List<ChatMessage>>, senderId: String, chatNameId: String, effectiveChatId:Int, refresh:MutableState<Boolean>) {
    CoroutineScope(Dispatchers.IO).launch {
        runBlocking {
            chatMessages.value = ChatHandler.getMessageList(senderId, chatNameId, effectiveChatId)
            Log.d("Server response", "getMessageList() executed")
            Log.d("Messages downloaded", chatMessages.toString())
        }
        refresh.value = false
        Log.d("DONE", "Messages fetched from server")
    }
}

//--------------------------------------------------------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(senderId: String, senderUsername:String, chatId: Int, chatNameId: String, chatName: String, navController: NavController) {

    val context = LocalContext.current
    var newMessage by remember { mutableStateOf("") }
    var chatMessages = remember { mutableStateOf(listOf<ChatMessage>()) }

    var sendMessage = remember { mutableStateOf(false) }
    var refresh = remember { mutableStateOf(true) }
    val effectiveChatId =
        if (chatId == 0) {
            createChatID(senderId, chatNameId)
        } else {
            chatId
        }

    // Coroutine to reload the messages of the chat
    DisposableEffect(refresh.value) {
        CoroutineScope(Dispatchers.IO).launch {
            runBlocking {
                delay(50)
                chatMessages.value = ChatHandler.getMessageList(senderId, chatNameId, effectiveChatId)
            }
            refresh.value = false
        }
        // Ensure the coroutine is cancelled when leaving the screen
        onDispose {
            chatMessages.value = listOf()
        }
    }


    // Periodic check for new messages
    DisposableEffect(Unit) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                // 10 seconds delay
                delay(10000)

                // Check if there are new messages to load
                runBlocking {
                    val receivedChat = ChatHandler.getChatById(effectiveChatId)
                    Log.d("ChatScreen", "Periodic chat messages check...")
                    Log.d("ChatScreen", "$effectiveChatId")
                    Log.d("ChatScreen", "$receivedChat")

                    // Returns true if the current user is the last receiver and the chat is unread
                    receivedChat?.let{ refresh.value = (senderId == receivedChat.lastReceiverId && receivedChat.unread) }
                }
            }
        }

        // Ensure the coroutine is cancelled when leaving the screen
        onDispose {
            Log.d("DISPOSE CHAT", "EXIT")
            job.cancel()
        }
    }



    LaunchedEffect(sendMessage.value) {
        if (newMessage.isNotBlank()) {

            val timestamp = formatDateTime(Date(System.currentTimeMillis()))
            val chatMessage = ChatMessage(
                id = 0, // set by server
                senderId = senderId,
                senderUsername = senderUsername,
                receiverId = chatNameId,
                receiverUsername = chatName,
                message = newMessage,
                timestamp = timestamp
            )

            val chat = Chat(
                id = effectiveChatId,
                lastSenderId = senderId,
                lastSenderUsername = senderUsername,
                lastReceiverId = chatNameId,
                lastReceiverUsername = chatName,
                lastMessage = newMessage,
                timestamp = timestamp,
                unread = true
            )

            // Add message on server
            runBlocking {

                try {
                    val res = ChatHandler.createMessage(chatMessage)
                    Log.d("Server response", res.toString())
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "CREATE MESSAGE ERROR, ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("ChatScreen", "Error sending message: ${e.message}")
                }

                // Chat creation or update
                try {
                    val res = ChatHandler.createOrUpdateChat(chat)
                    Log.d("Server response", res.toString())
                } catch (e: Exception) {
                    Log.e("ChatScreen", "Error creating or updating chat: ${e.message}")
                }
            }

            refresh.value = true
            refreshChat(chatMessages, senderId, chatNameId, effectiveChatId, refresh)
            sendMessage.value = false
            newMessage = ""
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = {
                MyTitle(text = chatName)
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = green2),
            navigationIcon = {
                // Add back icon with navigation action
                IconButton(
                    onClick = { navController.popBackStack() },
                    content = {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = superDarkGreen
                        )
                    }
                )
            }
        )

        if (refresh.value) {
            val context = LocalContext.current
            val configuration: Configuration = context.getResources().getConfiguration()
            var screenWidthDp = configuration.screenWidthDp
            var screenHeightDp = configuration.screenHeightDp
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoadingScreen(heightOfSection = screenHeightDp.dp, screenHeightDp = screenHeightDp, text = "")
            }
        }
        else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Chat Messages
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        items(chatMessages.value) { message ->
                            if (senderId == message.senderId) ChatMessageItem(
                                message = message,
                                sending = true
                            )
                            else ChatMessageItem(message = message, sending = false)
                        }
                    }
                }

                // Text Input Bar
                ChatTextInputBar(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    onSendClick = {
                        sendMessage.value = true
                    }
                )
            }
        }
    }
}



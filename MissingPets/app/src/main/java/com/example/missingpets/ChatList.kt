package com.example.missingpets


import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.missingpets.ui.theme.green2
import com.example.missingpets.ui.theme.superDarkGreen
import com.example.missingpets.ui.theme.superLightGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random


fun formatTime(dateTime: String): String {

    val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
    val outputFormat = SimpleDateFormat("HH:mm a", Locale.ENGLISH)

    return try {
        val date = inputFormat.parse(dateTime)
        date?.let {
            outputFormat.timeZone = TimeZone.getTimeZone("GMT")
            outputFormat.format(date)
        } ?: ""
    } catch (e: Exception) {
        e.printStackTrace()
        "Error parsing date"
    }
}

//--------------------------------------------------------------------------------------------------

@Composable
fun MessageOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.8f))
    ) {
        Text(
            text = message,
            color = superDarkGreen,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.Center)
        )
    }
}

//--------------------------------------------------------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(userId: String, username:String, navController: NavController) {
    // Select only the chats related to the user
    var userChatList by remember { mutableStateOf<List<Chat>>(emptyList()) }
    // For chats loading screen
    var loading = remember { mutableStateOf(true) }

    val context = LocalContext.current
    val configuration: Configuration = context.getResources().getConfiguration()
    var screenWidthDp = configuration.screenWidthDp
    var screenHeightDp = configuration.screenHeightDp
    val heightOfSection = (screenHeightDp * 0.8f).dp

    // Select only the chats related to the user
    var sizeUserChatList by remember { mutableStateOf(0) }

    // Get all chats from the server
    LaunchedEffect(loading.value) {
        withContext(Dispatchers.IO) {
            CoroutineScope(Dispatchers.IO).launch {
                runBlocking {
                    userChatList = ChatHandler.getChatList(userId)
                    sizeUserChatList = userChatList.size
                    Log.d("Server response", "getChatList() executed")
                }
                loading.value = false
                Log.d("DONE", "Chats fetched from server")
            }
        }
    }

    DisposableEffect(Unit) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                // 10 seconds delay
                delay(10000)

                // Check if there are new chats to load
                runBlocking {
                    val newUserChatList = ChatHandler.getChatList(userId)
                    Log.d("ChatsScreen", "Periodic chats check...")
                    if (newUserChatList.size > sizeUserChatList) {
                        loading.value = true
                    }
                }
            }
        }

        // Ensure the coroutine is cancelled when leaving the screen
        onDispose {
            Log.d("DISPOSE CHATS", "EXIT")
            job.cancel()
        }
    }


    // Loading screen display
    if (loading.value) {
        Column(
            Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ){
        LoadingScreen(heightOfSection = heightOfSection, screenHeightDp = screenHeightDp, text = "Loading chats...")
    } }
    else {
        if (userChatList.isEmpty()) { MessageOverlay(message = "No chats yet") }
    }

    Column {
        TopAppBar(
            title = {
                MyTitle(text = "Chats")
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = green2),
            navigationIcon = {
                // Add back icon with navigation action
                IconButton(
                    onClick = { navController.navigate(Routes.HOME) },
                    content = {
                        Icon(imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = superDarkGreen)
                    }
                )
            }
        )

        ChatList(userId = userId, chats = userChatList) { chat ->

            val chatNameId = if (userId != chat.lastSenderId) { chat.lastSenderId }
            else { chat.lastReceiverId }
            val chatName = if (userId != chat.lastSenderId) { chat.lastSenderUsername }
            else { chat.lastReceiverUsername }

            // Redirect to a specific chat on click
            navController.navigate(Routes.CHAT + "/${chat.id}" + "/$chatNameId" + "/$chatName")
        }
    }

}

//--------------------------------------------------------------------------------------------------

@Composable
fun ChatList(userId:String, chats: List<Chat>, onItemClick: (Chat) -> Unit) {
    LazyColumn {
        itemsIndexed(chats) { index, chat ->
            if (index > 0) {
                // Add a separator line if not the first item
                Divider(
                    color = superLightGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                )
            }
            ChatItem(userId = userId, chat = chat, onItemClick = onItemClick)
        }
    }
}

val userColors = mutableMapOf<String, Color>()

//--------------------------------------------------------------------------------------------------

@Composable
fun ChatItem(userId:String, chat: Chat, onItemClick: (Chat) -> Unit) {

    val chatName = if (userId != chat.lastSenderId) { chat.lastSenderUsername }
    else { chat.lastReceiverUsername }

    val userColors = mutableMapOf<String, Color>()
    val randomColor = userColors.getOrPut(chatName) {
        Color(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(chat) }
            .padding(16.dp)
    ) {
        Image(
            painter = ColorPainter(randomColor), // Image resource
            contentDescription = "Profile Image",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
        )

        Spacer(modifier = Modifier.width(16.dp))
        // Left side (username and last message)
        //define the maximum number of chars that i want to show in the preview of the msg
        val maxLength = 20
        val messageToShow = if (chat.lastMessage.length > maxLength) {
            chat.lastMessage.take(maxLength) + "..."
        } else {
            chat.lastMessage
        }
        Column {
            Text(text = chatName, fontWeight = FontWeight.Bold, color = superDarkGreen)
            if (chat.unread == false || chat.lastSenderUsername != chatName){
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = messageToShow, color = superDarkGreen)
            }
            else{
                Text(text = messageToShow, fontWeight = FontWeight.Bold, color = superDarkGreen)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Right side (possible unread messages and timestamp)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .align(Alignment.CenterVertically),
            horizontalAlignment = Alignment.End
        ) {
            if (userId == chat.lastSenderId || !chat.unread) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MyText(
                        text = formatTime(chat.timestamp)
                    )
                }
            } else {
                // Add unread message icon and highlight icon and time
                androidx.compose.material.Icon(
                    imageVector =Icons.Default.Circle ,
                    contentDescription = "unreadMsgs",
                    tint = superDarkGreen,
                    modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MyText(
                        text = formatTime(chat.timestamp)
                    )
                }
            }
        }
    }
}

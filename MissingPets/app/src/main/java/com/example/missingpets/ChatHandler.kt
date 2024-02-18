package com.example.missingpets

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

data class ChatMessage(
    val id: Int,
    val senderId: String,
    val senderUsername: String,
    val receiverId: String,
    val receiverUsername: String,
    val message: String,
    val timestamp: String
)

// List of all chats, unique entry for a specific pair of users
data class Chat(
    val id: Int,
    var lastSenderId: String,
    var lastSenderUsername: String,
    var lastReceiverId: String,
    var lastReceiverUsername: String,
    var lastMessage: String,
    var timestamp: String,
    var unread: Boolean
)


// Singleton object
object ChatHandler : ViewModel() {

    private lateinit var messageList:ArrayList<ChatMessage>
    private lateinit var chatList:ArrayList<Chat>
    private var chat:Chat? = null
    private var notify = false
    private var retrofit = ServerAPI.HelperClass.getInstance()


    //----------------------------------------------------------------------------------------------

    suspend fun getMessageList(userId: String, chatNameId: String, chatId: Int): ArrayList<ChatMessage> {

        Log.d("getMessageList", "Requesting messages from server")

        // Initialize the list
        messageList = ArrayList<ChatMessage>()

        try {
            val json = retrofit.messagesGet(userId, chatNameId, chatId)

            // Iteration on all messages
            for (obj in json) {
                val message = obj.asJsonArray
                messageList.add(ChatMessage(message[0].asInt, message[1].asString, message[2].asString, message[3].asString, message[4].asString, message[5].asString, message[6].asString))
            }
        } catch (e: Exception) {
            // Handle exception
            Log.d("getMessageList", "SERVER ERROR")
            e.printStackTrace()
        }
        return messageList
    }

    //----------------------------------------------------------------------------------------------

    suspend fun getChatById(chatId: Int): Chat? {

        Log.d("getChatById", "Requesting chat from server")

        try {
            val ret = retrofit.chatGet(chatId).asJsonArray
            if (ret.size() > 0) {
                val elem = ret[0].asJsonArray
                chat = Chat(
                    elem[0].asInt,
                    elem[1].asString,
                    elem[2].asString,
                    elem[3].asString,
                    elem[4].asString,
                    elem[5].asString,
                    elem[6].asString,
                    elem[7].asInt != 0
                )
            }
            else {
                chat = null
            }
        } catch (e: Exception) {
            // Handle exception
            Log.d("getChatById", "SERVER ERROR")
            e.printStackTrace()
        }
        return chat
    }

    //----------------------------------------------------------------------------------------------

    suspend fun getChatList(userId: String): ArrayList<Chat> {

        Log.d("getChatList", "Requesting chats from server")

        // Initialize the list
        chatList = ArrayList<Chat>()

        try {
            val json = retrofit.chatsGet(userId)

            // Iteration on all chats
            for (obj in json) {
                val chat = obj.asJsonArray
                chatList.add(Chat(chat[0].asInt, chat[1].asString, chat[2].asString, chat[3].asString, chat[4].asString, chat[5].asString, chat[6].asString, chat[7].asInt != 0))
            }
        } catch (e: Exception) {
            // Handle exception
            Log.d("getChatList", "SERVER ERROR")
            e.printStackTrace()
        }
        return chatList
    }

    //----------------------------------------------------------------------------------------------

    suspend fun createOrUpdateChat(newChat: Chat): Int {
        var ret = -1

        // Create a chat to send
        val chatToSend =
            Gson().toJson(newChat).toRequestBody("application/json".toMediaTypeOrNull())

        try {
            // Send PUT request
            val serverAnswer = retrofit.chatsPut(chatToSend)
            Log.d("createOrUpdateChat answer", serverAnswer)
            ret = serverAnswer.toInt()
        } catch (e: Exception) {
            // Handle exception
            Log.e("createOrUpdateChat", "SERVER PUT ERROR: ${e.message}")
            e.printStackTrace()
        }
        return ret
    }

    //----------------------------------------------------------------------------------------------

    suspend fun createMessage(newMessage: ChatMessage): Int {
        var ret = -1

        // Create a message to send
        val messageToSend =
            Gson().toJson(newMessage).toRequestBody("application/json".toMediaTypeOrNull())

        try {
            // Send POST request
            val serverAnswer = retrofit.messagesPost(messageToSend)
            Log.d("createMessage answer", serverAnswer)
            ret = serverAnswer.toInt()
        } catch (e: Exception) {
            // Handle exception
            Log.e("createMessage", "Exception during server request", e)
        }

        return ret
    }

    //----------------------------------------------------------------------------------------------

    suspend fun getNotifyFlag(userId: String): Boolean {

        Log.d("getNotifyFlag", "Requesting notify flag from server")

        notify = false

        try {
            val serverRet = retrofit.notifyNewMessages(userId)
            notify = (serverRet != "0")
        } catch (e: Exception) {
            // Handle exception
            Log.d("getNotifyFlag", "SERVER ERROR")
            e.printStackTrace()
        }
        return notify
    }

}

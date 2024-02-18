package com.example.missingpets

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID


data class Post(
    var post_id: Int,
    var user_id: String,
    var username: String,
    var pet_name: String,
    var pet_type: String,
    var date: String,
    var position: String,
    var address: String,
    var description: String,
)

// Lista di post
object PostsHandler : ViewModel() {

    private lateinit var postsList: ArrayList<Post>         // Posts list to show in HomeScreen
    private var matchingPostsList = ArrayList<Post>()       // Posts list obtained as result from a matching

    private var retrofit = ServerAPI.HelperClass.getInstance()

    // Save the date of the last time you requested the posts from the server.
    // So if too much time has passed, request them again
    private lateinit var lastServerRequestDate : LocalDateTime

    // How often to update the list of posts
    private val minutesBetweenUpdates = 5



    suspend fun getPostsList(): ArrayList<Post> {

        if (this::postsList.isInitialized && this::lastServerRequestDate.isInitialized) {

            // Check how long ago you got the posts list from the server
            val minutes = lastServerRequestDate.until( LocalDateTime.now(), ChronoUnit.MINUTES )

            // If more than minutesBetweenUpdates minutes have passed, get the posts again
            if (minutes >= minutesBetweenUpdates) {
                return getPostsListFromServer()
            }
            // Otherwise return the posts list you already had
            else {
                return postsList
            }
        }
        else {
            return getPostsListFromServer()
        }
    }

    //----------------------------------------------------------------------------------------------

    suspend fun getPostsListFromServer(): ArrayList<Post> {

        Log.d("POST", "requesting data from server")

        // Save the date
        lastServerRequestDate = LocalDateTime.now()

        postsList = ArrayList<Post>()

        try {
            val json = retrofit.postsGet()

            // itero su tutti i post
            for (obj in json) {
                val post = obj.asJsonArray
                postsList.add(Post(post[0].asInt, post[1].asString, post[2].asString, post[3].asString, post[4].asString, post[5].asString, post[6].asString, post[7].asString, post[8].asString))
            }
        } catch (e: Exception) {
            // handle exception
            Log.d("ERRORE SERVER", ":(")
            e.printStackTrace()
        }
        return postsList
    }

    //----------------------------------------------------------------------------------------------

    suspend fun createPost(user_id:String, username:String, petName:String, pet_type:String, date:String, position:String, description:String, photoPath:String): String {
        var res = "ok"

        // Prepare post to send (post_id and address have random values because they are set correctly by the server)
        val newPost = Post(0, user_id, username, petName, pet_type, date, position, "", description)
        val postToSend = RequestBody.create("application/json".toMediaTypeOrNull(), Gson().toJson(newPost))

        // Prepare photo to send
        val file = File(photoPath)
        val requestFile = RequestBody.create(MultipartBody.FORM, file)
        val photoToSend = MultipartBody.Part.createFormData("photo", file.name, requestFile)

        try {
            // send POST request
            val serverAnswer = retrofit.postsPost(postToSend, photoToSend)
            Log.d("RISPOSTA", serverAnswer)
        } catch (e: Exception) {
            // handle exception
            Log.d("ERRORE INVIO SERVER", ":(")
            e.printStackTrace()
            res = e.message!!
        }
        return res
    }

    //----------------------------------------------------------------------------------------------

    fun getLastMatchingResult(): ArrayList<Post> {
        return matchingPostsList
    }

    //----------------------------------------------------------------------------------------------

    suspend fun getBestMatchingPosts(user_id:Int, date:String, position:String, photoBitmap: Bitmap, context:Context): Int {

        Log.d("SCAN", "requesting matching posts from server")

        matchingPostsList = ArrayList<Post>()

        // Prepare fields
        val user_idToSend = RequestBody.create("text/plain".toMediaTypeOrNull(), user_id.toString())
        val dateToSend = RequestBody.create("text/plain".toMediaTypeOrNull(), date.toString())
        val positionToSend = RequestBody.create("text/plain".toMediaTypeOrNull(), position.toString())

        // Prepare photo
        val file = convertBitmapToFile(photoBitmap, context)
        val requestFile = RequestBody.create(MultipartBody.FORM, file)
        val photoToSend = MultipartBody.Part.createFormData("photo", file.name, requestFile)

        try {
            val json = retrofit.matchPost(user_idToSend, dateToSend, positionToSend, photoToSend)

            // iterate on all posts
            for (obj in json) {
                val post = obj.asJsonArray
                matchingPostsList.add(Post(post[0].asInt, post[1].asString, post[2].asString, post[3].asString, post[4].asString, post[5].asString, post[6].asString, post[7].asString, post[8].asString))
            }
        } catch (e: Exception) {
            // handle exception
            Log.d("ERRORE SCAN SERVER", ":(")
            e.printStackTrace()
            return -1
        }
        return 0
    }

    //----------------------------------------------------------------------------------------------

    suspend fun getUserPostList(user_id: String): ArrayList<Post> {
        var userPostsList = ArrayList<Post>()
        try {
            val json = retrofit.userpostsGet(user_id)

            // iterate on all posts
            for (obj in json) {
                val post = obj.asJsonArray
                userPostsList.add(Post(post[0].asInt, post[1].asString, post[2].asString, post[3].asString, post[4].asString, post[5].asString, post[6].asString, post[7].asString, post[8].asString))
            }
        } catch (e: Exception) {
            // handle exception
            Log.d("ERRORE SERVER USER POSTS", ":(")
            e.printStackTrace()
        }
        return userPostsList
    }

    //----------------------------------------------------------------------------------------------

    suspend fun deletePost(post_id: Int): String {
        var res = ""
        try {
            res = retrofit.postDelete(post_id.toString())
        } catch (e: Exception) {
            // handle exception
            Log.d("ERRORE SERVER DELETE POST", ":(")
            e.printStackTrace()
            res = e.message.toString()
        }
        return res
    }

    //----------------------------------------------------------------------------------------------

    // Because ScanActivity obtains a Bitmap, but we need a File to send to the server
    private fun convertBitmapToFile(imageBitmap: Bitmap, context: Context): File {
        val wrapper = ContextWrapper(context)
        var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")
        val stream: OutputStream = FileOutputStream(file)
        imageBitmap.compress(Bitmap.CompressFormat.JPEG,25,stream)
        stream.flush()
        stream.close()
        return file
    }
}

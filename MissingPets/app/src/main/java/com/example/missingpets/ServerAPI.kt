package com.example.missingpets

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit


class ServerAPI {

    interface ServerAPI {
        // Get 20 most recent posts
        @GET("/posts")
        suspend fun postsGet(): JsonArray

        // Create a new post
        @Multipart
        @POST("/posts")
        suspend fun postsPost(@Part("data") data: RequestBody, @Part image: MultipartBody.Part): String

        // Find best matches
        @Multipart
        @POST("/match")
        suspend fun matchPost(@Part("user_id") user_id: RequestBody, @Part("date") date: RequestBody, @Part("position") position: RequestBody, @Part image: MultipartBody.Part): JsonArray

        // Get the posts made by a specific user
        @GET("/users/{user_id}/posts")
        suspend fun userpostsGet(@Path(value = "user_id", encoded = true) user_id: String): JsonArray

        // To delete a post
        @DELETE("post/{post_id}")
        suspend fun postDelete(@Path(value = "post_id", encoded = true) post_id: String): String


        @GET("/messages")
        suspend fun messagesGet(@Query("userId") userId: String, @Query("chatNameId") chatNameId: String, @Query("chatId") chatId: Int): JsonArray

        @POST("/messages")
        suspend fun messagesPost(@Body data: RequestBody): String

        @GET("/chat")
        suspend fun chatGet(@Query("chatId") chatId: Int): JsonArray

        @GET("/chats")
        suspend fun chatsGet(@Query("userId") userId: String): JsonArray

        @PUT("/chats")
        suspend fun chatsPut(@Body data: RequestBody): String

        @GET("/notify")
        suspend fun notifyNewMessages(@Query("userId") userId: String): String


    }

    object HelperClass {

        var gson = GsonBuilder()
            .setLenient()
            .create()

        fun getInstance(): ServerAPI {
            val okHttpClient = OkHttpClient.Builder()
                .readTimeout(300, TimeUnit.SECONDS)
                .connectTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .build()
            val retrofit =
                Retrofit.Builder().baseUrl("https://maccproject2024.pythonanywhere.com")
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build().create(ServerAPI::class.java)
            return retrofit
        }
    }
}
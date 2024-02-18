package com.example.missingpets

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.missingpets.ui.theme.green2
import com.example.missingpets.ui.theme.superDarkGreen
import com.example.missingpets.ui.theme.superLightGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

var profilePostsList: MutableList<Post> = ArrayList<Post>()


@SuppressLint("UnrememberedMutableState")
@Composable
fun ProfileScreen(user_id:String, username:String, navController: NavController) {

    val context = LocalContext.current
    val configuration: Configuration = context.getResources().getConfiguration()
    var screenWidthDp = configuration.screenWidthDp
    var screenHeightDp = configuration.screenHeightDp
    val heightOfSection = (screenHeightDp * 0.8f).dp

    // To refresh when we delete a post
    val refresh: MutableState<Boolean> = mutableStateOf(true)


    Column(
        Modifier
            .background(Color.White)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    )
    {
        Heading(navController = navController, username = username, context = context)
        MyTitle(text = "Your announcements")

        var loading = remember { mutableStateOf(true) }
        LaunchedEffect(refresh.value) {
            withContext(Dispatchers.IO) {
                CoroutineScope(Dispatchers.IO).launch {
                    runBlocking {
                        profilePostsList = PostsHandler.getUserPostList(user_id)
                    }
                    loading.value = false
                    refresh.value = false
                    Log.d("DONE!", "obtained posts from server")
                }
            }
        }
        ProfilePostsListOrLoading(loading, navController, refresh)
    }
}

//----------------------------------------------------------------------------------------------

private fun reloadUserPosts(user_id:String, loading: MutableState<Boolean>, refresh:MutableState<Boolean>) {
    CoroutineScope(Dispatchers.IO).launch {
        runBlocking {
            profilePostsList = PostsHandler.getUserPostList(user_id)
        }
        loading.value = false
        refresh.value = false
        Log.d("DONE!", "obtained posts from server")
    }
}

//----------------------------------------------------------------------------------------------

@SuppressLint("UnrememberedMutableState")
@Composable
fun ProfilePostsListOrLoading(loading: MutableState<Boolean>, navController: NavController, refresh:MutableState<Boolean>) {

    val context = LocalContext.current
    val configuration: Configuration = context.getResources().getConfiguration()
    var screenWidthDp = configuration.screenWidthDp
    var screenHeightDp = configuration.screenHeightDp
    val heightOfSection = (screenHeightDp * 0.8f).dp

    if (loading.value) {
        LoadingScreen(heightOfSection = heightOfSection, screenHeightDp = screenHeightDp, text = "Loading announcements... ")
        return
    }

    // Posts list

    Column(
        Modifier
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (i:Post in profilePostsList) {
            ProfilePostElement(post = i, loading, refresh)
        }
        LaunchedEffect(refresh.value) {}
    }
}

//----------------------------------------------------------------------------------------------

@Composable
fun ProfilePostElement(post: Post, loading: MutableState<Boolean>, refresh:MutableState<Boolean>) {

    val context = LocalContext.current
    val configuration: Configuration = context.getResources().getConfiguration()
    var screenWidthDp = configuration.screenWidthDp
    var screenHeightDp = configuration.screenHeightDp

    val photoURL = "https://maccproject2024.pythonanywhere.com/photo?post_id=" + post.post_id.toString()

    fun formatDate(inputDate: String): String {
        val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

        return try {
            val date = inputFormat.parse(inputDate)

            // Format the date in the desired output format
            val formattedDate = outputFormat.format(date)

            formattedDate
        } catch (e: Exception) {
            e.printStackTrace()
            "Error parsing date"
        }
    }

    OutlinedCard(
        colors = CardDefaults.cardColors(
            containerColor = superLightGreen
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(shape = RoundedCornerShape(8.dp)),
        border = BorderStroke(1.dp, superDarkGreen),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Dialog state Manager
            val dialogState: MutableState<Boolean> = remember {mutableStateOf(false)}

            val imageModifier = Modifier
                .width((screenWidthDp / 3.4).dp)
                .height((screenWidthDp / 2.5).dp)
                .border(BorderStroke(1.dp, superDarkGreen))
                .background(green2)
                .clip(shape = RoundedCornerShape(8.dp))
                .clickable(onClick = { dialogState.value = true })

            Image(
                painter = rememberAsyncImagePainter(photoURL),
                contentDescription = "pet picture :3",
                modifier = imageModifier,
                contentScale = ContentScale.Crop
            )

            if (dialogState.value) {
                AlertDialog(
                    containerColor = superLightGreen,
                    onDismissRequest = { dialogState.value = false },
                    text = {
                        Box(
                            modifier = Modifier
                                .width(300.dp)
                                .height(360.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .border(BorderStroke(2.dp, Color.White))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(photoURL),
                                contentDescription = "pet picture :3",
                                modifier = Modifier
                                    .width((screenWidthDp * 0.8).dp)
                                    .height((screenHeightDp / 0.6).dp)
                                    .background(green2)
                                    .clip(shape = RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                    },
                    confirmButton = { },
                    dismissButton = {
                        MyButton(
                            text = "Close",
                            onClick = {dialogState.value = false}
                        )

                    }

                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = post.pet_name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = superDarkGreen
                )
                IconText(icon = Icons.Default.Person, text = post.username)
                IconText(icon = Icons.Default.DateRange, text = formatDate(post.date))
                IconText(icon = Icons.Default.LocationOn, text = post.address)
                IconText(icon = Icons.Default.Pets, text = post.pet_type)
                Spacer(modifier = Modifier.height(10.dp))
                MyText(text = post.description)

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DeleteButton(post.post_id, post.user_id, loading, refresh)
                }
            }
        }
    }
}

//----------------------------------------------------------------------------------------------

@Composable
fun DeleteButton(post_id:Int, user_id:String, loading:MutableState<Boolean>, refresh:MutableState<Boolean>) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                MyText(text = "Delete post")
            },
            text = {
                MyText(text = "Are you sure you want to delete your announcement? " +
                        "Operation is irreversible")
            },
            confirmButton = {
                MyButton("Confirm",
                    onClick = {
                        runBlocking {
                            val res = PostsHandler.deletePost(post_id)
                            Log.d("RISPOSTA DELETE", res)
                            showDialog = false

                            // refresh the posts
                            loading.value = true
                            refresh.value = true
                            reloadUserPosts(user_id, loading, refresh)
                        }
                    }
                )
            },
            dismissButton = {
                MyButton(text = "Cancel",
                    onClick = { showDialog = false }
                )
            }
        )
    }
    MyIconButton(icon = Icons.Default.Delete, onClick = {showDialog = true
    })
}

package com.example.missingpets

import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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


var postsList : MutableList<Post> = ArrayList<Post>()

//--------------------------------------------------------------------------------------------------

@Composable
fun HomeScreen(userId:String, username: String, navController: NavController) {

    val context = LocalContext.current
    // To show unread messages notification
    var notify = remember { mutableStateOf(false) }

    Column(
        Modifier
            .background(Color.White)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {

        Heading(navController = navController, username = username, context = context)
        MyTitle(text = "Latest announcements")

        var loading = remember { mutableStateOf(true) }
        var refreshing = remember { mutableStateOf(false) }

        //Takes the posts list in a asyncrhrounous coroutine
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                CoroutineScope(Dispatchers.IO).launch {
                    runBlocking {
                        postsList = PostsHandler.getPostsList()
                    }
                    loading.value = false
                    Log.d("DONE!", "obtained posts from server")
                }
            }
        }

        // To know if there are any unread messages
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                CoroutineScope(Dispatchers.IO).launch {
                    runBlocking {
                        notify.value = ChatHandler.getNotifyFlag(userId)
                        Log.d("HomeScreen", "notification check done")
                    }
                }
            }
        }

        if (notify.value) {
            Toast.makeText(
                context,
                "You got new unread messages",
                Toast.LENGTH_LONG
            ).show()
            notify.value = false
        }

        PostsListOrLoading(loading, refreshing, navController)
    }
}

//--------------------------------------------------------------------------------------------------

fun loadPostsAgain(loading:MutableState<Boolean>) {
    loading.value = true
    CoroutineScope(Dispatchers.IO).launch {
        runBlocking {
            postsList = PostsHandler.getPostsListFromServer()
        }
        loading.value = false
        Log.d("DONE!", "obtained posts from server")
    }
}

//--------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PostsListOrLoading(loading:MutableState<Boolean>, refreshing:MutableState<Boolean>,navController: NavController) {

    val context = LocalContext.current
    val configuration: Configuration = context.getResources().getConfiguration()
    var screenWidthDp = configuration.screenWidthDp
    var screenHeightDp = configuration.screenHeightDp
    val heightOfSection = (screenHeightDp * 0.8f).dp


    if (loading.value) {
        LoadingScreen(heightOfSection = heightOfSection, screenHeightDp = screenHeightDp, text = "Loading announcements...")
        return
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing.value,
        onRefresh = {
            Log.d("REFRESH", "yee")
            loadPostsAgain(loading)
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn {
            items(postsList.size) { i, ->
                PostElement(postsList[i], navController)
            }
        }
        PullRefreshIndicator(
            refreshing = refreshing.value,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

//--------------------------------------------------------------------------------------------------

@Composable
fun PostElement(post: Post, navController: NavController) {

    val context = LocalContext.current
    val configuration: Configuration = context.getResources().getConfiguration()
    var screenWidthDp = configuration.screenWidthDp
    var screenHeightDp = configuration.screenHeightDp

    val photoURL =
        "https://maccproject2024.pythonanywhere.com/photo?post_id=" + post.post_id.toString()

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

            // Post image
            Image(
                painter = rememberAsyncImagePainter(photoURL),
                contentDescription = "pet picture :3",
                modifier = imageModifier,
                contentScale = ContentScale.Crop
            )

            // Code to Show and Dismiss Dialog
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
                Spacer(modifier = Modifier.height(10.dp))
                IconText(icon = Icons.Default.Person, text = post.username)
                IconText(icon = Icons.Default.DateRange, text = formatDate(post.date))
                IconText(icon = Icons.Default.LocationOn, text = post.address)
                IconText(icon = Icons.Default.Pets, text = post.pet_type)
                Spacer(modifier = Modifier.height(10.dp))
                MyText(text = post.description)

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    NavigationButton(post.position, post.address)
                    ChatButton(navController, post.user_id, post.username)
                }
            }
        }
    }
}

//--------------------------------------------------------------------------------------------------

@Composable
fun ChatButton(navController:NavController, post_user_id:String, post_username:String) {
    MyIconButton(icon = Icons.Default.ChatBubble, onClick = {
        navController.navigate(Routes.CHAT + "/0" + "/" + post_user_id + "/" + post_username)
    })
}

//--------------------------------------------------------------------------------------------------

@Composable
fun NavigationButton(position:String, address:String) {
    val context = LocalContext.current

    // Take the coordinates from the variable "position"
    val coords = position.split(",")
    val lat = coords[0].trim().toDouble()
    val lon = coords[1].trim().toDouble()

    val intent = Intent(context, NavigationActivity::class.java)
    intent.putExtra("latitude", lat)     // latitude of destination
    intent.putExtra("longitude", lon)    // longitude of destination
    intent.putExtra("address", address)  // address of destination

    MyIconButton(icon = Icons.Default.Navigation, onClick = {
        context.startActivity(intent)
    })

}



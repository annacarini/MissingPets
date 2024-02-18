package com.example.missingpets

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import com.example.missingpets.ui.theme.MissingPetsTheme
import com.example.missingpets.ui.theme.green2
import com.example.missingpets.ui.theme.superDarkGreen
import com.example.missingpets.ui.theme.superLightGreen
import java.text.SimpleDateFormat
import java.util.Locale

class MatchResultActivity: ComponentActivity() {

    private var postsList : MutableList<Post> = ArrayList<Post>()

    var screenWidthDp = 0
    var screenHeightDp = 0

    private var username = "0"
    private var user_id = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        val bundle = intent.extras
        username = bundle!!.getString("username").toString()
        user_id = bundle!!.getString("user_id").toString()

        val configuration: Configuration = this.getResources().getConfiguration()
        screenWidthDp = configuration.screenWidthDp
        screenHeightDp = configuration.screenHeightDp

        // get result of matching
        postsList = PostsHandler.getLastMatchingResult()

        setContent {
            MissingPetsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = Routes.MATCH
                    ) {
                        composable(Routes.MATCH) { MatchScreen(navController = navController) }
                        composable(
                            route = Routes.CHAT + "/{chatId}/{chatNameId}/{chatName}",
                            arguments = listOf(
                                navArgument("chatId") {
                                    type = NavType.IntType
                                },
                                navArgument("chatNameId") {
                                    type = NavType.StringType
                                },
                                navArgument("chatName") {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getInt("chatId")
                            val chatNameId = backStackEntry.arguments?.getString("chatNameId")
                            val chatName = backStackEntry.arguments?.getString("chatName")
                            if (chatId != null && chatNameId != null && chatName != null) ChatScreen(user_id, username, chatId, chatNameId, chatName, navController)
                            else Log.e("AppActivity", "null chatNameId or chatName")
                        }
                        composable(Routes.EXIT) { finish() }
                    }
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    @Composable
    fun MatchScreen(navController: NavHostController) {
        Column(
            Modifier
                .background(Color.White)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Spacer(Modifier.fillMaxHeight(0.01f))
            // Title
            MyTitle(
                text = "Best matches"
            )

            // Sub-title
            MyText(
                text = "Found " + postsList.count() + " matching posts."
            )

            // Posts list
            Column(
                Modifier
                    .heightIn(0.dp, (screenHeightDp * 0.72f).dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // add an element for each post
                for (i:Post in postsList) {
                    PostElement(i, navController)
                }
            }

            // Button to go back to home
            MyButton("Home",
                onClick = { finish() }
            )
        }
    }

    //----------------------------------------------------------------------------------------------

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
                val dialogState: MutableState<Boolean> = remember { mutableStateOf(false) }

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
}
package com.example.missingpets

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.missingpets.ui.theme.MissingPetsTheme
import com.example.missingpets.ui.theme.green2
import com.example.missingpets.ui.theme.superDarkGreen



const val PET_TYPE_DOG: String = "Dog"
const val PET_TYPE_CAT: String = "Cat"
const val PET_TYPE_OTHER: String = "Other"

class AppActivity : ComponentActivity() {

    private var user_id: String = ""
    private var username: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //It takes userId and username from the Intent
        val bundle = intent.extras
        user_id = bundle!!.getString("user_id")!!
        username = bundle!!.getString("username")!!

        setContent {
            MissingPetsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    //To show the bottom navigation bar
                    val navController = rememberNavController()
                    Scaffold(
                        bottomBar = { BottomNavigationBar(user_id, username, navController = navController, this@AppActivity) }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Routes.HOME,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Routes.HOME) { HomeScreen(user_id, username, navController = navController) }
                            composable(Routes.CHATS) { ChatsScreen(user_id, username, navController = navController) }
                            composable(Routes.PROFILE) { ProfileScreen(user_id, username, navController = navController) }
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
    }
}


//----------------------------------------SCREENS' HEADING------------------------------------------
@Composable
fun Heading(navController: NavController, username: String, context : Context) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(100.dp)
    ) {
        Logo()
        Column (
            horizontalAlignment = Alignment.End
        ){
            Spacer(Modifier.fillMaxHeight(0.05f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ){
                IconText(icon = Icons.Default.Person, text = "Hi, $username")
                Spacer(Modifier.fillMaxWidth(0.03f))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.graphicsLayer { translationY = -5.dp.toPx()}
            ){
                Icon(Icons.Default.ExitToApp, contentDescription = "Exit Icon", tint = superDarkGreen)
                //Handle logout logic
                TextButton(onClick = {
                    authViewModel!!.signOut()
                    if (username != null) {
                        Toast.makeText(
                            context,
                            "See you soon, ${username}!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    navController.navigate(Routes.EXIT) }) {
                    MyText(text = "Logout")
                }
            }
        }
    }
}



//-------------------------------------BOTTOM NAVBAR-----------------------------------------------

//Define the icons and the icons text for each option of the bottom navbar
enum class BottomNavItem(val route: String, val icon: ImageVector) {
    Home(Routes.HOME, Icons.Default.Home),
    Scan("Scan", Icons.Default.PhotoCamera),
    Add("Add", Icons.Default.Add),
    Chats(Routes.CHATS, Icons.Default.ChatBubble),
    Profile(Routes.PROFILE, Icons.Default.Person)
}

@Composable
fun BottomNavigationBar(userId:String, username: String, navController: NavController, activity: ComponentActivity) {
    BottomNavigation(
        backgroundColor = green2
    ) {
        val context = LocalContext.current
        BottomNavItem.values().forEach { item ->
            BottomNavigationItem(
                selected = navController.currentDestination?.route == item.route,
                onClick = {
                    when (item) {
                        BottomNavItem.Home -> {
                            navController.navigate(Routes.HOME)
                        }

                        BottomNavItem.Scan -> {
                            val intent = Intent(context, ScanActivity::class.java)
                            intent.putExtra("user_id", userId)
                            intent.putExtra("username", username)
                            context.startActivity(intent)
                        }

                        BottomNavItem.Add -> {
                            val intent = Intent(context, CreatePostActivity::class.java)
                            intent.putExtra("user_id", userId)
                            intent.putExtra("username", username)
                            context.startActivity(intent)
                        }

                        BottomNavItem.Chats -> {
                            navController.navigate(Routes.CHATS)
                        }

                        BottomNavItem.Profile -> {
                            navController.navigate(Routes.PROFILE)
                        }
                    }
                },
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = superDarkGreen
                    )
                },
                label = { Text(item.route, color = superDarkGreen, fontSize = 14.sp ) }
            )
        }
    }
}





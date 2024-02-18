package com.example.missingpets

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.missingpets.ui.theme.superDarkGreen



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(auth: AuthViewModel, navController: NavController) {

    val emailState = remember { mutableStateOf("") }
    val passwordState = remember { mutableStateOf("") }

    val context = LocalContext.current

    val backgroundImageResource = R.drawable.sfondo

    // Page content
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        //Background image
        Image(
            painter = painterResource(id = backgroundImageResource),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Logo()
            Spacer(modifier = Modifier.height(32.dp))


            OutlinedTextField(
                value = emailState.value,
                onValueChange = {
                    emailState.value = it
                },
                label = { MyText("Email") },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedTextColor = superDarkGreen,
                    cursorColor = superDarkGreen,
                    focusedBorderColor = superDarkGreen
            ))

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = passwordState.value,
                onValueChange = {
                    passwordState.value = it
                },
                label = { MyText("Password") },
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedTextColor = superDarkGreen,
                    cursorColor = superDarkGreen,
                    focusedBorderColor = superDarkGreen
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            MyBigButton(text="Login", onClick = {
                // Login logic
                auth.signIn(
                    email = emailState.value,
                    password = passwordState.value
                ){ success ->
                    if (!success) {
                        // Authentication failed
                        Toast.makeText(
                            context,
                            "E-mail or password not valid",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else {
                        // Authentication successful
                        Toast.makeText(
                            context,
                            "Welcome, ${auth.currentUser()?.displayName}",
                            Toast.LENGTH_LONG
                        ).show()
                        // Start principal activity
                        val userId = auth.currentUser()?.uid.toString()
                        val username = auth.currentUser()?.displayName.toString()
                        val intent = Intent(context, AppActivity::class.java )
                        intent.putExtra("user_id", userId)
                        intent.putExtra("username", username)
                        context.startActivity(intent)
                    }
                }
            })

            TextButton(onClick = { navController.navigate(Routes.REGISTER) }) {
                MyText(
                    "Don't have an account? Register"
                )
            }
        }
    }
}

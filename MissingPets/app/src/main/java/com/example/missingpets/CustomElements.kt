package com.example.missingpets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.missingpets.ui.theme.green2
import com.example.missingpets.ui.theme.superDarkGreen


@Composable
fun MyButton(text: String, onClick: () -> Unit) {

    ElevatedButton(
        onClick = { onClick() },
        modifier = Modifier
            .padding(16.dp),
        colors = ButtonDefaults.buttonColors(green2),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 8.dp
        )
    )
    {
        Text(
            text = text,
            color = superDarkGreen
        )
    }
}

//--------------------------------------------------------------------------------------------------


@Composable
fun MyIconButton(icon: ImageVector, onClick: () -> Unit){
    Button(
        onClick = { onClick() },
        modifier = Modifier
            .padding(5.dp),
        colors = ButtonDefaults.buttonColors(green2),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Icon",
            tint = superDarkGreen
        )
    }
}

//--------------------------------------------------------------------------------------------------

@Composable
fun MyBigButton(text: String, onClick: () -> Unit) {

    ElevatedButton(
        onClick = { onClick() },
        modifier = Modifier
            .padding(16.dp),
        colors = ButtonDefaults.buttonColors(green2),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 8.dp
        )
    )
    {
        Text(
            text = text,
            color = superDarkGreen,
            fontSize = 18.sp
        )
    }
}

//--------------------------------------------------------------------------------------------------

@Composable
fun Logo() {
    Image(
        painter = painterResource(id = R.drawable.logo_quadrato),
        contentDescription = "Logo",
        modifier = Modifier
            .size(100.dp)
            .padding(top = 20.dp)
    )
}

//--------------------------------------------------------------------------------------------------

@Composable
fun MyTitle(text: String) {
    Text(
        text = text,
        fontSize = 20.sp,
        color = superDarkGreen
    )
}

//--------------------------------------------------------------------------------------------------

@Composable
fun MyText(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        color = superDarkGreen
    )
}

//--------------------------------------------------------------------------------------------------

@Composable
fun MyPostText(title: String, text: String){
    Text(
        buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(title)
            }
            append(text)
        },
        color = superDarkGreen)
}

//--------------------------------------------------------------------------------------------------

@Composable
fun LoadingScreen(heightOfSection: Dp, screenHeightDp: Int, text : String) {
    Column(
        modifier = Modifier
            .height(heightOfSection)
            .graphicsLayer { translationY = -20.dp.toPx()},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .width(64.dp),
            color = superDarkGreen
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            fontSize = 20.sp,
            modifier = Modifier.padding(25.dp),
            color = superDarkGreen
        )
    }
}

//--------------------------------------------------------------------------------------------------

@Composable
fun IconText(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = "User Icon", tint = superDarkGreen)
        MyText(text = text)

    }
}









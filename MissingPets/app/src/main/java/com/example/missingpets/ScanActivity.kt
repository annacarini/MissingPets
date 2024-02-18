package com.example.missingpets

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.missingpets.ui.theme.MissingPetsTheme
import com.example.missingpets.ui.theme.green2
import com.example.missingpets.ui.theme.superDarkGreen
import com.example.missingpets.ui.theme.superLightGreen
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


class ScanActivity : ComponentActivity() {


    private var username = "0"
    private var user_id = "0"

    // Photo taken from camera
    private var photo: Bitmap? = null

    private var loadedPhoto = false
    private var loadedGPS = false

    private lateinit var loading: MutableState<Boolean>

    // For GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 123
        private const val pic_id = 123
        private const val show_result_activity_id = 45
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // For GPS

        if (!checkGPSPermission())
            requestGPSPermission()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 1000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult == null) {
                    return
                }
                for (location in locationResult.locations) {
                    if (location != null) {

                        // Update position
                        lastLocation = location
                        loadedGPS = true
                    }
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())


        val configuration: Configuration = this.getResources().getConfiguration()
        var screenHeightDp = configuration.screenHeightDp

        val bundle = intent.extras
        username = bundle!!.getString("username").toString()
        user_id = bundle!!.getString("user_id").toString()



        setContent {
            MissingPetsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    loading = remember { mutableStateOf(false) }

                    if (loading.value) {
                        LoadingScreen(heightOfSection = screenHeightDp.dp, screenHeightDp = screenHeightDp, text = "Finding best matches...")
                    }
                    else {
                        CameraPageContent(loading)
                    }
                }
            }
        }

    }

    //----------------------------------------------------------------------------------------------

    @Composable
    fun CameraPageContent(loading: MutableState<Boolean>) {

        val context = LocalContext.current
        val configuration: Configuration = context.getResources().getConfiguration()
        var screenWidthDp = configuration.screenWidthDp
        var screenHeightDp = configuration.screenHeightDp

        val navController = rememberNavController()


        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Titolo
            Heading(navController, username, context)
            MyTitle(text = "Find a match")

            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = superLightGreen
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(shape = RoundedCornerShape(8.dp)),
                    border = BorderStroke(1.dp, superDarkGreen)
                ){
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        Text(text = "WHAT TO DO", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(5.dp))
                        MyText(text = "Take a picture of the pet you found: it will be matched with our" +
                                "database records. Now contact the matching-pet owner and help him/her " +
                                "to have his/her pet back!")
                    }

                }

                Column (
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ){
                    MyText(text = "Take a picture!")
                    MyIconButton(icon = Icons.Default.PhotoCamera,onClick = {cameraButtonOnClick()})

                    // Dialog state Manager
                    val dialogState: MutableState<Boolean> = remember { mutableStateOf(false) }

                    val imageModifier = Modifier
                        .width((screenWidthDp * 0.50).dp)
                        .height((screenHeightDp * 0.28).dp)
                        .border(BorderStroke(1.dp, superDarkGreen))
                        .clickable(onClick = { dialogState.value = true })

                    if (loadedPhoto) {
                        val photoBitmap = photo!!.asImageBitmap()
                        Image(
                            painter = BitmapPainter(photoBitmap, IntOffset.Zero, IntSize(photoBitmap.width, photoBitmap.height)),
                            contentDescription = "pic",
                            modifier = imageModifier,
                            contentScale = ContentScale.FillWidth
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
                                            painter = BitmapPainter(photoBitmap, IntOffset.Zero, IntSize(photoBitmap.width, photoBitmap.height)),
                                            contentDescription = "pic",
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
                    }
                    else {
                        val blankphoto = BitmapFactory.decodeResource(context.resources, R.drawable.blank).asImageBitmap()
                        Image(
                            painter = BitmapPainter(blankphoto, IntOffset.Zero, IntSize(blankphoto.width, blankphoto.height)),
                            contentDescription = "pic",
                            modifier = Modifier
                                .width((screenWidthDp * 0.50).dp)
                                .height((screenHeightDp * 0.28).dp)
                                .border(BorderStroke(1.dp, superDarkGreen)),
                            contentScale = ContentScale.FillWidth
                        )
                    }

                    MyButton(text = "Confirm", onClick = {confirmButtonOnClick(loading)})
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    fun cameraButtonOnClick() {
        if (checkCameraPermission()) {
            openCamera()
        } else {
            requestCameraPermission()
        }
    }

    //----------------------------------------------------------------------------------------------

    fun confirmButtonOnClick(loading: MutableState<Boolean>) {

        if (!loadedPhoto) {
            Toast.makeText(
                this@ScanActivity,
                "Take a photo first!",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (!loadedGPS) {
            Toast.makeText(
                this@ScanActivity,
                "Waiting for GPS...",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        loading.value = true


        CoroutineScope(Dispatchers.IO).launch {
            runBlocking{
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val date = LocalDateTime.now().format(formatter)
                val position = getPositionAsString(lastLocation!!)
                val res = PostsHandler.getBestMatchingPosts(0, date, position, photo!!, this@ScanActivity)
                Log.d("RISPOSTA MATCH", res.toString())
            }

            // Show result of matching in the activity MatchResultActivity
            val intent = Intent(this@ScanActivity, MatchResultActivity::class.java)
            intent.putExtra("user_id", user_id)
            intent.putExtra("username", username)
            startActivityForResult(intent, show_result_activity_id)
        }
    }

    //----------------------------------------------------------------------------------------------

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
    }

    //----------------------------------------------------------------------------------------------

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, pic_id)
    }

    //----------------------------------------------------------------------------------------------

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // When you come back here after taking the photo
        if (requestCode == pic_id && resultCode == RESULT_OK && data != null) {
            photo = data.extras?.get("data") as Bitmap?

            if (photo != null) {
                loadedPhoto = true
                loading.value = true
                loading.value = false
            }
        }

        // When you come back here from MatchResultActivity
        if (requestCode == show_result_activity_id) {
            finish()    // end this activity
        }
    }

    //----------------------------------------------------------------------------------------------

    fun getPositionAsString(location:Location): String {
        var lat = "%.6f".format(Locale.ENGLISH, location.latitude)
        var lon = "%.6f".format(Locale.ENGLISH, location.longitude)
        val str = lat + "," + lon
        return str
    }

    //----------------------------------------------------------------------------------------------

    // Camera permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    // GPS permissions
    private fun checkGPSPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val result1 = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
    }
    private fun requestGPSPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), 1)
    }
}
package com.example.missingpets

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.missingpets.ui.theme.MissingPetsTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.lang.Math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/*
        // This activity must be created by passing latitude and longitude of the destination
        // as parameters in the intent, in this way:
        val intent = Intent(this, NavigationActivity::class.java)
        intent.putExtra("latitude", 41.9338758)     // latitude of the destination
        intent.putExtra("longitude", 12.4780725)    // longitude of the destination
        intent.putExtra("address", "via abc, Roma")
        startActivity(intent)
 */

class NavigationActivity : ComponentActivity(), SensorEventListener {

    // COMPASS
    private var sensorManager: SensorManager? = null
    private var magnetometer: Sensor? = null
    private var accelerometer: Sensor? = null
    private val lastAccelerometer = FloatArray(3)
    private val lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    // Phone orientation w.r.t North Pole
    private var angleOrientation = mutableStateOf(0f)


    // INCLINATION
    private var inclinometer: Sensor? = null
    private var screenPitch: Float = 1f

    // GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    // DESTINATION (fixed)
    private var goalLocation: Location = Location("")
    private var goalAddress: String = ""

    // CURRENT POSITION (updated with GPS)
    private var currentLocation: Location = Location("")

    // Angle of destination w.r.t North pole
    private var bearing = mutableStateOf(0.0)

    // Distance between current position and destination
    private var distance = mutableStateOf(0.0)

    // To show loading screen
    private var waitingForGps = true

    private val recomposeToggleState: MutableState<Boolean> = mutableStateOf(false)


    // Current values
    private var currentAngle = 0f
    private var currentHeightModifier = 1f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Take parameters of destination passed in the intent
        val bundle = intent.extras
        goalLocation.latitude = bundle!!.getDouble("latitude")
        goalLocation.longitude = bundle!!.getDouble("longitude")
        goalAddress = bundle!!.getString("address").toString()


        // For the compass:
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)


        // For the inclination:
        inclinometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        (this.getSystemService(Context.SENSOR_SERVICE) as SensorManager).also {
            it.registerListener(this,
                it.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL)
        }


        // For the GPS:

        if (!checkPermission()) {
            requestPermission()
        }

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
                        updateCurrentLocation(location)
                        waitingForGps = false
                        manualRecompose()
                    }
                }
            }
        }
        startLocationUpdates()

        setContent {
            MissingPetsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TitleGoal()

                        Spacer(Modifier.fillMaxHeight(0.05f))

                        val context = LocalContext.current
                        val configuration: Configuration = context.getResources().getConfiguration()
                        var screenWidthDp = configuration.screenWidthDp
                        var screenHeightDp = configuration.screenHeightDp
                        val heightOfSection = (screenHeightDp * 0.8f).dp

                        if (waitingForGps) {
                            LoadingScreen(heightOfSection = heightOfSection, screenHeightDp = screenHeightDp, text = "Waiting for GPS... ")
                        }
                        else {
                            DistanceText()

                            ArrowCanvas()

                            // "Back" button
                            MyButton(text = "Back",
                                onClick = { finish() }
                            )
                        }
                    }

                    // To force the recompose
                    LaunchedEffect(recomposeToggleState.value) {}
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    @Composable
    fun TitleGoal() {
        val configuration: Configuration = this.getResources().getConfiguration()
        var screenWidthDp = configuration.screenWidthDp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MyTitle(
                text = "Navigation"
            )
            Spacer(modifier = Modifier.fillMaxHeight(0.05f))
            Box(
                modifier = Modifier.width((screenWidthDp*0.8).dp)
            ) {
                MyPostText(title = "Goal: ", text = goalAddress)
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    @Composable
    fun DistanceText() {
        val dist = distance.value.toFloat()
        var distanceString = ""
        if (dist < 1000) {
            distanceString = round(dist).toInt().toString() + " m"   // print the meters without decimal digits
        }
        else {
            distanceString = "%.2f".format(dist/1000) + " km"   // print kilometers with 2 decimal digits
        }

        Column() {
            MyPostText(title = "Distance: ", text = distanceString)
        }
    }

    //----------------------------------------------------------------------------------------------

    @Composable
    fun ArrowCanvas() {
        val arrow = BitmapFactory.decodeStream(this.assets.open("arrow.png"))
        val arrowBitmap = arrow!!.asImageBitmap()

        val configuration: Configuration = this.getResources().getConfiguration()
        var screenWidthDp = configuration.screenWidthDp
        var screenHeightDp = configuration.screenHeightDp

        // INIZIAL SIZE OF ARROW IMAGE
        val imageHeight = screenWidthDp
        val imageWidth = imageHeight

        var heightModifierGoal = abs(screenPitch) / 90  // because the factor must go from 0 to 1, while pitch goes from 0 to 90
        if (heightModifierGoal <= 0.01) heightModifierGoal = 0.01f


        // ANIMATION
        var enabled by remember { mutableStateOf(false) }

        val transition = updateTransition(targetState = enabled, label = "")

        val angle by transition.animateFloat(
            transitionSpec = {tween(durationMillis = 500, easing = FastOutSlowInEasing)},
            label = ""
        ) {
            when(it) {
                true -> currentAngle
                false -> -angleOrientation.value + bearing.value.toFloat()
            }
        }

        val heightModifier by transition.animateFloat(
            transitionSpec = {tween(durationMillis = 500, easing = LinearEasing)},
            label = ""
        ) {
            when(it) {
                true -> currentHeightModifier
                false -> heightModifierGoal
            }
        }


        Box(    // Goal: make the arrow pic occupy always the same space inside the page
            modifier = Modifier
                .height(imageHeight.dp)
                .width(imageWidth.dp)
        ) {
            Box(  // Goal: compress the arrow vertically using the inclination
                modifier = Modifier
                    .graphicsLayer(
                        scaleY = heightModifier,
                    )
                    .align(Alignment.Center)
            ) {
                Image(
                    bitmap = arrowBitmap,
                    contentDescription = "arrow",
                    modifier = Modifier
                        .rotate(angle),
                    contentScale = ContentScale.FillBounds      // otherwise it's not compressed
                )
            }
        }
    }



    /**********************************      COMPASS      **********************************/


    // Callback for sensors
    override fun onSensorChanged(event: SensorEvent) {

        var shouldRecompose = false

        // Magnetometer
        if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
            lastMagnetometerSet = true
        }
        // Accelerometer
        else if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
            lastAccelerometerSet = true
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                lastAccelerometer,
                lastMagnetometer
            )
            SensorManager.getOrientation(rotationMatrix, orientation)

            // values[0] = azimuth, values[1] = pitch, values[2] = roll

            // AZIMUTH
            val azimuthInRadians = orientation[0]
            val azimuthInDegrees = (Math.toDegrees(azimuthInRadians.toDouble()) + 360).toFloat() % 360
            if (abs(azimuthInDegrees - angleOrientation.value) >= 2) {
                angleOrientation.value = azimuthInDegrees
                shouldRecompose = true
            }

            // PITCH
            val pitchInRadians = orientation[1]
            var pitchInDegrees = (Math.toDegrees(pitchInRadians.toDouble()) + 360).toFloat() % 360
            pitchInDegrees = (pitchInDegrees - 270) % 90
            if (abs(pitchInDegrees - screenPitch) >= 2) {
                screenPitch = pitchInDegrees
                shouldRecompose = true
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    // Register listener for the sensor events
    override fun onResume() {
        super.onResume()
        sensorManager!!.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    //----------------------------------------------------------------------------------------------

    override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(this, magnetometer)
        sensorManager!!.unregisterListener(this, accelerometer)
    }

    //----------------------------------------------------------------------------------------------

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}




    /**********************************      GPS      **********************************/


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }

    //----------------------------------------------------------------------------------------------

    // Callback for GPS
    private fun updateCurrentLocation(newLocation: Location) {

        currentLocation = newLocation

        distance.value = computeDistance(currentLocation, goalLocation)
        bearing.value = computeBearing(currentLocation, goalLocation)
    }

    //----------------------------------------------------------------------------------------------

    // Distance in meters (keeping in mind that the Earth's surface is spherical)
    fun computeDistance(startPoint: Location, endPoint:Location): Double {
        val R = 6371e3                          // radius of the Earth in meters

        val phi1 = Math.toRadians(startPoint.latitude)
        val phi2 = Math.toRadians(endPoint.latitude)

        val deltaphi = Math.toRadians(endPoint.latitude-startPoint.latitude)
        val deltalambda = Math.toRadians(endPoint.longitude-startPoint.longitude)

        val a = sin(deltaphi/2) * sin(deltaphi/2) + cos(phi1) * cos(phi2) * sin(deltalambda/2) * sin(deltalambda/2)
        val c = 2 * atan2(Math.sqrt(a), sqrt(1-a))

        val distance = R * c  // in metres
        return distance
    }

    //----------------------------------------------------------------------------------------------

    // Bearing = angle between the axis connecting startPoint and North Pole,
    // and axis connecting startPoint and endPoint
    fun computeBearing(startPoint: Location, endPoint:Location): Double {
        val phi1 = Math.toRadians(startPoint.latitude)
        val lambda1 = Math.toRadians(startPoint.longitude)

        val phi2 = Math.toRadians(endPoint.latitude)
        val lambda2 = Math.toRadians(endPoint.longitude)

        val y = sin(lambda2-lambda1) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1)* cos(phi2) * cos(lambda2-lambda1)

        val theta = atan2(y, x)
        val bearing = (Math.toDegrees(theta) + 360) % 360       // in degrees

        return bearing
    }

    //----------------------------------------------------------------------------------------------

    // To recompose
    fun manualRecompose() {
        recomposeToggleState.value = !recomposeToggleState.value
    }

    //----------------------------------------------------------------------------------------------

    // GPS permission
    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val result1 = ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
    }
    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), 1)
    }
}
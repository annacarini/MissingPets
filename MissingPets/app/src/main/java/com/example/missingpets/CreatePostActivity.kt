package com.example.missingpets

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.missingpets.ui.theme.MissingPetsTheme
import com.example.missingpets.ui.theme.lightGreen
import com.example.missingpets.ui.theme.superDarkGreen
import com.example.missingpets.ui.theme.superLightGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class CreatePostActivity : ComponentActivity() {

    //set inside onCreate
    private var user_id = "0"
    private var username = "0"

    private lateinit var photo: AppCompatImageView
    private var petName = ""
    private var photoURI: Uri? = null
    private var date = ""
    private var pet_type = PET_TYPE_DOG
    private var position = ""
    private var description = ""

    // parameters for correct values
    private val descriptionMaxLength = 255
    private val petNameMaxLength = 20

    // to handle the map
    private lateinit var mapSelectorDialog : MapSelectorDialog

    // to show the loading screen
    private lateinit var loading: MutableState<Boolean>

    // for the calendar
    private var calendar = Calendar.getInstance()
    private var current_year = calendar.get(Calendar.YEAR)
    private var current_month = calendar.get(Calendar.MONTH)
    private var current_day = calendar.get(Calendar.DAY_OF_MONTH)

    // error indexes
    val petNameErrorIndex = 0
    val photoErrorIndex = 1
    val dateErrorIndex = 2
    val positionErrorIndex = 3

    private lateinit var photoSelected: MutableState<String>

    //----------------------------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {


        Log.d("CAMBIO ACTIVITY", "Sto creando l'activity CreatePostsActivity")

        super.onCreate(savedInstanceState)

        mapSelectorDialog = MapSelectorDialog(this)

        // Use goalLocation and the address of the intent
        val bundle = intent.extras
        user_id = bundle!!.getString("user_id").toString()
        username = bundle!!.getString("username").toString()

        setContent {
            MissingPetsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PageContent()
                }
            }
        }
    }

//--------------------------------------------------------------------------------------------------

    @Composable
    fun PageContent() {

        // to show error messages when inserted values are not valid
        val petNameError: MutableState<Boolean> = remember { mutableStateOf(false) }
        val photoError: MutableState<Boolean> = remember { mutableStateOf(false) }
        val dateError: MutableState<Boolean> = remember { mutableStateOf(false) }
        val positionError: MutableState<Boolean> = remember { mutableStateOf(false) }
        val descriptionError: MutableState<Boolean> = remember { mutableStateOf(false) }
        val missingFieldErrors : Array<MutableState<Boolean>> = arrayOf(petNameError, photoError, dateError, positionError, descriptionError)

        val context = LocalContext.current
        val configuration: Configuration = context.getResources().getConfiguration()
        var screenWidthDp = configuration.screenWidthDp
        var screenHeightDp = configuration.screenHeightDp
        val heightOfSection = (screenHeightDp * 0.8f).dp

        val navController = rememberNavController()
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Heading(navController, username, context)
            MyTitle(text = "Post new announcement")
            Spacer(modifier = Modifier.height(15.dp))

            // To show the loading screen when a post is sent
            loading = remember { mutableStateOf(false) }

            if (!loading.value) {
                // Form
                Form(missingFieldErrors)
            }
            else {
                LoadingScreen(heightOfSection = heightOfSection, screenHeightDp = screenHeightDp, text = "Creating post... ")
            }

        }
    }

//--------------------------------------------------------------------------------------------------

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Form(missingFieldErrors: Array<MutableState<Boolean>>, modifier: Modifier = Modifier) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            PetNameField(missingFieldErrors[petNameErrorIndex])
            PetTypeField()
            PhotoField(missingFieldErrors[photoErrorIndex])
            DateField(missingFieldErrors[dateErrorIndex])
            PositionField(missingFieldErrors[positionErrorIndex])
            Spacer(modifier = Modifier.height(8.dp))
            DescriptionField()

            // To create the post
            MyBigButton(text = "Post", onClick = {
                // To check if the fields are valid
                if (validateFields(missingFieldErrors)) {

                    loading.value = true

                    CoroutineScope(Dispatchers.IO).launch {
                        runBlocking {
                            val res = PostsHandler.createPost(user_id, username, petName, pet_type, date, position, description, getPath(photoURI))
                            Log.d("Server response", res.toString())
                        }
                        finish()
                    }
                }
            })
        }
    }

    //----------------------------------------------------------------------------------------------

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PetNameField(showPetNameError: MutableState<Boolean>) {
        var text by remember { mutableStateOf("") }

        Column {
            Row (
                verticalAlignment = Alignment.CenterVertically
            ){
                OutlinedTextField(
                    //value shown in the field
                    value = text,
                    onValueChange = {
                        text = it.take(petNameMaxLength)
                        petName = text
                        // to hide the error message
                        showPetNameError.value = false
                    },
                    label = { Text("Pet name", color = superDarkGreen) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedTextColor = superDarkGreen,
                        cursorColor = superDarkGreen,
                        focusedBorderColor = superDarkGreen
                    )
                )
            }
            // Error message
            if (showPetNameError.value) {
                Text(
                    text = "Please, add your pet's name",
                    fontSize = 14.sp,
                    color = Color.Red
                )
            }
        }
    }

//--------------------------------------------------------------------------------------------------

    @Composable
    fun PhotoField(showPhotoError: MutableState<Boolean>) {
        photoSelected = remember { mutableStateOf("") }

        Column {

            photo = AppCompatImageView(this@CreatePostActivity)

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row{
                        MyText(text = "Photo")
                        MyText(text = " " + photoSelected.value)
                    }
                    val pickImg =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                    MyIconButton(icon = Icons.Default.PhotoCamera, onClick = {
                        // ask for storage permissions
                        if (!checkStoragePermission()) {
                            requestStoragePermission()
                        } else {
                            changeImage.launch(pickImg)
                            showPhotoError.value = false
                        }
                    })

                    if (showPhotoError.value) {
                        Text(
                            text = "Please, choose a photo",
                            fontSize = 14.sp,
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    @Composable
    fun DateField(showDateError: MutableState<Boolean>) {
        var text by remember { mutableStateOf("") }

        Column {
            Row (
                verticalAlignment = Alignment.CenterVertically
            ){
                Column (
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    Row{
                        MyText(text = "Date")
                        MyText(text = ": " + text)
                    }

                    val datePickerDialog = DatePickerDialog(
                        this@CreatePostActivity,
                        {_, year : Int, month: Int, day: Int ->
                            val m = month+1
                            var month_string = m.toString()
                            if (m < 10)
                                month_string = "0" + month_string
                            var day_string = day.toString()
                            if (day < 10)
                                day_string = "0" + day_string
                            date = "$year-$month_string-$day_string"
                            text = date
                            Log.d("date", date)
                            showDateError.value = false
                        }, current_year, current_month, current_day
                    )

                    datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
                    MyIconButton(Icons.Default.CalendarMonth, onClick = {
                        datePickerDialog.show()
                    })
                }

                if (showDateError.value) {
                    Text(
                        text = "Please, choose a date",
                        fontSize = 14.sp,
                        color = Color.Red
                    )
                }
            }

        }
    }

    //----------------------------------------------------------------------------------------------

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PetTypeField() {
        val context = LocalContext.current
        var expanded by remember { mutableStateOf(false) }
        val options = arrayOf(PET_TYPE_DOG, PET_TYPE_CAT, PET_TYPE_OTHER)
        var selectedText by remember { mutableStateOf(options[0]) }

        Row (
            verticalAlignment = Alignment.CenterVertically
        ){
            Box(
                modifier = Modifier
                    .padding(32.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    },
                    modifier = Modifier
                        .border(2.dp, superDarkGreen)
                        .background(lightGreen)
                ) {
                    TextField(
                        value = selectedText,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { item ->
                            DropdownMenuItem(
                                text = { MyText(text = item) },
                                onClick = {
                                    selectedText = item
                                    expanded = false
                                    pet_type = item
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PositionField(showPositionError: MutableState<Boolean>) {
        var text by remember { mutableStateOf(position) }

        // Dialog state Manager
        val dialogState: MutableState<Boolean> = remember {mutableStateOf(false)}

        Column {
            Row (
                verticalAlignment = Alignment.CenterVertically
            ){
                Column (
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ){
                    Row{
                        MyText(text = "Position")
                        MyText(text = text)
                    }
                    Row(
                        modifier = Modifier.graphicsLayer { translationY = -7.dp.toPx()}
                    ){
                        // To open the map
                        MyIconButton(Icons.Default.LocationOn, onClick = {

                            if (!checkGPSPermission()) {
                                requestGPSPermission()
                            }
                            else {
                                dialogState.value = true
                            }
                        })
                    }
                }

                // Code to Show and Dismiss Dialog
                if (dialogState.value) {
                    AlertDialog(
                        containerColor = superLightGreen,
                        onDismissRequest = { dialogState.value = false },
                        title = {
                            MyText(text = "Choose the position")
                        },
                        text = {
                            Box(
                                modifier = Modifier
                                    .width(300.dp)
                                    .height(330.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .border(BorderStroke(4.dp, superDarkGreen))
                            ) {
                                mapSelectorDialog.MapSelector()
                            }

                        },
                        confirmButton = {
                            MyButton(text = "Ok", onClick = {
                                // passagli l'ultima posizione come startLocation
                                mapSelectorDialog.startLocation = mapSelectorDialog.getPosition().clone()

                                position = mapSelectorDialog.getPositionAsString()
                                text = " selected"

                                showPositionError.value = false     // hide error message
                                dialogState.value = false
                            }
                            )
                        }, dismissButton = {
                            MyIconButton(
                                icon = Icons.Default.GpsFixed,
                                onClick = {mapSelectorDialog.updatePositionWithGPS()}
                            )

                        }

                    )
                }

                // error message
                if (showPositionError.value) {
                    Text(
                        text = "Please, choose a position",
                        fontSize = 14.sp,
                        color = Color.Red
                    )
                }
            }
        }
    }

//--------------------------------------------------------------------------------------------------

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DescriptionField() {
        var text by remember { mutableStateOf(description) }
        Row (
            verticalAlignment = Alignment.CenterVertically
        ){
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it.take(descriptionMaxLength)
                    description = text
                },
                label = { Text("Pet description", color = superDarkGreen) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedTextColor = superDarkGreen,
                    cursorColor = superDarkGreen,
                    focusedBorderColor = superDarkGreen
                )
            )
        }
    }

    //----------------------------------------------------------------------------------------------


    fun validateFields(missingFieldErrors: Array<MutableState<Boolean>>): Boolean {


        missingFieldErrors[petNameErrorIndex].value = (petName == "")

        missingFieldErrors[photoErrorIndex].value = (photoURI == null)

        missingFieldErrors[dateErrorIndex].value = (date == "")

        missingFieldErrors[positionErrorIndex].value = (position == "")


        if (petName != "" && photoURI != null && date != "" && position != "") {
            if (description == "") {
                description = " "
            }
            return true
        }
        return false
    }

    private val changeImage =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val data = it.data
                val imgUri = data?.data
                photo.setImageURI(imgUri)
                if (imgUri != null) {
                    photoURI = imgUri
                }
                photoSelected.value = "selected"
            }
        }

    //----------------------------------------------------------------------------------------------


    fun getPath(uri: Uri?): String {
        val projection = arrayOf<String>(MediaStore.MediaColumns.DATA)
        val cursor = managedQuery(uri, projection, null, null, null)
        val column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        cursor.moveToFirst()
        return cursor.getString(column_index)
    }


    private fun checkStoragePermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        val result1 = ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return (result == PackageManager.PERMISSION_GRANTED) && (result1 == PackageManager.PERMISSION_GRANTED)
    }
    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ), 1)
    }

    // To ask for GPS permissions
    private fun checkGPSPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val result1 = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
    }
    private fun requestGPSPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), 1)
    }
}
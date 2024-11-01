package com.invincible.jedishare

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.service.autofill.OnClickAction
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Space
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import com.invincible.jedishare.presentation.Audio
import com.invincible.jedishare.presentation.AudioViewModel
import com.invincible.jedishare.presentation.Image
import com.invincible.jedishare.presentation.ImageViewModel
import com.invincible.jedishare.presentation.Video
import com.invincible.jedishare.presentation.VideoViewModel
import com.invincible.jedishare.ui.theme.JediShareTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.toSize
import androidx.core.content.ContextCompat
import coil.compose.rememberImagePainter
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.invincible.jedishare.domain.chat.FileInfo
import com.invincible.jedishare.presentation.components.CustomProgressIndicator
import com.invincible.jedishare.ui.theme.MyGray
import com.invincible.jedishare.ui.theme.MyRed
import com.invincible.jedishare.ui.theme.MyRedSecondary
import com.invincible.jedishare.ui.theme.MyRedSecondaryLight
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class SelectFile : ComponentActivity() {
    private val imageViewModel by viewModels<ImageViewModel>()
    private val videoViewModel by viewModels<VideoViewModel>()
    private val audioViewModel by viewModels<AudioViewModel>()

    public var list by mutableStateOf(emptyList<Uri?>())
    val data = registerForActivityResult(GetMultipleContents()){ items ->
        list += items
    }


    private val STORAGE_PERMISSION_CODE = 1

    private fun requestStoragePermission() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
        )

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE)
                return
            }
        }

        // Permission already granted, call your function here
        Log.e("HELLOME", "Storage permission granted")
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.e("HELLOME", "Storage permission granted")
                // Call your function to save the image here
            } else {
                Log.e("HELLOME", "Storage permission denied")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestStoragePermission()

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            0
        )

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"


        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)

            val images = mutableListOf<Image>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                images.add(Image(id, name, uri))
            }
            imageViewModel.updateImages(images)
        }

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)

            val videos = mutableListOf<Video>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                videos.add(Video(id, name, uri))
            }
            videoViewModel.updateVideos(videos)
        }

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)

            val audios = mutableListOf<Audio>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                audios.add(Audio(id, name, uri))
            }
            audioViewModel.updateAudios(audios)
        }

        setContent {
            var darkTheme by remember { mutableStateOf(false) }
            JediShareTheme(darkTheme = darkTheme) {
                val context = LocalContext.current

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Spacer(modifier = Modifier.size(16.dp))

                        Row (
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            val context = LocalContext.current as? ComponentActivity
                            Box(
                                modifier = Modifier
                                    .width(95.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null, // Disable the click animation
                                    ) {
                                        context?.finish()
                                    }
                                    .height(35.dp)
//                                    .background(Color.LightGray),
//                                contentAlignment = Alignment.
                            ){

                                Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = null,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .align(Alignment.CenterStart),
                                    tint = MyRed
                                )

                                var interactionSource = remember { MutableInteractionSource() }
                                Text(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd),
                                    text = "Home",
                                    fontSize = 20.sp,
                                    style = MaterialTheme.typography.h1,
                                    fontWeight = FontWeight.Bold,
                                    color = MyRed,
                                    textAlign = TextAlign.Center,
//                                    textDecoration = TextDecoration.Underline
                                )
                            }

                            Text(
                                text = "Select Files",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.h3,
                            )

                            Box(
                                modifier = Modifier
                                    .widthIn(max = 90.dp, min = 90.dp)
//                                    .background(Color.LightGray),
//                                contentAlignment = Alignment.
                            ){

                                Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = null,
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null, // Disable the click animation
                                        ) {
                                        }
                                        .size(35.dp)
                                        .align(Alignment.CenterStart),
                                    tint = Color.White
                                )

                                var interactionSource = remember { MutableInteractionSource() }
                                Text(
                                    text = "Home",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    style = MaterialTheme.typography.h4,
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) {
                                        }
                                        .align(Alignment.CenterEnd)
                                )
                            }

                        }
                        Divider(
                            color = Color.LightGray,
                            thickness = 2.dp,
                            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        )

                        if(list.isEmpty()){
//                            Spacer(modifier = Modifier.size(40.dp))
                            dropDownMenu(data)
                        }else {
                            Box(
                                modifier = Modifier
//                                    .height(300.dp)
                                    .padding(20.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MyRedSecondaryLight)
                                    .fillMaxWidth()
                                    .heightIn(max = 660.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.Bottom,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {

                                    Spacer(modifier = Modifier.size(12.dp))

                                    LazyColumn(
                                        modifier = Modifier
                                            .heightIn(max = 570.dp)
                                            .animateContentSize(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.Top,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        item{
                                            IconButton(
                                                onClick = {
                                                    data.launch("*/*")
                                                },
                                                modifier = Modifier
//                                                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 2.dp)
//                                                    .size(55.dp)
//                                                    .clip(CircleShape)
//                                                    .align(Alignment.CenterHorizontally)
                                            ) {
                                                Icon(
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                    ,
                                                    imageVector = Icons.Default.AddCircle,
                                                    contentDescription = null,
                                                    tint = MyRed,
                                                )
                                            }

                                        }
                                        items(list) { item ->
                                            Column(
                                                modifier = Modifier
                                                    .padding(
                                                        top = 8.dp,
                                                        bottom = 8.dp,
                                                        end = 8.dp
                                                    ) // Adjust padding as needed
                                                    .fillMaxWidth(),
                                                horizontalAlignment = Alignment.Start,
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .wrapContentSize()
                                                        .padding(
                                                            top = 4.dp,
                                                            bottom = 4.dp,
                                                            end = 4.dp
                                                        ) // Adjust padding as needed
                                                ) {


                                                    Spacer(modifier = Modifier.size(10.dp))

                                                    val uri = Uri.parse(item.toString())
                                                    val fileNameAndFormat =
                                                        getFileDetailsFromUri(uri, contentResolver)
                                                    val fileType = fileNameAndFormat.format?.let {
                                                        classifyFileType(it)
                                                    }

                                                    val icon: Painter
                                                    val iconTint: Color
                                                    val iconBackground: Color

                                                    if (fileType == "Photo") {
                                                        icon =
                                                            painterResource(id = R.drawable.photo_icon)
                                                        iconTint = Color(0xFF33A850)
                                                        iconBackground = Color(0x2233A850)
                                                    } else if (fileType == "Video") {
                                                        icon =
                                                            painterResource(id = R.drawable.video_icon)
                                                        iconTint = Color(0xFFC54EE6)
                                                        iconBackground = Color(0x22C54EE6)
                                                    } else if(fileType == "Music"){
                                                        icon =
                                                            painterResource(id = R.drawable.music_icon)
                                                        iconTint = Color(0xFFFF0000)
                                                        iconBackground = Color(0x22FF0000)
                                                    }else {
                                                        icon =
                                                            painterResource(id = R.drawable.document_icon)
                                                        iconTint = Color(0xFF4187E6)
                                                        iconBackground = Color(0x224187E6)
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                iconBackground,
                                                                shape = CircleShape
                                                            )
                                                            .size(50.dp),
                                                        contentAlignment = androidx.compose.ui.Alignment.Center,
                                                    ) {
                                                        Icon(
                                                            painter = icon,
                                                            contentDescription = null,
                                                            tint = iconTint,
                                                            modifier = Modifier.size(30.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.size(10.dp))

                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth(),
                                                        horizontalAlignment = Alignment.Start,
                                                        verticalArrangement = Arrangement.SpaceBetween
                                                    ) {

                                                        var expanded by remember{
                                                            mutableStateOf(false)
                                                        }
                                                        // this is to disable the ripple effect
                                                        val interactionSource = remember {
                                                            MutableInteractionSource()
                                                        }

                                                        fileNameAndFormat.fileName?.let {
                                                            Text(
                                                                text = if(fileNameAndFormat.format != "Document") it else it.substring(0, it.length - 4),
                                                                fontSize = 15.sp,
//                                                        style = MaterialTheme.typography.h4,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier
                                                                    .animateContentSize()
                                                                    .clickable(
                                                                        indication = null,
                                                                        interactionSource = interactionSource
                                                                    ) {
                                                                        expanded = !expanded
                                                                    },
                                                                maxLines = if(expanded) 10 else 2
//                                                            .padding(16.dp) // Adjust padding as needed
                                                            )

                                                            val sizeInBytes = fileNameAndFormat.size

                                                            val units = arrayOf(
                                                                "B",
                                                                "KB",
                                                                "MB",
                                                                "GB",
                                                                "TB",
                                                                "PB",
                                                                "EB",
                                                                "ZB",
                                                                "YB"
                                                            )
                                                            val digitGroups = (sizeInBytes?.let { it1 ->
                                                                Math.log10(
                                                                    it1.toDouble()
                                                                )
                                                            }?.div(Math.log10(1024.0)))?.toInt()

//                                                    val humanReadableSize = String.format("%.1f %s", sizeInBytes / digitGroups?.let { it1 -> Math.pow(1024.0, it1.toDouble()) }, units[digitGroups!!])

                                                            Text(
                                                                text = fileNameAndFormat.size?.let { it1 ->
                                                                    bytesToHumanReadableSize(
                                                                        it1.toDouble()
                                                                    )
                                                                }.toString(),
                                                                fontSize = 15.sp,
//                                                        style = MaterialTheme.typography.h6,
                                                            )

                                                        }
                                                    }
                                                }
//                                                Divider(
////                                                    color = Color(0xFFEEEEEE),
//                                                    color = Color.LightGray,
//                                                    thickness = 1.dp,
//                                                    modifier = Modifier.padding(
//                                                        vertical = 16.dp,
//                                                        horizontal = 16.dp
//                                                    )
//                                                )
                                            }
                                        }


                                    }

//                                    Spacer(modifier = Modifier.weight(1f))
                                }

                            }


                        }

                        Spacer(modifier = Modifier.weight(1f). background(Color.LightGray))
                        val buttonColor: Color
                        val buttonTextColor: Color
                        if(list.isEmpty()){
                            buttonColor = Color.LightGray
                            buttonTextColor = Color.Gray
                        }else{
                            buttonColor = Color(0xFFec1c22)
                            buttonTextColor = Color.White
                        }

                        val interactionSource = remember { MutableInteractionSource() }
                        Button(
                            enabled = list.isNotEmpty(),
                            onClick = {
                                if(list.isNotEmpty()){
                                    val destinationActivity =
                                        intent.getStringExtra("transferMethod")
                                    val isFromReceive = intent.getBooleanExtra("source", false)
                                    val intent: Intent

                                    if (destinationActivity == "Wifi-Direct") {
                                        intent = Intent(
                                            this@SelectFile,
                                            WifiDirectDeviceSelectActivity::class.java
                                        )
                                        intent.putExtra("transferMethod", "Bluetooth")
                                    } else {
                                        intent = Intent(this@SelectFile, DeviceList::class.java)
                                    }

                                    if (isFromReceive) {
                                        intent.putExtra("source", true)
                                    } else {
                                        intent.putExtra("source", false)
                                    }
                                    intent.putParcelableArrayListExtra(
                                        "urilist",
                                        ArrayList(list)
                                    )
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier
                                .padding(bottom = 20.dp, start = 20.dp, end = 20.dp)
                                .fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = buttonColor
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Transfer",
                                style = MaterialTheme.typography.h5,
                                color = buttonTextColor,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                    }

                }
            }
        }
    }
}


@Composable
fun dropDownMenu(data: ActivityResultLauncher<String>) {

    var expanded by remember { mutableStateOf(false) }
    val suggestions = listOf("All", "Photo", "Video", "Audio")
    var selectedText by remember { mutableStateOf("") }

    var textfieldSize by remember { mutableStateOf(Size.Zero) }

    val icon = if (expanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown


    Column(
        Modifier.padding(20.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top

    ) {

        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .height(300.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MyRedSecondaryLight)
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
//                This value is used to assign to the DropDown the same width
                    textfieldSize = coordinates.size.toSize()
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    data.launch("*/*")
//                    expanded = !expanded
                },
            contentAlignment = Alignment.Center,
        ){
            Column (
//                modifier =
//                    Modifier.clickable { expanded = !expanded },
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Icon(
                    modifier = Modifier
                        .size(60.dp)
                    ,
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = MyRed,
                )
                Text(
                    text = "Upload Files",
                    style = MaterialTheme.typography.h5,
                    color = Color.Gray,
                    fontSize = 30.sp
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { textfieldSize.width.toDp() })
//            .clip(RoundedCornerShape(50.dp))
//            .size(200.dp)
//            .width(100.dp)
        ) {
            suggestions.forEach { label ->
                DropdownMenuItem(onClick = {
                    selectedText = label
                    expanded = false
                    if(label == "All"){
                        data.launch("*/*")
                    }else if(label == "Photo"){
                        data.launch("*/*")
                    }else if(label == "Video"){
                        data.launch("video/*")
                    }else{
                        data.launch("audio/*")
                    }
                }) {
                    Text(text = label)
                }
            }
        }
    }
}

@Composable
fun AnimatedPreloader(modifier: Modifier = Modifier, drawable: Int, iterations: Int = LottieConstants.IterateForever,) {
    val preloaderLottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(
            drawable
        )
    )

    val preloaderProgress by animateLottieCompositionAsState(
        preloaderLottieComposition,
        iterations = iterations,
        isPlaying = true,

        )
    LottieAnimation(
        composition = preloaderLottieComposition,
        progress = preloaderProgress,
        modifier = modifier,
    )
}

fun classifyFileType(fileExtension: String): String {
    val photoExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "heic")
    val videoExtensions = listOf("mp4", "mov", "avi", "mkv", "wmv", "flv")
    val musicExtentions = listOf("mp3")
    val documentExtensions = listOf("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt")

    return when {
        photoExtensions.contains(fileExtension.toLowerCase()) -> "Photo"
        videoExtensions.contains(fileExtension.toLowerCase()) -> "Video"
        documentExtensions.contains(fileExtension.toLowerCase()) -> "Document"
        musicExtentions.contains(fileExtension.toLowerCase()) -> "Music"
        else -> "Document"
    }
}

fun bytesToHumanReadableSize(bytes: Double) = when {
    bytes >= 1 shl 30 -> "%.1f GB".format(bytes / (1 shl 30))
    bytes >= 1 shl 20 -> "%.1f MB".format(bytes / (1 shl 20))
    bytes >= 1 shl 10 -> "%.0f kB".format(bytes / (1 shl 10))
    else -> "$bytes bytes"
}

@Composable
fun ImageFromBitmap(bitmap: Bitmap?) {
    val imageBitmap: ImageBitmap? = bitmap?.asImageBitmap()

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

object ByteToBitmapConverter {

    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap? {
        if (byteArray.isEmpty()) {
            return null
        }

        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}

@Composable
fun TextWithBorder(text: String, borderColor: Color, borderWidth: Int, textColor: Color) {
    Surface(
        modifier = Modifier
            .wrapContentSize()
            .padding(4.dp) // Adjust padding as needed
            .border(width = borderWidth.dp, color = borderColor)
            .clip(MaterialTheme.shapes.medium)
    ) {
        Text(
            text = text,
            color = textColor,
            modifier = Modifier
                .padding(8.dp) // Adjust padding as needed
//                .background(Color.LightGray)
        )
    }
}

@Composable
fun CustomButton(buttonText: String, onClickAction: () -> Unit) {
    Button(onClick = {
        onClickAction.invoke()
    },
        //colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.Gray),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
        elevation = ButtonDefaults.elevation(defaultElevation = 10.dp,
            pressedElevation = 15.dp,
            disabledElevation = 0.dp)
    )

    {
        Text(text = buttonText,color = Color.Gray, fontSize = 15.sp)
    }
}

fun getFileDetailsFromUri(
    uri: Uri,
    contentResolver: ContentResolver
): FileInfo {
    var fileName: String? = null
    var format: String? = null
    var size: String? = null
    var mimeType: String? = null

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        //val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

        val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
        val mimeTypeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)

        if (cursor.moveToFirst()) {
            fileName = cursor.getString(nameColumn)
            format = fileName?.substringAfterLast('.', "")
            Log.e("HELLOMEE", fileName.toString() + format.toString())
            size = cursor.getLong(sizeColumn).toString()
            mimeType = cursor.getString(mimeTypeColumn)

            if (mimeType.isNullOrBlank()) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(format?.toLowerCase())
            }
        }
    }

    return FileInfo(fileName, format, size, mimeType)
}

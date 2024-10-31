package com.invincible.jedishare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.TextField
import com.invincible.jedishare.ui.theme.JediShareTheme

class SettingsActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesManager = PreferencesManager(this)
        setContent {
            val isDarkTheme = remember { mutableStateOf(preferencesManager.isDarkTheme()) }
            JediShareTheme(darkTheme = isDarkTheme.value) {
                SettingsScreen(preferencesManager)
            }
        }
    }
}

@Composable
fun SettingsScreen(preferencesManager: PreferencesManager) {
    var isDarkTheme by remember { mutableStateOf(preferencesManager.isDarkTheme()) }
    var storageLocation by remember { mutableStateOf(preferencesManager.getStorageLocation() ?: "") }
    var isBluetooth by remember { mutableStateOf(preferencesManager.isBluetooth()) }

    fun saveSettings() {
        preferencesManager.setDarkTheme(isDarkTheme)
        preferencesManager.setStorageLocation(storageLocation)
        preferencesManager.setFileTransferMode(isBluetooth)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.BottomCenter,
    ) {
        NavBar()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = "Settings", style = MaterialTheme.typography.h4)

            // Theme Selection
            Text(text = "Select Theme")
            Row {
                RadioButton(
                    selected = isDarkTheme,
                    onClick = { isDarkTheme = true }
                )
                Text(text = "Dark Mode")
                RadioButton(
                    selected = !isDarkTheme,
                    onClick = { isDarkTheme = false }
                )
                Text(text = "Light Mode")
            }

            // Default Storage Location
            Text(text = "Default Storage Location")
            TextField(
                value = storageLocation,
                onValueChange = { newValue -> storageLocation = newValue },
                label = { Text("Storage Location") }
            )

            // Default File Transfer Mode
            Text(text = "Default File Transfer Mode")
            Row {
                RadioButton(
                    selected = isBluetooth,
                    onClick = { isBluetooth = true }
                )
                Text(text = "Bluetooth")
                RadioButton(
                    selected = !isBluetooth,
                    onClick = { isBluetooth = false }
                )
                Text(text = "Wifi Direct")
            }

            // Save Button
            Button(onClick = { saveSettings() }) {
                Text("Save Settings")
            }
        }
    }
}




package com.invincible.jedishare

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    fun setDarkTheme(isDarkTheme: Boolean) {
        sharedPreferences.edit().putBoolean("dark_theme", isDarkTheme).apply()
    }

    fun isDarkTheme(): Boolean {
        return sharedPreferences.getBoolean("dark_theme", false)
    }

    fun setStorageLocation(location: String) {
        sharedPreferences.edit().putString("storage_location", location).apply()
    }

    fun getStorageLocation(): String? {
        return sharedPreferences.getString("storage_location", "")
    }

    fun setFileTransferMode(isBluetooth: Boolean) {
        sharedPreferences.edit().putBoolean("file_transfer_mode", isBluetooth).apply()
    }

    fun isBluetooth(): Boolean {
        return sharedPreferences.getBoolean("file_transfer_mode", true)
    }
}

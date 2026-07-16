package com.example.textez.storage

import android.content.SharedPreferences

object RecentFilesManager {

    const val PREFERENCES_NAME = "textez_preferences"
    const val MAX_RECENT_FILES = 10

    private const val KEY_RECENT_FILES = "recent_files"
    private const val SEPARATOR = "\n"

    fun getRecentFiles(
        preferences: SharedPreferences
    ): List<String> {

        val storedValue = preferences.getString(
            KEY_RECENT_FILES,
            ""
        ).orEmpty()

        if (storedValue.isBlank()) {
            return emptyList()
        }

        return storedValue
            .split(SEPARATOR)
            .filter { it.isNotBlank() }
    }

    fun saveRecentFiles(
        preferences: SharedPreferences,
        files: List<String>
    ) {
        preferences.edit()
            .putString(
                KEY_RECENT_FILES,
                files.joinToString(SEPARATOR)
            )
            .apply()
    }
}
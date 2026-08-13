package com.example.textez.storage

import android.content.SharedPreferences

object RecentFilesManager {

    const val PREFERENCES_NAME =
        "textez_preferences"

    const val MAX_RECENT_FILES =
        10

    private const val KEY_RECENT_FILES =
        "recent_files"

    private const val SEPARATOR =
        "\n"

    private const val TYPE_INTERNAL =
        "INTERNAL"

    private const val TYPE_URI =
        "URI"

    data class RecentFile(
        val displayName: String,
        val reference: String,
        val isExternal: Boolean
    )

    /**
     * Returns recent files in the new structured format.
     *
     * Older entries that only contain a filename are
     * automatically treated as internal TextEZ files.
     */
    fun getRecentFileItems(
        preferences: SharedPreferences
    ): List<RecentFile> {

        val storedValue =
            preferences.getString(
                KEY_RECENT_FILES,
                ""
            ).orEmpty()

        if (storedValue.isBlank()) {
            return emptyList()
        }

        return storedValue
            .split(SEPARATOR)
            .filter {
                it.isNotBlank()
            }
            .mapNotNull { entry ->
                parseEntry(entry)
            }
    }

    /**
     * Compatibility function for your existing code.
     *
     * It returns filenames/display names only.
     */
    fun getRecentFiles(
        preferences: SharedPreferences
    ): List<String> {

        return getRecentFileItems(
            preferences
        ).map {
            it.displayName
        }
    }

    /**
     * Saves old-style internal filenames.
     *
     * Existing code can continue calling this method.
     */
    fun saveRecentFiles(
        preferences: SharedPreferences,
        files: List<String>
    ) {
        val entries =
            files.map { fileName ->
                RecentFile(
                    displayName =
                        fileName,

                    reference =
                        fileName,

                    isExternal =
                        false
                )
            }

        saveRecentFileItems(
            preferences,
            entries
        )
    }

    /**
     * Adds an internal TextEZ file to Recent Files.
     */
    fun addInternalFile(
        preferences: SharedPreferences,
        fileName: String
    ) {
        val item =
            RecentFile(
                displayName =
                    fileName,

                reference =
                    fileName,

                isExternal =
                    false
            )

        addRecentFile(
            preferences,
            item
        )
    }

    /**
     * Adds a URI-backed document to Recent Files.
     */
    fun addExternalFile(
        preferences: SharedPreferences,
        displayName: String,
        uriString: String
    ) {
        val item =
            RecentFile(
                displayName =
                    displayName,

                reference =
                    uriString,

                isExternal =
                    true
            )

        addRecentFile(
            preferences,
            item
        )
    }

    /**
     * Removes an internal file by filename.
     */
    fun removeInternalFile(
        preferences: SharedPreferences,
        fileName: String
    ) {
        val updated =
            getRecentFileItems(
                preferences
            ).filterNot { item ->

                !item.isExternal &&
                        item.reference ==
                        fileName
            }

        saveRecentFileItems(
            preferences,
            updated
        )
    }

    /**
     * Removes an external file by URI.
     */
    fun removeExternalFile(
        preferences: SharedPreferences,
        uriString: String
    ) {
        val updated =
            getRecentFileItems(
                preferences
            ).filterNot { item ->

                item.isExternal &&
                        item.reference ==
                        uriString
            }

        saveRecentFileItems(
            preferences,
            updated
        )
    }

    /**
     * Saves recent files in the new structured format.
     */
    fun saveRecentFileItems(
        preferences: SharedPreferences,
        files: List<RecentFile>
    ) {
        val limitedFiles =
            files.take(
                MAX_RECENT_FILES
            )

        val serialized =
            limitedFiles.joinToString(
                SEPARATOR
            ) { item ->

                serializeEntry(item)
            }

        preferences.edit()
            .putString(
                KEY_RECENT_FILES,
                serialized
            )
            .apply()
    }

    private fun addRecentFile(
        preferences: SharedPreferences,
        newItem: RecentFile
    ) {
        val currentFiles =
            getRecentFileItems(
                preferences
            ).toMutableList()

        currentFiles.removeAll { item ->

            item.isExternal ==
                    newItem.isExternal &&
                    item.reference ==
                    newItem.reference
        }

        currentFiles.add(
            0,
            newItem
        )

        saveRecentFileItems(
            preferences,
            currentFiles
        )
    }

    private fun serializeEntry(
        item: RecentFile
    ): String {

        val type =
            if (item.isExternal) {
                TYPE_URI
            } else {
                TYPE_INTERNAL
            }

        return listOf(
            type,
            encode(item.displayName),
            encode(item.reference)
        ).joinToString("|")
    }

    private fun parseEntry(
        entry: String
    ): RecentFile? {

        /*
         * Compatibility with your old format:
         * Notes.txt
         *
         * Old entries did not contain "|".
         */
        if (!entry.contains("|")) {
            return RecentFile(
                displayName =
                    entry,

                reference =
                    entry,

                isExternal =
                    false
            )
        }

        val parts =
            entry.split(
                "|",
                limit = 3
            )

        if (parts.size != 3) {
            return null
        }

        val type =
            parts[0]

        val displayName =
            decode(parts[1])

        val reference =
            decode(parts[2])

        return when (type) {

            TYPE_INTERNAL -> {
                RecentFile(
                    displayName =
                        displayName,

                    reference =
                        reference,

                    isExternal =
                        false
                )
            }

            TYPE_URI -> {
                RecentFile(
                    displayName =
                        displayName,

                    reference =
                        reference,

                    isExternal =
                        true
                )
            }

            else -> null
        }
    }

    /**
     * Simple escaping so filenames and URIs can safely
     * contain characters used by our storage format.
     */
    private fun encode(
        value: String
    ): String {

        return android.util.Base64
            .encodeToString(
                value.toByteArray(
                    Charsets.UTF_8
                ),
                android.util.Base64.NO_WRAP
            )
    }

    private fun decode(
        value: String
    ): String {

        return try {
            String(
                android.util.Base64.decode(
                    value,
                    android.util.Base64.NO_WRAP
                ),
                Charsets.UTF_8
            )
        } catch (exception: Exception) {
            value
        }
    }
}
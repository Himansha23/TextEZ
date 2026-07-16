package com.example.textez.storage

import android.content.Context
import java.io.File
import java.util.Properties

object AutoSaveManager {

    private const val RECOVERY_FOLDER = "TextEZRecovery"
    private const val RECOVERY_FILE = "active_recovery.properties"

    private const val KEY_FILE_NAME = "file_name"
    private const val KEY_CONTENT = "content"
    private const val KEY_TIMESTAMP = "timestamp"

    data class RecoveryData(
        val fileName: String,
        val content: String,
        val timestamp: Long
    )

    private fun getRecoveryFile(context: Context): File {
        val folder = File(context.cacheDir, RECOVERY_FOLDER)

        if (!folder.exists()) {
            folder.mkdirs()
        }

        return File(folder, RECOVERY_FILE)
    }

    fun saveRecovery(
        context: Context,
        fileName: String,
        content: String
    ): Boolean {
        return try {
            val properties = Properties().apply {
                setProperty(KEY_FILE_NAME, fileName)
                setProperty(KEY_CONTENT, content)
                setProperty(
                    KEY_TIMESTAMP,
                    System.currentTimeMillis().toString()
                )
            }

            getRecoveryFile(context).outputStream().use { output ->
                properties.store(output, "TextEZ recovery data")
            }

            true
        } catch (exception: Exception) {
            false
        }
    }

    fun loadRecovery(context: Context): RecoveryData? {
        val recoveryFile = getRecoveryFile(context)

        if (!recoveryFile.exists()) {
            return null
        }

        return try {
            val properties = Properties()

            recoveryFile.inputStream().use { input ->
                properties.load(input)
            }

            RecoveryData(
                fileName = properties.getProperty(
                    KEY_FILE_NAME,
                    "Untitled.txt"
                ),
                content = properties.getProperty(
                    KEY_CONTENT,
                    ""
                ),
                timestamp = properties.getProperty(
                    KEY_TIMESTAMP,
                    "0"
                ).toLongOrNull() ?: 0L
            )
        } catch (exception: Exception) {
            null
        }
    }

    fun hasRecovery(context: Context): Boolean {
        return getRecoveryFile(context).exists()
    }

    fun clearRecovery(context: Context): Boolean {
        val recoveryFile = getRecoveryFile(context)

        return !recoveryFile.exists() || recoveryFile.delete()
    }
}
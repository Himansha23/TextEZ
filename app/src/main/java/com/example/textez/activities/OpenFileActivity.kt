package com.example.textez.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.textez.storage.RecentFilesManager

class OpenFileActivity : AppCompatActivity() {

    private val openDocumentLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            if (uri == null) {
                finish()
                return@registerForActivityResult
            }

            persistFilePermission(uri)
            addExternalFileToRecent(uri)
            openSelectedFile(uri)
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        openDocumentLauncher.launch(
            arrayOf(
                "text/plain",
                "text/markdown",
                "text/x-kotlin",
                "application/json",
                "application/xml",
                "text/xml",
                "text/html",
                "text/css",
                "text/javascript",
                "application/javascript"
            )
        )
    }

    private fun persistFilePermission(
        uri: Uri
    ) {
        try {
            contentResolver
                .takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
        } catch (_: SecurityException) {
            /*
             * Some providers do not support persistent
             * permissions. The file can still be used
             * during the current session.
             */
        }
    }

    private fun addExternalFileToRecent(
        uri: Uri
    ) {
        val preferences =
            getSharedPreferences(
                RecentFilesManager
                    .PREFERENCES_NAME,
                MODE_PRIVATE
            )

        val displayName =
            getDisplayName(uri)

        RecentFilesManager
            .addExternalFile(
                preferences = preferences,
                displayName = displayName,
                uriString = uri.toString()
            )
    }

    private fun getDisplayName(
        uri: Uri
    ): String {
        var fileName =
            "ExternalFile.txt"

        contentResolver.query(
            uri,
            arrayOf(
                android.provider
                    .OpenableColumns
                    .DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->

            val columnIndex =
                cursor.getColumnIndex(
                    android.provider
                        .OpenableColumns
                        .DISPLAY_NAME
                )

            if (
                columnIndex >= 0 &&
                cursor.moveToFirst()
            ) {
                fileName =
                    cursor.getString(
                        columnIndex
                    ) ?: fileName
            }
        }

        return fileName
    }

    private fun openSelectedFile(
        uri: Uri
    ) {
        val intent =
            Intent(
                this,
                EditorActivity::class.java
            ).apply {

                putExtra(
                    EditorActivity
                        .EXTRA_EXTERNAL_FILE_URI,
                    uri.toString()
                )
            }

        startActivity(intent)

        finish()
    }
}
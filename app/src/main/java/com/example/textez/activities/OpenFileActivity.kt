package com.example.textez.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

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
            openSelectedFile(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
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

    private fun persistFilePermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            /*
             Some document providers only grant temporary access.
             The file may still open normally during this session.
             */
        }
    }

    private fun openSelectedFile(uri: Uri) {
        val intent =
            Intent(this, EditorActivity::class.java).apply {
                putExtra(EditorActivity.EXTRA_EXTERNAL_FILE_URI, uri.toString())
            }

        startActivity(intent)
        finish()
    }
}
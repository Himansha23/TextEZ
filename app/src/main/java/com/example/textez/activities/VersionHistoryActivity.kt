package com.example.textez.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.textez.R
import com.example.textez.adapters.VersionAdapter
import com.example.textez.managers.VersionManager
import com.example.textez.models.Version
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VersionHistoryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_NAME = "file_name"
    }

    private lateinit var recyclerVersions: RecyclerView
    private lateinit var txtNoVersions: TextView
    private lateinit var txtHistoryFileName: TextView

    private var currentFileName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            R.layout.activity_version_history
        )

        recyclerVersions =
            findViewById(R.id.recyclerVersions)

        txtNoVersions =
            findViewById(R.id.txtNoVersions)

        txtHistoryFileName =
            findViewById(R.id.txtHistoryFileName)

        currentFileName =
            intent.getStringExtra(
                EXTRA_FILE_NAME
            ).orEmpty()

        if (currentFileName.isBlank()) {
            Toast.makeText(
                this,
                getString(R.string.file_not_found),
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        txtHistoryFileName.text =
            currentFileName

        recyclerVersions.layoutManager =
            LinearLayoutManager(this)

        loadVersions()
    }

    private fun loadVersions() {
        val versions =
            VersionManager.getVersions(
                context = this,
                fileName = currentFileName
            )

        if (versions.isEmpty()) {
            recyclerVersions.visibility =
                View.GONE

            txtNoVersions.visibility =
                View.VISIBLE

            return
        }

        recyclerVersions.visibility =
            View.VISIBLE

        txtNoVersions.visibility =
            View.GONE

        recyclerVersions.adapter =
            VersionAdapter(versions) { version ->
                showVersionPreview(version)
            }
    }

    private fun showVersionPreview(
        version: Version
    ) {
        val content =
            VersionManager.loadVersionContent(
                context = this,
                version = version
            )

        if (content == null) {
            Toast.makeText(
                this,
                getString(
                    R.string.version_load_failed
                ),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val formatter = SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        )

        val dateText = formatter.format(
            Date(version.createdAt)
        )

        val previewView = TextView(this).apply {
            text = content
            textSize = 15f
            setTextColor(
                getColor(android.R.color.black)
            )
            setBackgroundColor(
                getColor(android.R.color.white)
            )
            setPadding(40, 30, 40, 30)
            typeface =
                android.graphics.Typeface.MONOSPACE
            setTextIsSelectable(true)
        }

        AlertDialog.Builder(this)
            .setTitle(
                getString(
                    R.string.version_preview_title,
                    version.versionNumber
                )
            )
            .setMessage(
                "${version.versionName}\n$dateText"
            )
            .setView(previewView)
            .setPositiveButton(
                getString(R.string.close),
                null
            )
            .show()
    }
}
package com.example.textez.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.textez.R
import com.example.textez.storage.RecentFilesManager
import java.io.File

class OpenFileActivity : AppCompatActivity() {

    private lateinit var listFiles: ListView
    private lateinit var txtEmptyFiles: TextView

    private val displayedFileNames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_file)

        listFiles = findViewById(R.id.listFiles)
        txtEmptyFiles = findViewById(R.id.txtEmptyFiles)

        listFiles.setOnItemClickListener { _, _, position, _ ->
            val displayedName = displayedFileNames[position]

            // Remove the star used to mark recent files.
            val actualFileName = displayedName.removePrefix("★ ")

            val intent = Intent(
                this,
                EditorActivity::class.java
            ).apply {
                putExtra(
                    EditorActivity.EXTRA_FILE_NAME,
                    actualFileName
                )
            }

            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadFiles()
    }

    private fun loadFiles() {
        val folder = File(filesDir, "TextEZ")

        if (!folder.exists()) {
            folder.mkdirs()
        }

        val allFiles = folder.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

        val preferences = getSharedPreferences(
            RecentFilesManager.PREFERENCES_NAME,
            MODE_PRIVATE
        )

        val recentFiles = RecentFilesManager.getRecentFiles(
            preferences
        )

        val existingNames = allFiles
            .map { it.name }
            .toSet()

        val validRecentFiles = recentFiles.filter {
            it in existingNames
        }

        val otherFiles = allFiles
            .map { it.name }
            .filter { it !in validRecentFiles }

        displayedFileNames.clear()

        displayedFileNames.addAll(
            validRecentFiles.map { "★ $it" }
        )

        displayedFileNames.addAll(otherFiles)

        txtEmptyFiles.visibility = if (displayedFileNames.isEmpty()) {
            TextView.VISIBLE
        } else {
            TextView.GONE
        }

        listFiles.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            displayedFileNames
        )

        if (recentFiles.size != validRecentFiles.size) {
            RecentFilesManager.saveRecentFiles(
                preferences,
                validRecentFiles
            )
        }
    }
}
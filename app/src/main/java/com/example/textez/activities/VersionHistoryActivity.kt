package com.example.textez.activities

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.textez.R
import com.example.textez.adapters.VersionAdapter
import com.example.textez.managers.LineDiffManager
import com.example.textez.managers.VersionManager
import com.example.textez.models.Version
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VersionHistoryActivity :
    AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_NAME =
            "file_name"

        const val EXTRA_ROLLED_BACK =
            "rolled_back"
    }

    private lateinit var recyclerVersions:
            RecyclerView

    private lateinit var txtNoVersions:
            TextView

    private lateinit var txtHistoryFileName:
            TextView

    private var currentFileName = ""

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_version_history
        )

        recyclerVersions =
            findViewById(
                R.id.recyclerVersions
            )

        txtNoVersions =
            findViewById(
                R.id.txtNoVersions
            )

        txtHistoryFileName =
            findViewById(
                R.id.txtHistoryFileName
            )

        currentFileName =
            intent.getStringExtra(
                EXTRA_FILE_NAME
            ).orEmpty()

        if (currentFileName.isBlank()) {
            Toast.makeText(
                this,
                getString(
                    R.string.file_not_found
                ),
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

    override fun onResume() {
        super.onResume()

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
            VersionAdapter(
                versions
            ) { version ->

                showVersionActions(
                    version
                )
            }
    }

    private fun showVersionActions(
        version: Version
    ) {
        val actions =
            arrayOf(
                getString(
                    R.string.preview
                ),
                getString(
                    R.string.compare
                ),
                getString(
                    R.string.restore
                )
            )

        AlertDialog.Builder(this)
            .setTitle(
                getString(
                    R.string.version_actions_title,
                    version.versionNumber
                )
            )
            .setItems(
                actions
            ) { _, selectedIndex ->

                when (selectedIndex) {
                    0 -> {
                        showVersionPreview(
                            version
                        )
                    }

                    1 -> {
                        showVersionComparison(
                            version
                        )
                    }

                    2 -> {
                        confirmRollback(
                            version
                        )
                    }
                }
            }
            .setNegativeButton(
                getString(
                    R.string.close
                ),
                null
            )
            .show()
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
            showLoadFailure()
            return
        }

        showScrollableTextDialog(
            title = getString(
                R.string.version_preview_title,
                version.versionNumber
            ),
            message =
                createVersionDetails(
                    version
                ),
            content = content
        )
    }

    private fun showVersionComparison(
        version: Version
    ) {
        val selectedContent =
            VersionManager.loadVersionContent(
                context = this,
                version = version
            )

        val currentContent =
            VersionManager
                .loadCurrentFileContent(
                    context = this,
                    fileName = currentFileName
                )

        if (
            selectedContent == null ||
            currentContent == null
        ) {
            showLoadFailure()
            return
        }

        val formattedDiff =
            LineDiffManager.formatDiff(
                oldText =
                    selectedContent,

                newText =
                    currentContent
            )

        showScrollableTextDialog(
            title = getString(
                R.string.compare_version_title,
                version.versionNumber
            ),
            message = getString(
                R.string.diff_legend
            ),
            content = formattedDiff
        )
    }

    private fun confirmRollback(
        version: Version
    ) {
        AlertDialog.Builder(this)
            .setTitle(
                getString(
                    R.string.restore_version_title,
                    version.versionNumber
                )
            )
            .setMessage(
                getString(
                    R.string.restore_version_message
                )
            )
            .setPositiveButton(
                getString(
                    R.string.restore
                )
            ) { _, _ ->

                rollback(version)
            }
            .setNegativeButton(
                getString(
                    R.string.cancel
                ),
                null
            )
            .show()
    }

    private fun rollback(
        version: Version
    ) {
        val restored =
            VersionManager.rollbackToVersion(
                context = this,
                version = version
            )

        if (!restored) {
            Toast.makeText(
                this,
                getString(
                    R.string.rollback_failed
                ),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Toast.makeText(
            this,
            getString(
                R.string.rollback_successful,
                version.versionNumber
            ),
            Toast.LENGTH_SHORT
        ).show()

        val resultIntent =
            Intent().apply {

                putExtra(
                    EXTRA_ROLLED_BACK,
                    true
                )
            }

        setResult(
            RESULT_OK,
            resultIntent
        )

        finish()
    }

    private fun showScrollableTextDialog(
        title: String,
        message: String,
        content: String
    ) {
        val contentView =
            TextView(this).apply {

                text = content
                textSize = 14f
                typeface =
                    Typeface.MONOSPACE

                setTextIsSelectable(true)

                setPadding(
                    32,
                    24,
                    32,
                    24
                )
            }

        val scrollView =
            ScrollView(this).apply {

                addView(contentView)
            }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setView(scrollView)
            .setPositiveButton(
                getString(
                    R.string.close
                ),
                null
            )
            .show()
    }

    private fun createVersionDetails(
        version: Version
    ): String {

        val formatter =
            SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
            )

        val storageType =
            if (version.isBaseVersion) {
                getString(
                    R.string.base_version
                )
            } else {
                getString(
                    R.string.delta_version
                )
            }

        return getString(
            R.string.version_details_format,
            version.versionName,
            formatter.format(
                Date(version.createdAt)
            ),
            storageType
        )
    }

    private fun showLoadFailure() {
        Toast.makeText(
            this,
            getString(
                R.string.version_load_failed
            ),
            Toast.LENGTH_SHORT
        ).show()
    }
}
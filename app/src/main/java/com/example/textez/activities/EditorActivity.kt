package com.example.textez.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.text.method.KeyListener
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.textez.R
import com.example.textez.managers.KotlinSyntaxHighlighter
import com.example.textez.managers.MarkdownSyntaxHighlighter
import com.example.textez.managers.VersionManager
import com.example.textez.storage.AutoSaveManager
import com.example.textez.storage.RecentFilesManager
import java.io.File

class EditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_NAME =
            "file_name"

        private const val DEFAULT_FILE_NAME =
            "Untitled.txt"

        private const val AUTO_SAVE_INTERVAL =
            10_000L

        private const val SYNTAX_DELAY =
            180L

        private const val READ_ONLY_PREFERENCES =
            "textez_read_only_preferences"

        private const val READ_ONLY_KEY_PREFIX =
            "read_only_"
    }

    private lateinit var editorText:
            EditText

    private lateinit var txtFileName:
            TextView

    private lateinit var txtStatus:
            TextView

    private lateinit var btnSave:
            Button

    private lateinit var btnSaveAs:
            Button

    private lateinit var btnUndo:
            Button

    private lateinit var btnRedo:
            Button

    private lateinit var btnSearch:
            Button

    private lateinit var btnReplace:
            Button

    private lateinit var btnReadOnly:
            Button

    private lateinit var btnCreateVersion:
            Button

    private lateinit var btnVersionHistory:
            Button

    private lateinit var btnDelete: Button

    private var originalKeyListener:
            KeyListener? = null

    private var isReadOnly = false

    private val undoStack =
        mutableListOf<String>()

    private val redoStack =
        mutableListOf<String>()

    private var isUndoRedoAction =
        false

    private var previousText = ""

    private var currentFileName =
        DEFAULT_FILE_NAME

    private var lastSavedOrRecoveredContent =
        ""

    private var hasUnsavedChanges =
        false

    private val versionHistoryLauncher =
        registerForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) { result ->

            val rolledBack =
                result.resultCode ==
                        RESULT_OK &&
                        result.data
                            ?.getBooleanExtra(
                                VersionHistoryActivity
                                    .EXTRA_ROLLED_BACK,
                                false
                            ) == true

            if (rolledBack) {
                loadCurrentFile()
                loadReadOnlyState()
                updateReadOnlyInterface()
                updateStatus()

                Toast.makeText(
                    this,
                    getString(
                        R.string.rollback_loaded
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val syntaxHandler =
        Handler(
            Looper.getMainLooper()
        )

    private val syntaxHighlightRunnable =
        Runnable {

            if (!::editorText.isInitialized) {
                return@Runnable
            }

            when {
                isKotlinFile() -> {

                    MarkdownSyntaxHighlighter
                        .clear(
                            editorText.text
                        )

                    KotlinSyntaxHighlighter
                        .highlight(
                            editorText.text
                        )
                }

                isMarkdownFile() -> {

                    KotlinSyntaxHighlighter
                        .clear(
                            editorText.text
                        )

                    MarkdownSyntaxHighlighter
                        .highlight(
                            editorText.text
                        )
                }

                else -> {

                    KotlinSyntaxHighlighter
                        .clear(
                            editorText.text
                        )

                    MarkdownSyntaxHighlighter
                        .clear(
                            editorText.text
                        )
                }
            }
        }

    private val autoSaveHandler =
        Handler(
            Looper.getMainLooper()
        )

    private val autoSaveRunnable =
        object : Runnable {

            override fun run() {

                saveRecoveryIfNeeded()

                autoSaveHandler
                    .postDelayed(
                        this,
                        AUTO_SAVE_INTERVAL
                    )
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_editor
        )

        editorText =
            findViewById(
                R.id.editorText
            )

        txtFileName =
            findViewById(
                R.id.txtFileName
            )

        txtStatus =
            findViewById(
                R.id.txtStatus
            )

        btnSave =
            findViewById(
                R.id.btnSave
            )

        btnSaveAs =
            findViewById(
                R.id.btnSaveAs
            )

        btnDelete =
            findViewById(
                R.id.btnDelete
            )

        btnUndo =
            findViewById(
                R.id.btnUndo
            )

        btnRedo =
            findViewById(
                R.id.btnRedo
            )

        btnSearch =
            findViewById(
                R.id.btnSearch
            )

        btnReplace =
            findViewById(
                R.id.btnReplace
            )

        btnReadOnly =
            findViewById(
                R.id.btnReadOnly
            )

        btnCreateVersion =
            findViewById(
                R.id.btnCreateVersion
            )

        btnVersionHistory =
            findViewById(
                R.id.btnVersionHistory
            )

        originalKeyListener =
            editorText.keyListener

        val openedFileName =
            intent.getStringExtra(
                EXTRA_FILE_NAME
            )

        if (
            !openedFileName
                .isNullOrBlank()
        ) {
            currentFileName =
                openedFileName

            loadCurrentFile()
        }

        txtFileName.text =
            currentFileName

        previousText =
            editorText.text
                .toString()

        lastSavedOrRecoveredContent =
            editorText.text
                .toString()

        loadReadOnlyState()

        editorText
            .addTextChangedListener(
                object : TextWatcher {

                    override fun beforeTextChanged(
                        text: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        if (!isUndoRedoAction) {
                            previousText =
                                text.toString()
                        }
                    }

                    override fun onTextChanged(
                        text: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        // No action needed.
                    }

                    override fun afterTextChanged(
                        text: Editable?
                    ) {
                        if (!isUndoRedoAction) {
                            undoStack.add(
                                previousText
                            )

                            redoStack.clear()
                        }

                        hasUnsavedChanges =
                            editorText.text
                                .toString() !=
                                    lastSavedOrRecoveredContent

                        updateStatus()
                        scheduleSyntaxHighlighting()
                    }
                }
            )

        btnSave.setOnClickListener {

            if (isReadOnly) {
                return@setOnClickListener
            }

            if (
                currentFileName ==
                DEFAULT_FILE_NAME
            ) {
                showSaveAsDialog()
            } else {
                saveCurrentFile()
            }
        }

        btnSaveAs.setOnClickListener {

            if (!isReadOnly) {
                showSaveAsDialog()
            }
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        btnUndo.setOnClickListener {

            if (!isReadOnly) {
                undoText()
            }
        }

        btnRedo.setOnClickListener {

            if (!isReadOnly) {
                redoText()
            }
        }

        btnSearch.setOnClickListener {

            showSearchDialog()
        }

        btnReplace.setOnClickListener {

            if (!isReadOnly) {
                showReplaceDialog()
            }
        }

        btnReadOnly.setOnClickListener {

            toggleReadOnlyMode()
        }

        btnCreateVersion
            .setOnClickListener {

                showCreateVersionDialog()
            }

        btnVersionHistory
            .setOnClickListener {

                openVersionHistory()
            }

        updateReadOnlyInterface()
        updateStatus()
        scheduleSyntaxHighlighting()
        checkForRecovery()

        autoSaveHandler
            .postDelayed(
                autoSaveRunnable,
                AUTO_SAVE_INTERVAL
            )
    }

    private fun getTextEZFolder():
            File {

        val folder =
            File(
                filesDir,
                "TextEZ"
            )

        if (!folder.exists()) {
            folder.mkdirs()
        }

        return folder
    }

    private fun isKotlinFile():
            Boolean {

        return currentFileName
            .endsWith(
                suffix = ".kt",
                ignoreCase = true
            )
    }

    private fun isMarkdownFile():
            Boolean {

        return currentFileName
            .endsWith(
                suffix = ".md",
                ignoreCase = true
            ) ||
                currentFileName
                    .endsWith(
                        suffix = ".markdown",
                        ignoreCase = true
                    )
    }

    private fun scheduleSyntaxHighlighting() {

        syntaxHandler
            .removeCallbacks(
                syntaxHighlightRunnable
            )

        if (
            isKotlinFile() ||
            isMarkdownFile()
        ) {
            syntaxHandler
                .postDelayed(
                    syntaxHighlightRunnable,
                    SYNTAX_DELAY
                )
        } else {
            KotlinSyntaxHighlighter
                .clear(
                    editorText.text
                )

            MarkdownSyntaxHighlighter
                .clear(
                    editorText.text
                )
        }
    }

    private fun toggleReadOnlyMode() {

        isReadOnly =
            !isReadOnly

        saveReadOnlyState()
        updateReadOnlyInterface()
        updateStatus()

        val message =
            if (isReadOnly) {
                R.string.read_only_enabled
            } else {
                R.string.edit_mode_enabled
            }

        Toast.makeText(
            this,
            getString(message),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateReadOnlyInterface() {

        if (isReadOnly) {

            editorText.keyListener =
                null

            editorText.isCursorVisible =
                false

            editorText.isLongClickable =
                false

            editorText.clearFocus()

            btnReadOnly.text =
                getString(
                    R.string.edit_mode
                )

            btnSave.isEnabled =
                false

            btnSaveAs.isEnabled =
                false

            btnUndo.isEnabled =
                false

            btnRedo.isEnabled =
                false

            btnReplace.isEnabled =
                false

            btnSearch.isEnabled =
                true

            btnCreateVersion.isEnabled =
                true

            btnVersionHistory.isEnabled =
                true

            btnDelete.isEnabled = true

        } else {

            editorText.keyListener =
                originalKeyListener

            editorText.isCursorVisible =
                true

            editorText.isLongClickable =
                true

            editorText.isFocusable =
                true

            editorText.isFocusableInTouchMode =
                true

            btnReadOnly.text =
                getString(
                    R.string.read_only
                )

            btnSave.isEnabled =
                true

            btnSaveAs.isEnabled =
                true

            btnDelete.isEnabled = true


            btnUndo.isEnabled =
                true

            btnRedo.isEnabled =
                true

            btnSearch.isEnabled =
                true

            btnReplace.isEnabled =
                true

            btnCreateVersion.isEnabled =
                true

            btnVersionHistory.isEnabled =
                true
        }
    }

    private fun saveReadOnlyState() {

        val preferences =
            getSharedPreferences(
                READ_ONLY_PREFERENCES,
                MODE_PRIVATE
            )

        preferences.edit()
            .putBoolean(
                READ_ONLY_KEY_PREFIX +
                        currentFileName,
                isReadOnly
            )
            .apply()
    }

    private fun loadReadOnlyState() {

        val preferences =
            getSharedPreferences(
                READ_ONLY_PREFERENCES,
                MODE_PRIVATE
            )

        isReadOnly =
            preferences.getBoolean(
                READ_ONLY_KEY_PREFIX +
                        currentFileName,
                false
            )
    }

    private fun saveCurrentFile() {

        if (isReadOnly) {
            return
        }

        try {
            val file =
                File(
                    getTextEZFolder(),
                    currentFileName
                )

            val content =
                editorText.text
                    .toString()

            file.writeText(
                content,
                Charsets.UTF_8
            )

            txtFileName.text =
                currentFileName

            addToRecentFiles(
                currentFileName
            )

            lastSavedOrRecoveredContent =
                content

            hasUnsavedChanges =
                false

            AutoSaveManager
                .clearRecovery(
                    this
                )

            scheduleSyntaxHighlighting()

            Toast.makeText(
                this,
                getString(
                    R.string.file_saved
                ),
                Toast.LENGTH_SHORT
            ).show()

        } catch (exception: Exception) {

            Toast.makeText(
                this,
                getString(
                    R.string.file_save_error
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showDeleteConfirmation() {
        if (currentFileName == DEFAULT_FILE_NAME) {
            Toast.makeText(
                this,
                getString(
                    R.string.save_before_delete
                ),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                getString(
                    R.string.delete_file_title
                )
            )
            .setMessage(
                getString(
                    R.string.delete_file_message,
                    currentFileName
                )
            )
            .setPositiveButton(
                getString(R.string.delete)
            ) { _, _ ->

                deleteCurrentFile()
            }
            .setNegativeButton(
                getString(R.string.cancel),
                null
            )
            .show()
    }

    private fun deleteCurrentFile() {
        try {
            val fileToDelete =
                File(
                    getTextEZFolder(),
                    currentFileName
                )

            if (
                fileToDelete.exists() &&
                !fileToDelete.delete()
            ) {
                showDeleteFailed()
                return
            }

            val historyDeleted =
                VersionManager.deleteVersionHistory(
                    context = this,
                    fileName = currentFileName
                )

            if (!historyDeleted) {
                showDeleteFailed()
                return
            }

            removeFromRecentFiles(
                currentFileName
            )

            removeReadOnlyState(
                currentFileName
            )

            AutoSaveManager.clearRecovery(
                this
            )

            resetEditorAfterDelete()

            Toast.makeText(
                this,
                getString(
                    R.string.file_deleted
                ),
                Toast.LENGTH_SHORT
            ).show()

        } catch (exception: Exception) {
            showDeleteFailed()
        }
    }

    private fun removeFromRecentFiles(
        fileName: String
    ) {
        val preferences =
            getSharedPreferences(
                RecentFilesManager.PREFERENCES_NAME,
                MODE_PRIVATE
            )

        val updatedRecentFiles =
            RecentFilesManager
                .getRecentFiles(preferences)
                .filterNot {
                    it == fileName
                }

        RecentFilesManager.saveRecentFiles(
            preferences,
            updatedRecentFiles
        )
    }

    private fun removeReadOnlyState(
        fileName: String
    ) {
        val preferences =
            getSharedPreferences(
                READ_ONLY_PREFERENCES,
                MODE_PRIVATE
            )

        preferences.edit()
            .remove(
                READ_ONLY_KEY_PREFIX +
                        fileName
            )
            .apply()
    }

    private fun resetEditorAfterDelete() {
        isUndoRedoAction = true

        editorText.setText("")

        isUndoRedoAction = false

        currentFileName =
            DEFAULT_FILE_NAME

        txtFileName.text =
            currentFileName

        isReadOnly = false

        undoStack.clear()
        redoStack.clear()

        previousText = ""

        lastSavedOrRecoveredContent = ""

        hasUnsavedChanges = false

        updateReadOnlyInterface()
        updateStatus()
        scheduleSyntaxHighlighting()
    }

    private fun showDeleteFailed() {
        Toast.makeText(
            this,
            getString(
                R.string.file_delete_failed
            ),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showSaveAsDialog() {

        if (isReadOnly) {
            return
        }

        val input =
            EditText(this)

        input.hint =
            getString(
                R.string.file_name_hint
            )

        if (
            currentFileName !=
            DEFAULT_FILE_NAME
        ) {
            input.setText(
                currentFileName
            )

            input.selectAll()
        }

        AlertDialog.Builder(this)
            .setTitle(
                getString(
                    R.string.save_as
                )
            )
            .setView(input)
            .setPositiveButton(
                getString(
                    R.string.save
                )
            ) { _, _ ->

                val enteredName =
                    input.text
                        .toString()
                        .trim()

                if (
                    enteredName
                        .isBlank()
                ) {
                    Toast.makeText(
                        this,
                        getString(
                            R.string
                                .file_name_required
                        ),
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                val safeName =
                    sanitizeFileName(
                        enteredName
                    )

                if (safeName.isBlank()) {
                    Toast.makeText(
                        this,
                        getString(
                            R.string
                                .invalid_file_name
                        ),
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                val finalName =
                    addDefaultExtension(
                        safeName
                    )

                val targetFile =
                    File(
                        getTextEZFolder(),
                        finalName
                    )

                if (
                    targetFile.exists() &&
                    finalName !=
                    currentFileName
                ) {
                    showOverwriteConfirmation(
                        finalName
                    )
                } else {
                    currentFileName =
                        finalName

                    txtFileName.text =
                        currentFileName

                    isReadOnly =
                        false

                    saveReadOnlyState()
                    updateReadOnlyInterface()
                    saveCurrentFile()
                }
            }
            .setNegativeButton(
                getString(
                    R.string.cancel
                ),
                null
            )
            .show()
    }

    private fun sanitizeFileName(
        fileName: String
    ): String {

        return fileName.replace(
            Regex(
                """[\\/:*?"<>|]"""
            ),
            "_"
        )
    }

    private fun addDefaultExtension(
        fileName: String
    ): String {

        return if (
            fileName.contains(".")
        ) {
            fileName
        } else {
            "$fileName.txt"
        }
    }

    private fun showOverwriteConfirmation(
        fileName: String
    ) {
        AlertDialog.Builder(this)
            .setTitle(
                getString(
                    R.string.file_exists
                )
            )
            .setMessage(
                getString(
                    R.string.overwrite_message,
                    fileName
                )
            )
            .setPositiveButton(
                getString(
                    R.string.overwrite
                )
            ) { _, _ ->

                currentFileName =
                    fileName

                txtFileName.text =
                    currentFileName

                isReadOnly =
                    false

                saveReadOnlyState()
                updateReadOnlyInterface()
                saveCurrentFile()
            }
            .setNegativeButton(
                getString(
                    R.string.cancel
                ),
                null
            )
            .show()
    }

    private fun loadCurrentFile() {

        try {
            val file =
                File(
                    getTextEZFolder(),
                    currentFileName
                )

            if (!file.exists()) {
                Toast.makeText(
                    this,
                    getString(
                        R.string.file_not_found
                    ),
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            isUndoRedoAction =
                true

            val content =
                file.readText(
                    Charsets.UTF_8
                )

            editorText.setText(
                content
            )

            editorText.setSelection(
                content.length
            )

            isUndoRedoAction =
                false

            undoStack.clear()
            redoStack.clear()

            previousText =
                content

            lastSavedOrRecoveredContent =
                content

            hasUnsavedChanges =
                false

            addToRecentFiles(
                currentFileName
            )

            scheduleSyntaxHighlighting()

        } catch (exception: Exception) {

            isUndoRedoAction =
                false

            Toast.makeText(
                this,
                getString(
                    R.string.file_open_error
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showCreateVersionDialog() {

        if (
            currentFileName ==
            DEFAULT_FILE_NAME
        ) {
            Toast.makeText(
                this,
                getString(
                    R.string
                        .save_file_before_version
                ),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val input =
            EditText(this).apply {

                hint = getString(
                    R.string
                        .version_name_hint
                )
            }

        AlertDialog.Builder(this)
            .setTitle(
                getString(
                    R.string.create_version
                )
            )
            .setView(input)
            .setPositiveButton(
                getString(
                    R.string.create_version
                )
            ) { _, _ ->

                val versionName =
                    input.text
                        .toString()
                        .trim()

                if (
                    versionName.isBlank()
                ) {
                    Toast.makeText(
                        this,
                        getString(
                            R.string
                                .version_name_required
                        ),
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                createVersion(
                    versionName
                )
            }
            .setNegativeButton(
                getString(
                    R.string.cancel
                ),
                null
            )
            .show()
    }

    private fun createVersion(
        versionName: String
    ) {
        val version =
            VersionManager
                .createVersion(
                    context = this,
                    fileName =
                        currentFileName,
                    content =
                        editorText.text
                            .toString(),
                    versionName =
                        versionName
                )

        if (version != null) {
            Toast.makeText(
                this,
                getString(
                    R.string.version_created,
                    version.versionNumber
                ),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                getString(
                    R.string
                        .version_create_failed
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openVersionHistory() {

        if (
            currentFileName ==
            DEFAULT_FILE_NAME
        ) {
            Toast.makeText(
                this,
                getString(
                    R.string
                        .save_file_before_version
                ),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val historyIntent =
            Intent(
                this,
                VersionHistoryActivity::
                class.java
            ).apply {

                putExtra(
                    VersionHistoryActivity
                        .EXTRA_FILE_NAME,
                    currentFileName
                )
            }

        versionHistoryLauncher
            .launch(
                historyIntent
            )
    }

    private fun saveRecoveryIfNeeded() {

        if (!::editorText.isInitialized) {
            return
        }

        if (
            isReadOnly ||
            !hasUnsavedChanges
        ) {
            return
        }

        val content =
            editorText.text
                .toString()

        val recoverySaved =
            AutoSaveManager
                .saveRecovery(
                    context = this,
                    fileName =
                        currentFileName,
                    content =
                        content
                )

        if (recoverySaved) {
            lastSavedOrRecoveredContent =
                content

            hasUnsavedChanges =
                false

        } else {
            Toast.makeText(
                this,
                getString(
                    R.string.autosave_failed
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkForRecovery() {

        if (
            !AutoSaveManager
                .hasRecovery(this)
        ) {
            return
        }

        val recovery =
            AutoSaveManager
                .loadRecovery(
                    this
                ) ?: return

        val currentContent =
            editorText.text
                .toString()

        if (
            recovery.fileName ==
            currentFileName &&
            recovery.content ==
            currentContent
        ) {
            AutoSaveManager
                .clearRecovery(
                    this
                )

            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                getString(
                    R.string.recovery_title
                )
            )
            .setMessage(
                getString(
                    R.string.recovery_message,
                    recovery.fileName
                )
            )
            .setPositiveButton(
                getString(
                    R.string.restore
                )
            ) { _, _ ->

                isUndoRedoAction =
                    true

                currentFileName =
                    recovery.fileName

                txtFileName.text =
                    currentFileName

                editorText.setText(
                    recovery.content
                )

                editorText.setSelection(
                    recovery.content.length
                )

                isUndoRedoAction =
                    false

                undoStack.clear()
                redoStack.clear()

                previousText =
                    recovery.content

                lastSavedOrRecoveredContent =
                    recovery.content

                hasUnsavedChanges =
                    true

                loadReadOnlyState()
                updateReadOnlyInterface()
                updateStatus()
                scheduleSyntaxHighlighting()

                Toast.makeText(
                    this,
                    getString(
                        R.string
                            .recovery_restored
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(
                getString(
                    R.string.discard
                )
            ) { _, _ ->

                AutoSaveManager
                    .clearRecovery(
                        this
                    )

                lastSavedOrRecoveredContent =
                    editorText.text
                        .toString()

                hasUnsavedChanges =
                    false
            }
            .setCancelable(false)
            .show()
    }

    private fun addToRecentFiles(
        fileName: String
    ) {
        val preferences =
            getSharedPreferences(
                RecentFilesManager
                    .PREFERENCES_NAME,
                MODE_PRIVATE
            )

        val currentRecentFiles =
            RecentFilesManager
                .getRecentFiles(
                    preferences
                )
                .toMutableList()

        currentRecentFiles.remove(
            fileName
        )

        currentRecentFiles.add(
            0,
            fileName
        )

        val limitedFiles =
            currentRecentFiles.take(
                RecentFilesManager
                    .MAX_RECENT_FILES
            )

        RecentFilesManager
            .saveRecentFiles(
                preferences,
                limitedFiles
            )
    }

    private fun undoText() {

        if (isReadOnly) {
            return
        }

        if (undoStack.isEmpty()) {
            Toast.makeText(
                this,
                getString(
                    R.string.nothing_to_undo
                ),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        isUndoRedoAction =
            true

        val currentText =
            editorText.text
                .toString()

        redoStack.add(
            currentText
        )

        val previous =
            undoStack.removeAt(
                undoStack.lastIndex
            )

        editorText.setText(
            previous
        )

        editorText.setSelection(
            previous.length
        )

        isUndoRedoAction =
            false

        hasUnsavedChanges =
            previous !=
                    lastSavedOrRecoveredContent

        updateStatus()
        scheduleSyntaxHighlighting()
    }

    private fun redoText() {

        if (isReadOnly) {
            return
        }

        if (redoStack.isEmpty()) {
            Toast.makeText(
                this,
                getString(
                    R.string.nothing_to_redo
                ),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        isUndoRedoAction =
            true

        val currentText =
            editorText.text
                .toString()

        undoStack.add(
            currentText
        )

        val next =
            redoStack.removeAt(
                redoStack.lastIndex
            )

        editorText.setText(
            next
        )

        editorText.setSelection(
            next.length
        )

        isUndoRedoAction =
            false

        hasUnsavedChanges =
            next !=
                    lastSavedOrRecoveredContent

        updateStatus()
        scheduleSyntaxHighlighting()
    }

    private fun showSearchDialog() {

        val input =
            EditText(this)

        input.hint =
            getString(
                R.string.search_word
            )

        AlertDialog.Builder(this)
            .setTitle(
                getString(
                    R.string.search
                )
            )
            .setView(input)
            .setPositiveButton(
                getString(
                    R.string.find
                )
            ) { _, _ ->

                searchText(
                    input.text
                        .toString()
                )
            }
            .setNegativeButton(
                getString(
                    R.string.cancel
                ),
                null
            )
            .show()
    }

    private fun searchText(
        keyword: String
    ) {
        if (keyword.isBlank()) {
            return
        }

        val content =
            editorText.text
                .toString()

        val startPosition =
            editorText.selectionEnd
                .coerceAtLeast(0)

        var index =
            content.indexOf(
                string = keyword,
                startIndex =
                    startPosition,
                ignoreCase = true
            )

        if (index == -1) {
            index =
                content.indexOf(
                    string = keyword,
                    startIndex = 0,
                    ignoreCase = true
                )
        }

        if (index != -1) {
            editorText.requestFocus()

            editorText.setSelection(
                index,
                index + keyword.length
            )
        } else {
            Toast.makeText(
                this,
                getString(
                    R.string.text_not_found
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showReplaceDialog() {

        if (isReadOnly) {
            return
        }

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    40,
                    16,
                    40,
                    0
                )
            }

        val searchInput =
            EditText(this).apply {

                hint = getString(
                    R.string.search_word
                )
            }

        val replaceInput =
            EditText(this).apply {

                hint = getString(
                    R.string.replace_with
                )
            }

        layout.addView(
            searchInput
        )

        layout.addView(
            replaceInput
        )

        AlertDialog.Builder(this)
            .setTitle(
                getString(
                    R.string.replace
                )
            )
            .setView(layout)
            .setPositiveButton(
                getString(
                    R.string.replace_one
                )
            ) { _, _ ->

                replaceOne(
                    searchInput.text
                        .toString(),

                    replaceInput.text
                        .toString()
                )
            }
            .setNeutralButton(
                getString(
                    R.string.replace_all
                )
            ) { _, _ ->

                replaceAll(
                    searchInput.text
                        .toString(),

                    replaceInput.text
                        .toString()
                )
            }
            .setNegativeButton(
                getString(
                    R.string.cancel
                ),
                null
            )
            .show()
    }

    private fun replaceOne(
        search: String,
        replacement: String
    ) {
        if (
            isReadOnly ||
            search.isBlank()
        ) {
            return
        }

        val content =
            editorText.text
                .toString()

        val selectionStart =
            editorText.selectionStart

        val selectionEnd =
            editorText.selectionEnd

        if (
            selectionStart >= 0 &&
            selectionEnd >
            selectionStart &&
            content.substring(
                selectionStart,
                selectionEnd
            ).equals(
                search,
                ignoreCase = true
            )
        ) {
            editorText.text
                .replace(
                    selectionStart,
                    selectionEnd,
                    replacement
                )

            return
        }

        val index =
            content.indexOf(
                search,
                ignoreCase = true
            )

        if (index == -1) {
            Toast.makeText(
                this,
                getString(
                    R.string.text_not_found
                ),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        editorText.text
            .replace(
                index,
                index + search.length,
                replacement
            )
    }

    private fun replaceAll(
        search: String,
        replacement: String
    ) {
        if (
            isReadOnly ||
            search.isBlank()
        ) {
            return
        }

        val content =
            editorText.text
                .toString()

        if (
            !content.contains(
                search,
                ignoreCase = true
            )
        ) {
            Toast.makeText(
                this,
                getString(
                    R.string.text_not_found
                ),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val newContent =
            content.replace(
                oldValue = search,
                newValue = replacement,
                ignoreCase = true
            )

        editorText.setText(
            newContent
        )

        editorText.setSelection(
            newContent.length
        )

        hasUnsavedChanges =
            newContent !=
                    lastSavedOrRecoveredContent

        scheduleSyntaxHighlighting()
    }

    private fun updateStatus() {

        val text =
            editorText.text
                .toString()

        val lines =
            if (text.isEmpty()) {
                1
            } else {
                text.lines().size
            }

        txtStatus.text =
            if (isReadOnly) {
                getString(
                    R.string.status_read_only,
                    lines,
                    text.length
                )
            } else {
                getString(
                    R.string.status_editable,
                    lines,
                    text.length
                )
            }
    }

    override fun onPause() {

        saveRecoveryIfNeeded()

        super.onPause()
    }

    override fun onDestroy() {

        syntaxHandler
            .removeCallbacks(
                syntaxHighlightRunnable
            )

        autoSaveHandler
            .removeCallbacks(
                autoSaveRunnable
            )

        super.onDestroy()
    }
}
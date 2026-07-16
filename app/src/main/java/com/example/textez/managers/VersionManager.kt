package com.example.textez.managers

import android.content.Context
import com.example.textez.models.Version
import java.io.File
import java.util.Properties

object VersionManager {

    private const val VERSIONS_FOLDER =
        "TextEZVersions"

    private const val DOCUMENTS_FOLDER =
        "TextEZ"

    private const val BASE_EXTENSION =
        ".base"

    private const val PATCH_EXTENSION =
        ".patch"

    private const val METADATA_EXTENSION =
        ".properties"

    private const val STORAGE_BASE =
        "BASE"

    private const val STORAGE_PATCH =
        "PATCH"

    /**
     * Creates a new version for a saved document.
     *
     * The first version is stored as a complete base file.
     * Every later version is stored as a patch relative to
     * the immediately previous version.
     */
    fun createVersion(
        context: Context,
        fileName: String,
        content: String,
        versionName: String
    ): Version? {

        return try {
            val versionFolder =
                getVersionFolder(
                    context = context,
                    fileName = fileName
                )

            val existingVersions =
                getVersions(
                    context = context,
                    fileName = fileName
                )

            val nextVersionNumber =
                (
                        existingVersions.maxOfOrNull {
                            it.versionNumber
                        } ?: 0
                        ) + 1

            val formattedNumber =
                nextVersionNumber
                    .toString()
                    .padStart(
                        length = 4,
                        padChar = '0'
                    )

            val createdAt =
                System.currentTimeMillis()

            val previousVersion =
                existingVersions.maxByOrNull {
                    it.versionNumber
                }

            val isBaseVersion =
                previousVersion == null

            val storageFileName =
                if (isBaseVersion) {
                    "version_$formattedNumber$BASE_EXTENSION"
                } else {
                    "version_$formattedNumber$PATCH_EXTENSION"
                }

            val storageFile =
                File(
                    versionFolder,
                    storageFileName
                )

            if (isBaseVersion) {
                storageFile.writeText(
                    text = content,
                    charset = Charsets.UTF_8
                )
            } else {
                val previousContent =
                    loadVersionContent(
                        context = context,
                        version = previousVersion
                    ) ?: return null

                val patchText =
                    LineDiffManager.createPatch(
                        oldText = previousContent,
                        newText = content
                    )

                storageFile.writeText(
                    text = patchText,
                    charset = Charsets.UTF_8
                )
            }

            val metadataFileName =
                "version_$formattedNumber$METADATA_EXTENSION"

            val metadataFile =
                File(
                    versionFolder,
                    metadataFileName
                )

            val properties =
                Properties().apply {

                    setProperty(
                        "version_number",
                        nextVersionNumber.toString()
                    )

                    setProperty(
                        "version_name",
                        versionName
                    )

                    setProperty(
                        "file_name",
                        fileName
                    )

                    setProperty(
                        "storage_file",
                        storageFileName
                    )

                    setProperty(
                        "storage_type",
                        if (isBaseVersion) {
                            STORAGE_BASE
                        } else {
                            STORAGE_PATCH
                        }
                    )

                    setProperty(
                        "created_at",
                        createdAt.toString()
                    )

                    setProperty(
                        "previous_version",
                        previousVersion
                            ?.versionNumber
                            ?.toString()
                            .orEmpty()
                    )
                }

            metadataFile
                .outputStream()
                .use { outputStream ->

                    properties.store(
                        outputStream,
                        "TextEZ version metadata"
                    )
                }

            Version(
                versionNumber =
                    nextVersionNumber,

                versionName =
                    versionName,

                fileName =
                    fileName,

                storageFileName =
                    storageFileName,

                createdAt =
                    createdAt,

                isBaseVersion =
                    isBaseVersion,

                previousVersionNumber =
                    previousVersion
                        ?.versionNumber
            )

        } catch (exception: Exception) {
            null
        }
    }

    /**
     * Returns all versions for a document.
     *
     * The newest version appears first.
     */
    fun getVersions(
        context: Context,
        fileName: String
    ): List<Version> {

        return try {
            val versionFolder =
                getVersionFolder(
                    context = context,
                    fileName = fileName
                )

            versionFolder
                .listFiles()
                ?.filter { file ->

                    file.isFile &&
                            file.name.endsWith(
                                suffix = METADATA_EXTENSION,
                                ignoreCase = true
                            )
                }
                ?.mapNotNull { metadataFile ->

                    readVersionMetadata(
                        metadataFile
                    )
                }
                ?.sortedByDescending { version ->

                    version.versionNumber
                }
                .orEmpty()

        } catch (exception: Exception) {
            emptyList()
        }
    }

    /**
     * Reconstructs and returns the complete content of a
     * selected version.
     *
     * It starts from the base file and applies patches in
     * version-number order until the requested version is reached.
     */
    fun loadVersionContent(
        context: Context,
        version: Version
    ): String? {

        return try {
            val orderedVersions =
                getVersions(
                    context = context,
                    fileName = version.fileName
                ).sortedBy { item ->

                    item.versionNumber
                }

            var reconstructedContent: String? =
                null

            for (item in orderedVersions) {

                if (item.isBaseVersion) {
                    val baseFile =
                        File(
                            getVersionFolder(
                                context = context,
                                fileName = item.fileName
                            ),
                            item.storageFileName
                        )

                    if (!baseFile.exists()) {
                        return null
                    }

                    reconstructedContent =
                        baseFile.readText(
                            charset = Charsets.UTF_8
                        )

                } else {
                    val previousContent =
                        reconstructedContent
                            ?: return null

                    val patchFile =
                        File(
                            getVersionFolder(
                                context = context,
                                fileName = item.fileName
                            ),
                            item.storageFileName
                        )

                    if (!patchFile.exists()) {
                        return null
                    }

                    val patchText =
                        patchFile.readText(
                            charset = Charsets.UTF_8
                        )

                    reconstructedContent =
                        LineDiffManager.applyPatch(
                            oldText = previousContent,
                            patchText = patchText
                        ) ?: return null
                }

                if (
                    item.versionNumber ==
                    version.versionNumber
                ) {
                    return reconstructedContent
                }
            }

            null

        } catch (exception: Exception) {
            null
        }
    }

    /**
     * Reads the currently saved document from normal TextEZ storage.
     */
    fun loadCurrentFileContent(
        context: Context,
        fileName: String
    ): String? {

        return try {
            val documentFolder =
                File(
                    context.filesDir,
                    DOCUMENTS_FOLDER
                )

            val documentFile =
                File(
                    documentFolder,
                    fileName
                )

            if (!documentFile.exists()) {
                null
            } else {
                documentFile.readText(
                    charset = Charsets.UTF_8
                )
            }

        } catch (exception: Exception) {
            null
        }
    }

    /**
     * Restores the selected version into the normal document file.
     *
     * Before replacing the current file, TextEZ creates an
     * automatic backup version when the content is different.
     */
    fun rollbackToVersion(
        context: Context,
        version: Version
    ): Boolean {

        return try {
            val restoredContent =
                loadVersionContent(
                    context = context,
                    version = version
                ) ?: return false

            val documentFolder =
                File(
                    context.filesDir,
                    DOCUMENTS_FOLDER
                )

            if (!documentFolder.exists()) {
                documentFolder.mkdirs()
            }

            val currentFile =
                File(
                    documentFolder,
                    version.fileName
                )

            val currentContent =
                if (currentFile.exists()) {
                    currentFile.readText(
                        charset = Charsets.UTF_8
                    )
                } else {
                    ""
                }

            if (
                currentFile.exists() &&
                currentContent != restoredContent
            ) {
                val backupVersion =
                    createVersion(
                        context = context,
                        fileName = version.fileName,
                        content = currentContent,
                        versionName =
                            "Automatic backup before rollback"
                    )

                if (backupVersion == null) {
                    return false
                }
            }

            currentFile.writeText(
                text = restoredContent,
                charset = Charsets.UTF_8
            )

            true

        } catch (exception: Exception) {
            false
        }
    }

    /**
     * Reads one .properties metadata file and converts it
     * into a Version object.
     */
    private fun readVersionMetadata(
        metadataFile: File
    ): Version? {

        return try {
            val properties =
                Properties()

            metadataFile
                .inputStream()
                .use { inputStream ->

                    properties.load(
                        inputStream
                    )
                }

            val versionNumber =
                properties
                    .getProperty(
                        "version_number"
                    )
                    ?.toIntOrNull()
                    ?: return null

            val versionName =
                properties
                    .getProperty(
                        "version_name"
                    )
                    .orEmpty()

            val fileName =
                properties
                    .getProperty(
                        "file_name"
                    )
                    .orEmpty()

            val storageFileName =
                properties
                    .getProperty(
                        "storage_file"
                    )
                    .orEmpty()

            val storageType =
                properties
                    .getProperty(
                        "storage_type"
                    )
                    .orEmpty()

            val createdAt =
                properties
                    .getProperty(
                        "created_at"
                    )
                    ?.toLongOrNull()
                    ?: 0L

            val previousVersionNumber =
                properties
                    .getProperty(
                        "previous_version"
                    )
                    ?.toIntOrNull()

            if (
                fileName.isBlank() ||
                storageFileName.isBlank()
            ) {
                return null
            }

            Version(
                versionNumber =
                    versionNumber,

                versionName =
                    versionName,

                fileName =
                    fileName,

                storageFileName =
                    storageFileName,

                createdAt =
                    createdAt,

                isBaseVersion =
                    storageType == STORAGE_BASE,

                previousVersionNumber =
                    previousVersionNumber
            )

        } catch (exception: Exception) {
            null
        }
    }


    /**
     * Deletes all version-control files belonging to one document.
     */
    fun deleteVersionHistory(
        context: Context,
        fileName: String
    ): Boolean {
        return try {
            val versionFolder =
                getVersionFolder(
                    context = context,
                    fileName = fileName
                )

            if (!versionFolder.exists()) {
                true
            } else {
                versionFolder.deleteRecursively()
            }

        } catch (exception: Exception) {
            false
        }
    }



    /**
     * Creates and returns the version directory for one document.
     */
    private fun getVersionFolder(
        context: Context,
        fileName: String
    ): File {

        val versionsRootFolder =
            File(
                context.filesDir,
                VERSIONS_FOLDER
            )

        if (!versionsRootFolder.exists()) {
            versionsRootFolder.mkdirs()
        }

        val safeFolderName =
            fileName.replace(
                regex =
                    Regex(
                        """[\\/:*?"<>|.]"""
                    ),
                replacement = "_"
            )

        val documentVersionFolder =
            File(
                versionsRootFolder,
                safeFolderName
            )

        if (!documentVersionFolder.exists()) {
            documentVersionFolder.mkdirs()
        }

        return documentVersionFolder
    }
}
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

    fun createVersion(
        context: Context,
        fileName: String,
        content: String,
        versionName: String
    ): Version? {

        return try {
            val folder =
                getVersionFolder(
                    context,
                    fileName
                )

            val currentVersions =
                getVersions(
                    context,
                    fileName
                )

            val nextNumber =
                (
                        currentVersions
                            .maxOfOrNull {
                                it.versionNumber
                            } ?: 0
                        ) + 1

            val numberText =
                nextNumber
                    .toString()
                    .padStart(4, '0')

            val createdAt =
                System.currentTimeMillis()

            val previousVersion =
                currentVersions
                    .maxByOrNull {
                        it.versionNumber
                    }

            val isBase =
                previousVersion == null

            val storageFileName =
                if (isBase) {
                    "version_$numberText$BASE_EXTENSION"
                } else {
                    "version_$numberText$PATCH_EXTENSION"
                }

            val storageFile =
                File(
                    folder,
                    storageFileName
                )

            if (isBase) {
                storageFile.writeText(
                    content,
                    Charsets.UTF_8
                )
            } else {
                val previousContent =
                    loadVersionContent(
                        context,
                        previousVersion
                    ) ?: return null

                val patch =
                    LineDiffManager.createPatch(
                        previousContent,
                        content
                    )

                storageFile.writeText(
                    patch,
                    Charsets.UTF_8
                )
            }

            val metadataFile =
                File(
                    folder,
                    "version_$numberText$METADATA_EXTENSION"
                )

            val properties =
                Properties().apply {

                    setProperty(
                        "version_number",
                        nextNumber.toString()
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
                        if (isBase) {
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
                .use { output ->

                    properties.store(
                        output,
                        "TextEZ version metadata"
                    )
                }

            Version(
                versionNumber =
                    nextNumber,

                versionName =
                    versionName,

                fileName =
                    fileName,

                storageFileName =
                    storageFileName,

                createdAt =
                    createdAt,

                isBaseVersion =
                    isBase,

                previousVersionNumber =
                    previousVersion
                        ?.versionNumber
            )

        } catch (exception: Exception) {
            null
        }
    }

    fun getVersions(
        context: Context,
        fileName: String
    ): List<Version> {

        return try {
            val folder =
                getVersionFolder(
                    context,
                    fileName
                )

            folder
                .listFiles()
                ?.filter { file ->

                    file.isFile &&
                            file.name.endsWith(
                                METADATA_EXTENSION
                            )
                }
                ?.mapNotNull { file ->
                    readVersionMetadata(file)
                }
                ?.sortedByDescending {
                    it.versionNumber
                }
                .orEmpty()

        } catch (exception: Exception) {
            emptyList()
        }
    }

    fun loadVersionContent(
        context: Context,
        version: Version
    ): String? {

        return try {
            val ascendingVersions =
                getVersions(
                    context,
                    version.fileName
                ).sortedBy {
                    it.versionNumber
                }

            var reconstructedContent:
                    String? = null

            for (item in ascendingVersions) {

                if (item.isBaseVersion) {

                    val baseFile =
                        File(
                            getVersionFolder(
                                context,
                                item.fileName
                            ),
                            item.storageFileName
                        )

                    if (!baseFile.exists()) {
                        return null
                    }

                    reconstructedContent =
                        baseFile.readText(
                            Charsets.UTF_8
                        )

                } else {
                    val previousContent =
                        reconstructedContent
                            ?: return null

                    val patchFile =
                        File(
                            getVersionFolder(
                                context,
                                item.fileName
                            ),
                            item.storageFileName
                        )

                    if (!patchFile.exists()) {
                        return null
                    }

                    reconstructedContent =
                        LineDiffManager.applyPatch(
                            previousContent,
                            patchFile.readText(
                                Charsets.UTF_8
                            )
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

    fun loadCurrentFileContent(
        context: Context,
        fileName: String
    ): String? {

        return try {
            val file =
                File(
                    File(
                        context.filesDir,
                        DOCUMENTS_FOLDER
                    ),
                    fileName
                )

            if (!file.exists()) {
                null
            } else {
                file.readText(
                    Charsets.UTF_8
                )
            }

        } catch (exception: Exception) {
            null
        }
    }

    fun rollbackToVersion(
        context: Context,
        version: Version
    ): Boolean {

        return try {
            val versionContent =
                loadVersionContent(
                    context,
                    version
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
                        Charsets.UTF_8
                    )
                } else {
                    ""
                }

            /*
             * Protect the current state by creating
             * another version before rollback.
             */
            if (
                currentFile.exists() &&
                currentContent != versionContent
            ) {
                createVersion(
                    context = context,
                    fileName =
                        version.fileName,
                    content =
                        currentContent,
                    versionName =
                        "Automatic backup before rollback"
                )
            }

            currentFile.writeText(
                versionContent,
                Charsets.UTF_8
            )

            true

        } catch (exception: Exception) {
            false
        }
    }

    private fun readVersionMetadata(
        metadataFile: File
    ): Version? {

        return try {
            val properties =
                Properties()

            metadataFile
                .inputStream()
                .use { input ->

                    properties.load(input)
                }

            val versionNumber =
                properties.getProperty(
                    "version_number"
                )?.toIntOrNull()
                    ?: return null

            val versionName =
                properties.getProperty(
                    "version_name"
                ).orEmpty()

            val fileName =
                properties.getProperty(
                    "file_name"
                ).orEmpty()

            val storageFileName =
                properties.getProperty(
                    "storage_file"
                ).orEmpty()

            val storageType =
                properties.getProperty(
                    "storage_type"
                ).orEmpty()

            val createdAt =
                properties.getProperty(
                    "created_at"
                )?.toLongOrNull()
                    ?: 0L

            val previousVersion =
                properties.getProperty(
                    "previous_version"
                )?.toIntOrNull()

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
                    previousVersion
            )

        } catch (exception: Exception) {
            null
        }
    }

    private fun getVersionFolder(
        context: Context,
        fileName: String
    ): File {

        val rootFolder =
            File(
                context.filesDir,
                VERSIONS_FOLDER
            )

        if (!rootFolder.exists()) {
            rootFolder.mkdirs()
        }

        val safeFolderName =
            fileName.replace(
                Regex("""[\\/:*?"<>|.]"""),
                "_"
            )

        val fileFolder =
            File(
                rootFolder,
                safeFolderName
            )

        if (!fileFolder.exists()) {
            fileFolder.mkdirs()
        }

        return fileFolder
    }
}
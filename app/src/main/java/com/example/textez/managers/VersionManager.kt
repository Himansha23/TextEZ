package com.example.textez.managers

import android.content.Context
import com.example.textez.models.Version
import java.io.File
import java.util.Properties

object VersionManager {

    private const val VERSIONS_FOLDER = "TextEZVersions"
    private const val SNAPSHOT_EXTENSION = ".snapshot"
    private const val METADATA_EXTENSION = ".properties"

    fun createVersion(
        context: Context,
        fileName: String,
        content: String,
        versionName: String
    ): Version? {
        return try {
            val fileFolder = getVersionFolder(
                context,
                fileName
            )

            val nextVersionNumber =
                getNextVersionNumber(fileFolder)

            val numberText =
                nextVersionNumber.toString()
                    .padStart(4, '0')

            val snapshotFileName =
                "version_$numberText$SNAPSHOT_EXTENSION"

            val metadataFileName =
                "version_$numberText$METADATA_EXTENSION"

            val snapshotFile = File(
                fileFolder,
                snapshotFileName
            )

            val metadataFile = File(
                fileFolder,
                metadataFileName
            )

            snapshotFile.writeText(
                content,
                Charsets.UTF_8
            )

            val createdAt =
                System.currentTimeMillis()

            val properties = Properties().apply {
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
                    "snapshot_file",
                    snapshotFileName
                )

                setProperty(
                    "created_at",
                    createdAt.toString()
                )
            }

            metadataFile.outputStream().use {
                properties.store(
                    it,
                    "TextEZ version metadata"
                )
            }

            Version(
                versionNumber = nextVersionNumber,
                versionName = versionName,
                fileName = fileName,
                snapshotFileName = snapshotFileName,
                createdAt = createdAt
            )

        } catch (exception: Exception) {
            null
        }
    }

    private fun getVersionFolder(
        context: Context,
        fileName: String
    ): File {
        val rootFolder = File(
            context.filesDir,
            VERSIONS_FOLDER
        )

        if (!rootFolder.exists()) {
            rootFolder.mkdirs()
        }

        val safeFolderName = fileName.replace(
            Regex("""[\\/:*?"<>|.]"""),
            "_"
        )

        val fileFolder = File(
            rootFolder,
            safeFolderName
        )

        if (!fileFolder.exists()) {
            fileFolder.mkdirs()
        }

        return fileFolder
    }

    private fun getNextVersionNumber(
        folder: File
    ): Int {
        val existingNumbers = folder
            .listFiles()
            ?.filter {
                it.isFile &&
                        it.name.endsWith(
                            METADATA_EXTENSION
                        )
            }
            ?.mapNotNull { file ->
                Regex(
                    """version_(\d{4})\.properties"""
                )
                    .matchEntire(file.name)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
            }
            .orEmpty()

        return (existingNumbers.maxOrNull() ?: 0) + 1
    }
}
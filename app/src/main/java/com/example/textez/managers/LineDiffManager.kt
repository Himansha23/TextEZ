package com.example.textez.managers

import android.util.Base64

object LineDiffManager {

    enum class DiffType {
        UNCHANGED,
        ADDED,
        REMOVED
    }

    data class DiffLine(
        val type: DiffType,
        val text: String
    )

    private const val KEEP = "K"
    private const val ADD = "A"
    private const val DELETE = "D"
    private const val SEPARATOR = "|"

    fun calculateDiff(
        oldText: String,
        newText: String
    ): List<DiffLine> {

        val oldLines = toLines(oldText)
        val newLines = toLines(newText)

        val lcs = Array(oldLines.size + 1) {
            IntArray(newLines.size + 1)
        }

        for (i in oldLines.indices.reversed()) {
            for (j in newLines.indices.reversed()) {

                lcs[i][j] =
                    if (oldLines[i] == newLines[j]) {
                        lcs[i + 1][j + 1] + 1
                    } else {
                        maxOf(
                            lcs[i + 1][j],
                            lcs[i][j + 1]
                        )
                    }
            }
        }

        val result =
            mutableListOf<DiffLine>()

        var oldIndex = 0
        var newIndex = 0

        while (
            oldIndex < oldLines.size ||
            newIndex < newLines.size
        ) {
            when {
                oldIndex < oldLines.size &&
                        newIndex < newLines.size &&
                        oldLines[oldIndex] ==
                        newLines[newIndex] -> {

                    result.add(
                        DiffLine(
                            DiffType.UNCHANGED,
                            oldLines[oldIndex]
                        )
                    )

                    oldIndex++
                    newIndex++
                }

                newIndex < newLines.size &&
                        (
                                oldIndex >= oldLines.size ||
                                        lcs[oldIndex][newIndex + 1] >=
                                        lcs[oldIndex + 1][newIndex]
                                ) -> {

                    result.add(
                        DiffLine(
                            DiffType.ADDED,
                            newLines[newIndex]
                        )
                    )

                    newIndex++
                }

                oldIndex < oldLines.size -> {

                    result.add(
                        DiffLine(
                            DiffType.REMOVED,
                            oldLines[oldIndex]
                        )
                    )

                    oldIndex++
                }
            }
        }

        return result
    }

    fun createPatch(
        oldText: String,
        newText: String
    ): String {

        return calculateDiff(
            oldText,
            newText
        ).joinToString("\n") { line ->

            val operation =
                when (line.type) {
                    DiffType.UNCHANGED -> KEEP
                    DiffType.ADDED -> ADD
                    DiffType.REMOVED -> DELETE
                }

            "$operation$SEPARATOR${encode(line.text)}"
        }
    }

    fun applyPatch(
        oldText: String,
        patchText: String
    ): String? {

        val oldLines = toLines(oldText)

        val outputLines =
            mutableListOf<String>()

        var oldIndex = 0

        if (patchText.isBlank()) {
            return oldText
        }

        for (patchLine in patchText.lineSequence()) {

            val separatorIndex =
                patchLine.indexOf(SEPARATOR)

            if (separatorIndex <= 0) {
                return null
            }

            val operation =
                patchLine.substring(
                    0,
                    separatorIndex
                )

            val encodedText =
                patchLine.substring(
                    separatorIndex + 1
                )

            val lineText = try {
                decode(encodedText)
            } catch (exception: Exception) {
                return null
            }

            when (operation) {
                KEEP -> {
                    if (
                        oldIndex >= oldLines.size ||
                        oldLines[oldIndex] != lineText
                    ) {
                        return null
                    }

                    outputLines.add(lineText)
                    oldIndex++
                }

                DELETE -> {
                    if (
                        oldIndex >= oldLines.size ||
                        oldLines[oldIndex] != lineText
                    ) {
                        return null
                    }

                    oldIndex++
                }

                ADD -> {
                    outputLines.add(lineText)
                }

                else -> return null
            }
        }

        if (oldIndex != oldLines.size) {
            return null
        }

        return outputLines.joinToString("\n")
    }

    fun formatDiff(
        oldText: String,
        newText: String
    ): String {

        return calculateDiff(
            oldText,
            newText
        ).joinToString("\n") { line ->

            when (line.type) {
                DiffType.UNCHANGED ->
                    "  ${line.text}"

                DiffType.ADDED ->
                    "+ ${line.text}"

                DiffType.REMOVED ->
                    "- ${line.text}"
            }
        }
    }

    private fun toLines(
        text: String
    ): List<String> {

        return if (text.isEmpty()) {
            emptyList()
        } else {
            text.split("\n")
        }
    }

    private fun encode(
        value: String
    ): String {

        return Base64.encodeToString(
            value.toByteArray(
                Charsets.UTF_8
            ),
            Base64.NO_WRAP
        )
    }

    private fun decode(
        value: String
    ): String {

        return String(
            Base64.decode(
                value,
                Base64.NO_WRAP
            ),
            Charsets.UTF_8
        )
    }
}
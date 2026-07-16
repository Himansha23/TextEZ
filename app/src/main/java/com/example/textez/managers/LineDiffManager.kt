package com.example.textez.managers

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator

object LineDiffManager {

    private const val ORIGINAL_FILE_NAME =
        "previous-version.txt"

    private const val REVISED_FILE_NAME =
        "new-version.txt"

    /*
     * Creates a standard unified-diff patch.
     *
     * This patch is stored in the .patch file instead of
     * storing another complete copy of the document.
     */
    fun createPatch(
        oldText: String,
        newText: String
    ): String {
        val originalLines =
            textToLines(oldText)

        val revisedLines =
            textToLines(newText)

        val patch = DiffUtils.diff(
            originalLines,
            revisedLines
        )

        val unifiedDiff =
            UnifiedDiffUtils.generateUnifiedDiff(
                ORIGINAL_FILE_NAME,
                REVISED_FILE_NAME,
                originalLines,
                patch,
                0
            )

        return unifiedDiff.joinToString("\n")
    }

    /*
     * Reconstructs the next version by applying the stored
     * unified patch to the previous version.
     */
    fun applyPatch(
        oldText: String,
        patchText: String
    ): String? {
        return try {
            if (patchText.isBlank()) {
                return oldText
            }

            val originalLines =
                textToLines(oldText)

            val unifiedDiffLines =
                patchText.split("\n")

            val patch =
                UnifiedDiffUtils.parseUnifiedDiff(
                    unifiedDiffLines
                )

            val revisedLines =
                DiffUtils.patch(
                    originalLines,
                    patch
                )

            revisedLines.joinToString("\n")

        } catch (exception: Exception) {
            null
        }
    }

    /*
     * Generates the readable comparison displayed in the
     * Version History screen.
     *
     *   unchanged line
     * - removed line
     * + added line
     */
    fun formatDiff(
        oldText: String,
        newText: String
    ): String {
        val generator =
            DiffRowGenerator.create()
                .showInlineDiffs(false)
                .build()

        val rows =
            generator.generateDiffRows(
                textToLines(oldText),
                textToLines(newText)
            )

        if (rows.isEmpty()) {
            return "No differences"
        }

        return rows.joinToString("\n") { row ->

            when (row.tag) {
                DiffRow.Tag.EQUAL -> {
                    "  ${row.oldLine}"
                }

                DiffRow.Tag.DELETE -> {
                    "- ${row.oldLine}"
                }

                DiffRow.Tag.INSERT -> {
                    "+ ${row.newLine}"
                }

                DiffRow.Tag.CHANGE -> {
                    buildString {
                        append("- ")
                        append(row.oldLine)
                        append("\n+ ")
                        append(row.newLine)
                    }
                }
            }
        }
    }

    private fun textToLines(
        text: String
    ): List<String> {
        return if (text.isEmpty()) {
            emptyList()
        } else {
            text.split("\n")
        }
    }
}
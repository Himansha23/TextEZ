package com.example.textez.managers

import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

object MarkdownSyntaxHighlighter {

    private val headingColor = Color.parseColor("#7C3AED")
    private val boldColor = Color.parseColor("#1D4ED8")
    private val italicColor = Color.parseColor("#C2410C")
    private val codeColor = Color.parseColor("#047857")
    private val linkColor = Color.parseColor("#0369A1")
    private val quoteColor = Color.parseColor("#6B7280")
    private val listColor = Color.parseColor("#B45309")
    private val ruleColor = Color.parseColor("#9CA3AF")

    private val codeBackgroundColor = Color.parseColor("#E5E7EB")

    private val headingPattern = Regex(
        pattern = "^#{1,6}\\s+.*$",
        option = RegexOption.MULTILINE
    )

    private val boldPattern = Regex(
        pattern = "(\\*\\*|__)(.+?)\\1"
    )

    private val italicPattern = Regex(
        pattern = "(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)|(?<!_)_([^_\\n]+)_(?!_)"
    )

    private val inlineCodePattern = Regex(
        pattern = "`([^`\\n]+)`"
    )

    private val codeBlockPattern = Regex(
        pattern = "```.*?```",
        options = setOf(
            RegexOption.DOT_MATCHES_ALL,
            RegexOption.MULTILINE
        )
    )

    private val linkPattern = Regex(
        pattern = "\\[[^]]+]\\([^)]+\\)"
    )

    private val blockQuotePattern = Regex(
        pattern = "^>\\s?.*$",
        option = RegexOption.MULTILINE
    )

    private val unorderedListPattern = Regex(
        pattern = "^\\s*[-+*]\\s+",
        option = RegexOption.MULTILINE
    )

    private val orderedListPattern = Regex(
        pattern = "^\\s*\\d+\\.\\s+",
        option = RegexOption.MULTILINE
    )

    private val horizontalRulePattern = Regex(
        pattern = "^\\s*((---+)|(\\*\\*\\*+)|(___+))\\s*$",
        option = RegexOption.MULTILINE
    )

    fun highlight(editable: Editable) {
        clear(editable)

        applyColor(
            editable,
            headingPattern,
            headingColor
        )

        applyStyle(
            editable,
            headingPattern,
            Typeface.BOLD
        )

        applyColor(
            editable,
            boldPattern,
            boldColor
        )

        applyStyle(
            editable,
            boldPattern,
            Typeface.BOLD
        )

        applyColor(
            editable,
            italicPattern,
            italicColor
        )

        applyStyle(
            editable,
            italicPattern,
            Typeface.ITALIC
        )

        applyColor(
            editable,
            linkPattern,
            linkColor
        )

        applyColor(
            editable,
            blockQuotePattern,
            quoteColor
        )

        applyStyle(
            editable,
            blockQuotePattern,
            Typeface.ITALIC
        )

        applyColor(
            editable,
            unorderedListPattern,
            listColor
        )

        applyColor(
            editable,
            orderedListPattern,
            listColor
        )

        applyColor(
            editable,
            horizontalRulePattern,
            ruleColor
        )

        applyColor(
            editable,
            inlineCodePattern,
            codeColor
        )

        applyBackground(
            editable,
            inlineCodePattern,
            codeBackgroundColor
        )

        /*
         * Code blocks are applied last so their color overrides
         * Markdown symbols that appear inside the code block.
         */
        applyColor(
            editable,
            codeBlockPattern,
            codeColor
        )

        applyBackground(
            editable,
            codeBlockPattern,
            codeBackgroundColor
        )
    }

    fun clear(editable: Editable) {
        val colorSpans = editable.getSpans(
            0,
            editable.length,
            ForegroundColorSpan::class.java
        )

        colorSpans.forEach { span ->
            editable.removeSpan(span)
        }

        val styleSpans = editable.getSpans(
            0,
            editable.length,
            StyleSpan::class.java
        )

        styleSpans.forEach { span ->
            editable.removeSpan(span)
        }

        val backgroundSpans = editable.getSpans(
            0,
            editable.length,
            BackgroundColorSpan::class.java
        )

        backgroundSpans.forEach { span ->
            editable.removeSpan(span)
        }
    }

    private fun applyColor(
        editable: Editable,
        pattern: Regex,
        color: Int
    ) {
        pattern.findAll(editable.toString()).forEach { match ->

            editable.setSpan(
                ForegroundColorSpan(color),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun applyStyle(
        editable: Editable,
        pattern: Regex,
        style: Int
    ) {
        pattern.findAll(editable.toString()).forEach { match ->

            editable.setSpan(
                StyleSpan(style),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun applyBackground(
        editable: Editable,
        pattern: Regex,
        color: Int
    ) {
        pattern.findAll(editable.toString()).forEach { match ->

            editable.setSpan(
                BackgroundColorSpan(color),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}
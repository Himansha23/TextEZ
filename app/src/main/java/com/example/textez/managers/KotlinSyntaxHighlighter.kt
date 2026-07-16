package com.example.textez.managers

import android.graphics.Color
import android.text.Editable
import android.text.Spanned
import android.text.style.ForegroundColorSpan

object KotlinSyntaxHighlighter {

    private val keywordColor = Color.parseColor("#7C3AED")
    private val stringColor = Color.parseColor("#15803D")
    private val commentColor = Color.parseColor("#6B7280")
    private val annotationColor = Color.parseColor("#C2410C")
    private val numberColor = Color.parseColor("#0369A1")

    private val kotlinKeywords = listOf(
        "as",
        "break",
        "class",
        "continue",
        "do",
        "else",
        "false",
        "for",
        "fun",
        "if",
        "in",
        "interface",
        "is",
        "null",
        "object",
        "package",
        "return",
        "super",
        "this",
        "throw",
        "true",
        "try",
        "typealias",
        "typeof",
        "val",
        "var",
        "when",
        "while",
        "by",
        "catch",
        "constructor",
        "delegate",
        "dynamic",
        "field",
        "file",
        "finally",
        "get",
        "import",
        "init",
        "param",
        "property",
        "receiver",
        "set",
        "setparam",
        "where",
        "actual",
        "abstract",
        "annotation",
        "companion",
        "const",
        "crossinline",
        "data",
        "enum",
        "expect",
        "external",
        "final",
        "infix",
        "inline",
        "inner",
        "internal",
        "lateinit",
        "noinline",
        "open",
        "operator",
        "out",
        "override",
        "private",
        "protected",
        "public",
        "reified",
        "sealed",
        "suspend",
        "tailrec",
        "vararg"
    )

    private val keywordPattern = Regex(
        "\\b(${kotlinKeywords.joinToString("|")})\\b"
    )

    private val stringPattern = Regex(
        "\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'"
    )

    private val singleLineCommentPattern = Regex(
        "//.*$",
        RegexOption.MULTILINE
    )

    private val multiLineCommentPattern = Regex(
        "/\\*.*?\\*/",
        setOf(
            RegexOption.DOT_MATCHES_ALL,
            RegexOption.MULTILINE
        )
    )

    private val annotationPattern = Regex(
        "@[A-Za-z_][A-Za-z0-9_.]*"
    )

    private val numberPattern = Regex(
        "\\b\\d+(?:\\.\\d+)?\\b"
    )

    fun highlight(editable: Editable) {
        clear(editable)

        applyPattern(
            editable = editable,
            pattern = keywordPattern,
            color = keywordColor
        )

        applyPattern(
            editable = editable,
            pattern = numberPattern,
            color = numberColor
        )

        applyPattern(
            editable = editable,
            pattern = annotationPattern,
            color = annotationColor
        )

        applyPattern(
            editable = editable,
            pattern = stringPattern,
            color = stringColor
        )

        /*
         * Comments are applied last so that keywords or strings
         * inside comments use the comment color.
         */
        applyPattern(
            editable = editable,
            pattern = singleLineCommentPattern,
            color = commentColor
        )

        applyPattern(
            editable = editable,
            pattern = multiLineCommentPattern,
            color = commentColor
        )
    }

    fun clear(editable: Editable) {
        val existingSpans = editable.getSpans(
            0,
            editable.length,
            ForegroundColorSpan::class.java
        )

        existingSpans.forEach { span ->
            editable.removeSpan(span)
        }
    }

    private fun applyPattern(
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
}
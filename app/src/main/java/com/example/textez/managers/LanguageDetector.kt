package com.example.textez.managers

object LanguageDetector {

    enum class Language {
        KOTLIN,
        MARKDOWN,
        PLAIN_TEXT
    }

    /**
     * Detection order:
     *
     * 1. Recognized saved-file extension.
     * 2. Typed document content.
     * 3. Plain text when confidence is low.
     */
    fun detect(
        fileName: String,
        content: String
    ): Language {

        detectFromExtension(fileName)?.let { language ->
            return language
        }

        if (content.isBlank()) {
            return Language.PLAIN_TEXT
        }

        val kotlinScore =
            calculateKotlinScore(content)

        val markdownScore =
            calculateMarkdownScore(content)

        return when {
            kotlinScore >= MINIMUM_SCORE &&
                    kotlinScore > markdownScore -> {
                Language.KOTLIN
            }

            markdownScore >= MINIMUM_SCORE &&
                    markdownScore > kotlinScore -> {
                Language.MARKDOWN
            }

            else -> {
                Language.PLAIN_TEXT
            }
        }
    }

    private fun detectFromExtension(
        fileName: String
    ): Language? {

        return when {
            fileName.endsWith(
                suffix = ".kt",
                ignoreCase = true
            ) ||
                    fileName.endsWith(
                        suffix = ".kts",
                        ignoreCase = true
                    ) -> {
                Language.KOTLIN
            }

            fileName.endsWith(
                suffix = ".md",
                ignoreCase = true
            ) ||
                    fileName.endsWith(
                        suffix = ".markdown",
                        ignoreCase = true
                    ) -> {
                Language.MARKDOWN
            }

            else -> null
        }
    }

    private fun calculateKotlinScore(
        content: String
    ): Int {

        var score = 0

        val strongPatterns = listOf(
            Regex("""\bfun\s+\w+\s*\("""),
            Regex("""\b(class|object|interface)\s+\w+"""),
            Regex("""\bpackage\s+[\w.]+"""),
            Regex("""\bimport\s+[\w.]+"""),
            Regex("""\b(val|var)\s+\w+\s*[:=]""")
        )

        val normalPatterns = listOf(
            Regex("""\b(when|override|data|sealed|lateinit|companion)\b"""),
            Regex("""\b(if|else|for|while|return|try|catch)\b"""),
            Regex("""println\s*\("""),
            Regex("""@\w+"""),
            Regex("""//[^\n]*"""),
            Regex("""/\*[\s\S]*?\*/"""),
            Regex("""\?\."""),
            Regex("""!!"""),
            Regex("""->""")
        )

        strongPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(content)) {
                score += 2
            }
        }

        normalPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(content)) {
                score++
            }
        }

        return score
    }

    private fun calculateMarkdownScore(
        content: String
    ): Int {

        var score = 0

        val strongPatterns = listOf(
            Regex(
                pattern = """^#{1,6}\s+.+""",
                option = RegexOption.MULTILINE
            ),
            Regex("""\[[^]]+]\([^)]+\)"""),
            Regex("""```[\s\S]*?```"""),
            Regex(
                pattern = """^\s*[-+*]\s+.+""",
                option = RegexOption.MULTILINE
            )
        )

        val normalPatterns = listOf(
            Regex(
                pattern = """^\s*\d+\.\s+.+""",
                option = RegexOption.MULTILINE
            ),
            Regex("""\*\*[^*\n]+\*\*"""),
            Regex("""__[^_\n]+__"""),
            Regex("""(?<!\*)\*[^*\n]+\*(?!\*)"""),
            Regex("""(?<!_)_[^_\n]+_(?!_)"""),
            Regex(
                pattern = """^>\s?.+""",
                option = RegexOption.MULTILINE
            ),
            Regex("""`[^`\n]+`"""),
            Regex(
                pattern = """^\s*(---+|\*\*\*+|___+)\s*$""",
                option = RegexOption.MULTILINE
            ),
            Regex("""!\[[^]]*]\([^)]+\)""")
        )

        strongPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(content)) {
                score += 2
            }
        }

        normalPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(content)) {
                score++
            }
        }

        return score
    }

    private const val MINIMUM_SCORE = 3
}
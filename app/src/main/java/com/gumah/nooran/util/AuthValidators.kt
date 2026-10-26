package com.gumah.nooran.util

import android.util.Patterns

object AuthValidators {

    private val RESERVED_USERNAMES = setOf(
        "admin", "nooran", "support", "root", "moderator", "mod",
        "official", "verified", "help", "api", "system", "security"
    )

    sealed interface UsernameValidationResult {
        data object Valid : UsernameValidationResult
        data class Invalid(val message: String) : UsernameValidationResult
    }

    fun validateUsername(username: String): UsernameValidationResult {
        val trimmed = username.trim().lowercase()
        if (trimmed.length < 3) {
            return UsernameValidationResult.Invalid("اسم المستخدم يجب ألا يقل عن 3 أحرف")
        }
        if (trimmed.length > 20) {
            return UsernameValidationResult.Invalid("اسم المستخدم يجب ألا يتجاوز 20 حرفاً")
        }
        if (!trimmed.first().isLetter()) {
            return UsernameValidationResult.Invalid("اسم المستخدم يجب أن يبدأ بحرف إنجليزي")
        }
        val regex = Regex("^[a-z][a-z0-9_]{2,19}$")
        if (!regex.matches(trimmed)) {
            return UsernameValidationResult.Invalid("مسموح فقط بالأحرف الإنجليزية والأرقام والشرطة السفلية _")
        }
        if (RESERVED_USERNAMES.contains(trimmed)) {
            return UsernameValidationResult.Invalid("هذا الاسم محجوز للنظام")
        }
        return UsernameValidationResult.Valid
    }

    fun generateUsernameSuggestions(base: String): List<String> {
        val clean = base.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "user" }
        return listOf(
            "${clean}_1",
            "${clean}_7",
            "${clean}2026",
            "${clean}_nooran"
        )
    }

    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return "البريد الإلكتروني مطلوب"
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            return "صيغة البريد الإلكتروني غير صحيحة"
        }
        return null
    }

    fun validateFullName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return "الاسم الكامل مطلوب"
        if (trimmed.length < 2) return "الاسم قصير جداً"
        if (trimmed.length > 60) return "الاسم طويل جداً"
        return null
    }

    enum class PasswordStrength(val label: String, val level: Int) {
        EMPTY("", 0),
        WEAK("ضعيفة", 1),
        MEDIUM("متوسطة", 2),
        STRONG("قوية وآمنة", 3)
    }

    fun evaluatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.EMPTY
        if (password.length < 8) return PasswordStrength.WEAK

        val hasLetters = password.any { it.isLetter() }
        val hasDigits = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        return when {
            hasLetters && hasDigits && hasSpecial && password.length >= 10 -> PasswordStrength.STRONG
            hasLetters && hasDigits -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
}

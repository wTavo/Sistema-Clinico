package com.example.sistemaclinico.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

// Limpia espacios innecesarios de una cadena:
// - Elimina espacios al inicio y al final.
// - Colapsa múltiples espacios consecutivos en un solo espacio.
fun String.cleanSpaces(): String =
    this.trim().replace(Regex("\\s+"), " ")

// Obtiene el texto de un EditText completamente limpio:
// sin espacios al inicio, sin espacios al final y con máximo un espacio entre palabras.
fun EditText.getCleanText(): String =
    this.text?.toString()?.cleanSpaces().orEmpty()

// TextWatcher que previene en tiempo real:
// 1. Espacios al inicio del texto.
// 2. Espacios múltiples consecutivos (solo permite 1 espacio entre letras/palabras).
// Y al perder el foco elimina cualquier espacio final automáticamente.
class CleanWhitespaceTextWatcher(private val editText: EditText) : TextWatcher {

    private var isSelfEditing = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (isSelfEditing || s == null) return

        val original = s.toString()
        var modified = original

        // 1. No puede iniciar con espacio
        while (modified.startsWith(" ")) {
            modified = modified.drop(1)
        }

        // 2. Solo puede haber un espacio en medio de 2 letras (sin espacios dobles)
        while (modified.contains("  ")) {
            modified = modified.replace("  ", " ")
        }

        if (modified != original) {
            isSelfEditing = true
            val oldSelection = editText.selectionStart
            val diff = original.length - modified.length
            val newSelection = (oldSelection - diff).coerceIn(0, modified.length)

            editText.setText(modified)
            editText.setSelection(newSelection)
            isSelfEditing = false
        }
    }
}

// Habilita la sanitización completa de espacios en el EditText:
// - En tiempo real: no permite espacios al inicio ni dobles espacios en medio.
// - Al perder el foco: quita automáticamente cualquier espacio al final.
fun EditText.enableWhitespaceSanitization() {
    this.addTextChangedListener(CleanWhitespaceTextWatcher(this))
    this.setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
            val current = this.text?.toString() ?: ""
            val trimmed = current.trimEnd()
            if (trimmed != current) {
                this.setText(trimmed)
            }
        }
    }
}

// Aplica la sanitización de espacios a múltiples EditText en una sola llamada.
fun setupWhitespaceSanitization(vararg editTexts: EditText?) {
    editTexts.forEach { it?.enableWhitespaceSanitization() }
}

// Calcula la edad en años al día de hoy a partir de la fecha de nacimiento.
// Soporta formatos: AAAA-MM-DD, AAAA/MM/DD, DD/MM/AAAA, DD-MM-AAAA.
fun calculateAge(birthDateStr: String): Int? {
    if (birthDateStr.isBlank()) return null
    try {
        val clean = birthDateStr.trim()
        val parts = if (clean.contains("-")) clean.split("-") else if (clean.contains("/")) clean.split("/") else return null
        if (parts.size != 3) return null

        val (year, month, day) = if (parts[0].length == 4) {
            // YYYY-MM-DD o YYYY/MM/DD
            Triple(parts[0].toIntOrNull() ?: return null, parts[1].toIntOrNull() ?: return null, parts[2].toIntOrNull() ?: return null)
        } else {
            // DD/MM/YYYY o DD-MM-YYYY
            Triple(parts[2].toIntOrNull() ?: return null, parts[1].toIntOrNull() ?: return null, parts[0].toIntOrNull() ?: return null)
        }

        if (year !in 1900..2100 || month !in 1..12 || day !in 1..31) return null

        val today = java.util.Calendar.getInstance()
        val birth = java.util.Calendar.getInstance().apply {
            set(year, month - 1, day)
        }

        var age = today.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
        if (today.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) {
            age--
        }
        return if (age in 0..150) age else null
    } catch (e: Exception) {
        return null
    }
}

// Formatea la fecha de nacimiento con la edad calculada al día de hoy.
fun formatBirthDateWithAge(birthDateStr: String): String {
    if (birthDateStr.isBlank()) return "N/D"
    val age = calculateAge(birthDateStr)
    return if (age != null) {
        "$birthDateStr ($age años)"
    } else {
        birthDateStr
    }
}


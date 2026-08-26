package com.example.sistemaclinico.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

// Máscara automática de entrada de texto para campos de fecha con formato DD/MM/AAAA
class DateMaskTextWatcher(private val editText: EditText) : TextWatcher {

    // Bandera para evitar llamadas recursivas durante la modificación del texto
    private var isUpdating = false
    // Almacena el estado previo del texto antes de la edición
    private var oldText = ""

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // Registra el texto previo para seguimiento de modificaciones
        oldText = s?.toString() ?: ""
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (isUpdating || s == null) return

        // Extrae exclusivamente los caracteres numéricos ingresados
        val clean = s.toString().replace("/", "").filter { it.isDigit() }

        // Aplica las diagonales delimitadoras en formato DD/MM/AAAA
        val formatted = buildString {
            for (i in clean.indices) {
                if (i == 2 || i == 4) append("/")
                if (i < 8) append(clean[i])
            }
        }

        // Si el texto resultante requiere máscara, actualiza el campo y posiciona el cursor al final
        if (formatted != s.toString()) {
            isUpdating = true
            editText.setText(formatted)
            editText.setSelection(formatted.length.coerceAtMost(editText.text.length))
            isUpdating = false
        }
    }
}

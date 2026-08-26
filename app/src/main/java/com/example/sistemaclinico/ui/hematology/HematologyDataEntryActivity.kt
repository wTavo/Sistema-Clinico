package com.example.sistemaclinico.ui.hematology

// Módulo de carga, procesamiento y evaluación automática de archivos de laboratorio (CSV y TXT).
// Realiza respaldo local de seguridad, historial de análisis y vinculación directa a expedientes de pacientes.

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.sistemaclinico.R
import com.example.sistemaclinico.data.DatabaseHelper
import com.example.sistemaclinico.data.FileAnalysisRecord
import com.example.sistemaclinico.databinding.ActivityHematologyDataEntryBinding
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HematologyDataEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHematologyDataEntryBinding
    private lateinit var dbHelper: DatabaseHelper
    private var lastAnalyzedRecord: FileAnalysisRecord? = null

    private val sampleCsvContent = """
        folio,fecha,rbc,hb,hct,wbc,neut,linf,plt
        LAB-001,25/08/2026,4.80,14.2,42.6,6.80,60.0,30.0,250000
    """.trimIndent()

    private val sampleTxtContent = """
        FOLIO = LAB-001
        FECHA = 25/08/2026
        RBC = 4.80
        HB = 14.2
        HCT = 42.6
        WBC = 6.80
        NEUT = 60.0
        LINF = 30.0
        PLT = 250000
    """.trimIndent()

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            processSelectedFile(uri)
        }
    }

    // Inicializa la actividad, View Binding, barra de estado y listeners
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configura el color de la barra de estado
        window.statusBarColor = ContextCompat.getColor(this, R.color.secondary_crimson_dark)

        // Infla y establece el contenido de la actividad usando View Binding
        binding = ActivityHematologyDataEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa la base de datos y la acción de regreso
        dbHelper = DatabaseHelper.getInstance(this)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Configura los listeners y carga el historial
        setupActions()
        loadHistoryList()
    }

    // Configura los botones de selección de archivo, plantillas y vinculación
    private fun setupActions() {
        binding.btnSelectFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        binding.btnViewTemplateGuide.setOnClickListener {
            showTemplateGuideDialog()
        }

        binding.btnCurrentAssignToPatient.setOnClickListener {
            lastAnalyzedRecord?.let { record ->
                showAssignAnalysisToPatientDialog(record)
            } ?: Toast.makeText(this, getString(R.string.toast_file_no_recent_analysis), Toast.LENGTH_SHORT).show()
        }
    }

    // Lee, procesa y genera la copia local del archivo seleccionado
    private fun processSelectedFile(uri: Uri) {
        try {
            val contentResolver = applicationContext.contentResolver
            val fileName = getFileName(uri)

            var rawContent = ""
            val lines = mutableListOf<String>()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    rawContent = reader.readText()
                }
            }

            rawContent.lines().forEach { line ->
                if (line.isNotBlank()) lines.add(line.trim())
            }

            if (lines.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_file_empty), Toast.LENGTH_SHORT).show()
                return
            }

            val record = parseLinesToRecord(fileName, lines)
            if (record != null) {
                val mimeType = if (fileName.endsWith(".txt", ignoreCase = true)) "text/plain" else "text/csv"
                val copyFileName = saveCopyOfUploadedFile(fileName, rawContent, mimeType, record.folio)
                val recordWithCopy = record.copy(nombreCopia = copyFileName)
                saveAndDisplayRecord(recordWithCopy)
                Toast.makeText(this, getString(R.string.toast_file_analysis_completed, copyFileName), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, getString(R.string.toast_file_parse_error), Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_file_read_error, e.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    // Interpreta el contenido del archivo y calcula índices hematológicos
    private fun parseLinesToRecord(fileName: String, lines: List<String>): FileAnalysisRecord? {
        val today = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        var folio = "FILE-${System.currentTimeMillis() % 10000}"
        var rbc = 0.0
        var hb = 0.0
        var hct = 0.0
        var wbc = 0.0
        var neut = 60.0
        var linf = 30.0
        var mono = 6.0
        var eos = 3.0
        var plt = 250.0

        val isKeyValue = lines.any { it.contains("=") || it.contains(":") }

        if (isKeyValue) {
            for (line in lines) {
                val delimiter = if (line.contains("=")) "=" else ":"
                val parts = line.split(delimiter, limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().uppercase()
                    val value = parts[1].trim()
                    when {
                        key.contains("FOLIO") -> folio = value
                        key == "RBC" || key.contains("ERITRO") || key.contains("ROJO") -> rbc = value.toDoubleOrNull() ?: 0.0
                        key == "HB" || key.contains("HEMO") -> hb = value.toDoubleOrNull() ?: 0.0
                        key == "HCT" || key.contains("HEMATO") -> hct = value.toDoubleOrNull() ?: 0.0
                        key == "WBC" || key.contains("LEUCO") || key.contains("BLANCO") -> wbc = value.toDoubleOrNull() ?: 0.0
                        key == "NEUT" || key.contains("NEUTRO") -> neut = value.toDoubleOrNull() ?: 60.0
                        key == "LINF" || key.contains("LINFO") -> linf = value.toDoubleOrNull() ?: 30.0
                        key == "MONO" -> mono = value.toDoubleOrNull() ?: 6.0
                        key == "EOS" -> eos = value.toDoubleOrNull() ?: 3.0
                        key == "PLT" || key.contains("PLAQUET") -> {
                            val rawPlt = value.replace(",", "").toDoubleOrNull() ?: 250.0
                            plt = if (rawPlt > 1000) rawPlt / 1000.0 else rawPlt
                        }
                    }
                }
            }
        } else {
            val headerIndex = lines.indexOfFirst { it.contains("rbc", ignoreCase = true) || it.contains("hb", ignoreCase = true) }
            val dataLine = if (headerIndex != -1 && headerIndex + 1 < lines.size) lines[headerIndex + 1] else lines.first()

            val tokens = dataLine.split(Regex("[,;\\t]")).map { it.trim() }
            if (tokens.size >= 4) {
                val numbers = tokens.mapNotNull { it.replace(",", ".").toDoubleOrNull() }
                if (numbers.size >= 3) {
                    rbc = numbers.getOrNull(0) ?: 4.5
                    hb = numbers.getOrNull(1) ?: 14.0
                    hct = numbers.getOrNull(2) ?: (hb * 3.0)
                    wbc = numbers.getOrNull(3) ?: 6.5
                    plt = (numbers.getOrNull(4) ?: 250.0).let { if (it > 1000) it / 1000.0 else it }
                }
            }
        }

        if (rbc <= 0.0 && hb <= 0.0) {
            return null
        }

        if (hct <= 0.0) hct = hb * 3.0
        val vcm = if (rbc > 0) (hct * 10.0) / rbc else 90.0
        val hcm = if (rbc > 0) (hb * 10.0) / rbc else 30.0
        val chcm = if (hct > 0) (hb * 100.0) / hct else 33.3
        val pltFinal = if (plt < 1000) plt * 1000.0 else plt

        val (diag, flag) = evaluateHematology(rbc, hb, hct, vcm, hcm, chcm, wbc, neut, linf, pltFinal)

        return FileAnalysisRecord(
            nombreArchivo = fileName,
            fechaAnalisis = today,
            folio = folio,
            rbc = rbc,
            hb = hb,
            hct = hct,
            vcm = vcm,
            hcm = hcm,
            chcm = chcm,
            wbc = wbc,
            neut = neut,
            linf = linf,
            mono = mono,
            eos = eos,
            plt = pltFinal,
            diagnostico = diag,
            flag = flag,
            pacienteVinculado = ""
        )
    }

    // Evalúa los parámetros hematológicos y genera el diagnóstico automático
    private fun evaluateHematology(
        rbc: Double, hb: Double, hct: Double, vcm: Double, hcm: Double, chcm: Double,
        wbc: Double, neut: Double, linf: Double, plt: Double
    ): Pair<String, String> {
        val findings = mutableListOf<String>()
        var flag = "NORMAL"

        // Serie Roja
        if (hb < 12.0) {
            flag = "ANEMIA"
            if (vcm < 80.0) findings.add(getString(R.string.diag_microcytic_anemia))
            else if (vcm > 100.0) findings.add(getString(R.string.diag_macrocytic_anemia))
            else findings.add(getString(R.string.diag_normocytic_anemia))
        } else if (hb > 17.5 || rbc > 6.0) {
            flag = "POLIGLOBULIA"
            findings.add(getString(R.string.diag_polyglobulia))
        }

        // Serie Blanca
        if (wbc > 11.0) {
            flag = if (flag == "NORMAL") "LEUCOCITOSIS" else flag
            if (neut > 75.0) findings.add(getString(R.string.diag_leukocytosis_neutrophilia))
            else if (linf > 50.0) findings.add(getString(R.string.diag_leukocytosis_lymphocytosis))
            else findings.add(getString(R.string.diag_leukocytosis_reactive))
        } else if (wbc < 4.0) {
            flag = "LEUCOPENIA"
            findings.add(getString(R.string.diag_leukopenia))
        }

        // Serie Plaquetaria
        if (plt < 150000) {
            flag = "TROMBOCITOPENIA"
            findings.add(getString(R.string.diag_thrombocytopenia, String.format(Locale.US, "%,d", plt.toLong())))
        } else if (plt > 450000) {
            findings.add(getString(R.string.diag_thrombocytosis_reactive))
        }

        val diag = if (findings.isEmpty()) {
            getString(R.string.diag_full_normal)
        } else {
            findings.joinToString(". ") + "."
        }

        return Pair(diag, flag)
    }

    // Inserta el registro analizado en SQLite y refresca la interfaz
    private fun saveAndDisplayRecord(record: FileAnalysisRecord) {
        val id = dbHelper.insertHistorialArchivo(record)
        val savedRecord = record.copy(id = id)
        lastAnalyzedRecord = savedRecord

        renderCurrentResultCard(savedRecord)
        loadHistoryList()
    }

    // Muestra la tarjeta del análisis actual con sus tablas de parámetros
    private fun renderCurrentResultCard(record: FileAnalysisRecord) {
        binding.cardCurrentAnalysisResult.visibility = View.VISIBLE
        binding.tvCurrentFileName.text = record.nombreArchivo
        binding.tvCurrentTimestamp.text = getString(R.string.file_analyzed_timestamp_format, record.fechaAnalisis, record.folio)
        binding.tvCurrentFlagBadge.text = record.flag
        binding.tvCurrentDiagnosticText.text = record.diagnostico

        val flagColor = when {
            record.flag.contains("ANEMIA", ignoreCase = true) || record.flag.contains("TROMBO", ignoreCase = true) -> 0xFFE11D48.toInt()
            record.flag.contains("LEUCO", ignoreCase = true) || record.flag.contains("POLI", ignoreCase = true) -> 0xFFD97706.toInt()
            else -> 0xFF0D9488.toInt()
        }
        binding.tvCurrentFlagBadge.setTextColor(flagColor)

        val table = binding.tableCurrentResult
        table.removeAllViews()

        addTableHeader(table, listOf(getString(R.string.th_parameter), getString(R.string.th_value), getString(R.string.th_reference)))
        addTableRow(table, getString(R.string.bio_rbc), String.format(Locale.US, "%.2f", record.rbc) + " 10⁶/µL", "4.00 - 5.50")
        addTableRow(table, getString(R.string.bio_hb), String.format(Locale.US, "%.1f", record.hb) + " g/dL", "12.0 - 16.0")
        addTableRow(table, getString(R.string.bio_hct), String.format(Locale.US, "%.1f", record.hct) + " %", "36.0 - 48.0")
        addTableRow(table, getString(R.string.bio_vcm), String.format(Locale.US, "%.1f", record.vcm) + " fL", "80.0 - 98.0")
        addTableRow(table, getString(R.string.bio_hcm), String.format(Locale.US, "%.1f", record.hcm) + " pg", "27.0 - 33.0")
        addTableRow(table, getString(R.string.bio_chcm), String.format(Locale.US, "%.1f", record.chcm) + " g/dL", "32.0 - 36.0")
        addTableRow(table, getString(R.string.bio_wbc), String.format(Locale.US, "%.2f", record.wbc) + " 10³/µL", "4.50 - 11.00")
        addTableRow(table, getString(R.string.bio_neut), String.format(Locale.US, "%.1f", record.neut) + " %", "40.0 - 70.0")
        addTableRow(table, getString(R.string.bio_linf), String.format(Locale.US, "%.1f", record.linf) + " %", "20.0 - 45.0")
        addTableRow(table, getString(R.string.bio_plt), String.format(Locale.US, "%,d", record.plt.toLong()) + " /µL", "150,000 - 450,000")
    }

    // Agrega una fila de encabezado a la tabla de parámetros
    private fun addTableHeader(table: TableLayout, headers: List<String>) {
        val row = TableRow(this).apply {
            setBackgroundColor(0xFFF1F5F9.toInt())
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        for (h in headers) {
            val tv = TextView(this).apply {
                text = h
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(this@HematologyDataEntryActivity, R.color.text_primary))
                gravity = Gravity.START
                setPadding(dp(4), dp(2), dp(4), dp(2))
            }
            row.addView(tv)
        }
        table.addView(row)
    }

    // Agrega una fila de parámetro con resultado, rango de referencia y unidad a la tabla
    private fun addTableRow(table: TableLayout, param: String, result: String, ref: String) {
        val row = TableRow(this).apply {
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }
        val tvParam = TextView(this).apply {
            text = param
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@HematologyDataEntryActivity, R.color.text_primary))
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }
        val tvRes = TextView(this).apply {
            text = result
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@HematologyDataEntryActivity, R.color.text_primary))
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }
        val tvRef = TextView(this).apply {
            text = ref
            textSize = 11.5f
            setTextColor(ContextCompat.getColor(this@HematologyDataEntryActivity, R.color.text_secondary))
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }

        row.addView(tvParam)
        row.addView(tvRes)
        row.addView(tvRef)
        table.addView(row)
    }

    // Consulta y renderiza el historial de archivos analizados desde SQLite
    private fun loadHistoryList() {
        val history = dbHelper.getHistorialArchivos()
        val container = binding.containerAnalysisHistory
        container.removeAllViews()

        binding.tvHistoryCountBadge.text = getString(R.string.file_history_count_format, history.size)

        if (history.isEmpty()) {
            binding.tvEmptyHistory.visibility = View.VISIBLE
            return
        }

        binding.tvEmptyHistory.visibility = View.GONE

        history.forEach { record ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@HematologyDataEntryActivity, R.drawable.bg_chip_soft)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dp(10) }
            }

            // Fila superior: Nombre de archivo + Fecha
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvFile = TextView(this).apply {
                text = record.nombreArchivo
                textSize = 14.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(this@HematologyDataEntryActivity, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            topRow.addView(tvFile)

            val tvDate = TextView(this).apply {
                text = record.fechaAnalisis
                textSize = 11.5f
                setTextColor(ContextCompat.getColor(this@HematologyDataEntryActivity, R.color.text_muted))
            }
            topRow.addView(tvDate)
            card.addView(topRow)

            // Resumen de parámetros
            val tvSummary = TextView(this).apply {
                text = getString(R.string.file_history_metrics_format, record.rbc.toString(), record.hb.toString(), record.wbc.toString(), record.plt.toInt().toString())
                textSize = 12.5f
                setTextColor(ContextCompat.getColor(this@HematologyDataEntryActivity, R.color.text_secondary))
                setPadding(0, dp(4), 0, dp(4))
            }
            card.addView(tvSummary)

            // Diagnóstico
            val tvDiag = TextView(this).apply {
                text = record.diagnostico
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                val flagColor = if (record.flag.contains("NORMAL", true)) 0xFF0D9488.toInt() else 0xFFE11D48.toInt()
                setTextColor(flagColor)
            }
            card.addView(tvDiag)

            if (record.pacienteVinculado.isNotBlank()) {
                val tvLinked = TextView(this).apply {
                    text = getString(R.string.file_history_linked_format, record.pacienteVinculado)
                    textSize = 11.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(this@HematologyDataEntryActivity, R.color.primary_teal))
                    setPadding(0, dp(4), 0, 0)
                }
                card.addView(tvLinked)
            }

            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener {
                showHistoryDetailDialog(record)
            }

            // Fila de acciones (Ver detalles / Vincular / Eliminar)
            val actionRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, 0)
            }

            val btnAssign = TextView(this).apply {
                text = if (record.pacienteVinculado.isBlank()) getString(R.string.btn_link_patient) else getString(R.string.btn_reassign)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(this@HematologyDataEntryActivity, R.color.primary_teal))
                setPadding(dp(6), dp(4), dp(6), dp(4))
                setOnClickListener {
                    showAssignAnalysisToPatientDialog(record)
                }
            }
            actionRow.addView(btnAssign)

            val btnViewDetail = TextView(this).apply {
                text = getString(R.string.btn_see_details)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(this@HematologyDataEntryActivity, R.color.primary_teal))
                setPadding(dp(6), dp(4), dp(6), dp(4))
                setOnClickListener {
                    showHistoryDetailDialog(record)
                }
            }
            actionRow.addView(btnViewDetail)

            val btnDelete = TextView(this).apply {
                text = getString(R.string.dialog_delete_confirm_btn)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(this@HematologyDataEntryActivity, R.color.secondary_crimson))
                setPadding(dp(6), dp(4), dp(4), dp(4))
                setOnClickListener {
                    showConfirmDeleteDialog(record)
                }
            }
            actionRow.addView(btnDelete)

            card.addView(actionRow)
            container.addView(card)
        }
    }

    // Muestra la confirmación para eliminar un análisis del historial y su archivo respaldado
    private fun showConfirmDeleteDialog(record: FileAnalysisRecord) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvConfirmDeleteTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvConfirmDeleteMessage)
        val btnCancel = dialogView.findViewById<View>(R.id.btnConfirmDeleteCancel)
        val btnAccept = dialogView.findViewById<View>(R.id.btnConfirmDeleteAccept)

        tvTitle.text = getString(R.string.dialog_confirm_delete_title)
        tvMessage.text = getString(R.string.dialog_delete_history_detail_msg, record.nombreArchivo, record.folio)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnAccept.setOnClickListener {
            dialog.dismiss()
            dbHelper.deleteHistorialArchivo(record.id)
            if (record.nombreCopia.isNotBlank()) {
                deleteCopyOfFile(record.nombreCopia)
            } else {
                deleteCopyOfFile("${record.folio}_${record.nombreArchivo}")
            }
            loadHistoryList()
            Toast.makeText(this, getString(R.string.toast_analysis_history_deleted, record.nombreArchivo), Toast.LENGTH_SHORT).show()
        }

        dialog.applyStandardDialogStyle(dialogView)
    }

    // Genera y almacena una copia de seguridad del archivo en el almacenamiento del dispositivo
    private fun saveCopyOfUploadedFile(originalFileName: String, content: String, mimeType: String, folio: String): String {
        val extension = if (originalFileName.contains(".")) originalFileName.substringAfterLast(".") else if (mimeType.contains("csv")) "csv" else "txt"
        val baseName = if (originalFileName.contains(".")) originalFileName.substringBeforeLast(".") else originalFileName
        val copyFileName = "${folio}_${baseName}.${extension}"

        try {
            val relativePath = "${android.os.Environment.DIRECTORY_DOWNLOADS}/Casos Clinicos"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, copyFileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(content.toByteArray(Charsets.UTF_8))
                    }
                    return copyFileName
                }
            }

            val targetDir = File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                "Casos Clinicos"
            )
            if (!targetDir.exists()) targetDir.mkdirs()

            val file = File(targetDir, copyFileName)
            file.writeText(content, Charsets.UTF_8)
            return copyFileName
        } catch (e: Exception) {
            try {
                val fallbackDir = File(
                    getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: filesDir,
                    "Casos Clinicos"
                )
                if (!fallbackDir.exists()) fallbackDir.mkdirs()
                val file = File(fallbackDir, copyFileName)
                file.writeText(content, Charsets.UTF_8)
                return copyFileName
            } catch (ex: Exception) {
                return copyFileName
            }
        }
    }

    // Elimina la copia de seguridad física del almacenamiento local
    private fun deleteCopyOfFile(copyFileName: String) {
        if (copyFileName.isBlank()) return
        try {
            // 1. Eliminación directa en Casos Clinicos/
            val file = File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                "Casos Clinicos/$copyFileName"
            )
            if (file.exists()) {
                file.delete()
            }

            // 2. Eliminación en MediaStore (Android 10+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val uri = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val selection = "${android.provider.MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(copyFileName)
                contentResolver.delete(uri, selection, selectionArgs)
            }

            // 3. Fallback
            val fallbackFile = File(
                getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: filesDir,
                "Casos Clinicos/$copyFileName"
            )
            if (fallbackFile.exists()) {
                fallbackFile.delete()
            }
        } catch (e: Exception) {
            // Silent catch
        }
    }

    // Muestra el detalle modal completo de un análisis almacenado en el historial
    private fun showHistoryDetailDialog(record: FileAnalysisRecord) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_file_analysis_detail, null)
        val tvFileName = dialogView.findViewById<TextView>(R.id.tvDetailFileName)
        val tvTimestamp = dialogView.findViewById<TextView>(R.id.tvDetailTimestamp)
        val tvFlag = dialogView.findViewById<TextView>(R.id.tvDetailFlagBadge)
        val table = dialogView.findViewById<TableLayout>(R.id.tableDetailResult)
        val tvDiagnostic = dialogView.findViewById<TextView>(R.id.tvDetailDiagnosticText)
        val btnAssign = dialogView.findViewById<View>(R.id.btnDetailAssignToPatient)
        val btnClose = dialogView.findViewById<View>(R.id.btnDetailClose)

        tvFileName.text = record.nombreArchivo
        tvTimestamp.text = getString(R.string.file_analyzed_timestamp_format, record.fechaAnalisis, record.folio)
        tvFlag.text = record.flag
        tvDiagnostic.text = record.diagnostico

        val flagColor = when {
            record.flag.contains("ANEMIA", ignoreCase = true) || record.flag.contains("TROMBO", ignoreCase = true) -> 0xFFE11D48.toInt()
            record.flag.contains("LEUCO", ignoreCase = true) || record.flag.contains("POLI", ignoreCase = true) -> 0xFFD97706.toInt()
            else -> 0xFF0D9488.toInt()
        }
        tvFlag.setTextColor(flagColor)

        table.removeAllViews()
        addTableHeader(table, listOf(getString(R.string.th_parameter), getString(R.string.th_value), getString(R.string.th_reference)))
        addTableRow(table, getString(R.string.bio_rbc), String.format(Locale.US, "%.2f", record.rbc) + " 10⁶/µL", "4.00 - 5.50")
        addTableRow(table, getString(R.string.bio_hb), String.format(Locale.US, "%.1f", record.hb) + " g/dL", "12.0 - 16.0")
        addTableRow(table, getString(R.string.bio_hct), String.format(Locale.US, "%.1f", record.hct) + " %", "36.0 - 48.0")
        addTableRow(table, getString(R.string.bio_vcm), String.format(Locale.US, "%.1f", record.vcm) + " fL", "80.0 - 98.0")
        addTableRow(table, getString(R.string.bio_hcm), String.format(Locale.US, "%.1f", record.hcm) + " pg", "27.0 - 33.0")
        addTableRow(table, getString(R.string.bio_chcm), String.format(Locale.US, "%.1f", record.chcm) + " g/dL", "32.0 - 36.0")
        addTableRow(table, getString(R.string.bio_wbc), String.format(Locale.US, "%.2f", record.wbc) + " 10³/µL", "4.50 - 11.00")
        addTableRow(table, getString(R.string.bio_neut), String.format(Locale.US, "%.1f", record.neut) + " %", "40.0 - 70.0")
        addTableRow(table, getString(R.string.bio_linf), String.format(Locale.US, "%.1f", record.linf) + " %", "20.0 - 45.0")
        addTableRow(table, getString(R.string.bio_plt), String.format(Locale.US, "%,d", record.plt.toLong()) + " /µL", "150,000 - 450,000")

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnAssign.setOnClickListener {
            dialog.dismiss()
            showAssignAnalysisToPatientDialog(record)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.applyStandardDialogStyle(dialogView)
    }

    // Muestra el diálogo modal para vincular el análisis al expediente de un paciente
    private fun showAssignAnalysisToPatientDialog(record: FileAnalysisRecord) {
        val patients = dbHelper.getAllPatientsSimpleList()
        if (patients.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_no_patients_in_system), Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_select_item_list, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvSelectorTitle)
        val tvSub = dialogView.findViewById<TextView>(R.id.tvSelectorSubtitle)
        val container = dialogView.findViewById<LinearLayout>(R.id.containerSelectorItems)
        val btnCancel = dialogView.findViewById<View>(R.id.btnSelectorCancel)

        tvTitle.text = getString(R.string.dialog_assign_patient_title)
        tvSub.text = getString(R.string.dialog_assign_patient_sub, record.folio, record.nombreArchivo)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        patients.forEach { (clavePac, displayName) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@HematologyDataEntryActivity, R.drawable.bg_chip_soft)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dp(8) }
                setOnClickListener {
                    dialog.dismiss()
                    assignRecordToPatient(record, clavePac, displayName)
                }
            }

            val tvName = TextView(this).apply {
                text = displayName
                setTextColor(ContextCompat.getColor(this@HematologyDataEntryActivity, R.color.text_primary))
                textSize = 14.5f
                typeface = Typeface.DEFAULT_BOLD
            }
            card.addView(tvName)

            val tvAction = TextView(this).apply {
                text = getString(R.string.btn_assign_bio_and_diag)
                setTextColor(ContextCompat.getColor(this@HematologyDataEntryActivity, R.color.primary_teal))
                textSize = 12f
                setPadding(0, dp(3), 0, 0)
            }
            card.addView(tvAction)

            container.addView(card)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.applyStandardDialogStyle(dialogView, maxScrollHeightDp = 460)
    }

    // Asocia el estudio hematológico analizado al expediente del paciente en SQLite
    private fun assignRecordToPatient(record: FileAnalysisRecord, clavePac: String, displayName: String) {
        val fecha = record.fechaAnalisis.substringBefore(" ").ifEmpty {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        }

        val success = dbHelper.insertBiometria(
            clavePaciente = clavePac,
            fecha = fecha,
            eritrocitos = record.rbc,
            hemoglobina = record.hb,
            hematocrito = record.hct,
            vcm = record.vcm,
            hcm = record.hcm,
            chcm = record.chcm,
            leucocitos = record.wbc,
            neutrofilos = record.neut,
            linfocitos = record.linf,
            monocitos = record.mono,
            eosinofilos = record.eos,
            plaquetas = record.plt,
            folio = record.folio,
            tipoMuestra = "",
            responsable = "",
            observacionesFrotis = "",
            diagnosticoManual = record.diagnostico
        )

        if (success) {
            if (record.id > 0) {
                dbHelper.updateHistorialPacienteVinculado(record.id, displayName)
            }
            lastAnalyzedRecord = lastAnalyzedRecord?.copy(pacienteVinculado = displayName)
            loadHistoryList()
            Toast.makeText(this, getString(R.string.toast_analysis_assigned_to_patient, displayName), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, getString(R.string.toast_analysis_assign_error), Toast.LENGTH_SHORT).show()
        }
    }

    // Muestra la guía modal con formatos de archivo admitidos y botones de descarga de plantilla
    private fun showTemplateGuideDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_file_format_guide, null)
        val btnDownloadCsv = dialogView.findViewById<View>(R.id.btnDownloadCsvGuide)
        val btnDownloadTxt = dialogView.findViewById<View>(R.id.btnDownloadTxtGuide)
        val btnClose = dialogView.findViewById<View>(R.id.btnGuideClose)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnDownloadCsv.setOnClickListener {
            saveAndOpenFile("ejemplo_citometria.csv", sampleCsvContent, "text/csv", "examples")
        }

        btnDownloadTxt.setOnClickListener {
            saveAndOpenFile("ejemplo_citometria.txt", sampleTxtContent, "text/plain", "examples")
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.applyStandardDialogStyle(dialogView)
    }

    // Guarda y abre una plantilla didáctica en una aplicación externa compatible
    private fun saveAndOpenFile(fileName: String, content: String, mimeType: String, subFolder: String = "examples") {
        try {
            val relativeSubPath = if (subFolder.isNotEmpty()) {
                "${android.os.Environment.DIRECTORY_DOWNLOADS}/Casos Clinicos/$subFolder"
            } else {
                "${android.os.Environment.DIRECTORY_DOWNLOADS}/Casos Clinicos"
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativeSubPath)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(content.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(this, getString(R.string.toast_file_template_saved_location, subFolder, fileName), Toast.LENGTH_LONG).show()

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(intent, getString(R.string.file_open_chooser_format, fileName))
                    startActivity(chooser)
                    return
                }
            }

            val targetDir = File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                "Casos Clinicos" + if (subFolder.isNotEmpty()) "/$subFolder" else ""
            )
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val file = File(targetDir, fileName)
            file.writeText(content, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, getString(R.string.file_open_chooser_format, fileName))
            startActivity(chooser)
            Toast.makeText(this, getString(R.string.toast_file_template_saved_location, subFolder, fileName), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            try {
                val fallbackDir = File(
                    getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: filesDir,
                    "Casos Clinicos/$subFolder"
                )
                if (!fallbackDir.exists()) fallbackDir.mkdirs()
                val file = File(fallbackDir, fileName)
                file.writeText(content, Charsets.UTF_8)
                val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.file_open_chooser_format, fileName)))
                Toast.makeText(this, getString(R.string.toast_file_template_saved_location, subFolder, fileName), Toast.LENGTH_LONG).show()
            } catch (ex: Exception) {
                Toast.makeText(this, getString(R.string.toast_file_read_error, ex.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Obtiene el nombre del archivo seleccionado mediante el ContentResolver
    private fun getFileName(uri: Uri): String {
        var name = "archivo_hematologia.csv"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex) ?: name
            }
        }
        return name
    }

    // Aplica el estilo estándar responsivo a los diálogos modales
    private fun AlertDialog.applyStandardDialogStyle(dialogView: View, maxScrollHeightDp: Int = 460) {
        window?.let { win ->
            win.setBackgroundDrawableResource(android.R.color.transparent)
            win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val screenHeight = displayMetrics.heightPixels

        val maxAllowedScrollHeight = ((screenHeight * 0.60).toInt()).coerceAtMost((maxScrollHeightDp * density).toInt())

        val scrollView = dialogView.findViewById<ScrollView>(R.id.dialogScrollView)
            ?: findFirstScrollView(dialogView)

        scrollView?.let { sv ->
            sv.post {
                val contentHeight = sv.getChildAt(0)?.measuredHeight ?: sv.height
                if (contentHeight > maxAllowedScrollHeight) {
                    sv.layoutParams.height = maxAllowedScrollHeight
                    sv.requestLayout()
                }
            }
        }
        show()
    }

    // Busca de forma recursiva el primer ScrollView dentro de la jerarquía de vistas
    private fun findFirstScrollView(view: View): ScrollView? {
        if (view is ScrollView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findFirstScrollView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    // Convierte valores de dp a píxeles de pantalla
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

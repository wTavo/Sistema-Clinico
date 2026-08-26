package com.example.sistemaclinico.utils

// Generador nativo de reportes médicos en formato PDF a partir del expediente integral del paciente.
// Utiliza las APIs de impresión y gráficos de Android para exportar documentos clínicos estructurados.

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.sistemaclinico.R
import com.example.sistemaclinico.data.PatientProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    // Dimensiones estándar de página A4 en puntos tipográficos
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    // Paleta de colores clínicos para renderizado de tablas y encabezados
    private val COLOR_PRIMARY = Color.rgb(15, 118, 110)       // Teal #0F766E
    private val COLOR_PRIMARY_DARK = Color.rgb(17, 94, 89)   // Teal dark #115E59
    private val COLOR_SECONDARY = Color.rgb(220, 38, 38)     // Crimson #DC2626
    private val COLOR_TEXT_PRIMARY = Color.rgb(15, 23, 42)    // Slate #0F172A
    private val COLOR_TEXT_MUTED = Color.rgb(100, 116, 139)  // Muted #64748B
    private val COLOR_BG_LIGHT = Color.rgb(248, 250, 252)     // Light #F8FAFC
    private val COLOR_BORDER = Color.rgb(226, 232, 240)       // Border #E2E8F0
    private val COLOR_CARD_BG = Color.rgb(241, 245, 249)      // Card #F1F5F9

    // Genera el documento PDF con todas las secciones clínicas del expediente del paciente
    fun generatePatientPdf(context: Context, patient: PatientProfile): Uri? {
        val pdfDoc = PdfDocument()
        var pageNumber = 1
        val pages = mutableListOf<PdfDocument.Page>()

        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = pdfDoc.startPage(pageInfo)
        var canvas = currentPage.canvas

        var currentY = MARGIN

        fun startNewPage() {
            drawFooter(context, canvas, pageNumber)
            pdfDoc.finishPage(currentPage)
            pages.add(currentPage)

            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            currentPage = pdfDoc.startPage(pageInfo)
            canvas = currentPage.canvas
            currentY = MARGIN
            drawPageHeader(context, canvas, patient, false)
            currentY += 45f
        }

        fun ensureSpace(neededHeight: Float) {
            if (currentY + neededHeight > PAGE_HEIGHT - 50f) {
                startNewPage()
            }
        }

        // 1. Cabecera Principal de Página 1
        drawPageHeader(context, canvas, patient, true)
        currentY += 75f

        // 2. Tarjeta: Datos Generales del Paciente
        ensureSpace(120f)
        currentY = drawPatientGeneralInfo(context, canvas, patient, currentY)

        // 3. Sección: Alergias
        ensureSpace(50f)
        currentY = drawAllergiesSection(context, canvas, patient, currentY, ::startNewPage)

        // 4. Sección: Biometrías Hemáticas y Citometrías
        ensureSpace(50f)
        currentY = drawBiometriasSection(context, canvas, patient, currentY, ::startNewPage)

        // 5. Sección: Historial Oncológico
        ensureSpace(50f)
        currentY = drawOncologySection(context, canvas, patient, currentY, ::startNewPage)

        // Terminar última página
        drawFooter(context, canvas, pageNumber)
        pdfDoc.finishPage(currentPage)

        // Guardar archivo PDF en Descargas / Casos Clinicos / expedientes / [Nombre del Paciente] /
        val formattedCode = if (patient.clave.startsWith("PAC-", ignoreCase = true)) patient.clave.uppercase() else "PAC-${patient.clave}"
        val sanitizedName = patient.nombreCompleto.replace(Regex("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ_]"), "_").replace(Regex("_+"), "_").trim('_')
        val patientFolderName = patient.nombreCompleto.replace(Regex("[/\\\\:*?\"<>|]"), "").trim()
        val timeWithSeconds = SimpleDateFormat("HH-mm-ss", Locale.getDefault()).format(Date())

        // Nombre del archivo solo con: Clave, Nombre del paciente y Hora con segundos
        val fileName = "${formattedCode}_${sanitizedName}_${timeWithSeconds}.pdf"

        val uri = savePdfToDownloads(context, pdfDoc, fileName, patientFolderName)
        pdfDoc.close()

        return uri
    }

    // Dibuja el encabezado institucional en la parte superior de cada página
    private fun drawPageHeader(context: Context, canvas: Canvas, patient: PatientProfile, isFirstPage: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Barra superior decorativa
        paint.color = COLOR_PRIMARY
        canvas.drawRect(MARGIN, MARGIN, MARGIN + CONTENT_WIDTH, MARGIN + (if (isFirstPage) 60f else 35f), paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        if (isFirstPage) {
            paint.textSize = 15f
            canvas.drawText(context.getString(R.string.pdf_header_title), MARGIN + 14f, MARGIN + 26f, paint)

            paint.textSize = 9.5f
            paint.typeface = Typeface.DEFAULT
            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            canvas.drawText(context.getString(R.string.pdf_header_issued_date, nowStr), MARGIN + 14f, MARGIN + 46f, paint)
        } else {
            paint.textSize = 11f
            val formattedCode = if (patient.clave.startsWith("PAC-", ignoreCase = true)) patient.clave.uppercase() else "PAC-${patient.clave}"
            canvas.drawText(context.getString(R.string.pdf_header_expediente_format, patient.nombreCompleto, formattedCode), MARGIN + 12f, MARGIN + 22f, paint)
        }
    }

    // Dibuja la tarjeta con datos generales, somatometría y tipo sanguíneo
    private fun drawPatientGeneralInfo(context: Context, canvas: Canvas, patient: PatientProfile, startY: Float): Float {
        val y = startY
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cardHeight = 100f
        val rect = RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + cardHeight)

        paint.color = COLOR_CARD_BG
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = COLOR_BORDER
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.style = Paint.Style.FILL

        // Título de la tarjeta
        paint.color = COLOR_PRIMARY_DARK
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(context.getString(R.string.pdf_section_patient_general_title), MARGIN + 14f, y + 20f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9.5f
        val col1X = MARGIN + 14f
        val col2X = MARGIN + (CONTENT_WIDTH / 2f) + 10f

        val formattedCode = if (patient.clave.startsWith("PAC-", ignoreCase = true)) patient.clave.uppercase() else "PAC-${patient.clave}"

        // Fila 1
        paint.color = COLOR_TEXT_MUTED
        canvas.drawText(context.getString(R.string.pdf_field_clave_folio), col1X, y + 40f, paint)
        canvas.drawText(context.getString(R.string.pdf_field_blood_type), col2X, y + 40f, paint)

        paint.color = COLOR_TEXT_PRIMARY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formattedCode, col1X + 70f, y + 40f, paint)

        paint.color = COLOR_SECONDARY
        canvas.drawText(if (patient.tipoSangre.isNotEmpty()) patient.tipoSangre else "N/D", col2X + 80f, y + 40f, paint)

        // Fila 2
        paint.typeface = Typeface.DEFAULT
        paint.color = COLOR_TEXT_MUTED
        canvas.drawText(context.getString(R.string.pdf_field_full_name), col1X, y + 58f, paint)
        canvas.drawText(context.getString(R.string.pdf_field_sex), col2X, y + 58f, paint)

        paint.color = COLOR_TEXT_PRIMARY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(patient.nombreCompleto, col1X + 90f, y + 58f, paint)
        canvas.drawText(if (patient.sexo.equals("F", true)) "Femenino (F)" else "Masculino (M)", col2X + 80f, y + 58f, paint)

        // Fila 3
        paint.typeface = Typeface.DEFAULT
        paint.color = COLOR_TEXT_MUTED
        canvas.drawText(context.getString(R.string.pdf_field_birthdate), col1X, y + 76f, paint)
        canvas.drawText(context.getString(R.string.pdf_field_weight_height), col2X, y + 76f, paint)

        paint.color = COLOR_TEXT_PRIMARY
        val birthWithAge = formatBirthDateWithAge(patient.fechaNacimiento)
        canvas.drawText(birthWithAge, col1X + 90f, y + 76f, paint)

        val pesoStr = if (patient.peso.isNotEmpty()) "${patient.peso} kg" else "--"
        val estStr = if (patient.estatura.isNotEmpty()) "${patient.estatura} m" else "--"
        canvas.drawText("$pesoStr  •  $estStr", col2X + 80f, y + 76f, paint)

        return y + cardHeight + 14f
    }

    // Dibuja la sección y tabla de alergias diagnosticadas en el paciente
    private fun drawAllergiesSection(context: Context, canvas: Canvas, patient: PatientProfile, startY: Float, onNewPage: () -> Unit): Float {
        var y = startY
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Encabezado de Sección
        y = drawSectionTitle(canvas, context.getString(R.string.pdf_section_allergies_title, patient.alergias.size), y)

        if (patient.alergias.isEmpty()) {
            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText(context.getString(R.string.pdf_empty_allergies), MARGIN + 10f, y + 14f, paint)
            return y + 26f
        }

        // Cabecera de Tabla
        val colClave = MARGIN
        val colNombre = MARGIN + 65f
        val colCat = MARGIN + 240f
        val colFecha = MARGIN + 390f

        paint.color = COLOR_PRIMARY
        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 20f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(context.getString(R.string.pdf_th_clave), colClave + 6f, y + 14f, paint)
        canvas.drawText(context.getString(R.string.pdf_th_substance), colNombre + 6f, y + 14f, paint)
        canvas.drawText(context.getString(R.string.pdf_th_category), colCat + 6f, y + 14f, paint)
        canvas.drawText(context.getString(R.string.pdf_th_diag_date), colFecha + 6f, y + 14f, paint)
        y += 20f

        paint.typeface = Typeface.DEFAULT
        for ((index, allergy) in patient.alergias.withIndex()) {
            if (y > PAGE_HEIGHT - 60f) {
                onNewPage()
                y = MARGIN + 45f
            }

            paint.color = if (index % 2 == 0) Color.WHITE else COLOR_BG_LIGHT
            canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 20f, paint)

            paint.color = COLOR_TEXT_PRIMARY
            paint.textSize = 8.5f
            canvas.drawText(allergy.claveAlergia, colClave + 6f, y + 13f, paint)
            canvas.drawText(allergy.nombre, colNombre + 6f, y + 13f, paint)
            val catText = if (allergy.categoria.isNotEmpty()) allergy.categoria else if (allergy.severidad.isNotEmpty()) allergy.severidad else "General"
            canvas.drawText(catText, colCat + 6f, y + 13f, paint)
            canvas.drawText(if (allergy.fecha.isNotEmpty()) allergy.fecha else "--", colFecha + 6f, y + 13f, paint)

            // Línea separadora
            paint.color = COLOR_BORDER
            paint.strokeWidth = 0.5f
            canvas.drawLine(MARGIN, y + 20f, MARGIN + CONTENT_WIDTH, y + 20f, paint)

            y += 20f
        }

        return y + 12f
    }

    // Dibuja las tarjetas individuales con los estudios de biometría hemática completa
    private fun drawBiometriasSection(context: Context, canvas: Canvas, patient: PatientProfile, startY: Float, onNewPage: () -> Unit): Float {
        var y = startY
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        y = drawSectionTitle(canvas, context.getString(R.string.pdf_section_biometrias_title, patient.biometrias.size), y)

        if (patient.biometrias.isEmpty()) {
            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText(context.getString(R.string.pdf_empty_biometrias), MARGIN + 10f, y + 14f, paint)
            return y + 26f
        }

        for (bio in patient.biometrias) {
            if (y > PAGE_HEIGHT - 120f) {
                onNewPage()
                y = MARGIN + 45f
            }

            val boxHeight = 85f
            val rect = RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + boxHeight)

            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, 6f, 6f, paint)

            paint.color = COLOR_BORDER
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(rect, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            // Encabezado de la muestra
            paint.color = COLOR_CARD_BG
            canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 22f), 6f, 6f, paint)

            paint.color = COLOR_TEXT_PRIMARY
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val folioStr = if (bio.folio.isNotEmpty()) bio.folio else bio.idEstudio
            canvas.drawText(context.getString(R.string.pdf_biometria_folio_date_format, folioStr, bio.fecha), MARGIN + 10f, y + 15f, paint)

            paint.color = COLOR_SECONDARY
            val flagStr = if (bio.diagnostico.contains("Normal", ignoreCase = true)) "NORMAL" else "OBSERVACIÓN"
            canvas.drawText(flagStr, MARGIN + CONTENT_WIDTH - 85f, y + 15f, paint)

            // Fila de parámetros numéricos
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8.5f
            paint.color = COLOR_TEXT_MUTED

            val row1Y = y + 38f
            canvas.drawText("RBC: ${bio.eritrocitos} 10⁶/µL", MARGIN + 10f, row1Y, paint)
            canvas.drawText("Hb: ${bio.hemoglobina} g/dL", MARGIN + 115f, row1Y, paint)
            canvas.drawText("Hct: ${bio.hematocrito} %", MARGIN + 215f, row1Y, paint)
            canvas.drawText("VCM: ${bio.vcm} fL", MARGIN + 315f, row1Y, paint)
            canvas.drawText("PLT: ${bio.plaquetas} /µL", MARGIN + 415f, row1Y, paint)

            val row2Y = y + 54f
            canvas.drawText("WBC: ${bio.leucocitos} 10³/µL", MARGIN + 10f, row2Y, paint)
            canvas.drawText("Neut: ${bio.neutrofilos} %", MARGIN + 115f, row2Y, paint)
            canvas.drawText("Linf: ${bio.linfocitos} %", MARGIN + 215f, row2Y, paint)
            canvas.drawText("HCM: ${bio.hcm} pg", MARGIN + 315f, row2Y, paint)
            canvas.drawText("CHCM: ${bio.chcm} g/dL", MARGIN + 415f, row2Y, paint)

            // Interpretación clínica
            paint.color = COLOR_TEXT_PRIMARY
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val diagText = if (bio.diagnostico.isNotEmpty()) bio.diagnostico else context.getString(R.string.pdf_diag_default)
            canvas.drawText(context.getString(R.string.pdf_interpretation_format, diagText), MARGIN + 10f, y + 74f, paint)

            y += boxHeight + 10f
        }

        return y + 6f
    }

    // Dibuja la sección y tabla de antecedentes oncológicos y tratamientos
    private fun drawOncologySection(context: Context, canvas: Canvas, patient: PatientProfile, startY: Float, onNewPage: () -> Unit): Float {
        var y = startY
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        y = drawSectionTitle(canvas, context.getString(R.string.pdf_section_oncology_title, patient.tratamientos.size), y)

        if (patient.tratamientos.isEmpty()) {
            paint.color = COLOR_TEXT_MUTED
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText(context.getString(R.string.pdf_empty_oncology), MARGIN + 10f, y + 14f, paint)
            return y + 26f
        }

        val colClave = MARGIN
        val colTipo = MARGIN + 65f
        val colFecha = MARGIN + 200f
        val colTerapia = MARGIN + 310f
        val colRes = MARGIN + 420f

        paint.color = COLOR_PRIMARY_DARK
        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 20f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(context.getString(R.string.pdf_th_clave), colClave + 6f, y + 14f, paint)
        canvas.drawText(context.getString(R.string.pdf_th_cancer_type), colTipo + 6f, y + 14f, paint)
        canvas.drawText(context.getString(R.string.pdf_th_detection), colFecha + 6f, y + 14f, paint)
        canvas.drawText(context.getString(R.string.pdf_th_start_treatment), colTerapia + 6f, y + 14f, paint)
        canvas.drawText(context.getString(R.string.pdf_th_results), colRes + 6f, y + 14f, paint)
        y += 20f

        paint.typeface = Typeface.DEFAULT
        for ((index, onco) in patient.tratamientos.withIndex()) {
            if (y > PAGE_HEIGHT - 60f) {
                onNewPage()
                y = MARGIN + 45f
            }

            paint.color = if (index % 2 == 0) Color.WHITE else COLOR_BG_LIGHT
            canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 20f, paint)

            paint.color = COLOR_TEXT_PRIMARY
            paint.textSize = 8.5f
            canvas.drawText(onco.claveCancer, colClave + 6f, y + 13f, paint)
            canvas.drawText(onco.tipoCancer, colTipo + 6f, y + 13f, paint)
            canvas.drawText(if (onco.fechaDeteccion.isNotEmpty()) onco.fechaDeteccion else "--", colFecha + 6f, y + 13f, paint)
            canvas.drawText(if (onco.fechaInicioTratamiento.isNotEmpty()) onco.fechaInicioTratamiento else "--", colTerapia + 6f, y + 13f, paint)
            canvas.drawText(if (onco.resultado.isNotEmpty()) onco.resultado else context.getString(R.string.pdf_in_followup), colRes + 6f, y + 13f, paint)

            paint.color = COLOR_BORDER
            paint.strokeWidth = 0.5f
            canvas.drawLine(MARGIN, y + 20f, MARGIN + CONTENT_WIDTH, y + 20f, paint)

            y += 20f
        }

        return y + 12f
    }

    // Dibuja el título de sección con línea decorativa inferior
    private fun drawSectionTitle(canvas: Canvas, title: String, startY: Float): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = COLOR_TEXT_PRIMARY
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, MARGIN + 4f, startY + 14f, paint)

        paint.color = COLOR_PRIMARY
        paint.strokeWidth = 2f
        canvas.drawLine(MARGIN, startY + 20f, MARGIN + 40f, startY + 20f, paint)

        return startY + 28f
    }

    // Dibuja el pie de página con leyenda de confidencialidad y número de página
    private fun drawFooter(context: Context, canvas: Canvas, pageNumber: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = COLOR_BORDER
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, PAGE_HEIGHT - 32f, MARGIN + CONTENT_WIDTH, PAGE_HEIGHT - 32f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(context.getString(R.string.pdf_footer_confidential), MARGIN, PAGE_HEIGHT - 18f, paint)

        val pageStr = context.getString(R.string.pdf_footer_page_format, pageNumber)
        val textWidth = paint.measureText(pageStr)
        canvas.drawText(pageStr, MARGIN + CONTENT_WIDTH - textWidth, PAGE_HEIGHT - 18f, paint)
    }

    // Guarda el PDF generado en el almacenamiento público y solicita su apertura en visor externo
    private fun savePdfToDownloads(context: Context, pdfDocument: PdfDocument, fileName: String, patientFolderName: String): Uri? {
        try {
            val mimeType = "application/pdf"
            val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/Casos Clinicos/expedientes/$patientFolderName"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        pdfDocument.writeTo(out)
                    }
                    Toast.makeText(context, context.getString(R.string.toast_pdf_saved_location, patientFolderName, fileName), Toast.LENGTH_LONG).show()

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(intent, context.getString(R.string.pdf_chooser_title))
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                    return uri
                }
            }

            val targetDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Casos Clinicos/expedientes/$patientFolderName"
            )
            if (!targetDir.exists()) targetDir.mkdirs()

            val file = File(targetDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, context.getString(R.string.pdf_chooser_title))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Toast.makeText(context, context.getString(R.string.toast_pdf_saved_location, patientFolderName, fileName), Toast.LENGTH_LONG).show()
            return uri
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.toast_pdf_generate_error, e.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
            return null
        }
    }
}

package com.example.sistemaclinico.ui.clinical

// Vista integral del expediente clínico de un paciente.
// Consolida datos generales, medidas, alergias activas con tratamiento de rescate,
// historial de glóbulos, antecedentes oncológicos, biometrías hemáticas y exportación a PDF.

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.sistemaclinico.R
import com.example.sistemaclinico.data.AllergyDetail
import com.example.sistemaclinico.data.BiometriaHematica
import com.example.sistemaclinico.data.DatabaseHelper
import com.example.sistemaclinico.data.DiagnosticoOncologico
import com.example.sistemaclinico.data.PatientProfile
import com.example.sistemaclinico.databinding.ActivityPatientDetailBinding

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDetailBinding
    internal lateinit var dbHelper: DatabaseHelper
    private val dialogManager by lazy { PatientDetailDialogManager(this, dbHelper) }
    internal var patientProfile: PatientProfile? = null
    private var currentPatientCode: String = ""
    private var isSpeedDialOpen = false

    // Inicializa la actividad, View Binding y componentes del expediente
    override fun onCreate(savedInstanceState: Bundle?) {
        // Infla y establece el contenido de la actividad usando View Binding
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper.getInstance(this)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val passedPatient = intent.getSerializableExtra("patient") as? PatientProfile
        currentPatientCode = intent.getStringExtra("patient_code") ?: passedPatient?.clave ?: ""

        loadPatientProfile(passedPatient)
        setupSpeedDial()
    }

    // Consulta el perfil completo del paciente y renderiza sus secciones clínicas
    internal fun loadPatientProfile(fallback: PatientProfile? = null) {
        patientProfile = if (currentPatientCode.isNotEmpty()) {
            dbHelper.getPatientProfile(currentPatientCode) ?: fallback
        } else {
            fallback
        }

        if (patientProfile == null) {
            Toast.makeText(this, getString(R.string.toast_patient_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        renderPatientHeader(patientProfile!!)
        renderAllergies(patientProfile!!.alergias)
        setupBiometrias(patientProfile!!)
        renderOncology(patientProfile!!.tratamientos)
    }

    // Renderiza la tarjeta superior con datos personales, medidas y tipo de sangre
    private fun renderPatientHeader(patient: PatientProfile) {
        binding.tvDetailInitials.text = patient.iniciales
        binding.tvDetailFullName.text = patient.nombreCompleto

        val formattedCode = if (patient.clave.startsWith("PAC-", ignoreCase = true)) {
            patient.clave.uppercase()
        } else {
            "PAC-${patient.clave}"
        }
        binding.tvDetailCode.text = getString(R.string.patient_detail_code_label_format, formattedCode)

        binding.tvDetailBloodType.text = patient.tipoSangre.ifEmpty { "N/D" }
        binding.tvDetailWeight.text = if (patient.peso.isNotEmpty()) "${patient.peso} kg" else "--"
        binding.tvDetailHeight.text = if (patient.estatura.isNotEmpty()) "${patient.estatura} m" else "--"
        binding.tvDetailSex.text = if (patient.sexo.equals("F", ignoreCase = true)) "F" else "M"
        binding.tvDetailBirthDate.text = com.example.sistemaclinico.utils.formatBirthDateWithAge(patient.fechaNacimiento)
    }

    // Renderiza el listado de alergias diagnosticadas en el paciente
    private fun renderAllergies(allergies: List<AllergyDetail>) {
        binding.containerAllergies.removeAllViews()
        binding.tvAllergiesCountBadge.text = allergies.size.toString()

        if (allergies.isEmpty()) {
            binding.tvEmptyAllergies.visibility = View.VISIBLE
            return
        }
        binding.tvEmptyAllergies.visibility = View.GONE

        allergies.forEach { allergy ->
            binding.containerAllergies.addView(buildAllergyItem(allergy))
        }
    }

    // Construye la vista individual para cada alergia diagnosticada
    private fun buildAllergyItem(allergy: AllergyDetail): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@PatientDetailActivity, R.drawable.bg_chip_soft)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(10) }
        }

        // Fila 1: Nombre + Badge de Categoría + Badge de Severidad
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvName = TextView(this).apply {
            text = allergy.nombre
            setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.text_primary))
            textSize = 15.5f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topRow.addView(tvName)

        if (allergy.categoria.isNotEmpty()) {
            val badgeCat = TextView(this).apply {
                text = allergy.categoria
                setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.primary_teal))
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                background = ContextCompat.getDrawable(this@PatientDetailActivity, R.drawable.bg_chip_soft)
                setPadding(dp(6), dp(2), dp(6), dp(2))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = dp(6) }
            }
            topRow.addView(badgeCat)
        }

        val isSevere = allergy.severidad.contains("Severa", ignoreCase = true)
        val isLeve = allergy.severidad.contains("Leve", ignoreCase = true)
        val badgeSev = TextView(this).apply {
            text = allergy.severidad.ifEmpty { "Moderada" }
            setTextColor(
                if (isSevere) ContextCompat.getColor(this@PatientDetailActivity, R.color.secondary_crimson)
                else if (isLeve) 0xFF16A34A.toInt()
                else 0xFFD97706.toInt()
            )
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            background = ContextCompat.getDrawable(
                this@PatientDetailActivity,
                if (isSevere) R.drawable.bg_badge_blood else R.drawable.bg_chip_soft
            )
            setPadding(dp(8), dp(2), dp(8), dp(2))
        }
        topRow.addView(badgeSev)
        container.addView(topRow)

        // Fila 2: Fecha de diagnóstico
        if (allergy.fecha.isNotEmpty()) {
            val tvDate = TextView(this).apply {
                text = getString(R.string.allergy_diagnosis_date_format, allergy.fecha)
                setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.text_muted))
                textSize = 12f
                setPadding(0, dp(2), 0, 0)
            }
            container.addView(tvDate)
        }

        // Fila 3: Reacción del paciente (Título en negrita, descripción normal)
        if (allergy.sintomas.isNotEmpty()) {
            val prefix = getString(R.string.allergy_reaction_prefix)
            val spanSintomas = android.text.SpannableStringBuilder().apply {
                append(prefix)
                setSpan(android.text.style.StyleSpan(Typeface.BOLD), 0, prefix.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                append(allergy.sintomas)
            }
            val tvSintomas = TextView(this).apply {
                text = spanSintomas
                setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.text_primary))
                textSize = 13f
                setPadding(0, dp(4), 0, 0)
            }
            container.addView(tvSintomas)
        }

        // Fila 4: Protocolo de Rescate / Emergencia (Título en negrita, contenido normal)
        if (allergy.tratamientoRescate.isNotEmpty()) {
            val prefix = getString(R.string.allergy_rescue_prefix)
            val spanRescate = android.text.SpannableStringBuilder().apply {
                append(prefix)
                setSpan(android.text.style.StyleSpan(Typeface.BOLD), 0, prefix.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                append(allergy.tratamientoRescate)
            }
            val tvRescate = TextView(this).apply {
                text = spanRescate
                setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.primary_teal))
                textSize = 12.5f
                setPadding(0, dp(2), 0, 0)
            }
            container.addView(tvRescate)
        }

        return container
    }

    // Configura y renderiza el historial de biometrías hemáticas del paciente
    private fun setupBiometrias(patient: PatientProfile) {
        val biometrias = patient.biometrias
        binding.tvBiometriaCountBadge.text = biometrias.size.toString()
        binding.chipGroupBiometriaStudies.removeAllViews()
        binding.containerBiometriaPanel.removeAllViews()

        if (biometrias.isEmpty()) {
            binding.tvEmptyBlood.visibility = View.VISIBLE
            binding.hsvBiometriaStudies.visibility = View.GONE
            return
        }

        binding.tvEmptyBlood.visibility = View.GONE
        binding.hsvBiometriaStudies.visibility = View.VISIBLE

        biometrias.forEachIndexed { index, bio ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                id = View.generateViewId()
                text = getString(R.string.biometria_study_num_format, index + 1, bio.fecha)
                isCheckable = true
                isChecked = index == 0
                setOnClickListener {
                    renderBiometriaPanel(bio)
                }
            }
            binding.chipGroupBiometriaStudies.addView(chip)
        }

        renderBiometriaPanel(biometrias.first())
    }

    private data class AnalyteItem(
        val name: String,
        val valueStr: String,
        val refRange: String,
        val status: StatusLevel
    )

    private enum class StatusLevel { NORMAL, LOW, HIGH }

    // Evalúa si un valor numérico se encuentra en rango normal, bajo o alto
    private fun evalRange(valNum: Double, minRef: Double, maxRef: Double): StatusLevel {
        return when {
            valNum < minRef -> StatusLevel.LOW
            valNum > maxRef -> StatusLevel.HIGH
            else -> StatusLevel.NORMAL
        }
    }

    // Renderiza el panel detallado de biometría hemática con series y trazabilidad
    private fun renderBiometriaPanel(bio: BiometriaHematica) {
        binding.containerBiometriaPanel.removeAllViews()
        val isFemale = patientProfile?.sexo.equals("F", ignoreCase = true)

        // 1. SERIE ROJA (Eritroide) con rangos diferenciados por sexo
        val rbcVal = bio.eritrocitos.toDoubleOrNull() ?: 0.0
        val hbVal = bio.hemoglobina.toDoubleOrNull() ?: 0.0
        val hctVal = bio.hematocrito.toDoubleOrNull() ?: 0.0
        val vcmVal = bio.vcm.toDoubleOrNull() ?: 0.0
        val hcmVal = bio.hcm.toDoubleOrNull() ?: if (rbcVal > 0) ((hbVal * 10.0) / rbcVal) else 30.0
        val chcmVal = bio.chcm.toDoubleOrNull() ?: if (hctVal > 0) ((hbVal * 100.0) / hctVal) else 33.3

        val rbcMin = if (isFemale) 4.2 else 4.7
        val rbcMax = if (isFemale) 5.4 else 6.1
        val rbcRefStr = if (isFemale) getString(R.string.bio_rbc_ref_female) else getString(R.string.bio_rbc_ref_male)

        val hbMin = if (isFemale) 12.0 else 13.8
        val hbMax = if (isFemale) 15.5 else 17.5
        val hbRefStr = if (isFemale) getString(R.string.bio_hb_ref_female) else getString(R.string.bio_hb_ref_male)

        val hctMin = if (isFemale) 37.0 else 42.0
        val hctMax = if (isFemale) 47.0 else 52.0
        val hctRefStr = if (isFemale) getString(R.string.bio_hct_ref_female) else getString(R.string.bio_hct_ref_male)

        val cardRed = buildSeriesCard(
            title = getString(R.string.biometria_serie_red_gender_format, getString(R.string.biometria_serie_red), if (isFemale) getString(R.string.gender_female) else getString(R.string.gender_male)),
            colorRes = R.color.secondary_crimson,
            items = listOf(
                AnalyteItem(getString(R.string.bio_rbc), "${bio.eritrocitos} ×10⁶/µL", rbcRefStr, evalRange(rbcVal, rbcMin, rbcMax)),
                AnalyteItem(getString(R.string.bio_hb), "${bio.hemoglobina} g/dL", hbRefStr, evalRange(hbVal, hbMin, hbMax)),
                AnalyteItem(getString(R.string.bio_hct), "${bio.hematocrito} %", hctRefStr, evalRange(hctVal, hctMin, hctMax)),
                AnalyteItem(getString(R.string.bio_vcm), "${bio.vcm} fL", getString(R.string.bio_vcm_ref), evalRange(vcmVal, 80.0, 100.0)),
                AnalyteItem(getString(R.string.bio_hcm), "${String.format("%.1f", hcmVal)} pg", getString(R.string.bio_hcm_ref), evalRange(hcmVal, 27.0, 33.0)),
                AnalyteItem(getString(R.string.bio_chcm), "${String.format("%.1f", chcmVal)} g/dL", getString(R.string.bio_chcm_ref), evalRange(chcmVal, 32.0, 36.0))
            )
        )
        binding.containerBiometriaPanel.addView(cardRed)

        // 2. SERIE BLANCA (Leucocitaria)
        val cardWhite = buildSeriesCard(
            title = getString(R.string.biometria_serie_white),
            colorRes = R.color.tertiary_blue,
            items = listOf(
                AnalyteItem(getString(R.string.bio_wbc), "${bio.leucocitos} ×10³/µL", getString(R.string.bio_wbc_ref), evalRange(bio.leucocitos.toDoubleOrNull() ?: 0.0, 4.5, 11.0)),
                AnalyteItem(getString(R.string.bio_neut), "${bio.neutrofilos} %", getString(R.string.bio_neut_ref), evalRange(bio.neutrofilos.toDoubleOrNull() ?: 0.0, 45.0, 70.0)),
                AnalyteItem(getString(R.string.bio_linf), "${bio.linfocitos} %", getString(R.string.bio_linf_ref), evalRange(bio.linfocitos.toDoubleOrNull() ?: 0.0, 20.0, 45.0)),
                AnalyteItem(getString(R.string.bio_mono), "${bio.monocitos} %", getString(R.string.bio_mono_ref), evalRange(bio.monocitos.toDoubleOrNull() ?: 0.0, 2.0, 10.0)),
                AnalyteItem(getString(R.string.bio_eos), "${bio.eosinofilos} %", getString(R.string.bio_eos_ref), evalRange(bio.eosinofilos.toDoubleOrNull() ?: 0.0, 1.0, 4.0))
            )
        )
        binding.containerBiometriaPanel.addView(cardWhite)

        // 3. SERIE PLAQUETARIA (Hemostasia)
        val cardPlt = buildSeriesCard(
            title = getString(R.string.biometria_serie_platelets),
            colorRes = R.color.primary_teal,
            items = listOf(
                AnalyteItem(getString(R.string.bio_plt), "${bio.plaquetas} ×10³/µL", getString(R.string.bio_plt_ref), evalRange(bio.plaquetas.toDoubleOrNull() ?: 150.0, 150.0, 450.0))
            )
        )
        binding.containerBiometriaPanel.addView(cardPlt)

        // 4. CONTROL DE CALIDAD Y TRAZABILIDAD
        val traceBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@PatientDetailActivity, R.drawable.bg_chip_soft)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(8) }
        }

        val tvTraceHeader = TextView(this).apply {
            text = getString(R.string.biometria_traceability_title)
            setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.text_primary))
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(4))
        }
        traceBox.addView(tvTraceHeader)

        // Agrega una fila de trazabilidad para fechas de toma y análisis
        fun addTraceRow(label: String, value: String) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(2), 0, dp(2))
            }
            val tvLbl = TextView(this).apply {
                text = label
                setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.text_muted))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(dp(130), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val tvVal = TextView(this).apply {
                text = value
                setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.text_primary))
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(tvLbl)
            row.addView(tvVal)
            traceBox.addView(row)
        }

        addTraceRow(getString(R.string.bio_folio_label), bio.folio.ifEmpty { "BH-${bio.idEstudio}" })
        addTraceRow(getString(R.string.bio_sample_label), bio.tipoMuestra.ifEmpty { "--" })
        addTraceRow(getString(R.string.bio_responsible_label), bio.responsable.ifEmpty { "--" })
        addTraceRow(getString(R.string.bio_frotis_label), bio.observacionesFrotis.ifEmpty { "--" })

        binding.containerBiometriaPanel.addView(traceBox)

        // 5. INTERPRETACIÓN CLÍNICA AUTOMATIZADA
        val diagBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@PatientDetailActivity, R.drawable.bg_chip_soft)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = dp(2) }
        }

        val tvDiagHeader = TextView(this).apply {
            text = getString(R.string.biometria_diagnosis_title)
            setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.text_primary))
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        diagBox.addView(tvDiagHeader)

        val tvDiagContent = TextView(this).apply {
            text = bio.diagnostico.ifBlank { getString(R.string.bio_default_normal_interpretation) }
            setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.primary_teal))
            textSize = 13f
            setPadding(0, dp(4), 0, 0)
        }
        diagBox.addView(tvDiagContent)

        binding.containerBiometriaPanel.addView(diagBox)
    }

    // Construye la tarjeta visual contenedora de una serie hematológica con sus analitos
    private fun buildSeriesCard(title: String, colorRes: Int, items: List<AnalyteItem>): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@PatientDetailActivity, R.drawable.bg_chip_soft)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(8) }
        }

        val tvTitle = TextView(this).apply {
            text = title
            setTextColor(ContextCompat.getColor(this@PatientDetailActivity, colorRes))
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(6))
        }
        container.addView(tvTitle)

        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(3), 0, dp(3))
            }

            val tvName = TextView(this).apply {
                text = item.name
                setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.text_primary))
                textSize = 12.5f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f)
            }
            row.addView(tvName)

            val tvVal = TextView(this).apply {
                text = item.valueStr
                setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.text_primary))
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
            }
            row.addView(tvVal)

            val badge = TextView(this).apply {
                text = when (item.status) {
                    StatusLevel.NORMAL -> getString(R.string.status_normal)
                    StatusLevel.LOW -> getString(R.string.status_low)
                    StatusLevel.HIGH -> getString(R.string.status_high)
                }
                setTextColor(when (item.status) {
                    StatusLevel.NORMAL -> 0xFF16A34A.toInt()
                    StatusLevel.LOW -> 0xFFDC2626.toInt()
                    StatusLevel.HIGH -> 0xFFD97706.toInt()
                })
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(dp(60), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            row.addView(badge)

            val tvRef = TextView(this).apply {
                text = item.refRange
                setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.text_muted))
                textSize = 11f
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(dp(95), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            row.addView(tvRef)

            container.addView(row)
        }

        return container
    }

    // Renderiza el historial de patologías oncológicas del paciente
    private fun renderOncology(treatments: List<DiagnosticoOncologico>) {
        binding.containerCancer.removeAllViews()
        binding.tvCancerCountBadge.text = treatments.size.toString()

        if (treatments.isEmpty()) {
            binding.tvEmptyCancer.visibility = View.VISIBLE
            return
        }
        binding.tvEmptyCancer.visibility = View.GONE

        treatments.forEach { t ->
            binding.containerCancer.addView(buildOncologyItem(t))
        }
    }

    // Construye la vista individual para una patología oncológica
    private fun buildOncologyItem(t: DiagnosticoOncologico): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@PatientDetailActivity, R.drawable.bg_chip_soft)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(8) }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvName = TextView(this).apply {
            text = t.tipoCancer
            setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.text_primary))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topRow.addView(tvName)

        val isPositive = t.resultado.equals("Positivo", ignoreCase = true)
        val badgeResult = TextView(this).apply {
            text = t.resultado.ifEmpty { "Evaluado" }
            setTextColor(if (isPositive) ContextCompat.getColor(this@PatientDetailActivity, R.color.secondary_crimson)
            else 0xFF16A34A.toInt())
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = ContextCompat.getDrawable(this@PatientDetailActivity, if (isPositive) R.drawable.bg_badge_blood else R.drawable.bg_chip_soft)
            setPadding(dp(8), dp(2), dp(8), dp(2))
        }
        topRow.addView(badgeResult)
        container.addView(topRow)

        if (t.fechaDeteccion.isNotEmpty()) {
            val tvDet = TextView(this).apply {
                text = getString(R.string.oncology_detected_date_format, t.fechaDeteccion)
                setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.text_secondary))
                textSize = 12.5f
                setPadding(0, dp(4), 0, 0)
            }
            container.addView(tvDet)
        }

        if (t.fechaInicioTratamiento.isNotEmpty() || t.fechaFinalTratamiento.isNotEmpty()) {
            val tvTrat = TextView(this).apply {
                text = getString(R.string.oncology_period_to_format, t.fechaInicioTratamiento.ifEmpty { "--" }, t.fechaFinalTratamiento.ifEmpty { "--" })
                setTextColor(ContextCompat.getColor(this@PatientDetailActivity, R.color.text_secondary))
                textSize = 12.5f
                setPadding(0, dp(2), 0, 0)
            }
            container.addView(tvTrat)
        }

        return container
    }

    // Configura el botón flotante de acciones rápidas (Speed Dial)
    private fun setupSpeedDial() {
        binding.fabMainAction.setOnClickListener {
            toggleSpeedDial()
        }
        binding.fabDimOverlay.setOnClickListener {
            toggleSpeedDial(false)
        }

        // Acciones del Speed Dial (los 3 botones flotantes)
        binding.btnDialAssignData.setOnClickListener {
            toggleSpeedDial(false)
            dialogManager.showAssignDataChoiceDialog()
        }
        binding.fabActionAssignData.setOnClickListener {
            toggleSpeedDial(false)
            dialogManager.showAssignDataChoiceDialog()
        }

        binding.btnDialDownloadPdf.setOnClickListener {
            toggleSpeedDial(false)
            patientProfile?.let { patient ->
                com.example.sistemaclinico.utils.PdfReportGenerator.generatePatientPdf(this, patient)
            } ?: Toast.makeText(this, getString(R.string.toast_loading_expediente), Toast.LENGTH_SHORT).show()
        }
        binding.fabActionDownloadPdf.setOnClickListener {
            toggleSpeedDial(false)
            patientProfile?.let { patient ->
                com.example.sistemaclinico.utils.PdfReportGenerator.generatePatientPdf(this, patient)
            } ?: Toast.makeText(this, getString(R.string.toast_loading_expediente), Toast.LENGTH_SHORT).show()
        }

        binding.btnDialEditPatient.setOnClickListener {
            toggleSpeedDial(false)
            dialogManager.showPatientEditHubDialog()
        }
        binding.fabActionEditPatient.setOnClickListener {
            toggleSpeedDial(false)
            dialogManager.showPatientEditHubDialog()
        }

        binding.btnDialDeletePatient.setOnClickListener {
            toggleSpeedDial(false)
            dialogManager.showDeletePatientDialog()
        }
        binding.fabActionDeletePatient.setOnClickListener {
            toggleSpeedDial(false)
            dialogManager.showDeletePatientDialog()
        }
    }

    // Controla la apertura y cierre animado del menú flotante de acciones
    private fun toggleSpeedDial(open: Boolean? = null) {
        val shouldOpen = open ?: !isSpeedDialOpen
        if (shouldOpen == isSpeedDialOpen) return
        isSpeedDialOpen = shouldOpen

        if (isSpeedDialOpen) {
            binding.fabDimOverlay.apply {
                alpha = 0f
                visibility = View.VISIBLE
                animate().alpha(1f).setDuration(200).start()
            }
            binding.layoutSpeedDialMenu.apply {
                alpha = 0f
                translationY = 40f
                visibility = View.VISIBLE
                animate().alpha(1f).translationY(0f).setDuration(220).start()
            }
            binding.fabMainAction.animate().rotation(45f).setDuration(200).start()
        } else {
            binding.fabDimOverlay.animate().alpha(0f).setDuration(180).withEndAction {
                binding.fabDimOverlay.visibility = View.GONE
            }.start()
            binding.layoutSpeedDialMenu.animate().alpha(0f).translationY(40f).setDuration(180).withEndAction {
                binding.layoutSpeedDialMenu.visibility = View.GONE
            }.start()
            binding.fabMainAction.animate().rotation(0f).setDuration(200).start()
        }
    }

    // Convierte valores de dp a píxeles de pantalla
    internal fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

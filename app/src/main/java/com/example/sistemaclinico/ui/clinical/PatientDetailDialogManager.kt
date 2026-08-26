package com.example.sistemaclinico.ui.clinical

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.sistemaclinico.R
import com.example.sistemaclinico.data.AllergyDetail
import com.example.sistemaclinico.data.BiometriaHematica
import com.example.sistemaclinico.data.DatabaseHelper
import com.example.sistemaclinico.data.DiagnosticoOncologico
import com.example.sistemaclinico.data.PatientProfile
import com.example.sistemaclinico.utils.DateMaskTextWatcher
import com.example.sistemaclinico.utils.getCleanText
import com.example.sistemaclinico.utils.setupWhitespaceSanitization

// Gestor modular de diálogos clínicos del expediente del paciente
class PatientDetailDialogManager(
    private val activity: PatientDetailActivity,
    private val dbHelper: DatabaseHelper
) {

    // Muestra el panel central de opciones de edición del expediente
    fun showPatientEditHubDialog() {
        val patient = activity.patientProfile ?: return
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_patient_edit_hub, null)

        val tvSub = dialogView.findViewById<TextView>(R.id.tvHubSubtitle)
        val cardGeneral = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardHubGeneral)
        val cardAllergies = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardHubAllergies)
        val tvAllergiesSub = dialogView.findViewById<TextView>(R.id.tvHubAllergiesSub)
        val cardBlood = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardHubBlood)
        val tvBloodSub = dialogView.findViewById<TextView>(R.id.tvHubBloodSub)
        val cardOncology = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardHubOncology)
        val tvOncologySub = dialogView.findViewById<TextView>(R.id.tvHubOncologySub)
        val btnClose = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHubClose)

        val formattedCode = if (patient.clave.startsWith("PAC-", ignoreCase = true)) patient.clave.uppercase() else "PAC-${patient.clave}"
        tvSub.text = activity.getString(R.string.dialog_patient_sub_format, patient.nombreCompleto, formattedCode)

        val nAllergies = patient.alergias.size
        tvAllergiesSub.text = if (nAllergies > 0) activity.getString(R.string.hub_summary_allergies_count, nAllergies) else activity.getString(R.string.hub_summary_no_allergies)

        val totalBlood = patient.biometrias.size
        tvBloodSub.text = if (totalBlood > 0) activity.getString(R.string.hub_summary_biometrias_count, totalBlood) else activity.getString(R.string.hub_summary_no_biometrias)

        val nOnco = patient.tratamientos.size
        tvOncologySub.text = if (nOnco > 0) activity.getString(R.string.hub_summary_cancer_count, nOnco) else activity.getString(R.string.hub_summary_no_cancer)

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        cardGeneral.setOnClickListener {
            dialog.dismiss()
            showEditPatientDialog(patient)
        }
        cardAllergies.setOnClickListener {
            dialog.dismiss()
            showSelectAllergyToEditDialog(patient)
        }
        cardBlood.setOnClickListener {
            dialog.dismiss()
            showSelectBiometriaToEditDialog(patient)
        }
        cardOncology.setOnClickListener {
            dialog.dismiss()
            showSelectCancerToEditDialog(patient)
        }
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }


    // Muestra el selector modal de alergias para editar o desvincular
    fun showSelectAllergyToEditDialog(patient: PatientProfile) {
        if (patient.alergias.isEmpty()) {
            Toast.makeText(activity, activity.getString(R.string.toast_no_allergies_registered), Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_select_item_list, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvSelectorTitle)
        val tvSub = dialogView.findViewById<TextView>(R.id.tvSelectorSubtitle)
        val container = dialogView.findViewById<LinearLayout>(R.id.containerSelectorItems)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectorCancel)

        tvTitle.text = activity.getString(R.string.detail_allergies_title)
        tvSub.text = activity.getString(R.string.dialog_select_allergy_to_modify)

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        patient.alergias.forEach { allergy ->
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(activity, R.drawable.bg_chip_soft)
                setPadding(activity.dp(14), activity.dp(10), activity.dp(14), activity.dp(10))
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = activity.dp(8) }
                setOnClickListener {
                    dialog.dismiss()
                    showEditPatientAllergyDialog(allergy)
                }
            }

            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvName = TextView(activity).apply {
                text = allergy.nombre
                setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                textSize = 14.5f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(tvName)

            val badge = TextView(activity).apply {
                text = allergy.severidad.ifEmpty { activity.getString(R.string.default_severity_moderate) }
                setTextColor(ContextCompat.getColor(activity, R.color.primary_teal))
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(activity.dp(6), activity.dp(2), activity.dp(6), activity.dp(2))
            }
            row.addView(badge)
            card.addView(row)

            if (allergy.fecha.isNotEmpty() || allergy.sintomas.isNotEmpty()) {
                val tvDet = TextView(activity).apply {
                    text = if (allergy.fecha.isNotEmpty()) "Fecha: ${allergy.fecha}" else allergy.sintomas
                    setTextColor(ContextCompat.getColor(activity, R.color.text_muted))
                    textSize = 12f
                    setPadding(0, activity.dp(2), 0, 0)
                }
                card.addView(tvDet)
            }

            container.addView(card)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }


    // Muestra el selector modal de estudios hematológicos para modificar o eliminar
    fun showSelectBiometriaToEditDialog(patient: PatientProfile) {
        if (patient.biometrias.isEmpty()) {
            Toast.makeText(activity, activity.getString(R.string.toast_no_biometrias_registered), Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_select_item_list, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvSelectorTitle)
        val tvSub = dialogView.findViewById<TextView>(R.id.tvSelectorSubtitle)
        val container = dialogView.findViewById<LinearLayout>(R.id.containerSelectorItems)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectorCancel)

        tvTitle.text = activity.getString(R.string.detail_blood_samples_title)
        tvSub.text = activity.getString(R.string.dialog_select_biometria_to_modify)

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        patient.biometrias.forEachIndexed { index, bio ->
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(activity, R.drawable.bg_chip_soft)
                setPadding(activity.dp(14), activity.dp(10), activity.dp(14), activity.dp(10))
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = activity.dp(8) }
                setOnClickListener {
                    dialog.dismiss()
                    showEditBiometriaDialog(bio)
                }
            }

            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvTitleBio = TextView(activity).apply {
                text = activity.getString(R.string.biometria_study_num_dot_format, index + 1, bio.fecha)
                setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                textSize = 14.5f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(tvTitleBio)

            val tvHb = TextView(activity).apply {
                text = activity.getString(R.string.biometria_hb_format, bio.hemoglobina)
                setTextColor(ContextCompat.getColor(activity, R.color.secondary_crimson))
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }
            row.addView(tvHb)
            card.addView(row)

            val tvSummary = TextView(activity).apply {
                text = activity.getString(R.string.biometria_summary_metrics_format, bio.eritrocitos, bio.leucocitos, bio.plaquetas)
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                textSize = 12f
                setPadding(0, activity.dp(3), 0, 0)
            }
            card.addView(tvSummary)

            if (bio.diagnostico.isNotBlank()) {
                val tvDiag = TextView(activity).apply {
                    text = bio.diagnostico
                    setTextColor(ContextCompat.getColor(activity, R.color.primary_teal))
                    textSize = 11.5f
                    setPadding(0, activity.dp(2), 0, 0)
                }
                card.addView(tvDiag)
            }

            container.addView(card)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }


    // Muestra el selector modal de diagnósticos oncológicos para modificar o desvincular
    fun showSelectCancerToEditDialog(patient: PatientProfile) {
        if (patient.tratamientos.isEmpty()) {
            Toast.makeText(activity, activity.getString(R.string.toast_no_cancer_registered), Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_select_item_list, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvSelectorTitle)
        val tvSub = dialogView.findViewById<TextView>(R.id.tvSelectorSubtitle)
        val container = dialogView.findViewById<LinearLayout>(R.id.containerSelectorItems)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectorCancel)

        tvTitle.text = activity.getString(R.string.detail_oncology_title)
        tvSub.text = activity.getString(R.string.dialog_select_cancer_to_modify)

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        patient.tratamientos.forEach { t ->
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(activity, R.drawable.bg_chip_soft)
                setPadding(activity.dp(14), activity.dp(10), activity.dp(14), activity.dp(10))
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = activity.dp(8) }
                setOnClickListener {
                    dialog.dismiss()
                    showEditPatientCancerDialog(t)
                }
            }

            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvName = TextView(activity).apply {
                text = t.tipoCancer
                setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                textSize = 14.5f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(tvName)

            val isPos = t.resultado.equals("Positivo", ignoreCase = true)
            val badge = TextView(activity).apply {
                text = t.resultado.ifEmpty { activity.getString(R.string.default_evaluated_status) }
                setTextColor(if (isPos) ContextCompat.getColor(activity, R.color.secondary_crimson) else 0xFF16A34A.toInt())
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(activity.dp(6), activity.dp(2), activity.dp(6), activity.dp(2))
            }
            row.addView(badge)
            card.addView(row)

            val detText = if (t.fechaDeteccion.isNotEmpty()) {
                activity.getString(R.string.oncology_detect_short_format, t.fechaDeteccion)
            } else if (t.fechaInicioTratamiento.isNotEmpty()) {
                activity.getString(R.string.oncology_start_short_format, t.fechaInicioTratamiento)
            } else {
                ""
            }

            if (detText.isNotEmpty()) {
                val tvDet = TextView(activity).apply {
                    text = detText
                    setTextColor(ContextCompat.getColor(activity, R.color.text_muted))
                    textSize = 12f
                    setPadding(0, activity.dp(2), 0, 0)
                }
                card.addView(tvDet)
            }

            container.addView(card)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }


    // Muestra el diálogo modal para editar los datos generales y medidas del paciente
    fun showEditPatientDialog(patient: PatientProfile) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_edit_patient, null)

        val nameParts = patient.nombreCompleto.split(" ")
        val defaultNombre = if (nameParts.isNotEmpty()) nameParts.first() else ""
        val defaultPat = if (nameParts.size > 1) nameParts[1] else ""
        val defaultMat = if (nameParts.size > 2) nameParts.drop(2).joinToString(" ") else ""

        val etName = dialogView.findViewById<EditText>(R.id.etEditPatientName).apply { setText(defaultNombre) }
        val etPat = dialogView.findViewById<EditText>(R.id.etEditPatientLastNameFather).apply { setText(defaultPat) }
        val etMat = dialogView.findViewById<EditText>(R.id.etEditPatientLastNameMother).apply { setText(defaultMat) }
        val etWeight = dialogView.findViewById<EditText>(R.id.etEditPatientWeight).apply { setText(patient.peso) }
        val etHeight = dialogView.findViewById<EditText>(R.id.etEditPatientHeight).apply { setText(patient.estatura) }
        val etBlood = dialogView.findViewById<EditText>(R.id.etEditPatientBloodType).apply { setText(patient.tipoSangre) }
        val etBirth = dialogView.findViewById<EditText>(R.id.etEditPatientBirthDate).apply {
            setText(patient.fechaNacimiento)
            addTextChangedListener(DateMaskTextWatcher(this))
        }

        val chipMale = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipEditSexMale)
        val chipFemale = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipEditSexFemale)
        if (patient.sexo.equals("F", ignoreCase = true) || patient.sexo.startsWith("Fem", ignoreCase = true)) {
            chipFemale?.isChecked = true
        } else {
            chipMale?.isChecked = true
        }

        setupWhitespaceSanitization(etName, etPat, etMat, etWeight, etHeight, etBlood)

        val tvSub = dialogView.findViewById<TextView>(R.id.tvEditPatientSubtitle)
        val formattedCode = if (patient.clave.startsWith("PAC-", ignoreCase = true)) patient.clave.uppercase() else "PAC-${patient.clave}"
        tvSub?.text = activity.getString(R.string.dialog_patient_sub_dot_format, patient.nombreCompleto, formattedCode)

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        val btnCancel = dialogView.findViewById<View>(R.id.btnEditPatientCancel)
        val btnSave = dialogView.findViewById<View>(R.id.btnEditPatientSave)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newName = etName.getCleanText()
            val newPat = etPat.getCleanText()
            val newMat = etMat.getCleanText()
            val newWeight = etWeight.getCleanText()
            val newHeight = etHeight.getCleanText()
            val newBlood = etBlood.getCleanText()
            val newBirth = etBirth.getCleanText()
            val newSex = if (chipFemale?.isChecked == true) "F" else "M"

            if (newName.isEmpty() && newPat.isEmpty()) {
                Toast.makeText(activity, activity.getString(R.string.toast_patient_name_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalName = if (newName.isNotEmpty()) newName else activity.getString(R.string.default_patient_fallback_name)

            val success = dbHelper.updatePaciente(
                clave = patient.clave,
                nombre = finalName,
                apellidoPat = newPat,
                apellidoMat = newMat,
                peso = newWeight,
                estatura = newHeight,
                tipoSangre = newBlood,
                fechaNacimiento = newBirth,
                sexo = newSex
            )

            if (success) {
                Toast.makeText(activity, activity.getString(R.string.toast_expediente_updated), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                activity.loadPatientProfile()
            } else {
                Toast.makeText(activity, activity.getString(R.string.toast_expediente_update_error), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.applyStandardDialogStyle(dialogView)
    }


    // Muestra el diálogo modal para editar los detalles o desvincular una alergia diagnosticada
    fun showEditPatientAllergyDialog(allergy: AllergyDetail) {
        val patient = activity.patientProfile ?: return
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_edit_patient_allergy, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogAllergyTitle)
        val actvSeverity = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.actvDialogAllergySeverity)
        val etSymptoms = dialogView.findViewById<EditText>(R.id.etDialogAllergySymptoms)
        val etRescue = dialogView.findViewById<EditText>(R.id.etDialogAllergyRescue)
        val etDate = dialogView.findViewById<EditText>(R.id.etDialogAllergyDate)

        tvTitle.text = activity.getString(R.string.dialog_edit_allergy_title_format, allergy.nombre)
        val severities = activity.resources.getStringArray(R.array.allergy_severities).toList()
        actvSeverity.setAdapter(android.widget.ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, severities))
        actvSeverity.setText(allergy.severidad.ifEmpty { "Moderada" }, false)

        etSymptoms.setText(allergy.sintomas)
        etRescue.setText(allergy.tratamientoRescate)
        etDate.setText(allergy.fecha)
        etDate.addTextChangedListener(DateMaskTextWatcher(etDate))

        setupWhitespaceSanitization(etSymptoms, etRescue)

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        val btnCancel = dialogView.findViewById<View>(R.id.btnEditAllergyCancel)
        val btnSave = dialogView.findViewById<View>(R.id.btnEditAllergySave)
        val btnDelete = dialogView.findViewById<View>(R.id.btnEditAllergyDelete)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newSev = actvSeverity.getCleanText().ifEmpty { activity.getString(R.string.default_severity_moderate) }
            val newSymptoms = etSymptoms.getCleanText()
            val newRescue = etRescue.getCleanText()
            val newDate = etDate.getCleanText()

            val success = dbHelper.updatePacienteAlergia(
                clavePaciente = patient.clave,
                claveAlergia = allergy.claveAlergia,
                fecha = newDate,
                severidad = newSev,
                sintomas = newSymptoms,
                rescate = newRescue
            )
            if (success) {
                Toast.makeText(activity, activity.getString(R.string.toast_allergy_updated), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                activity.loadPatientProfile()
            } else {
                Toast.makeText(activity, activity.getString(R.string.toast_allergy_update_error), Toast.LENGTH_SHORT).show()
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.dialog_confirm_delete_title))
                .setMessage(activity.getString(R.string.dialog_delete_allergy_msg, allergy.nombre))
                .setPositiveButton(activity.getString(R.string.dialog_delete_confirm_btn)) { _, _ ->
                    val deleted = dbHelper.deletePacienteAlergia(patient.clave, allergy.claveAlergia)
                    if (deleted) {
                        Toast.makeText(activity, activity.getString(R.string.toast_allergy_unlinked), Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        activity.loadPatientProfile()
                    }
                }
                .setNegativeButton(activity.getString(R.string.dialog_cancel_btn), null)
                .show()
        }

        dialog.applyStandardDialogStyle(dialogView)
    }


    // Muestra el diálogo modal para editar los valores de una biometría hemática completa
    fun showEditBiometriaDialog(bio: BiometriaHematica) {
        val patient = activity.patientProfile ?: return
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_edit_biometria, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogBioTitle)
        val etFolio = dialogView.findViewById<EditText>(R.id.etBioFolio)
        val etDate = dialogView.findViewById<EditText>(R.id.etBioDate)
        val etRBC = dialogView.findViewById<EditText>(R.id.etBioRBC)
        val etHb = dialogView.findViewById<EditText>(R.id.etBioHb)
        val etHct = dialogView.findViewById<EditText>(R.id.etBioHct)
        val etVCM = dialogView.findViewById<EditText>(R.id.etBioVCM)
        val etHCM = dialogView.findViewById<EditText>(R.id.etBioHCM)
        val etCHCM = dialogView.findViewById<EditText>(R.id.etBioCHCM)
        val etWBC = dialogView.findViewById<EditText>(R.id.etBioWBC)
        val etNeut = dialogView.findViewById<EditText>(R.id.etBioNeut)
        val etLinf = dialogView.findViewById<EditText>(R.id.etBioLinf)
        val etMono = dialogView.findViewById<EditText>(R.id.etBioMono)
        val etEos = dialogView.findViewById<EditText>(R.id.etBioEos)
        val etPLT = dialogView.findViewById<EditText>(R.id.etBioPLT)
        val etSample = dialogView.findViewById<EditText>(R.id.etBioSample)
        val etResp = dialogView.findViewById<EditText>(R.id.etBioResp)
        val etFrotis = dialogView.findViewById<EditText>(R.id.etBioFrotis)
        val etDiag = dialogView.findViewById<EditText>(R.id.etBioDiag)

        tvTitle.text = activity.getString(R.string.dialog_edit_biometria_title_format, patient.nombreCompleto)
        etFolio.setText(bio.folio)
        etDate.setText(bio.fecha)
        etRBC.setText(bio.eritrocitos)
        etHb.setText(bio.hemoglobina)
        etHct.setText(bio.hematocrito)
        etVCM.setText(bio.vcm)
        etHCM.setText(bio.hcm)
        etCHCM.setText(bio.chcm)
        etWBC.setText(bio.leucocitos)
        etNeut.setText(bio.neutrofilos)
        etLinf.setText(bio.linfocitos)
        etMono.setText(bio.monocitos)
        etEos.setText(bio.eosinofilos)
        etPLT.setText(bio.plaquetas)
        etSample.setText(bio.tipoMuestra)
        etResp.setText(bio.responsable)
        etFrotis.setText(bio.observacionesFrotis)
        etDiag.setText(bio.diagnostico)

        etDate.addTextChangedListener(DateMaskTextWatcher(etDate))

        var isCalculating = false
        // Recalcula automáticamente los índices hematológicos calculados (Hct, VCM, HCM, CHCM)
        fun recalculateDialogIndices() {
            if (isCalculating) return
            val rbc = etRBC.getCleanText().toDoubleOrNull() ?: 0.0
            val hb = etHb.getCleanText().toDoubleOrNull() ?: 0.0
            if (hb > 0.0) {
                isCalculating = true
                val hct = hb * 3.0
                etHct.setText(String.format(java.util.Locale.US, "%.1f", hct))
                if (rbc > 0.0) {
                    val vcm = (hct * 10.0) / rbc
                    val hcm = (hb * 10.0) / rbc
                    val chcm = (hb * 100.0) / hct
                    etVCM.setText(String.format(java.util.Locale.US, "%.1f", vcm))
                    etHCM.setText(String.format(java.util.Locale.US, "%.1f", hcm))
                    etCHCM.setText(String.format(java.util.Locale.US, "%.1f", chcm))
                }
                isCalculating = false
            }
        }

        val autoWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { recalculateDialogIndices() }
        }
        etRBC.addTextChangedListener(autoWatcher)
        etHb.addTextChangedListener(autoWatcher)

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBioCancel)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBioSave)
        val btnDelete = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBioDelete)

        btnSave.text = activity.getString(R.string.dialog_save_btn)
        btnDelete.visibility = View.VISIBLE

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newFolio = etFolio.getCleanText()
            val newDate = etDate.getCleanText()
            val rbc = etRBC.getCleanText().toDoubleOrNull() ?: 4.5
            val hb = etHb.getCleanText().toDoubleOrNull() ?: 14.0
            val hct = etHct.getCleanText().toDoubleOrNull() ?: (hb * 3.0)
            val vcm = etVCM.getCleanText().toDoubleOrNull() ?: ((hct * 10.0) / rbc)
            val hcm = etHCM.getCleanText().toDoubleOrNull() ?: ((hb * 10.0) / rbc)
            val chcm = etCHCM.getCleanText().toDoubleOrNull() ?: ((hb * 100.0) / hct)
            val wbc = etWBC.getCleanText().toDoubleOrNull() ?: 7.0
            val neut = etNeut.getCleanText().toDoubleOrNull() ?: 60.0
            val linf = etLinf.getCleanText().toDoubleOrNull() ?: 30.0
            val mono = etMono.getCleanText().toDoubleOrNull() ?: 6.0
            val eos = etEos.getCleanText().toDoubleOrNull() ?: 3.0
            val plt = etPLT.getCleanText().toDoubleOrNull() ?: 250.0
            val sample = etSample.getCleanText()
            val resp = etResp.getCleanText()
            val frotis = etFrotis.getCleanText()
            val diag = etDiag.getCleanText()

            val success = dbHelper.updateBiometria(
                idEstudio = bio.idEstudio,
                fecha = newDate,
                eritrocitos = rbc,
                hemoglobina = hb,
                hematocrito = hct,
                vcm = vcm,
                hcm = hcm,
                chcm = chcm,
                leucocitos = wbc,
                neutrofilos = neut,
                linfocitos = linf,
                monocitos = mono,
                eosinofilos = eos,
                plaquetas = plt,
                sexo = patient.sexo,
                folio = newFolio,
                tipoMuestra = sample,
                responsable = resp,
                observacionesFrotis = frotis,
                diagnosticoManual = diag
            )
            if (success) {
                Toast.makeText(activity, activity.getString(R.string.toast_biometria_updated), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                activity.loadPatientProfile()
            } else {
                Toast.makeText(activity, activity.getString(R.string.toast_biometria_update_error), Toast.LENGTH_SHORT).show()
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.dialog_confirm_delete_title))
                .setMessage(activity.getString(R.string.dialog_delete_biometria_msg))
                .setPositiveButton(activity.getString(R.string.dialog_delete_confirm_btn)) { _, _ ->
                    val deleted = dbHelper.deleteBiometria(bio.idEstudio)
                    if (deleted) {
                        Toast.makeText(activity, activity.getString(R.string.toast_biometria_deleted), Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        activity.loadPatientProfile()
                    }
                }
                .setNegativeButton(activity.getString(R.string.dialog_cancel_btn), null)
                .show()
        }

        dialog.applyStandardDialogStyle(dialogView)
    }




    // Muestra el diálogo modal para editar el estado o fechas de un diagnóstico oncológico
    fun showEditPatientCancerDialog(cancer: DiagnosticoOncologico) {
        val patient = activity.patientProfile ?: return
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_edit_patient_cancer, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogCancerTitle)
        val actvResult = dialogView.findViewById<AutoCompleteTextView>(R.id.actvDialogCancerResult)
        val etDetectDate = dialogView.findViewById<EditText>(R.id.etDialogCancerDetectDate)
        val etStartDate = dialogView.findViewById<EditText>(R.id.etDialogCancerStartDate)
        val etEndDate = dialogView.findViewById<EditText>(R.id.etDialogCancerEndDate)

        val resultsList = activity.resources.getStringArray(R.array.oncology_results).toList()
        actvResult.setAdapter(ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, resultsList))
        actvResult.setText(if (cancer.resultado.isNotBlank()) cancer.resultado else "Positivo", false)

        tvTitle.text = activity.getString(R.string.dialog_edit_cancer_title_format, cancer.tipoCancer)
        etDetectDate.setText(cancer.fechaDeteccion)
        etStartDate.setText(cancer.fechaInicioTratamiento)
        etEndDate.setText(cancer.fechaFinalTratamiento)

        etDetectDate.addTextChangedListener(DateMaskTextWatcher(etDetectDate))
        etStartDate.addTextChangedListener(DateMaskTextWatcher(etStartDate))
        etEndDate.addTextChangedListener(DateMaskTextWatcher(etEndDate))

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        val btnCancel = dialogView.findViewById<View>(R.id.btnEditCancerCancel)
        val btnSave = dialogView.findViewById<View>(R.id.btnEditCancerSave)
        val btnDelete = dialogView.findViewById<View>(R.id.btnEditCancerDelete)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newResult = actvResult.getCleanText().ifEmpty { activity.getString(R.string.default_result_positive) }
            val newDetect = etDetectDate.getCleanText()
            val newStart = etStartDate.getCleanText()
            val newEnd = etEndDate.getCleanText()

            val success = dbHelper.updatePacienteCancer(
                clavePaciente = patient.clave,
                claveCancer = cancer.claveCancer,
                resultado = newResult,
                fechaDetectada = newDetect,
                fechaInicio = newStart,
                fechaFin = newEnd
            )
            if (success) {
                Toast.makeText(activity, activity.getString(R.string.toast_cancer_updated), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                activity.loadPatientProfile()
            } else {
                Toast.makeText(activity, activity.getString(R.string.toast_cancer_update_error), Toast.LENGTH_SHORT).show()
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.dialog_confirm_delete_title))
                .setMessage(activity.getString(R.string.dialog_delete_cancer_msg, cancer.tipoCancer))
                .setPositiveButton(activity.getString(R.string.dialog_delete_confirm_btn)) { _, _ ->
                    val deleted = dbHelper.deletePacienteCancer(patient.clave, cancer.claveCancer)
                    if (deleted) {
                        Toast.makeText(activity, activity.getString(R.string.toast_cancer_unlinked), Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        activity.loadPatientProfile()
                    }
                }
                .setNegativeButton(activity.getString(R.string.dialog_cancel_btn), null)
                .show()
        }

        dialog.applyStandardDialogStyle(dialogView)
    }

    private var isSpeedDialOpen = false


    // Muestra el menú de selección para asociar nuevos datos al expediente
    fun showAssignDataChoiceDialog() {
        val patient = activity.patientProfile ?: return
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_assign_data_choice, null)

        val tvSub = dialogView.findViewById<TextView>(R.id.tvAssignChoiceSub)
        val cardBio = dialogView.findViewById<View>(R.id.cardAssignBiometria)
        val cardAllergy = dialogView.findViewById<View>(R.id.cardAssignAllergy)
        val cardCancer = dialogView.findViewById<View>(R.id.cardAssignCancer)
        val btnCancel = dialogView.findViewById<View>(R.id.btnAssignChoiceCancel)

        val formattedCode = if (patient.clave.startsWith("PAC-", ignoreCase = true)) patient.clave.uppercase() else "PAC-${patient.clave}"
        tvSub.text = activity.getString(R.string.dialog_patient_sub_dot_format, patient.nombreCompleto, formattedCode)

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        cardBio.setOnClickListener {
            dialog.dismiss()
            showAddBiometriaDialog()
        }

        cardAllergy.setOnClickListener {
            dialog.dismiss()
            showAddAllergyDialog()
        }

        cardCancer.setOnClickListener {
            dialog.dismiss()
            showAddCancerDialog()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }


    // Muestra la confirmación para eliminar definitivamente el expediente del paciente
    fun showDeletePatientDialog() {
        val current = activity.patientProfile ?: return
        val formattedCode = if (current.clave.startsWith("PAC-", ignoreCase = true)) current.clave.uppercase() else "PAC-${current.clave}"
        val totalBlood = current.globulosRojos.size + current.globulosBlancos.size + current.biometrias.size

        val message = buildString {
            append(activity.getString(R.string.dialog_delete_patient_detail_confirm_msg, current.nombreCompleto, formattedCode))
            append("\n")
            append(activity.getString(R.string.dialog_delete_patient_item_personal))
            append("\n")
            if (current.alergias.isNotEmpty()) {
                append(activity.getString(R.string.dialog_delete_patient_item_allergies, current.alergias.size))
                append("\n")
            }
            if (totalBlood > 0) {
                append(activity.getString(R.string.dialog_delete_patient_item_blood, totalBlood))
                append("\n")
            }
            if (current.tratamientos.isNotEmpty()) {
                append(activity.getString(R.string.dialog_delete_patient_item_cancer, current.tratamientos.size))
                append("\n")
            }
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.dialog_delete_patient_expediente_title))
            .setMessage(message)
            .setPositiveButton(activity.getString(R.string.dialog_delete_definitively_btn)) { _, _ ->
                val deleted = dbHelper.deleteRecord(DatabaseHelper.TABLE_PACIENTES, "clave_paciente", current.clave)
                if (deleted) {
                    Toast.makeText(activity, activity.getString(R.string.toast_patient_deleted, current.nombreCompleto), Toast.LENGTH_SHORT).show()
                    activity.finish()
                } else {
                    Toast.makeText(activity, activity.getString(R.string.toast_patient_delete_error), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(activity.getString(R.string.dialog_cancel_btn), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(activity, R.color.secondary_crimson))
        }
        dialog.show()
    }

    // Muestra el formulario para registrar un nuevo estudio hematológico completo
    fun showAddBiometriaDialog() {
        val patient = activity.patientProfile ?: return
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_edit_biometria, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogBioTitle)
        val etFolio = dialogView.findViewById<EditText>(R.id.etBioFolio)
        val etDate = dialogView.findViewById<EditText>(R.id.etBioDate)
        val etRBC = dialogView.findViewById<EditText>(R.id.etBioRBC)
        val etHb = dialogView.findViewById<EditText>(R.id.etBioHb)
        val etHct = dialogView.findViewById<EditText>(R.id.etBioHct)
        val etVCM = dialogView.findViewById<EditText>(R.id.etBioVCM)
        val etHCM = dialogView.findViewById<EditText>(R.id.etBioHCM)
        val etCHCM = dialogView.findViewById<EditText>(R.id.etBioCHCM)
        val etWBC = dialogView.findViewById<EditText>(R.id.etBioWBC)
        val etNeut = dialogView.findViewById<EditText>(R.id.etBioNeut)
        val etLinf = dialogView.findViewById<EditText>(R.id.etBioLinf)
        val etMono = dialogView.findViewById<EditText>(R.id.etBioMono)
        val etEos = dialogView.findViewById<EditText>(R.id.etBioEos)
        val etPLT = dialogView.findViewById<EditText>(R.id.etBioPLT)
        val etSample = dialogView.findViewById<EditText>(R.id.etBioSample)
        val etResp = dialogView.findViewById<EditText>(R.id.etBioResp)
        val etFrotis = dialogView.findViewById<EditText>(R.id.etBioFrotis)
        val etDiag = dialogView.findViewById<EditText>(R.id.etBioDiag)

        tvTitle.text = activity.getString(R.string.dialog_register_biometria_title_format, patient.nombreCompleto)
        etDate.addTextChangedListener(DateMaskTextWatcher(etDate))

        var isCalculating = false
        // Recalcula los índices hematológicos según los valores ingresados de hemoglobina y eritrocitos
        fun recalculateIndices() {
            if (isCalculating) return
            val rbc = etRBC.getCleanText().toDoubleOrNull() ?: 0.0
            val hb = etHb.getCleanText().toDoubleOrNull() ?: 0.0
            if (hb > 0.0) {
                isCalculating = true
                val hct = hb * 3.0
                etHct.setText(String.format(java.util.Locale.US, "%.1f", hct))
                if (rbc > 0.0) {
                    val vcm = (hct * 10.0) / rbc
                    val hcm = (hb * 10.0) / rbc
                    val chcm = (hb * 100.0) / hct
                    etVCM.setText(String.format(java.util.Locale.US, "%.1f", vcm))
                    etHCM.setText(String.format(java.util.Locale.US, "%.1f", hcm))
                    etCHCM.setText(String.format(java.util.Locale.US, "%.1f", chcm))
                }
                isCalculating = false
            }
        }

        val autoWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { recalculateIndices() }
        }
        etRBC.addTextChangedListener(autoWatcher)
        etHb.addTextChangedListener(autoWatcher)

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        val btnCancel = dialogView.findViewById<View>(R.id.btnBioCancel)
        val btnSave = dialogView.findViewById<View>(R.id.btnBioSave)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val folio = etFolio.getCleanText()
            val fecha = etDate.getCleanText()
            val rbc = etRBC.getCleanText().toDoubleOrNull() ?: 0.0
            val hb = etHb.getCleanText().toDoubleOrNull() ?: 0.0
            val hct = etHct.getCleanText().toDoubleOrNull() ?: (hb * 3.0)
            val vcm = etVCM.getCleanText().toDoubleOrNull() ?: if (rbc > 0) ((hct * 10.0) / rbc) else 90.0
            val hcm = etHCM.getCleanText().toDoubleOrNull() ?: if (rbc > 0) ((hb * 10.0) / rbc) else 30.0
            val chcm = etCHCM.getCleanText().toDoubleOrNull() ?: if (hct > 0) ((hb * 100.0) / hct) else 33.3
            val wbc = etWBC.getCleanText().toDoubleOrNull() ?: 0.0
            val neut = etNeut.getCleanText().toDoubleOrNull() ?: 60.0
            val linf = etLinf.getCleanText().toDoubleOrNull() ?: 30.0
            val mono = etMono.getCleanText().toDoubleOrNull() ?: 6.0
            val eos = etEos.getCleanText().toDoubleOrNull() ?: 3.0
            val plt = etPLT.getCleanText().toDoubleOrNull() ?: 0.0
            val sample = etSample.getCleanText()
            val resp = etResp.getCleanText()
            val frotis = etFrotis.getCleanText()
            val diag = etDiag.getCleanText()

            if (fecha.isEmpty()) {
                Toast.makeText(activity, activity.getString(R.string.toast_biometria_date_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (rbc <= 0.0 || hb <= 0.0 || wbc <= 0.0 || plt <= 0.0) {
                Toast.makeText(activity, activity.getString(R.string.toast_biometria_values_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = dbHelper.insertBiometria(
                clavePaciente = patient.clave,
                fecha = fecha,
                eritrocitos = rbc,
                hemoglobina = hb,
                hematocrito = hct,
                vcm = vcm,
                hcm = hcm,
                chcm = chcm,
                leucocitos = wbc,
                neutrofilos = neut,
                linfocitos = linf,
                monocitos = mono,
                eosinofilos = eos,
                plaquetas = plt,
                sexo = patient.sexo,
                folio = folio,
                tipoMuestra = sample,
                responsable = resp,
                observacionesFrotis = frotis,
                diagnosticoManual = diag
            )

            if (success) {
                Toast.makeText(activity, activity.getString(R.string.toast_biometria_assigned), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                activity.loadPatientProfile()
            } else {
                Toast.makeText(activity, activity.getString(R.string.toast_biometria_assign_error), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.applyStandardDialogStyle(dialogView)
    }


    // Muestra el diálogo para asociar una alergia del catálogo al paciente
    fun showAddAllergyDialog() {
        val patient = activity.patientProfile ?: return
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_add_patient_allergy, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvAddAllergyPatientTitle)
        val actvCatalog = dialogView.findViewById<AutoCompleteTextView>(R.id.actvAddAllergyCatalog)
        val actvSeverity = dialogView.findViewById<AutoCompleteTextView>(R.id.actvAddAllergySeverity)
        val etSymptoms = dialogView.findViewById<EditText>(R.id.etAddAllergySymptoms)
        val etRescue = dialogView.findViewById<EditText>(R.id.etAddAllergyRescue)
        val etDate = dialogView.findViewById<EditText>(R.id.etAddAllergyDate)

        tvTitle.text = activity.getString(R.string.dialog_assign_allergy_title_format, patient.nombreCompleto)
        etDate.addTextChangedListener(DateMaskTextWatcher(etDate))

        val allergiesList = dbHelper.getAllAllergiesSimpleList()
        val allergyNames = allergiesList.map { it.second }
        var selectedAllergyKey = if (allergiesList.isNotEmpty()) allergiesList.first().first else ""

        actvCatalog.setAdapter(ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, allergyNames))
        actvCatalog.setOnItemClickListener { _, _, pos, _ ->
            selectedAllergyKey = allergiesList.getOrNull(pos)?.first.orEmpty()
        }
        if (allergiesList.isNotEmpty()) {
            actvCatalog.setText(allergiesList.first().second, false)
        }

        val severities = activity.resources.getStringArray(R.array.allergy_severities).toList()
        actvSeverity.setAdapter(ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, severities))
        actvSeverity.setText(activity.getString(R.string.default_severity_moderate), false)

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        val btnCancel = dialogView.findViewById<View>(R.id.btnAddAllergyCancel)
        val btnSave = dialogView.findViewById<View>(R.id.btnAddAllergySave)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val fecha = etDate.getCleanText()
            val severidad = actvSeverity.getCleanText().ifEmpty { activity.getString(R.string.default_severity_moderate) }
            val sintomas = etSymptoms.getCleanText()
            val rescate = etRescue.getCleanText()

            if (selectedAllergyKey.isEmpty()) {
                Toast.makeText(activity, activity.getString(R.string.toast_allergy_select_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = dbHelper.insertPacienteAlergia(
                clavePaciente = patient.clave,
                claveAlergia = selectedAllergyKey,
                fecha = fecha,
                severidad = severidad,
                sintomas = symptomsSanitized(sintomas),
                rescate = rescate
            )

            if (success) {
                Toast.makeText(activity, activity.getString(R.string.toast_allergy_assigned), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                activity.loadPatientProfile()
            } else {
                Toast.makeText(activity, activity.getString(R.string.toast_allergy_assign_error), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.applyStandardDialogStyle(dialogView)
    }

    // Sanea el texto de síntomas evitando registros vacíos
    private fun symptomsSanitized(text: String): String = text.trim()



    // Muestra el diálogo para registrar una nueva patología oncológica al expediente
    fun showAddCancerDialog() {
        val patient = activity.patientProfile ?: return
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_add_patient_cancer, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvAddCancerPatientTitle)
        val actvCatalog = dialogView.findViewById<AutoCompleteTextView>(R.id.actvAddCancerCatalog)
        val actvResult = dialogView.findViewById<AutoCompleteTextView>(R.id.actvAddCancerResult)
        val etDetectDate = dialogView.findViewById<EditText>(R.id.etAddCancerDetectDate)
        val etStartDate = dialogView.findViewById<EditText>(R.id.etAddCancerStartDate)
        val etEndDate = dialogView.findViewById<EditText>(R.id.etAddCancerEndDate)

        val resultsList = activity.resources.getStringArray(R.array.oncology_results).toList()
        actvResult.setAdapter(ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, resultsList))
        actvResult.setText(activity.getString(R.string.default_result_positive), false)

        tvTitle.text = activity.getString(R.string.dialog_assign_cancer_title_format, patient.nombreCompleto)
        etDetectDate.addTextChangedListener(DateMaskTextWatcher(etDetectDate))
        etStartDate.addTextChangedListener(DateMaskTextWatcher(etStartDate))
        etEndDate.addTextChangedListener(DateMaskTextWatcher(etEndDate))

        val cancerList = dbHelper.getAllCancerSimpleList()
        val cancerNames = cancerList.map { it.second }
        var selectedCancerKey = if (cancerList.isNotEmpty()) cancerList.first().first else ""

        actvCatalog.setAdapter(ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, cancerNames))
        actvCatalog.setOnItemClickListener { _, _, pos, _ ->
            selectedCancerKey = cancerList.getOrNull(pos)?.first.orEmpty()
        }
        if (cancerList.isNotEmpty()) {
            actvCatalog.setText(cancerList.first().second, false)
        }

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        val btnCancel = dialogView.findViewById<View>(R.id.btnAddCancerCancel)
        val btnSave = dialogView.findViewById<View>(R.id.btnAddCancerSave)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val resultado = actvResult.getCleanText().ifEmpty { activity.getString(R.string.default_result_positive) }
            val fechaDet = etDetectDate.getCleanText()
            val fechaIni = etStartDate.getCleanText()
            val fechaFin = etEndDate.getCleanText()

            if (selectedCancerKey.isEmpty()) {
                Toast.makeText(activity, activity.getString(R.string.toast_cancer_select_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = dbHelper.insertPacienteCancer(
                clavePaciente = patient.clave,
                claveCancer = selectedCancerKey,
                resultado = resultado,
                fechaDetectada = fechaDet,
                fechaInicio = fechaIni,
                fechaFin = fechaFin
            )

            if (success) {
                Toast.makeText(activity, activity.getString(R.string.toast_cancer_assigned), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                activity.loadPatientProfile()
            } else {
                Toast.makeText(activity, activity.getString(R.string.toast_cancer_assign_error), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.applyStandardDialogStyle(dialogView)
    }


    // Aplica el estilo estándar responsivo a los diálogos modales
    private fun AlertDialog.applyStandardDialogStyle(dialogView: View, maxScrollHeightDp: Int = 480) {
        window?.let { win ->
            win.setBackgroundDrawableResource(android.R.color.transparent)
            win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        val displayMetrics = activity.resources.displayMetrics
        val density = displayMetrics.density
        val screenHeight = displayMetrics.heightPixels
        
        // Permite crecer dinámicamente hasta el 65% de la pantalla para máxima holgura
        val maxAllowedScrollHeight = ((screenHeight * 0.65).toInt()).coerceAtMost((maxScrollHeightDp * density).toInt())

        val scrollView = dialogView.findViewById<ScrollView>(R.id.dialogScrollView)
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

}

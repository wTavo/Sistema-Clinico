package com.example.sistemaclinico.ui.clinical


import android.database.Cursor
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.sistemaclinico.R
import com.example.sistemaclinico.data.DatabaseHelper
import com.example.sistemaclinico.databinding.ActivityClinicalTableViewerBinding
import com.example.sistemaclinico.utils.enableWhitespaceSanitization
import com.example.sistemaclinico.utils.getCleanText
import com.example.sistemaclinico.utils.setupWhitespaceSanitization

class ClinicalTableViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClinicalTableViewerBinding
    private lateinit var dbHelper: DatabaseHelper
    private var currentTableKey = "alergias"
    private var isSpeedDialOpen = false

    companion object {
        private const val COLOR_ROW_ODD = 0xFFF8FAFC.toInt()
    }

    private data class TableConfig(
        val friendlyHeaders: List<String>,
        val loadData: (String) -> Cursor?,
        val deleteTable: String? = null,
        val deletePkColumn: String? = null
    )

    private val configs: Map<String, TableConfig> by lazy {
        mapOf(
            "alergias" to TableConfig(
                friendlyHeaders = listOf("Clave", "Nombre del Alérgeno", "Categoría"),
                loadData = { q -> dbHelper.getDatosAlergias(q) },
                deleteTable = DatabaseHelper.TABLE_ALERGIAS,
                deletePkColumn = "clave_alergia"
            ),
            "tipoCancer" to TableConfig(
                friendlyHeaders = listOf("Clave", "Tipo de Cáncer", "Descripción Médica"),
                loadData = { q -> dbHelper.getDatosCancer(q) },
                deleteTable = DatabaseHelper.TABLE_TIPOCANCER,
                deletePkColumn = "clave_cancer"
            )
        )
    }

    // Inicializa la actividad, View Binding y carga la tabla clínica solicitada
    override fun onCreate(savedInstanceState: Bundle?) {
        // Infla y establece el contenido de la vista con View Binding
        super.onCreate(savedInstanceState)
        binding = ActivityClinicalTableViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper.getInstance(this)
        currentTableKey = intent.getStringExtra("EXTRA_TABLE_KEY")
            ?: intent.getStringExtra("tabla")
            ?: "alergias"

        val defaultTitle = if (currentTableKey == "alergias") "Catálogo de Alergias" else "Catálogo Oncológico"
        val title = intent.getStringExtra("EXTRA_TABLE_TITLE")
            ?: intent.getStringExtra("title")
            ?: defaultTitle

        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupSpeedDial()

        binding.etSearch.enableWhitespaceSanitization()
        binding.etSearch.doAfterTextChanged { text ->
            loadTableData(text?.toString()?.trim().orEmpty())
        }

        loadTableData()
    }

    // Configura los listeners y acciones del botón flotante Speed Dial
    private fun setupSpeedDial() {
        val isAllergy = currentTableKey == "alergias"
        binding.tvDialAddLabel.text = if (isAllergy) "Registrar nueva alergia" else "Registrar tipo de cáncer"
        binding.tvDialEditLabel.text = if (isAllergy) "Modificar alérgeno" else "Modificar tipo de cáncer"
        binding.tvDialDeleteLabel.text = if (isAllergy) "Eliminar alérgeno" else "Eliminar del catálogo"

        binding.fabMainActions.setOnClickListener {
            toggleSpeedDial()
        }

        binding.fabDimOverlay.setOnClickListener {
            toggleSpeedDial(false)
        }

        // Opción 1: Registrar en Catálogo
        binding.btnDialAddCatalog.setOnClickListener {
            toggleSpeedDial(false)
            showAddCatalogItemDialog()
        }
        binding.fabActionAddCatalog.setOnClickListener {
            toggleSpeedDial(false)
            showAddCatalogItemDialog()
        }

        // Opción 2: Modificar Registro
        binding.btnDialEditCatalog.setOnClickListener {
            toggleSpeedDial(false)
            showSelectCatalogItemDialog(isForDelete = false)
        }
        binding.fabActionEditCatalog.setOnClickListener {
            toggleSpeedDial(false)
            showSelectCatalogItemDialog(isForDelete = false)
        }

        // Opción 3: Eliminar Registro
        binding.btnDialDeleteCatalog.setOnClickListener {
            toggleSpeedDial(false)
            showSelectCatalogItemDialog(isForDelete = true)
        }
        binding.fabActionDeleteCatalog.setOnClickListener {
            toggleSpeedDial(false)
            showSelectCatalogItemDialog(isForDelete = true)
        }
    }

    // Controla la animación de apertura y cierre del menú flotante Speed Dial
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
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .start()
            }
            binding.fabMainActions.animate().rotation(45f).setDuration(200).start()
        } else {
            binding.fabDimOverlay.animate()
                .alpha(0f)
                .setDuration(180)
                .withEndAction { binding.fabDimOverlay.visibility = View.GONE }
                .start()
            binding.layoutSpeedDialMenu.animate()
                .alpha(0f)
                .translationY(40f)
                .setDuration(180)
                .withEndAction { binding.layoutSpeedDialMenu.visibility = View.GONE }
                .start()
            binding.fabMainActions.animate().rotation(0f).setDuration(180).start()
        }
    }

    // Consulta los registros de la tabla seleccionada en SQLite y construye la vista tabular
    private fun loadTableData(filterQuery: String = "") {
        val config = configs[currentTableKey] ?: return

        binding.tableData.removeAllViews()

        val cursor = config.loadData(filterQuery)

        if (cursor == null || cursor.count == 0) {
            binding.tvRecordCount.text = getString(R.string.records_count_format, 0)
            binding.tvEmpty.visibility = View.VISIBLE
            binding.horizontalScrollView.visibility = View.GONE
            cursor?.close()
            return
        }

        binding.tvEmpty.visibility = View.GONE
        binding.horizontalScrollView.visibility = View.VISIBLE
        binding.tvRecordCount.text = getString(R.string.records_count_format, cursor.count)

        // Encabezados
        val headerRow = TableRow(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@ClinicalTableViewerActivity, R.color.primary_teal))
            setPadding(0, dp(10), 0, dp(10))
        }
        for (header in config.friendlyHeaders) {
            headerRow.addView(createCell(header, isHeader = true, isWhite = true))
        }
        binding.tableData.addView(headerRow)

        // Filas de datos
        var rowIndex = 0
        if (cursor.moveToFirst()) {
            do {
                val dataRow = TableRow(this).apply {
                    val bg = if (rowIndex % 2 == 0) 0xFFFFFFFF.toInt() else COLOR_ROW_ODD
                    setBackgroundColor(bg)
                    setPadding(0, dp(8), 0, dp(8))
                }

                for (i in 0 until cursor.columnCount) {
                    val rawVal = cursor.getString(i) ?: ""
                    val displayVal = if (i == 0) {
                        when (currentTableKey) {
                            "alergias" -> if (rawVal.startsWith("ALG-", ignoreCase = true)) rawVal.uppercase() else "ALG-$rawVal"
                            "tipoCancer" -> if (rawVal.startsWith("CA-", ignoreCase = true)) rawVal.uppercase() else "CA-$rawVal"
                            else -> rawVal
                        }
                    } else rawVal

                    dataRow.addView(createCell(displayVal, isHeader = false, isWhite = false, isClave = (i == 0)))
                }

                binding.tableData.addView(dataRow)
                rowIndex++
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    // Muestra el diálogo modal para dar de alta un nuevo elemento en el catálogo activo
    private fun showAddCatalogItemDialog() {
        val isAllergy = currentTableKey == "alergias"
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_catalog, null)

        val flIcon = dialogView.findViewById<FrameLayout>(R.id.flCatalogIcon)
        val ivIcon = dialogView.findViewById<ImageView>(R.id.ivCatalogIcon)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogCatalogTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvDialogCatalogSubtitle)
        val etCode = dialogView.findViewById<EditText>(R.id.etDialogCode)
        val tilName = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilName)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogName)
        val tilCategory = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilCategory)
        val actvCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.actvDialogCategory)
        val tilDesc = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilDesc)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDialogDesc)
        val btnCancel = dialogView.findViewById<View>(R.id.btnDialogCatalogCancel)
        val btnSave = dialogView.findViewById<View>(R.id.btnDialogCatalogSave)

        val nextCode = if (isAllergy) dbHelper.getNextAllergyId() else dbHelper.getNextCancerId()
        etCode.setText(nextCode)

        if (isAllergy) {
            flIcon.background = ContextCompat.getDrawable(this, R.drawable.bg_circle_teal)
            ivIcon.setImageResource(R.drawable.ic_hospital)
            tvTitle.text = getString(R.string.dialog_catalog_add_allergy_title)
            tvSubtitle.text = getString(R.string.dialog_catalog_add_allergy_sub)
            tilName.hint = getString(R.string.dialog_catalog_add_allergy_hint)
            tilCategory.visibility = View.VISIBLE
            tilDesc.visibility = View.GONE

            val categories = resources.getStringArray(R.array.allergy_categories).toList()
            actvCategory.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories))
            actvCategory.setText("Medicamento", false)
        } else {
            flIcon.background = ContextCompat.getDrawable(this, R.drawable.bg_circle_teal)
            ivIcon.setImageResource(R.drawable.ic_table)
            tvTitle.text = getString(R.string.dialog_catalog_add_cancer_title)
            tvSubtitle.text = getString(R.string.dialog_catalog_add_cancer_sub)
            tilName.hint = getString(R.string.dialog_catalog_add_cancer_hint)
            tilCategory.visibility = View.GONE
            tilDesc.visibility = View.VISIBLE
            tilDesc.hint = getString(R.string.dialog_catalog_desc_hint)
        }

        setupWhitespaceSanitization(etName, etDesc)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.getCleanText()
            val category = actvCategory.getCleanText().ifEmpty { "General" }
            val desc = etDesc.getCleanText()

            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_catalog_name_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = if (isAllergy) {
                dbHelper.insertAlergia(nextCode, name, category, desc)
            } else {
                dbHelper.insertTipoCancer(nextCode, name, desc)
            }

            if (success) {
                Toast.makeText(this, getString(R.string.toast_catalog_saved, nextCode), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadTableData(binding.etSearch.text?.toString().orEmpty())
            } else {
                Toast.makeText(this, getString(R.string.toast_catalog_save_error), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.applyStandardDialogStyle(dialogView)
    }

    // Muestra el selector modal para elegir qué registro editar o eliminar del catálogo
    private fun showSelectCatalogItemDialog(isForDelete: Boolean) {
        val config = configs[currentTableKey] ?: return
        val cursor = config.loadData("") ?: return

        val rowsList = mutableListOf<List<String>>()
        if (cursor.moveToFirst()) {
            do {
                val rowValues = mutableListOf<String>()
                for (i in 0 until cursor.columnCount) {
                    val rawVal = cursor.getString(i) ?: ""
                    val displayVal = if (i == 0) {
                        when (currentTableKey) {
                            "alergias" -> if (rawVal.startsWith("ALG-", ignoreCase = true)) rawVal.uppercase() else "ALG-$rawVal"
                            "tipoCancer" -> if (rawVal.startsWith("CA-", ignoreCase = true)) rawVal.uppercase() else "CA-$rawVal"
                            else -> rawVal
                        }
                    } else rawVal
                    rowValues.add(displayVal)
                }
                rowsList.add(rowValues)
            } while (cursor.moveToNext())
        }
        cursor.close()

        if (rowsList.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_catalog_empty), Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_select_item_list, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvSelectorTitle)
        val tvSub = dialogView.findViewById<TextView>(R.id.tvSelectorSubtitle)
        val container = dialogView.findViewById<LinearLayout>(R.id.containerSelectorItems)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectorCancel)

        val isAllergy = currentTableKey == "alergias"
        val itemName = if (isAllergy) getString(R.string.table_allergies_singular) else getString(R.string.table_cancer_singular)

        if (isForDelete) {
            tvTitle.text = getString(if (isAllergy) R.string.dialog_catalog_delete_allergy_title else R.string.dialog_catalog_delete_cancer_title)
            tvSub.text = getString(R.string.dialog_catalog_select_delete, itemName)
        } else {
            tvTitle.text = getString(if (isAllergy) R.string.dialog_catalog_edit_allergy_title else R.string.dialog_catalog_edit_cancer_title)
            tvSub.text = getString(R.string.dialog_catalog_select_edit, itemName)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        rowsList.forEach { rowValues ->
            val clave = rowValues.getOrNull(0) ?: ""
            val nombre = rowValues.getOrNull(1) ?: ""
            val detalle = rowValues.getOrNull(2) ?: ""

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@ClinicalTableViewerActivity, R.drawable.bg_chip_soft)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dp(8) }
                setOnClickListener {
                    dialog.dismiss()
                    if (isForDelete) {
                        confirmDeleteRow(config, clave, nombre)
                    } else {
                        showEditDialog(config, rowValues)
                    }
                }
            }

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvClave = TextView(this).apply {
                text = clave
                setTextColor(
                    if (isForDelete) ContextCompat.getColor(this@ClinicalTableViewerActivity, R.color.secondary_crimson)
                    else ContextCompat.getColor(this@ClinicalTableViewerActivity, R.color.primary_teal)
                )
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(dp(75), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            topRow.addView(tvClave)

            val tvNom = TextView(this).apply {
                text = nombre
                setTextColor(ContextCompat.getColor(this@ClinicalTableViewerActivity, R.color.text_primary))
                textSize = 14.5f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            topRow.addView(tvNom)
            card.addView(topRow)

            if (detalle.isNotEmpty()) {
                val tvDet = TextView(this).apply {
                    text = detalle
                    setTextColor(ContextCompat.getColor(this@ClinicalTableViewerActivity, R.color.text_secondary))
                    textSize = 12.5f
                    setPadding(0, dp(4), 0, 0)
                }
                card.addView(tvDet)
            }

            container.addView(card)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.applyStandardDialogStyle(dialogView)
    }

    // Muestra el diálogo modal para modificar los campos de un registro seleccionado
    private fun showEditDialog(config: TableConfig, rowValues: List<String>) {
        val clave = rowValues.getOrNull(0) ?: return
        val currentName = rowValues.getOrNull(1) ?: ""
        val isAllergy = currentTableKey == "alergias"
        val currentCategory = if (isAllergy) rowValues.getOrNull(2) ?: "General" else ""
        val currentDesc = if (!isAllergy) rowValues.getOrNull(2) ?: "" else ""

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_catalog, null)

        val flIcon = dialogView.findViewById<FrameLayout>(R.id.flCatalogIcon)
        val ivIcon = dialogView.findViewById<ImageView>(R.id.ivCatalogIcon)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogCatalogTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvDialogCatalogSubtitle)
        val etCode = dialogView.findViewById<EditText>(R.id.etDialogCode)
        val tilName = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilName)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogName)
        val tilCategory = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilCategory)
        val actvCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.actvDialogCategory)
        val tilDesc = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilDesc)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDialogDesc)
        val btnCancel = dialogView.findViewById<View>(R.id.btnDialogCatalogCancel)
        val btnSave = dialogView.findViewById<View>(R.id.btnDialogCatalogSave)

        etCode.setText(clave)
        etName.setText(currentName)

        if (isAllergy) {
            flIcon.background = ContextCompat.getDrawable(this, R.drawable.bg_circle_teal)
            ivIcon.setImageResource(R.drawable.ic_hospital)
            tvTitle.text = getString(R.string.dialog_catalog_edit_allergy_title)
            tvSubtitle.text = getString(R.string.dialog_catalog_edit_allergy_sub)
            tilName.hint = getString(R.string.dialog_catalog_add_allergy_hint)
            tilCategory.visibility = View.VISIBLE
            tilDesc.visibility = View.GONE

            val categories = resources.getStringArray(R.array.allergy_categories).toList()
            actvCategory.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories))
            actvCategory.setText(currentCategory.ifEmpty { "General" }, false)
        } else {
            flIcon.background = ContextCompat.getDrawable(this, R.drawable.bg_circle_teal)
            ivIcon.setImageResource(R.drawable.ic_table)
            tvTitle.text = getString(R.string.dialog_catalog_edit_cancer_title)
            tvSubtitle.text = getString(R.string.dialog_catalog_edit_cancer_sub)
            tilName.hint = getString(R.string.cancer_name_hint)
            tilCategory.visibility = View.GONE
            tilDesc.visibility = View.VISIBLE
            tilDesc.hint = getString(R.string.dialog_catalog_desc_hint)
            etDesc.setText(currentDesc)
        }

        setupWhitespaceSanitization(etName, etDesc)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newName = etName.getCleanText()
            val newCategory = actvCategory.getCleanText().ifEmpty { "General" }
            val newDesc = etDesc.getCleanText()

            if (newName.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_catalog_name_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = if (isAllergy) {
                dbHelper.updateAlergia(clave, newName, newCategory, newDesc)
            } else if (currentTableKey == "tipoCancer") {
                dbHelper.updateTipoCancer(clave, newName, newDesc)
            } else false

            if (success) {
                Toast.makeText(this, getString(R.string.toast_catalog_updated), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadTableData(binding.etSearch.text?.toString().orEmpty())
            } else {
                Toast.makeText(this, getString(R.string.toast_catalog_update_error), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.applyStandardDialogStyle(dialogView)
    }

    // Muestra el diálogo de confirmación para eliminar un registro del catálogo
    private fun confirmDeleteRow(config: TableConfig, clave: String, nombre: String = "") {
        val table = config.deleteTable ?: return
        val pkCol = config.deletePkColumn ?: return

        val displayTitle = if (nombre.isNotEmpty()) "$clave ($nombre)" else clave

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.table_viewer_delete_title))
            .setMessage(getString(R.string.dialog_catalog_delete_msg, displayTitle))
            .setPositiveButton(getString(R.string.dialog_delete_definitively_btn)) { _, _ ->
                val numVal = clave.removePrefix("ALG-").removePrefix("CA-").trim().toIntOrNull()
                val whereVal = numVal?.toString() ?: clave

                val deleted = dbHelper.writableDatabase.delete(
                    table,
                    "$pkCol = ? OR CAST($pkCol AS TEXT) = ?",
                    arrayOf(whereVal, clave)
                )
                if (deleted > 0) {
                    Toast.makeText(this, getString(R.string.dialog_catalog_delete_success, displayTitle), Toast.LENGTH_SHORT).show()
                    loadTableData(binding.etSearch.text?.toString().orEmpty())
                } else {
                    Toast.makeText(this, getString(R.string.table_viewer_delete_error), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel_btn), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.secondary_crimson))
        }
        dialog.show()
    }

    // Aplica el estilo estándar responsivo a los diálogos modales
    private fun AlertDialog.applyStandardDialogStyle(dialogView: View, maxScrollHeightDp: Int = 480) {
        window?.let { win ->
            win.setBackgroundDrawableResource(android.R.color.transparent)
            win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val screenHeight = displayMetrics.heightPixels

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

    // Crea y formatea una celda de texto para la tabla dinámica
    private fun createCell(text: String, isHeader: Boolean, isWhite: Boolean, isClave: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            setPadding(dp(14), dp(8), dp(14), dp(8))
            textSize = if (isHeader) 13.5f else 13f
            val textColor = if (isWhite || isHeader) {
                ContextCompat.getColor(this@ClinicalTableViewerActivity, R.color.white)
            } else if (isClave) {
                ContextCompat.getColor(this@ClinicalTableViewerActivity, R.color.primary_teal)
            } else {
                0xFF334155.toInt()
            }
            setTextColor(textColor)
            if (isHeader || isClave) {
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    // Convierte valores de dp a píxeles de pantalla
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

package com.example.sistemaclinico.ui.clinical

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sistemaclinico.R
import com.example.sistemaclinico.data.DatabaseHelper
import com.example.sistemaclinico.databinding.ActivityPatientDirectoryBinding
import com.example.sistemaclinico.utils.DateMaskTextWatcher
import com.example.sistemaclinico.utils.getCleanText
import com.example.sistemaclinico.utils.setupWhitespaceSanitization

class PatientDirectoryActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "PatientDirectoryPrefs"
        private const val KEY_SORT_MODE = "sort_mode"
        private const val SORT_ASC = "ASC"
        private const val SORT_DESC = "DESC"
        private const val SORT_NAME = "NAME"
    }

    private lateinit var binding: ActivityPatientDirectoryBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: PatientAdapter
    private lateinit var prefs: SharedPreferences
    private var currentSortMode = SORT_ASC
    private var isSpeedDialOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Infla y establece el contenido de la actividad usando View Binding
        binding = ActivityPatientDirectoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa el gestor de base de datos y las preferencias compartidas
        dbHelper = DatabaseHelper.getInstance(this)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentSortMode = prefs.getString(KEY_SORT_MODE, SORT_ASC) ?: SORT_ASC

        // Configura el botón de regreso en la barra superior
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Configura el listado, ordenamiento y botón flotante
        setupRecyclerView()
        setupSortChips()
        setupSpeedDial()
        loadPatients(binding.etSearch.text?.toString()?.trim().orEmpty())

        // Configura el buscador en tiempo real
        binding.etSearch.doAfterTextChanged { text ->
            loadPatients(text?.toString()?.trim().orEmpty())
        }
    }

    override fun onResume() {
        super.onResume()
        // Recarga la lista de pacientes al volver a la pantalla
        loadPatients(binding.etSearch.text?.toString()?.trim().orEmpty())
    }

    // Configura los listeners del botón flotante y el fondo oscuro
    private fun setupSpeedDial() {
        binding.fabMainActions.setOnClickListener {
            toggleSpeedDial()
        }

        binding.fabDimOverlay.setOnClickListener {
            toggleSpeedDial(false)
        }

        binding.btnDialAddPatient.setOnClickListener {
            toggleSpeedDial(false)
            showAddPatientDialog()
        }
        binding.fabActionAddPatient.setOnClickListener {
            toggleSpeedDial(false)
            showAddPatientDialog()
        }
    }

    // Controla la animación de apertura y cierre del menú flotante
    private fun toggleSpeedDial(open: Boolean? = null) {
        val shouldOpen = open ?: !isSpeedDialOpen
        if (shouldOpen == isSpeedDialOpen) return
        isSpeedDialOpen = shouldOpen

        if (isSpeedDialOpen) {
            // Muestra el fondo oscuro con animación de fade-in
            binding.fabDimOverlay.apply {
                alpha = 0f
                visibility = View.VISIBLE
                animate().alpha(1f).setDuration(200).start()
            }
            // Despliega el menú de acciones hacia arriba
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
            // Oculta el fondo oscuro con fade-out
            binding.fabDimOverlay.animate()
                .alpha(0f)
                .setDuration(180)
                .withEndAction { binding.fabDimOverlay.visibility = View.GONE }
                .start()
            // Repliega el menú de acciones
            binding.layoutSpeedDialMenu.animate()
                .alpha(0f)
                .translationY(40f)
                .setDuration(180)
                .withEndAction { binding.layoutSpeedDialMenu.visibility = View.GONE }
                .start()
            binding.fabMainActions.animate().rotation(0f).setDuration(180).start()
        }
    }

    // Muestra el diálogo modal para registrar un nuevo paciente
    private fun showAddPatientDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_patient, null)

        val etCode = dialogView.findViewById<EditText>(R.id.etAddPatientCode)
        val etName = dialogView.findViewById<EditText>(R.id.etAddPatientName)
        val etLastNameFather = dialogView.findViewById<EditText>(R.id.etAddPatientLastNameFather)
        val etLastNameMother = dialogView.findViewById<EditText>(R.id.etAddPatientLastNameMother)
        val etWeight = dialogView.findViewById<EditText>(R.id.etAddPatientWeight)
        val etHeight = dialogView.findViewById<EditText>(R.id.etAddPatientHeight)
        val etBloodType = dialogView.findViewById<EditText>(R.id.etAddPatientBloodType)
        val chipFemale = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipAddSexFemale)
        val etBirthDate = dialogView.findViewById<EditText>(R.id.etAddPatientBirthDate)
        val btnCancel = dialogView.findViewById<View>(R.id.btnAddPatientCancel)
        val btnSave = dialogView.findViewById<View>(R.id.btnAddPatientSave)

        // Asigna el siguiente ID sugerido de paciente
        val nextId = dbHelper.getNextPatientId()
        etCode.setText(nextId)

        // Asigna máscara de fecha y saneamiento de espacios
        etBirthDate.addTextChangedListener(DateMaskTextWatcher(etBirthDate))
        setupWhitespaceSanitization(etName, etLastNameFather, etLastNameMother, etWeight, etHeight, etBloodType)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Cierra el diálogo al presionar Cancelar
        btnCancel.setOnClickListener { dialog.dismiss() }

        // Guarda el paciente al presionar Guardar
        btnSave.setOnClickListener {
            val nombre = etName.getCleanText()
            val apPaterno = etLastNameFather.getCleanText()
            val apMaterno = etLastNameMother.getCleanText()
            val peso = etWeight.getCleanText()
            val estatura = etHeight.getCleanText()
            val tipoSangre = etBloodType.getCleanText()
            val sexo = if (chipFemale?.isChecked == true) "F" else "M"
            val fechaNac = etBirthDate.getCleanText()

            // Valida campos obligatorios
            if (nombre.isEmpty() || apPaterno.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_patient_required_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Inserta el paciente en la base de datos
            val success = dbHelper.insertPaciente(nextId, nombre, apPaterno, apMaterno, peso, estatura, tipoSangre, fechaNac, sexo)
            if (success) {
                Toast.makeText(this, getString(R.string.toast_patient_registered, nextId), Toast.LENGTH_LONG).show()
                dialog.dismiss()
                loadPatients(binding.etSearch.text?.toString()?.trim().orEmpty())
            } else {
                Toast.makeText(this, getString(R.string.toast_patient_register_error), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.applyStandardDialogStyle(dialogView)
    }

    // Aplica el estilo estándar con límites de altura y teclado reactivo
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

    // Configura los Chips para ordenar por clave ascendente, descendente o nombre
    private fun setupSortChips() {
        when (currentSortMode) {
            SORT_DESC -> binding.chipSortDesc.isChecked = true
            SORT_NAME -> binding.chipSortName.isChecked = true
            else -> binding.chipSortAsc.isChecked = true
        }

        binding.chipGroupSort.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            currentSortMode = when (checkedIds.first()) {
                R.id.chipSortDesc -> SORT_DESC
                R.id.chipSortName -> SORT_NAME
                else -> SORT_ASC
            }
            prefs.edit().putString(KEY_SORT_MODE, currentSortMode).apply()
            loadPatients(binding.etSearch.text?.toString()?.trim().orEmpty())
        }
    }

    // Configura el RecyclerView y el adaptador de tarjetas
    private fun setupRecyclerView() {
        adapter = PatientAdapter(emptyList()) { patient ->
            // Abre el expediente individual al tocar la tarjeta del paciente
            val intent = Intent(this, PatientDetailActivity::class.java).apply {
                putExtra("patient", patient)
                putExtra("patient_code", patient.clave)
            }
            startActivity(intent)
        }
        binding.rvPatients.layoutManager = LinearLayoutManager(this)
        binding.rvPatients.adapter = adapter
    }

    // Consulta los pacientes en SQLite aplicando filtros y ordenamiento
    private fun loadPatients(query: String) {
        val rawPatients = dbHelper.getPatientProfiles(query)

        val sortedPatients = when (currentSortMode) {
            SORT_DESC -> rawPatients.sortedByDescending { it.clave.removePrefix("PAC-").trim().toIntOrNull() ?: 0 }
            SORT_NAME -> rawPatients.sortedBy { it.nombreCompleto.lowercase() }
            else -> rawPatients.sortedBy { it.clave.removePrefix("PAC-").trim().toIntOrNull() ?: 0 }
        }

        adapter.updateList(sortedPatients)
        binding.tvRecordCount.text = getString(R.string.table_viewer_count, sortedPatients.size)

        // Muestra u oculta el aviso de lista vacía
        if (sortedPatients.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvPatients.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvPatients.visibility = View.VISIBLE
        }
    }
}

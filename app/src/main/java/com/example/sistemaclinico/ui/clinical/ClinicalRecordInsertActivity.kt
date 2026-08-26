package com.example.sistemaclinico.ui.clinical

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sistemaclinico.R
import com.example.sistemaclinico.data.DatabaseHelper
import com.example.sistemaclinico.databinding.ActivityClinicalRecordInsertBinding
import com.example.sistemaclinico.utils.DateMaskTextWatcher
import com.example.sistemaclinico.utils.getCleanText
import com.example.sistemaclinico.utils.setupWhitespaceSanitization
import java.util.concurrent.Executors

class ClinicalRecordInsertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClinicalRecordInsertBinding
    private lateinit var dbHelper: DatabaseHelper
    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    // Variables para almacenar las claves autogeneradas
    private var nextPatientId = ""
    private var nextAllergyId = ""
    private var nextCancerId = ""

    // Vistas infladas bajo demanda (ViewStub)
    private var viewAllergy: View? = null
    private var viewCancer: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Infla y establece el contenido de la actividad usando View Binding
        binding = ActivityClinicalRecordInsertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtiene la instancia única de la base de datos
        dbHelper = DatabaseHelper.getInstance(this)

        // Configura el botón de regreso en la barra superior
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Inicializa los formularios y listeners
        setupPatientFormDirect()
        setupChipSelectors()
        loadDataAsync()
    }

    // Método para consultar los IDs autogenerados en segundo plano
    private fun loadDataAsync() {
        backgroundExecutor.execute {
            val pId = dbHelper.getNextPatientId()
            val aId = dbHelper.getNextAllergyId()
            val cId = dbHelper.getNextCancerId()

            // Devuelve los resultados al hilo principal de la interfaz
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                nextPatientId = pId
                nextAllergyId = aId
                nextCancerId = cId

                updatePatientFormId()
                viewAllergy?.findViewById<EditText>(R.id.etAllergyCode)?.setText(nextAllergyId)
                viewCancer?.findViewById<EditText>(R.id.etCancerCode)?.setText(nextCancerId)
            }
        }
    }

    // Método para configurar el selector de pestañas (Chips)
    private fun setupChipSelectors() {
        binding.chipGroupCatalogs.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chipPatient -> showPatientForm()
                R.id.chipAllergy -> showAllergyForm()
                R.id.chipCancer -> showCancerForm()
            }
        }
    }

    // Método para ocultar todos los formularios activos
    private fun hideAllForms() {
        binding.includePatientForm.root.visibility = View.GONE
        viewAllergy?.visibility = View.GONE
        viewCancer?.visibility = View.GONE
    }

    // Configura las validaciones y el guardado del paciente
    private fun setupPatientFormDirect() {
        val form = binding.includePatientForm
        // Asigna la máscara de fecha DD/MM/AAAA
        form.etBirthDate.addTextChangedListener(DateMaskTextWatcher(form.etBirthDate))
        // Configura la limpieza automática de espacios en blanco
        setupWhitespaceSanitization(form.etName, form.etLastNameFather, form.etLastNameMother, form.etWeight, form.etHeight, form.etBloodType)

        // Configura el botón de guardar paciente
        form.btnSavePatient.setOnClickListener {
            val nombre = form.etName.getCleanText()
            val apPaterno = form.etLastNameFather.getCleanText()
            val apMaterno = form.etLastNameMother.getCleanText()
            val peso = form.etWeight.getCleanText()
            val estatura = form.etHeight.getCleanText()
            val tipoSangre = form.etBloodType.getCleanText()
            val sexo = if (form.chipPatientSexFemale.isChecked) "F" else "M"
            val fechaNac = form.etBirthDate.getCleanText()

            // Válida que los campos obligatorios contengan información
            if (nombre.isEmpty() || apPaterno.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_patient_required_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Inserta el nuevo paciente en SQLite
            val clave = nextPatientId
            val success = dbHelper.insertPaciente(clave, nombre, apPaterno, apMaterno, peso, estatura, tipoSangre, fechaNac, sexo)
            if (success) {
                Toast.makeText(this, getString(R.string.toast_patient_registered, clave), Toast.LENGTH_LONG).show()
                // Limpia los campos del formulario
                form.etName.text?.clear()
                form.etLastNameFather.text?.clear()
                form.etLastNameMother.text?.clear()
                form.etWeight.text?.clear()
                form.etHeight.text?.clear()
                form.etBloodType.text?.clear()
                form.etBirthDate.text?.clear()
                form.chipPatientSexMale.isChecked = true
                // Actualiza los IDs disponibles
                loadDataAsync()
            } else {
                Toast.makeText(this, getString(R.string.toast_patient_register_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Actualiza la clave de paciente en la vista del formulario
    private fun updatePatientFormId() {
        binding.includePatientForm.etPatientCode.setText(nextPatientId)
    }

    // Muestra el formulario de alta de paciente
    private fun showPatientForm() {
        hideAllForms()
        binding.includePatientForm.root.visibility = View.VISIBLE
    }

    // Muestra el formulario de alta de alergia
    private fun showAllergyForm() {
        hideAllForms()
        if (viewAllergy == null) {
            viewAllergy = binding.stubAllergy.inflate()
            setupAllergyForm(viewAllergy!!)
        }
        viewAllergy?.visibility = View.VISIBLE
    }

    // Configura los campos y el guardado de nueva alergia
    private fun setupAllergyForm(view: View) {
        val etCode = view.findViewById<EditText>(R.id.etAllergyCode)
        val etName = view.findViewById<EditText>(R.id.etAllergyName)
        val actvCategory = view.findViewById<AutoCompleteTextView>(R.id.actvAllergyCategory)
        val btnSave = view.findViewById<Button>(R.id.btnSaveAllergy)

        // Configura el catálogo de categorías disponibles
        val categories = listOf("Medicamento", "Alimento", "Ambiental", "Insecto", "Contacto", "Otro")
        actvCategory.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories))
        actvCategory.setText("Medicamento", false)

        etCode.setText(nextAllergyId)
        setupWhitespaceSanitization(etName)

        // Configura el botón de guardar alergia
        btnSave.setOnClickListener {
            val nombre = etName.getCleanText()
            val categoria = actvCategory.getCleanText().ifEmpty { "General" }

            if (nombre.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_catalog_name_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Inserta la nueva alergia en SQLite
            val clave = nextAllergyId
            val success = dbHelper.insertAlergia(clave, nombre, categoria)
            if (success) {
                Toast.makeText(this, getString(R.string.toast_catalog_saved, clave), Toast.LENGTH_LONG).show()
                etName.text?.clear()
                loadDataAsync()
            } else {
                Toast.makeText(this, getString(R.string.toast_catalog_save_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Muestra el formulario de alta de neoplasia
    private fun showCancerForm() {
        hideAllForms()
        if (viewCancer == null) {
            viewCancer = binding.stubCancer.inflate()
            setupCancerForm(viewCancer!!)
        }
        viewCancer?.visibility = View.VISIBLE
    }

    // Configura los campos y el guardado de nuevo tipo de cáncer
    private fun setupCancerForm(view: View) {
        val etCode = view.findViewById<EditText>(R.id.etCancerCode)
        val etName = view.findViewById<EditText>(R.id.etCancerName)
        val etDesc = view.findViewById<EditText>(R.id.etCancerDesc)
        val btnSave = view.findViewById<Button>(R.id.btnSaveCancer)

        etCode.setText(nextCancerId)
        setupWhitespaceSanitization(etName, etDesc)

        // Configura el botón de guardar neoplasia
        btnSave.setOnClickListener {
            val tipo = etName.getCleanText()
            val descripcion = etDesc.getCleanText()

            if (tipo.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_catalog_name_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Inserta el nuevo tipo de cáncer en SQLite
            val clave = nextCancerId
            val success = dbHelper.insertTipoCancer(clave, tipo, descripcion)
            if (success) {
                Toast.makeText(this, getString(R.string.toast_catalog_saved, clave), Toast.LENGTH_LONG).show()
                etName.text?.clear()
                etDesc.text?.clear()
                loadDataAsync()
            } else {
                Toast.makeText(this, getString(R.string.toast_catalog_save_error), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

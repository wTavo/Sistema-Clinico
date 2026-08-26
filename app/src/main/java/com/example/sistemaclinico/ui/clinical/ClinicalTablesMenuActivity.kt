package com.example.sistemaclinico.ui.clinical

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sistemaclinico.R
import com.example.sistemaclinico.databinding.ActivityClinicalTablesMenuBinding

class ClinicalTablesMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClinicalTablesMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Infla y establece el contenido de la actividad usando View Binding
        binding = ActivityClinicalTablesMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura la acción de regreso en la barra de herramientas
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Abre el directorio unificado de pacientes
        binding.cardTablePatients.setOnClickListener {
            startActivity(Intent(this, PatientDirectoryActivity::class.java))
        }

        // Abre el catálogo de alergias
        binding.cardTableAllergies.setOnClickListener {
            openTable("alergias", getString(R.string.table_allergies_title))
        }

        // Abre el catálogo de tipos de cáncer
        binding.cardTableCancerTypes.setOnClickListener {
            openTable("tipoCancer", getString(R.string.table_cancer_title))
        }
    }

    // Método para abrir el visor interactivo de tablas clínicas
    private fun openTable(tableKey: String, title: String) {
        startActivity(Intent(this, ClinicalTableViewerActivity::class.java).apply {
            putExtra("tabla", tableKey)
            putExtra("title", title)
        })
    }
}

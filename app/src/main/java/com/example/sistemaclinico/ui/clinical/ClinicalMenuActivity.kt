package com.example.sistemaclinico.ui.clinical

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sistemaclinico.databinding.ActivityClinicalMenuBinding

class ClinicalMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClinicalMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Infla y establece el contenido de la actividad usando View Binding
        binding = ActivityClinicalMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura la acción de regreso en la barra superior
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Inicializa la conexión a SQLite en segundo plano para respuesta inmediata al tocar botones
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            com.example.sistemaclinico.data.DatabaseHelper.getInstance(this).readableDatabase
        }

        // Configura los botones para consultar las tablas clínicas
        binding.cardViewTables.setOnClickListener { openTables() }
        binding.btnViewTables.setOnClickListener { openTables() }

        // Configura los botones para registrar nuevos datos
        binding.cardInsertData.setOnClickListener { openInsert() }
        binding.btnInsertData.setOnClickListener { openInsert() }
    }

    // Método para abrir la lista de tablas clínicas
    private fun openTables() {
        val intent = Intent(this, ClinicalTablesMenuActivity::class.java)
        startActivity(intent)
    }

    // Método para abrir el formulario de registro de datos
    private fun openInsert() {
        val intent = Intent(this, ClinicalRecordInsertActivity::class.java)
        startActivity(intent)
    }
}

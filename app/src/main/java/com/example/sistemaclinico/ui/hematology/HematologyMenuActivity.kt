package com.example.sistemaclinico.ui.hematology

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.sistemaclinico.R
import com.example.sistemaclinico.databinding.ActivityHematologyMenuBinding

class HematologyMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHematologyMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configura el color de la barra de estado
        window.statusBarColor = ContextCompat.getColor(this, R.color.secondary_crimson_dark)

        // Infla y establece el contenido de la actividad usando View Binding
        binding = ActivityHematologyMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura la acción de regreso en la barra superior
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Configura los botones para abrir la bandeja de muestras didácticas
        binding.cardViewSamples.setOnClickListener { openSamples() }
        binding.btnSamples.setOnClickListener { openSamples() }

        // Configura los botones para abrir el módulo de carga y evaluación de archivos
        binding.cardDataEntry.setOnClickListener { openDataEntry() }
        binding.btnDataEntry.setOnClickListener { openDataEntry() }
    }

    // Inicia la actividad de muestras didácticas de laboratorio
    private fun openSamples() {
        startActivity(Intent(this, HematologySamplesActivity::class.java))
    }

    // Inicia la actividad de carga y análisis de archivos CSV y TXT
    private fun openDataEntry() {
        startActivity(Intent(this, HematologyDataEntryActivity::class.java))
    }
}

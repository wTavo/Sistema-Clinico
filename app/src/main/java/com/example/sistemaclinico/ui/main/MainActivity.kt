package com.example.sistemaclinico.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.sistemaclinico.databinding.ActivityMainBinding
import com.example.sistemaclinico.ui.clinical.ClinicalTablesMenuActivity
import com.example.sistemaclinico.ui.hematology.HematologyMenuActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Fuerza el modo claro en toda la aplicación
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // Infla y establece el contenido de la actividad usando View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura los eventos de clic para el módulo de Laboratorio Hematológico
        binding.cardHematology.setOnClickListener { openHematology() }
        binding.btnOpenHematology.setOnClickListener { openHematology() }

        // Configura los eventos de clic para el módulo de Expediente Clínico
        binding.cardClinical.setOnClickListener { openClinical() }
        binding.btnOpenClinical.setOnClickListener { openClinical() }
    }

    // Método para iniciar el módulo de laboratorio hematológico
    private fun openHematology() {
        val intent = Intent(this, HematologyMenuActivity::class.java)
        startActivity(intent)
    }

    // Método para iniciar el módulo de tablas y expedientes clínicos
    private fun openClinical() {
        val intent = Intent(this, ClinicalTablesMenuActivity::class.java)
        startActivity(intent)
    }
}

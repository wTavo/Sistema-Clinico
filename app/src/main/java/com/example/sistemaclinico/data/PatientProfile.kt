package com.example.sistemaclinico.data

// Modelos de datos y estructuras consolidadas para el expediente médico del paciente,
// biometrías hemáticas, muestras y trazabilidad de análisis de archivos.

import java.io.Serializable

data class AllergyDetail(
    val claveAlergia: String = "",
    val nombre: String,
    val categoria: String = "",
    val severidad: String = "",
    val sintomas: String = "",
    val tratamientoRescate: String = "",
    val fecha: String = ""
) : Serializable

data class BloodSample(
    val noOperacion: String,
    val cantidad: String,
    val fecha: String
) : Serializable

data class DiagnosticoOncologico(
    val claveCancer: String = "",
    val tipoCancer: String,
    val resultado: String,
    val fechaDeteccion: String,
    val fechaInicioTratamiento: String = "",
    val fechaFinalTratamiento: String = ""
) : Serializable

data class BiometriaHematica(
    val idEstudio: String,
    val clavePaciente: String,
    val fecha: String,
    val folio: String = "",
    // Serie Roja
    val eritrocitos: String, // x10^6 / µL (ej. 4.85)
    val hemoglobina: String, // g/dL (ej. 14.5)
    val hematocrito: String, // % (ej. 43.5)
    val vcm: String,         // fL (ej. 89.0)
    val hcm: String = "",    // pg (ej. 29.8)
    val chcm: String = "",   // g/dL (ej. 33.3)
    // Serie Blanca
    val leucocitos: String,  // x10^3 / µL (ej. 7.2)
    val neutrofilos: String, // % (ej. 60)
    val linfocitos: String,  // % (ej. 30)
    val monocitos: String,   // % (ej. 6)
    val eosinofilos: String, // % (ej. 3)
    // Serie Plaquetaria
    val plaquetas: String,   // x10^3 / µL (ej. 250)
    // Trazabilidad y Control de Calidad
    val tipoMuestra: String = "",
    val responsable: String = "",
    val observacionesFrotis: String = "",
    // Interpretación
    val diagnostico: String = ""
) : Serializable

data class PatientProfile(
    val clave: String,
    val nombre: String,
    val apellidoPat: String,
    val apellidoMat: String,
    val peso: String,
    val estatura: String,
    val tipoSangre: String,
    val fechaNacimiento: String,
    val sexo: String = "M", // "M" (Masculino) o "F" (Femenino)
    val alergias: List<AllergyDetail> = emptyList(),
    val globulosRojos: List<BloodSample> = emptyList(),
    val globulosBlancos: List<BloodSample> = emptyList(),
    val tratamientos: List<DiagnosticoOncologico> = emptyList(),
    val biometrias: List<BiometriaHematica> = emptyList()
) : Serializable {
    val nombreCompleto: String get() = "$nombre $apellidoPat $apellidoMat".trim()

    val iniciales: String get() {
        val n = nombre.firstOrNull()?.uppercaseChar() ?: 'P'
        val a = apellidoPat.firstOrNull()?.uppercaseChar() ?: ' '
        return "$n$a".trim()
    }
}

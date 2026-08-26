package com.example.sistemaclinico.ui.clinical

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sistemaclinico.data.PatientProfile
import com.example.sistemaclinico.R
import com.example.sistemaclinico.databinding.ItemPatientCardBinding

// Adaptador de RecyclerView para mostrar las tarjetas de pacientes en el directorio
class PatientAdapter(
    private var patients: List<PatientProfile>,
    private val onPatientClick: (PatientProfile) -> Unit
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    // Actualiza los datos de la lista de pacientes y refresca la vista
    fun updateList(newList: List<PatientProfile>) {
        patients = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        // Infla la vista de la tarjeta de paciente
        val binding = ItemPatientCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        // Enlaza la información del paciente en la posición correspondiente
        holder.bind(patients[position])
    }

    override fun getItemCount(): Int = patients.size

    // Contenedor de vistas para la tarjeta de cada paciente
    inner class PatientViewHolder(private val binding: ItemPatientCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Asigna los datos del paciente a los elementos de la interfaz
        fun bind(patient: PatientProfile) {
            val ctx = binding.root.context
            binding.tvInitials.text = patient.iniciales
            binding.tvPatientName.text = patient.nombreCompleto

            // Formatea la clave del paciente con el prefijo PAC
            val formattedCode = if (patient.clave.startsWith("PAC-", ignoreCase = true)) {
                patient.clave.uppercase()
            } else {
                "PAC-${patient.clave}"
            }
            binding.tvPatientCode.text = ctx.getString(R.string.patient_code_label_format, formattedCode)
            binding.tvBloodBadge.text = ctx.getString(R.string.patient_blood_badge_format, patient.tipoSangre.ifEmpty { "N/D" })

            // Configura los valores de peso, estatura y fecha de nacimiento
            val pesoStr = if (patient.peso.isNotEmpty()) "${patient.peso} kg" else "--"
            val estStr = if (patient.estatura.isNotEmpty()) "${patient.estatura} m" else "--"
            binding.tvWeightHeight.text = ctx.getString(R.string.patient_weight_height_format, pesoStr, estStr)
            binding.tvBirthDate.text = ctx.getString(R.string.patient_birthdate_format, com.example.sistemaclinico.utils.formatBirthDateWithAge(patient.fechaNacimiento))

            // Muestra el resumen de alergias diagnosticadas
            val nAlergias = patient.alergias.size
            binding.tvAllergySummary.text = if (nAlergias > 0) {
                ctx.getString(R.string.chip_allergies_count, nAlergias, if (nAlergias > 1) "s" else "")
            } else {
                ctx.getString(R.string.chip_allergies_zero)
            }

            // Muestra el resumen de estudios hematológicos (biometrías completas)
            val totalEstudios = patient.biometrias.size + patient.globulosRojos.size + patient.globulosBlancos.size
            binding.tvBloodSampleSummary.text = if (totalEstudios > 0) {
                ctx.getString(R.string.chip_studies_count, totalEstudios, if (totalEstudios > 1) "s" else "")
            } else {
                ctx.getString(R.string.chip_studies_zero)
            }

            // Muestra el resumen de diagnósticos oncológicos
            val nOnco = patient.tratamientos.size
            binding.tvCancerSummary.text = if (nOnco > 0) {
                ctx.getString(R.string.chip_cancer_count, nOnco, if (nOnco > 1) "s" else "")
            } else {
                ctx.getString(R.string.chip_cancer_zero)
            }

            // Configura el evento de clic para abrir el expediente completo
            binding.cardPatient.setOnClickListener {
                onPatientClick(patient)
            }
        }
    }
}

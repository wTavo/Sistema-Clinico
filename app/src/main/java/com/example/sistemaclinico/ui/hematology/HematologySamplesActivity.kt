package com.example.sistemaclinico.ui.hematology

// Bandeja interactiva de muestras hematológicas didácticas.
// Contiene 10 casos clínicos simulados para evaluación de patologías frecuentes y críticas.

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.sistemaclinico.R
import com.example.sistemaclinico.databinding.ActivityHematologySamplesBinding
import com.google.android.material.card.MaterialCardView
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HematologySamplesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHematologySamplesBinding

    // Modelo de datos para las muestras didácticas de biometría hemática
    data class AnalyzerSample(
        val sampleNumber: Int,
        val folio: String,
        val fecha: String,
        val rbc: Double,
        val hb: Double,
        val hct: Double,
        val vcm: Double,
        val hcm: Double,
        val chcm: Double,
        val wbc: Double,
        val neut: Double,
        val linf: Double,
        val mono: Double,
        val eos: Double,
        val plt: Double,
        val tipoMuestra: String,
        val responsable: String,
        val observacionesFrotis: String,
        val diagnosticoManual: String,
        val flag: String,
        val flagColor: Int
    ) : Serializable

    // Recursos gráficos asociados a cada tubo de muestra
    private val sampleDrawables = intArrayOf(
        R.drawable.estado1, R.drawable.estado2, R.drawable.estado3,
        R.drawable.estado4, R.drawable.estado5, R.drawable.estado6,
        R.drawable.estado7, R.drawable.estado8, R.drawable.estado9,
        R.drawable.estado10
    )

    // Catálogo precargado de casos clínicos didácticos
    private val sampleProfiles by lazy { generateClinicalSamples() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configura el color de la barra de estado
        window.statusBarColor = ContextCompat.getColor(this, R.color.secondary_crimson_dark)

        // Infla y establece el contenido de la actividad usando View Binding
        binding = ActivityHematologySamplesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura el botón de regreso en la barra superior
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Construye la cuadrícula interactiva de muestras
        setupSampleGrid()
    }

    // Construye dinámicamente las tarjetas de muestras en la cuadrícula
    private fun setupSampleGrid() {
        binding.gridSamples.removeAllViews()

        for (i in 0 until 10) {
            val sample = sampleProfiles[i]

            val card = MaterialCardView(this).apply {
                radius = dp(14).toFloat()
                cardElevation = dp(3).toFloat()
                setCardBackgroundColor(ContextCompat.getColor(this@HematologySamplesActivity, R.color.white))
                strokeWidth = dp(1)
                strokeColor = ContextCompat.getColor(this@HematologySamplesActivity, R.color.stroke_border)
                isClickable = true
                isFocusable = true

                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                    setMargins(dp(6), dp(6), dp(6), dp(6))
                }
                layoutParams = params

                setOnClickListener {
                    openSampleReport(sample)
                }
            }

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }

            // Fila superior: Tubo # y Folio
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvTitle = TextView(this).apply {
                text = "Tubo #${sample.sampleNumber}"
                textSize = 14.5f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(this@HematologySamplesActivity, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                maxLines = 1
            }
            topRow.addView(tvTitle)

            val tvFolio = TextView(this).apply {
                text = sample.folio
                textSize = 11.5f
                setTextColor(ContextCompat.getColor(this@HematologySamplesActivity, R.color.secondary_crimson))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                maxLines = 1
            }
            topRow.addView(tvFolio)
            container.addView(topRow)

            // Badge de Diagnóstico (En su propia fila para que nunca corte el título de Tubo #X)
            val tvFlag = TextView(this).apply {
                text = sample.flag
                textSize = 10f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(sample.flagColor)
                background = ContextCompat.getDrawable(this@HematologySamplesActivity, R.drawable.bg_chip_soft)
                setPadding(dp(8), dp(3), dp(8), dp(3))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.topMargin = dp(6)
                    it.bottomMargin = dp(6)
                }
            }
            container.addView(tvFlag)

            // Métricas clave en resumen
            val tvMetrics = TextView(this).apply {
                text = "RBC: ${sample.rbc} • Hb: ${sample.hb}\nWBC: ${sample.wbc} • PLT: ${sample.plt.toInt()}"
                textSize = 11.5f
                setTextColor(ContextCompat.getColor(this@HematologySamplesActivity, R.color.text_secondary))
                setLineSpacing(dp(2).toFloat(), 1f)
            }
            container.addView(tvMetrics)

            card.addView(container)
            binding.gridSamples.addView(card)
        }
    }

    // Inicia la actividad de reporte de resultados para la muestra seleccionada
    private fun openSampleReport(sample: AnalyzerSample) {
        val intent = Intent(this, HematologyResultsActivity::class.java).apply {
            putExtra("EXTRA_SAMPLE", sample)
        }
        startActivity(intent)
    }

    // Genera el catálogo didáctico con 10 perfiles hematológicos reales y patologías frecuentes
    private fun generateClinicalSamples(): List<AnalyzerSample> {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        return listOf(
            AnalyzerSample(
                sampleNumber = 1,
                folio = "CTRL-001",
                fecha = today,
                rbc = 4.90,
                hb = 14.5,
                hct = 43.5,
                vcm = 88.8,
                hcm = 29.6,
                chcm = 33.3,
                wbc = 6.80,
                neut = 58.0,
                linf = 32.0,
                mono = 6.5,
                eos = 2.5,
                plt = 250.0,
                tipoMuestra = "Sangre venosa (EDTA)",
                responsable = "Caso didáctico simulado",
                observacionesFrotis = "Eritrocitos normocíticos normocrómicos, serie leucocitaria y plaquetaria adecuadas.",
                diagnosticoManual = "Control Hematológico Normal. Parámetros biológicos dentro de límites de referencia.",
                flag = "NORMAL",
                flagColor = 0xFF0D9488.toInt()
            ),
            AnalyzerSample(
                sampleNumber = 2,
                folio = "ANM-002",
                fecha = today,
                rbc = 3.20,
                hb = 7.8,
                hct = 21.9,
                vcm = 68.5,
                hcm = 24.3,
                chcm = 35.6,
                wbc = 7.80,
                neut = 62.0,
                linf = 28.0,
                mono = 6.0,
                eos = 3.0,
                plt = 480.0,
                tipoMuestra = "Sangre venosa (EDTA)",
                responsable = "Caso didáctico simulado",
                observacionesFrotis = "Microcitosis e hipocromía marcadas (++), dianocitos, trombocitosis reactiva.",
                diagnosticoManual = "Anemia Microcítica Hipocrómica Severa (Compatible con Deficiencia de Hierro / Ferropénica).",
                flag = "ANEMIA FERROPÉNICA",
                flagColor = 0xFFE11D48.toInt()
            ),
            AnalyzerSample(
                sampleNumber = 3,
                folio = "MEG-003",
                fecha = today,
                rbc = 2.10,
                hb = 7.2,
                hct = 24.8,
                vcm = 118.0,
                hcm = 34.3,
                chcm = 29.0,
                wbc = 3.20,
                neut = 50.0,
                linf = 42.0,
                mono = 6.0,
                eos = 2.0,
                plt = 95.0,
                tipoMuestra = "Sangre venosa (EDTA)",
                responsable = "Caso didáctico simulado",
                observacionesFrotis = "Macrocitos ovales, neutrófilos hipersegmentados (pleocariocitos), anisocitosis marcada.",
                diagnosticoManual = "Anemia Macrocítica con Pancitopenia Leve (Compatible con Anemia Megaloblástica por Déficit B12/Folatos).",
                flag = "ANEMIA MACROCÍTICA",
                flagColor = 0xFFE11D48.toInt()
            ),
            AnalyzerSample(
                sampleNumber = 4,
                folio = "LEUK-004",
                fecha = today,
                rbc = 2.40,
                hb = 6.9,
                hct = 20.7,
                vcm = 86.2,
                hcm = 28.7,
                chcm = 33.3,
                wbc = 78.50,
                neut = 15.0,
                linf = 10.0,
                mono = 5.0,
                eos = 1.0,
                plt = 22.0,
                tipoMuestra = "Sangre venosa (EDTA)",
                responsable = "Caso didáctico simulado",
                observacionesFrotis = "Presencia de 69% de blastos leucémicos con cuerpos de Auer, hiato leucémico, plaquetopenia crítica.",
                diagnosticoManual = "Hiperleucocitosis con Pancitopenia Secundaria. ALERTA CRÍTICA: Sugestivo de Leucemia Mieloide Aguda.",
                flag = "LEUCEMIA AGUDA",
                flagColor = 0xFF991B1B.toInt()
            ),
            AnalyzerSample(
                sampleNumber = 5,
                folio = "INF-005",
                fecha = today,
                rbc = 4.60,
                hb = 13.5,
                hct = 40.5,
                vcm = 88.0,
                hcm = 29.3,
                chcm = 33.3,
                wbc = 23.40,
                neut = 88.0,
                linf = 7.0,
                mono = 4.0,
                eos = 1.0,
                plt = 185.0,
                tipoMuestra = "Sangre venosa (EDTA)",
                responsable = "Caso didáctico simulado",
                observacionesFrotis = "Neutrofilia marcada con desviación a la izquierda (bandas), granulaciones tóxicas y cuerpos de Döhle.",
                diagnosticoManual = "Leucocitosis Neutrofílica Severa (Reacción Leucemoide / Sepsis Bacteriana Aguda).",
                flag = "NEUTROFILIA / SEPSIS",
                flagColor = 0xFFD97706.toInt()
            ),
            AnalyzerSample(
                sampleNumber = 6,
                folio = "VIR-006",
                fecha = today,
                rbc = 4.75,
                hb = 14.1,
                hct = 42.3,
                vcm = 89.0,
                hcm = 29.7,
                chcm = 33.3,
                wbc = 14.20,
                neut = 24.0,
                linf = 68.0,
                mono = 6.0,
                eos = 2.0,
                plt = 190.0,
                tipoMuestra = "Sangre venosa (EDTA)",
                responsable = "Caso didáctico simulado",
                observacionesFrotis = "Linfocitos reactivos activados tipo Downey (linfocitosis atípica).",
                diagnosticoManual = "Linfocitosis Reactiva Absoluta (Compatible con Mononucleosis Infecciosa / Cuadro Viral Agudo).",
                flag = "LINFOCITOSIS VIRAL",
                flagColor = 0xFF2563EB.toInt()
            ),
            AnalyzerSample(
                sampleNumber = 7,
                folio = "PTI-007",
                fecha = today,
                rbc = 4.80,
                hb = 14.0,
                hct = 42.0,
                vcm = 87.5,
                hcm = 29.2,
                chcm = 33.3,
                wbc = 6.50,
                neut = 60.0,
                linf = 31.0,
                mono = 6.0,
                eos = 3.0,
                plt = 15.0,
                tipoMuestra = "Sangre venosa (EDTA)",
                responsable = "Caso didáctico simulado",
                observacionesFrotis = "Plaquetopenia severa en frotis (< 2 por campo de inmersión), eritrocitos y leucocitos normales.",
                diagnosticoManual = "Trombocitopenia Aislada Severa. Alto riesgo hemorrágico (Compatible con Púrpura Trombocitopénica Inmune).",
                flag = "TROMBOCITOPENIA CRÍTICA",
                flagColor = 0xFF991B1B.toInt()
            ),
            AnalyzerSample(
                sampleNumber = 8,
                folio = "PV-008",
                fecha = today,
                rbc = 6.90,
                hb = 19.8,
                hct = 59.4,
                vcm = 86.1,
                hcm = 28.7,
                chcm = 33.3,
                wbc = 12.50,
                neut = 70.0,
                linf = 22.0,
                mono = 6.0,
                eos = 2.0,
                plt = 450.0,
                tipoMuestra = "Sangre venosa (EDTA)",
                responsable = "Caso didáctico simulado",
                observacionesFrotis = "Eritrocitosis marcada con apiñamiento eritrocitario, hiperviscosidad frotis denso.",
                diagnosticoManual = "Poliglobulia Absoluta / Policitemia Vera (Trastorno Mieloproliferativo Crónico).",
                flag = "POLICITEMIA VERA",
                flagColor = 0xFFE11D48.toInt()
            ),
            AnalyzerSample(
                sampleNumber = 9,
                folio = "EOS-009",
                fecha = today,
                rbc = 4.65,
                hb = 13.8,
                hct = 41.4,
                vcm = 89.0,
                hcm = 29.7,
                chcm = 33.3,
                wbc = 11.80,
                neut = 46.0,
                linf = 26.0,
                mono = 4.0,
                eos = 24.0,
                plt = 235.0,
                tipoMuestra = "Sangre venosa (EDTA)",
                responsable = "Caso didáctico simulado",
                observacionesFrotis = "Eosinofilia marcada con gránulos bilobulados prominentes.",
                diagnosticoManual = "Eosinofilia Periférica Severa (Compatible con Reacción Alérgica Sistémica / Parasitosis Helmíntica).",
                flag = "EOSINOFILIA SEVERA",
                flagColor = 0xFFD97706.toInt()
            ),
            AnalyzerSample(
                sampleNumber = 10,
                folio = "HEM-010",
                fecha = today,
                rbc = 2.70,
                hb = 7.9,
                hct = 24.3,
                vcm = 90.0,
                hcm = 29.3,
                chcm = 32.5,
                wbc = 9.80,
                neut = 65.0,
                linf = 25.0,
                mono = 6.0,
                eos = 4.0,
                plt = 210.0,
                tipoMuestra = "Sangre venosa (EDTA)",
                responsable = "Caso didáctico simulado",
                observacionesFrotis = "Policromatofilia abundante, esferocitos, esquistocitos, reticulocitosis indirecta.",
                diagnosticoManual = "Anemia Hemolítica Normocítica Regenerativa (Sugestivo de Anemia Hemolítica Autoinmune).",
                flag = "ANEMIA HEMOLÍTICA",
                flagColor = 0xFFE11D48.toInt()
            )
        )
    }

    // Convierte valores de dp a píxeles de pantalla
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

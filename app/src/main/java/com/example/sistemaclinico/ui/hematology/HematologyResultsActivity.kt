package com.example.sistemaclinico.ui.hematology

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.sistemaclinico.R
import com.example.sistemaclinico.databinding.ActivityHematologyResultsBinding

class HematologyResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHematologyResultsBinding
    private var sampleData: HematologySamplesActivity.AnalyzerSample? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configura el color de la barra de estado
        window.statusBarColor = ContextCompat.getColor(this, R.color.secondary_crimson_dark)

        // Infla y establece el contenido de la actividad usando View Binding
        binding = ActivityHematologyResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura la acción de regreso en la barra superior
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Recupera los datos de la muestra transferidos por el Intent
        sampleData = intent.getSerializableExtra("EXTRA_SAMPLE") as? HematologySamplesActivity.AnalyzerSample

        val sample = sampleData
        if (sample == null) {
            Toast.makeText(this, getString(R.string.toast_no_sample_data), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Renderiza el reporte analítico completo
        renderSampleReport(sample)
    }

    // Renderiza el encabezado y las series de la biometría hemática
    private fun renderSampleReport(sample: HematologySamplesActivity.AnalyzerSample) {
        binding.tvSampleHeaderTitle.text = getString(R.string.sample_header_title_format, sample.sampleNumber, sample.folio)
        binding.tvSampleHeaderSub.text = getString(R.string.sample_header_sub_format, sample.responsable, sample.fecha)
        binding.tvSampleHeaderFlag.text = sample.flag
        binding.tvSampleHeaderFlag.setTextColor(sample.flagColor)
        binding.tvDiagnosticText.text = getString(R.string.sample_diag_frotis_format, sample.diagnosticoManual, sample.observacionesFrotis)

        renderRedSeries(sample)
        renderWhiteSeries(sample)
        renderPlateletSeries(sample)
    }

    // Renderiza la tabla de parámetros de la serie roja
    private fun renderRedSeries(sample: HematologySamplesActivity.AnalyzerSample) {
        val table = binding.tableRedSeries
        table.removeAllViews()

        addTableHeader(table, listOf(getString(R.string.th_parameter), getString(R.string.th_result), getString(R.string.th_reference), getString(R.string.th_unit)))
        addTableRow(table, getString(R.string.bio_rbc), String.format(java.util.Locale.US, "%.2f", sample.rbc), "4.00 - 5.50", "10⁶/µL")
        addTableRow(table, getString(R.string.bio_hb), String.format(java.util.Locale.US, "%.1f", sample.hb), "12.0 - 16.0", "g/dL")
        addTableRow(table, getString(R.string.bio_hct), String.format(java.util.Locale.US, "%.1f", sample.hct), "36.0 - 48.0", "%")
        addTableRow(table, getString(R.string.bio_vcm), String.format(java.util.Locale.US, "%.1f", sample.vcm), "80.0 - 98.0", "fL")
        addTableRow(table, getString(R.string.bio_hcm), String.format(java.util.Locale.US, "%.1f", sample.hcm), "27.0 - 33.0", "pg")
        addTableRow(table, getString(R.string.bio_chcm), String.format(java.util.Locale.US, "%.1f", sample.chcm), "32.0 - 36.0", "g/dL")
    }

    // Renderiza la tabla de parámetros de la serie blanca
    private fun renderWhiteSeries(sample: HematologySamplesActivity.AnalyzerSample) {
        val table = binding.tableWhiteSeries
        table.removeAllViews()

        addTableHeader(table, listOf(getString(R.string.th_parameter), getString(R.string.th_result), getString(R.string.th_reference), getString(R.string.th_unit)))
        addTableRow(table, getString(R.string.bio_wbc), String.format(java.util.Locale.US, "%.2f", sample.wbc), "4.50 - 11.00", "10³/µL")
        addTableRow(table, getString(R.string.bio_neut), String.format(java.util.Locale.US, "%.1f", sample.neut), "40.0 - 70.0", "%")
        addTableRow(table, getString(R.string.bio_linf), String.format(java.util.Locale.US, "%.1f", sample.linf), "20.0 - 45.0", "%")
        addTableRow(table, getString(R.string.bio_mono), String.format(java.util.Locale.US, "%.1f", sample.mono), "2.0 - 10.0", "%")
        addTableRow(table, getString(R.string.bio_eos), String.format(java.util.Locale.US, "%.1f", sample.eos), "1.0 - 5.0", "%")
    }

    // Renderiza la tabla de parámetros de la serie plaquetaria
    private fun renderPlateletSeries(sample: HematologySamplesActivity.AnalyzerSample) {
        val table = binding.tablePlateletSeries
        table.removeAllViews()

        addTableHeader(table, listOf(getString(R.string.th_parameter), getString(R.string.th_result), getString(R.string.th_reference), getString(R.string.th_unit)))
        addTableRow(table, getString(R.string.bio_plt), String.format(java.util.Locale.US, "%,d", sample.plt.toLong()), "150,000 - 450,000", "/µL")
    }

    // Agrega una fila de encabezado a la tabla especificada
    private fun addTableHeader(table: TableLayout, headers: List<String>) {
        val row = TableRow(this).apply {
            setBackgroundColor(0xFFF1F5F9.toInt())
            setPadding(dp(6), dp(8), dp(6), dp(8))
        }
        for (h in headers) {
            val tv = TextView(this).apply {
                text = h
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(this@HematologyResultsActivity, R.color.text_primary))
                gravity = Gravity.START
                setPadding(dp(6), dp(2), dp(6), dp(2))
            }
            row.addView(tv)
        }
        table.addView(row)
    }

    // Agrega una fila de datos a la tabla especificada
    private fun addTableRow(table: TableLayout, param: String, result: String, ref: String, unit: String) {
        val row = TableRow(this).apply {
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val tvParam = TextView(this).apply {
            text = param
            textSize = 12.5f
            setTextColor(ContextCompat.getColor(this@HematologyResultsActivity, R.color.text_primary))
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }
        val tvRes = TextView(this).apply {
            text = result
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@HematologyResultsActivity, R.color.text_primary))
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }
        val tvRef = TextView(this).apply {
            text = ref
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@HematologyResultsActivity, R.color.text_secondary))
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }
        val tvUnit = TextView(this).apply {
            text = unit
            textSize = 11.5f
            setTextColor(ContextCompat.getColor(this@HematologyResultsActivity, R.color.text_muted))
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }

        row.addView(tvParam)
        row.addView(tvRes)
        row.addView(tvRef)
        row.addView(tvUnit)
        table.addView(row)
    }

    // Convierte valores de dp a píxeles de pantalla
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

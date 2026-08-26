package com.example.sistemaclinico.data

// Gestor central de base de datos SQLite para el Sistema Clínico.
// Implementa el patrón SQLiteOpenHelper para la creación nativa de esquemas DDL (10 tablas),
// operaciones CRUD completas, integridad referencial con eliminación en cascada,
// fórmulas de índices eritrocitarios y cálculo automático de diagnósticos hematológicos.

import com.example.sistemaclinico.R
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "SistemaClinico.db"
        private const val DATABASE_VERSION = 1
        private const val TAG = "DatabaseHelper"

        const val TABLE_PACIENTES          = "datos_pacientes"
        const val TABLE_ALERGIAS           = "datos_alergias"
        const val TABLE_TIPOSANGRE         = "datos_tiposangre"
        const val TABLE_CLIC_PAC           = "datos_clic_pac"
        const val TABLE_PAC_GR             = "datos_pac_gr"
        const val TABLE_PAC_GB             = "datos_pac_gb"
        const val TABLE_TIPOCANCER         = "datos_tipocancer"
        const val TABLE_PAC_ENFER          = "datos_pac_enfermedad"
        const val TABLE_BIOMETRIA          = "biometria_hematica"
        const val TABLE_HISTORIAL_ARCHIVOS = "historial_analisis_archivos"

        @Volatile
        private var instance: DatabaseHelper? = null

        // Retorna la instancia Singleton única de DatabaseHelper
        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    // Inicializa las tablas al crear la base de datos por primera vez
    override fun onCreate(db: SQLiteDatabase) {
        createAllTables(db)
    }

    // Actualiza el esquema de la base de datos
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        createAllTables(db)
    }

    // Garantiza la existencia de las tablas cada vez que se abre la base de datos
    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        createAllTables(db)
    }

    // Crea todas las tablas del sistema clínico de forma nativa e idempotente (DDL en código).
    private fun createAllTables(db: SQLiteDatabase) {
        try {
            // 1. Pacientes
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_PACIENTES (
                    clave_paciente INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre VARCHAR(50) NOT NULL,
                    apellido_pat VARCHAR(50) NOT NULL,
                    apellido_mat VARCHAR(50) NOT NULL,
                    peso FLOAT NOT NULL,
                    estatura FLOAT NOT NULL,
                    tipo_sangre VARCHAR(20),
                    fecha_nacimiento DATE NOT NULL,
                    sexo TEXT DEFAULT 'M'
                )
            """.trimIndent())

            // 2. Catálogo de Alergias
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_ALERGIAS (
                    clave_alergia INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre VARCHAR(50) NOT NULL,
                    descripcion VARCHAR(100) NOT NULL,
                    categoria TEXT DEFAULT 'General',
                    contraindicaciones TEXT
                )
            """.trimIndent())

            // 3. Catálogo de Tipos de Sangre
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_TIPOSANGRE (
                    clave_tiposangre INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre VARCHAR(20) NOT NULL,
                    descripcion VARCHAR(50) NOT NULL
                )
            """.trimIndent())

            // 4. Relación Paciente - Alergia
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_CLIC_PAC (
                    no_operacion INTEGER PRIMARY KEY AUTOINCREMENT,
                    aux_clave_pac INTEGER NOT NULL,
                    aux_clave_alergia INTEGER NOT NULL,
                    fecha_operacion DATE NOT NULL,
                    sintomas_paciente TEXT,
                    severidad TEXT DEFAULT 'Moderada',
                    tratamiento_rescate TEXT
                )
            """.trimIndent())

            // 5. Conteo de Glóbulos Rojos (GR)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_PAC_GR (
                    no_operacion INTEGER PRIMARY KEY AUTOINCREMENT,
                    aux_clave_pac INTEGER NOT NULL,
                    cant_gr INT NOT NULL,
                    fecha_operacion DATE NOT NULL
                )
            """.trimIndent())

            // 6. Conteo de Glóbulos Blancos (GB)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_PAC_GB (
                    no_operacion INTEGER PRIMARY KEY AUTOINCREMENT,
                    aux_clave_pac INTEGER NOT NULL,
                    cant_gb INT NOT NULL,
                    fecha_operacion DATE NOT NULL
                )
            """.trimIndent())

            // 7. Catálogo Oncológico
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_TIPOCANCER (
                    clave_cancer INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre VARCHAR(20) NOT NULL,
                    descripcion VARCHAR(100) NOT NULL
                )
            """.trimIndent())

            // 8. Relación Paciente - Enfermedad / Cáncer
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_PAC_ENFER (
                    no_operacion INTEGER PRIMARY KEY AUTOINCREMENT,
                    aux_clave_pac INTEGER NOT NULL,
                    aux_clave_cancer INTEGER NOT NULL,
                    fecha_detectada DATE NOT NULL,
                    fecha_inicio_tratamiento DATE NOT NULL,
                    fecha_final_tratamiento DATE NOT NULL,
                    resultado VARCHAR(20) NOT NULL
                )
            """.trimIndent())

            // 9. Biometría Hemática Completa
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_BIOMETRIA (
                    id_estudio INTEGER PRIMARY KEY AUTOINCREMENT,
                    clave_paciente TEXT NOT NULL,
                    fecha TEXT NOT NULL,
                    folio TEXT,
                    eritrocitos REAL NOT NULL,
                    hemoglobina REAL NOT NULL,
                    hematocrito REAL NOT NULL,
                    vcm REAL NOT NULL,
                    hcm REAL,
                    chcm REAL,
                    leucocitos REAL NOT NULL,
                    neutrofilos REAL NOT NULL,
                    linfocitos REAL NOT NULL,
                    monocitos REAL NOT NULL,
                    eosinofilos REAL NOT NULL,
                    plaquetas REAL NOT NULL,
                    tipo_muestra TEXT DEFAULT 'Sangre venosa (EDTA)',
                    responsable TEXT DEFAULT 'Q.F.B. Especialista',
                    frotis TEXT DEFAULT 'Normocítico, normocrómico',
                    diagnostico TEXT
                )
            """.trimIndent())

            // 10. Historial de Análisis de Archivos
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_HISTORIAL_ARCHIVOS (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre_archivo TEXT,
                    nombre_copia TEXT,
                    fecha_analisis TEXT,
                    folio TEXT,
                    rbc REAL,
                    hb REAL,
                    hct REAL,
                    vcm REAL,
                    hcm REAL,
                    chcm REAL,
                    wbc REAL,
                    neut REAL,
                    linf REAL,
                    mono REAL,
                    eos REAL,
                    plt REAL,
                    diagnostico TEXT,
                    flag TEXT,
                    paciente_vinculado TEXT
                )
            """.trimIndent())

        } catch (e: Exception) {
            Log.e(TAG, "Error creando esquemas de base de datos", e)
        }
    }

    // Consulta los pacientes en SQLite con soporte de búsqueda por texto
    fun getDatosPacientes(query: String = ""): Cursor? {
        val db = readableDatabase
        return if (query.isEmpty()) {
            db.rawQuery("SELECT clave_paciente, nombre, apellido_pat, apellido_mat, peso, estatura, tipo_sangre, fecha_nacimiento, COALESCE(sexo, 'M') AS sexo FROM $TABLE_PACIENTES", null)
        } else {
            val q = "%$query%"
            db.rawQuery(
                """SELECT clave_paciente, nombre, apellido_pat, apellido_mat, peso, estatura, tipo_sangre, fecha_nacimiento, COALESCE(sexo, 'M') AS sexo 
                   FROM $TABLE_PACIENTES 
                   WHERE CAST(clave_paciente AS TEXT) LIKE ? 
                      OR nombre LIKE ? 
                      OR apellido_pat LIKE ? 
                      OR apellido_mat LIKE ? 
                      OR (nombre || ' ' || apellido_pat || ' ' || apellido_mat) LIKE ?
                      OR tipo_sangre LIKE ?""",
                arrayOf(q, q, q, q, q, q)
            )
        }
    }

    // Consulta y unifica los perfiles de pacientes con todas sus tablas asociadas
    fun getPatientProfiles(query: String = ""): List<PatientProfile> {
        val profiles = mutableListOf<PatientProfile>()
        val db = readableDatabase
        val patCursor = getDatosPacientes(query) ?: return profiles

        patCursor.use { pc ->
            if (!pc.moveToFirst()) return profiles
            do {
                val clave = pc.getString(0) ?: continue
                val nombre = pc.getString(1) ?: ""
                val apat = pc.getString(2) ?: ""
                val amat = pc.getString(3) ?: ""
                val peso = pc.getString(4) ?: ""
                val estatura = pc.getString(5) ?: ""
                val sangre = pc.getString(6) ?: ""
                val fechaNac = pc.getString(7) ?: ""
                val sexo = if (pc.columnCount > 8) (pc.getString(8) ?: "M") else "M"

                // 1. Alergias del paciente
                val alergias = mutableListOf<AllergyDetail>()
                try {
                    db.rawQuery(
                        """SELECT cp.aux_clave_alergia, a.nombre, COALESCE(a.categoria, 'General'), cp.severidad, cp.sintomas_paciente, cp.tratamiento_rescate, cp.fecha_operacion 
                           FROM $TABLE_CLIC_PAC cp
                           INNER JOIN $TABLE_ALERGIAS a ON cp.aux_clave_alergia = a.clave_alergia
                           WHERE CAST(cp.aux_clave_pac AS TEXT) = ?""",
                        arrayOf(clave)
                    ).use { c ->
                        if (c.moveToFirst()) {
                            do {
                                val id = c.getString(0) ?: ""
                                val nom = c.getString(1) ?: ""
                                val cat = c.getString(2) ?: ""
                                val sev = c.getString(3) ?: ""
                                val sint = c.getString(4) ?: ""
                                val resc = c.getString(5) ?: ""
                                val fecha = c.getString(6) ?: ""
                                alergias.add(AllergyDetail(
                                    claveAlergia = id,
                                    nombre = nom,
                                    categoria = cat,
                                    severidad = sev,
                                    sintomas = sint,
                                    tratamientoRescate = resc,
                                    fecha = fecha
                                ))
                            } while (c.moveToNext())
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error consultando alergias para paciente $clave", e)
                }

                // 2. Muestras de Glóbulos Rojos
                val gr = mutableListOf<BloodSample>()
                try {
                    db.rawQuery(
                        "SELECT no_operacion, cant_gr, fecha_operacion FROM $TABLE_PAC_GR WHERE CAST(aux_clave_pac AS TEXT) = ? ORDER BY fecha_operacion ASC, no_operacion ASC",
                        arrayOf(clave)
                    ).use { c ->
                        if (c.moveToFirst()) {
                            do {
                                val rawOp = c.getString(0) ?: ""
                                val formattedOp = if (rawOp.startsWith("GR-", ignoreCase = true)) rawOp.uppercase() else "GR-$rawOp"
                                gr.add(BloodSample(
                                    noOperacion = formattedOp,
                                    cantidad = c.getString(1) ?: "",
                                    fecha = c.getString(2) ?: ""
                                ))
                            } while (c.moveToNext())
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error consultando glóbulos rojos para paciente $clave", e)
                }

                // 3. Muestras de Glóbulos Blancos
                val gb = mutableListOf<BloodSample>()
                try {
                    db.rawQuery(
                        "SELECT no_operacion, cant_gb, fecha_operacion FROM $TABLE_PAC_GB WHERE CAST(aux_clave_pac AS TEXT) = ? ORDER BY fecha_operacion ASC, no_operacion ASC",
                        arrayOf(clave)
                    ).use { c ->
                        if (c.moveToFirst()) {
                            do {
                                val rawOp = c.getString(0) ?: ""
                                val formattedOp = if (rawOp.startsWith("GB-", ignoreCase = true)) rawOp.uppercase() else "GB-$rawOp"
                                gb.add(BloodSample(
                                    noOperacion = formattedOp,
                                    cantidad = c.getString(1) ?: "",
                                    fecha = c.getString(2) ?: ""
                                ))
                            } while (c.moveToNext())
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error consultando glóbulos blancos para paciente $clave", e)
                }

                // 4. Diagnósticos y tratamientos oncológicos
                val tratamientos = mutableListOf<DiagnosticoOncologico>()
                try {
                    db.rawQuery(
                        """SELECT pe.aux_clave_cancer, c.nombre, pe.resultado, pe.fecha_detectada, pe.fecha_inicio_tratamiento, pe.fecha_final_tratamiento 
                           FROM $TABLE_PAC_ENFER pe
                           INNER JOIN $TABLE_TIPOCANCER c ON pe.aux_clave_cancer = c.clave_cancer
                           WHERE CAST(pe.aux_clave_pac AS TEXT) = ?""",
                        arrayOf(clave)
                    ).use { c ->
                        if (c.moveToFirst()) {
                            do {
                                val id = c.getString(0) ?: ""
                                val nom = c.getString(1) ?: ""
                                val res = c.getString(2) ?: ""
                                val fDet = c.getString(3) ?: ""
                                val fIni = c.getString(4) ?: ""
                                val fFin = c.getString(5) ?: ""
                                tratamientos.add(DiagnosticoOncologico(
                                    claveCancer = id,
                                    tipoCancer = nom,
                                    resultado = res,
                                    fechaDeteccion = fDet,
                                    fechaInicioTratamiento = fIni,
                                    fechaFinalTratamiento = fFin
                                ))
                            } while (c.moveToNext())
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error consultando patologías oncológicas para paciente $clave", e)
                }

                // 5. Biometrías hemáticas completas con trazabilidad y sexo
                val biometrias = mutableListOf<BiometriaHematica>()
                try {
                    db.rawQuery(
                        """SELECT id_estudio, clave_paciente, fecha, COALESCE(folio, 'BH-' || id_estudio), eritrocitos, hemoglobina, hematocrito, vcm, COALESCE(hcm, ROUND((hemoglobina * 10.0) / eritrocitos, 1)), COALESCE(chcm, ROUND((hemoglobina * 100.0) / hematocrito, 1)), leucocitos, neutrofilos, linfocitos, monocitos, eosinofilos, plaquetas, COALESCE(tipo_muestra, 'Sangre venosa (EDTA)'), COALESCE(responsable, 'Q.F.B. Especialista'), COALESCE(frotis, 'Normocítico, normocrómico'), diagnostico 
                           FROM $TABLE_BIOMETRIA 
                           WHERE CAST(clave_paciente AS TEXT) = ? OR clave_paciente = ?
                           ORDER BY id_estudio ASC""",
                        arrayOf(clave, "PAC-$clave")
                    ).use { c ->
                        if (c.moveToFirst()) {
                            do {
                                biometrias.add(BiometriaHematica(
                                    idEstudio = c.getString(0) ?: "",
                                    clavePaciente = c.getString(1) ?: "",
                                    fecha = c.getString(2) ?: "",
                                    folio = c.getString(3) ?: "BH-${c.getString(0)}",
                                    eritrocitos = String.format("%.2f", c.getDouble(4)),
                                    hemoglobina = String.format("%.1f", c.getDouble(5)),
                                    hematocrito = String.format("%.1f", c.getDouble(6)),
                                    vcm = String.format("%.1f", c.getDouble(7)),
                                    hcm = String.format("%.1f", c.getDouble(8)),
                                    chcm = String.format("%.1f", c.getDouble(9)),
                                    leucocitos = String.format("%.1f", c.getDouble(10)),
                                    neutrofilos = String.format("%.1f", c.getDouble(11)),
                                    linfocitos = String.format("%.1f", c.getDouble(12)),
                                    monocitos = String.format("%.1f", c.getDouble(13)),
                                    eosinofilos = String.format("%.1f", c.getDouble(14)),
                                    plaquetas = String.format("%.0f", c.getDouble(15)),
                                    tipoMuestra = c.getString(16) ?: "Sangre venosa (EDTA)",
                                    responsable = c.getString(17) ?: "Q.F.B. Especialista",
                                    observacionesFrotis = c.getString(18) ?: "Normocítico, normocrómico",
                                    diagnostico = c.getString(19) ?: ""
                                ))
                            } while (c.moveToNext())
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error consultando biometrías hemáticas para paciente $clave", e)
                }

                profiles.add(PatientProfile(
                    clave = clave,
                    nombre = nombre,
                    apellidoPat = apat,
                    apellidoMat = amat,
                    peso = peso,
                    estatura = estatura,
                    tipoSangre = sangre,
                    fechaNacimiento = fechaNac,
                    sexo = sexo,
                    alergias = alergias,
                    globulosRojos = gr,
                    globulosBlancos = gb,
                    tratamientos = tratamientos,
                    biometrias = biometrias
                ))
            } while (pc.moveToNext())
        }
        return profiles
    }

    // Consulta el perfil clínico consolidado de un paciente específico por su clave
    fun getPatientProfile(clavePaciente: String): PatientProfile? {
        val list = getPatientProfiles(clavePaciente)
        return list.firstOrNull { it.clave.equals(clavePaciente, ignoreCase = true) } ?: list.firstOrNull()
    }

    // Consulta el catálogo de alergias registradas
    fun getDatosAlergias(query: String = ""): Cursor? {
        val db = readableDatabase
        return if (query.isEmpty()) {
            db.rawQuery("SELECT clave_alergia, nombre, COALESCE(categoria, 'General') AS categoria FROM $TABLE_ALERGIAS", null)
        } else {
            val q = "%$query%"
            db.rawQuery(
                """SELECT clave_alergia, nombre, COALESCE(categoria, 'General') AS categoria 
                   FROM $TABLE_ALERGIAS 
                   WHERE CAST(clave_alergia AS TEXT) LIKE ? OR nombre LIKE ? OR categoria LIKE ?""",
                arrayOf(q, q, q)
            )
        }
    }

    // Consulta el catálogo de neoplasias y tipos de cáncer
    fun getDatosCancer(query: String = ""): Cursor? {
        val db = readableDatabase
        return if (query.isEmpty()) {
            db.rawQuery("SELECT clave_cancer, nombre, descripcion FROM $TABLE_TIPOCANCER", null)
        } else {
            val q = "%$query%"
            db.rawQuery(
                "SELECT clave_cancer, nombre, descripcion FROM $TABLE_TIPOCANCER WHERE CAST(clave_cancer AS TEXT) LIKE ? OR nombre LIKE ? OR descripcion LIKE ?",
                arrayOf(q, q, q)
            )
        }
    }

    // Inserta un nuevo paciente en la tabla datos_pacientes
    fun insertPaciente(
        clave: String,
        nombre: String,
        apellidoPat: String,
        apellidoMat: String,
        peso: String,
        estatura: String,
        tipoSangre: String,
        fechaNacimiento: String,
        sexo: String = "M"
    ): Boolean {
        val values = ContentValues().apply {
            val numClave = clave.removePrefix("PAC-").trim().toIntOrNull()
            if (numClave != null) {
                put("clave_paciente", numClave)
            } else {
                put("clave_paciente", clave)
            }
            put("nombre",           nombre)
            put("apellido_pat",     apellidoPat)
            put("apellido_mat",     apellidoMat)
            put("peso",             peso.toDoubleOrNull() ?: 0.0)
            put("estatura",         estatura.toDoubleOrNull() ?: 0.0)
            put("tipo_sangre",      tipoSangre)
            put("fecha_nacimiento", fechaNacimiento)
            put("sexo",             sexo)
        }
        return writableDatabase.insert(TABLE_PACIENTES, null, values) != -1L
    }

    // Inserta una nueva alergia en el catálogo datos_alergias
    fun insertAlergia(clave: String, nombre: String, categoria: String = "General", descripcion: String = ""): Boolean {
        val values = ContentValues().apply {
            val numClave = clave.removePrefix("ALG-").trim().toIntOrNull()
            if (numClave != null) {
                put("clave_alergia", numClave)
            } else {
                put("clave_alergia", clave)
            }
            put("nombre",      nombre)
            put("categoria",   categoria)
            put("descripcion", if (descripcion.isNotBlank()) descripcion else "Alergia de tipo $categoria")
        }
        return writableDatabase.insert(TABLE_ALERGIAS, null, values) != -1L
    }

    // Inserta un nuevo tipo de cáncer en el catálogo datos_tipocancer
    fun insertTipoCancer(clave: String, nombre: String, descripcion: String): Boolean {
        val values = ContentValues().apply {
            val numClave = clave.removePrefix("CA-").trim().toIntOrNull()
            if (numClave != null) {
                put("clave_cancer", numClave)
            } else {
                put("clave_cancer", clave)
            }
            put("nombre",      nombre)
            put("descripcion", descripcion)
        }
        return writableDatabase.insert(TABLE_TIPOCANCER, null, values) != -1L
    }

    // Vincula una alergia diagnosticada al expediente de un paciente
    fun insertPacienteAlergia(
        clavePaciente: String,
        claveAlergia: String,
        fecha: String,
        severidad: String = "Moderada",
        sintomas: String = "",
        rescate: String = ""
    ): Boolean {
        val numPac = clavePaciente.removePrefix("PAC-").trim().toIntOrNull() ?: clavePaciente
        val numAlg = claveAlergia.removePrefix("ALG-").trim().toIntOrNull() ?: claveAlergia

        val values = ContentValues().apply {
            if (numPac is Int) put("aux_clave_pac", numPac) else put("aux_clave_pac", clavePaciente)
            if (numAlg is Int) put("aux_clave_alergia", numAlg) else put("aux_clave_alergia", claveAlergia)
            put("fecha_operacion", fecha)
            put("severidad", severidad)
            put("sintomas_paciente", sintomas)
            put("tratamiento_rescate", rescate)
        }
        return writableDatabase.insert(TABLE_CLIC_PAC, null, values) != -1L
    }

    // Vincula un diagnóstico y tratamiento oncológico al expediente del paciente
    fun insertPacienteCancer(
        clavePaciente: String,
        claveCancer: String,
        resultado: String,
        fechaDetectada: String,
        fechaInicio: String = "",
        fechaFin: String = ""
    ): Boolean {
        val numPac = clavePaciente.removePrefix("PAC-").trim().toIntOrNull() ?: clavePaciente
        val numCancer = claveCancer.removePrefix("CA-").trim().toIntOrNull() ?: claveCancer

        val values = ContentValues().apply {
            if (numPac is Int) put("aux_clave_pac", numPac) else put("aux_clave_pac", clavePaciente)
            if (numCancer is Int) put("aux_clave_cancer", numCancer) else put("aux_clave_cancer", claveCancer)
            put("resultado", resultado)
            put("fecha_detectada", fechaDetectada)
            put("fecha_inicio_tratamiento", fechaInicio)
            put("fecha_final_tratamiento", fechaFin)
        }
        return writableDatabase.insert(TABLE_PAC_ENFER, null, values) != -1L
    }

    // Obtiene una lista simplificada de pacientes (clave y nombre completo)
    fun getAllPatientsSimpleList(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        readableDatabase.rawQuery("SELECT clave_paciente, nombre, apellido_pat, apellido_mat FROM $TABLE_PACIENTES ORDER BY clave_paciente ASC", null).use { c ->
            if (c.moveToFirst()) {
                do {
                    val id = c.getString(0) ?: ""
                    val nom = "${c.getString(1) ?: ""} ${c.getString(2) ?: ""} ${c.getString(3) ?: ""}".trim()
                    val formattedId = if (id.startsWith("PAC-", ignoreCase = true)) id else "PAC-$id"
                    list.add(Pair(id, "$formattedId - $nom"))
                } while (c.moveToNext())
            }
        }
        return list
    }

    // Obtiene una lista simplificada de alergias del catálogo (clave y nombre)
    fun getAllAllergiesSimpleList(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        readableDatabase.rawQuery("SELECT clave_alergia, nombre FROM $TABLE_ALERGIAS ORDER BY clave_alergia ASC", null).use { c ->
            if (c.moveToFirst()) {
                do {
                    val id = c.getString(0) ?: ""
                    val nom = c.getString(1) ?: ""
                    val formattedId = if (id.startsWith("ALG-", ignoreCase = true)) id else "ALG-$id"
                    list.add(Pair(id, "$formattedId - $nom"))
                } while (c.moveToNext())
            }
        }
        return list
    }

    // Obtiene una lista simplificada de neoplasias del catálogo (clave y nombre)
    fun getAllCancerSimpleList(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        readableDatabase.rawQuery("SELECT clave_cancer, nombre FROM $TABLE_TIPOCANCER ORDER BY clave_cancer ASC", null).use { c ->
            if (c.moveToFirst()) {
                do {
                    val id = c.getString(0) ?: ""
                    val nom = c.getString(1) ?: ""
                    val formattedId = if (id.startsWith("CA-", ignoreCase = true)) id else "CA-$id"
                    list.add(Pair(id, "$formattedId - $nom"))
                } while (c.moveToNext())
            }
        }
        return list
    }

    // Actualiza los datos generales y somatometría del paciente
    fun updatePaciente(
        clave: String,
        nombre: String,
        apellidoPat: String,
        apellidoMat: String,
        peso: String,
        estatura: String,
        tipoSangre: String,
        fechaNacimiento: String,
        sexo: String = "M"
    ): Boolean {
        val numVal = clave.removePrefix("PAC-").trim().toIntOrNull()
        val whereVal = numVal?.toString() ?: clave

        val values = ContentValues().apply {
            put("nombre",           nombre)
            put("apellido_pat",     apellidoPat)
            put("apellido_mat",     apellidoMat)
            put("peso",             peso.toFloatOrNull() ?: 0f)
            put("estatura",         estatura.toFloatOrNull() ?: 0f)
            put("tipo_sangre",      tipoSangre)
            put("fecha_nacimiento", fechaNacimiento)
            put("sexo",             if (sexo.equals("F", true) || sexo.startsWith("Fem", true)) "F" else "M")
        }
        val rows = writableDatabase.update(
            TABLE_PACIENTES,
            values,
            "clave_paciente = ? OR CAST(clave_paciente AS TEXT) = ?",
            arrayOf(whereVal, clave)
        )
        return rows > 0
    }

    // Actualiza un registro del catálogo de alergias
    fun updateAlergia(clave: String, nombre: String, categoria: String = "General", descripcion: String = ""): Boolean {
        val numVal = clave.removePrefix("ALG-").trim().toIntOrNull()
        val whereVal = numVal?.toString() ?: clave

        val values = ContentValues().apply {
            put("nombre",    nombre)
            put("categoria", categoria)
            if (descripcion.isNotBlank()) {
                put("descripcion", descripcion)
            }
        }
        val rows = writableDatabase.update(
            TABLE_ALERGIAS,
            values,
            "clave_alergia = ? OR CAST(clave_alergia AS TEXT) = ?",
            arrayOf(whereVal, clave)
        )
        return rows > 0
    }

    // Actualiza un registro del catálogo de tipos de cáncer
    fun updateTipoCancer(clave: String, nombre: String, descripcion: String): Boolean {
        val numVal = clave.removePrefix("CA-").trim().toIntOrNull()
        val whereVal = numVal?.toString() ?: clave

        val values = ContentValues().apply {
            put("nombre",      nombre)
            put("descripcion", descripcion)
        }
        val rows = writableDatabase.update(
            TABLE_TIPOCANCER,
            values,
            "clave_cancer = ? OR CAST(clave_cancer AS TEXT) = ?",
            arrayOf(whereVal, clave)
        )
        return rows > 0
    }

    // Actualiza la severidad, síntomas o tratamiento de una alergia asociada
    fun updatePacienteAlergia(
        clavePaciente: String,
        claveAlergia: String,
        fecha: String,
        severidad: String,
        sintomas: String,
        rescate: String
    ): Boolean {
        val numPac = clavePaciente.removePrefix("PAC-").trim().toIntOrNull() ?: clavePaciente
        val numAlg = claveAlergia.removePrefix("ALG-").trim().toIntOrNull() ?: claveAlergia

        val values = ContentValues().apply {
            put("fecha_operacion", fecha)
            put("severidad", severidad)
            put("sintomas_paciente", sintomas)
            put("tratamiento_rescate", rescate)
        }
        val rows = writableDatabase.update(
            TABLE_CLIC_PAC,
            values,
            "(aux_clave_pac = ? OR CAST(aux_clave_pac AS TEXT) = ?) AND (aux_clave_alergia = ? OR CAST(aux_clave_alergia AS TEXT) = ?)",
            arrayOf(numPac.toString(), clavePaciente, numAlg.toString(), claveAlergia)
        )
        return rows > 0
    }

    // Desvincula una alergia del expediente del paciente
    fun deletePacienteAlergia(clavePaciente: String, claveAlergia: String): Boolean {
        val numPac = clavePaciente.removePrefix("PAC-").trim().toIntOrNull() ?: clavePaciente
        val numAlg = claveAlergia.removePrefix("ALG-").trim().toIntOrNull() ?: claveAlergia
        val rows = writableDatabase.delete(
            TABLE_CLIC_PAC,
            "(aux_clave_pac = ? OR CAST(aux_clave_pac AS TEXT) = ?) AND (aux_clave_alergia = ? OR CAST(aux_clave_alergia AS TEXT) = ?)",
            arrayOf(numPac.toString(), clavePaciente, numAlg.toString(), claveAlergia)
        )
        return rows > 0
    }

    // Actualiza el resultado o fechas de un diagnóstico oncológico asociado
    fun updatePacienteCancer(
        clavePaciente: String,
        claveCancer: String,
        resultado: String,
        fechaDetectada: String,
        fechaInicio: String,
        fechaFin: String
    ): Boolean {
        val numPac = clavePaciente.removePrefix("PAC-").trim().toIntOrNull() ?: clavePaciente
        val numCancer = claveCancer.removePrefix("CA-").trim().toIntOrNull() ?: claveCancer

        val values = ContentValues().apply {
            put("resultado", resultado)
            put("fecha_detectada", fechaDetectada)
            put("fecha_inicio_tratamiento", fechaInicio)
            put("fecha_final_tratamiento", fechaFin)
        }
        val rows = writableDatabase.update(
            TABLE_PAC_ENFER,
            values,
            "(aux_clave_pac = ? OR CAST(aux_clave_pac AS TEXT) = ?) AND (aux_clave_cancer = ? OR CAST(aux_clave_cancer AS TEXT) = ?)",
            arrayOf(numPac.toString(), clavePaciente, numCancer.toString(), claveCancer)
        )
        return rows > 0
    }

    // Desvincula un diagnóstico oncológico del expediente del paciente
    fun deletePacienteCancer(clavePaciente: String, claveCancer: String): Boolean {
        val numPac = clavePaciente.removePrefix("PAC-").trim().toIntOrNull() ?: clavePaciente
        val numCancer = claveCancer.removePrefix("CA-").trim().toIntOrNull() ?: claveCancer
        val rows = writableDatabase.delete(
            TABLE_PAC_ENFER,
            "(aux_clave_pac = ? OR CAST(aux_clave_pac AS TEXT) = ?) AND (aux_clave_cancer = ? OR CAST(aux_clave_cancer AS TEXT) = ?)",
            arrayOf(numPac.toString(), clavePaciente, numCancer.toString(), claveCancer)
        )
        return rows > 0
    }

    // Elimina un registro genérico garantizando la integridad referencial en cascada
    fun deleteRecord(tableName: String, primaryKeyCol: String, value: String): Boolean {
        val numVal = value.removePrefix("PAC-").removePrefix("ALG-").removePrefix("GR-").removePrefix("GB-").removePrefix("CA-").trim().toIntOrNull()
        val whereVal = numVal?.toString() ?: value

        // Integridad referencial: limpieza en cascada para evitar registros huérfanos
        when (tableName) {
            TABLE_PACIENTES -> {
                writableDatabase.delete(TABLE_CLIC_PAC, "aux_clave_pac = ? OR CAST(aux_clave_pac AS TEXT) = ?", arrayOf(whereVal, value))
                writableDatabase.delete(TABLE_PAC_GR, "aux_clave_pac = ? OR CAST(aux_clave_pac AS TEXT) = ?", arrayOf(whereVal, value))
                writableDatabase.delete(TABLE_PAC_GB, "aux_clave_pac = ? OR CAST(aux_clave_pac AS TEXT) = ?", arrayOf(whereVal, value))
                writableDatabase.delete(TABLE_PAC_ENFER, "aux_clave_pac = ? OR CAST(aux_clave_pac AS TEXT) = ?", arrayOf(whereVal, value))
                writableDatabase.delete(TABLE_BIOMETRIA, "clave_paciente = ? OR CAST(clave_paciente AS TEXT) = ?", arrayOf(whereVal, value))
            }
            TABLE_ALERGIAS -> {
                writableDatabase.delete(TABLE_CLIC_PAC, "aux_clave_alergia = ? OR CAST(aux_clave_alergia AS TEXT) = ?", arrayOf(whereVal, value))
            }
            TABLE_TIPOCANCER -> {
                writableDatabase.delete(TABLE_PAC_ENFER, "aux_clave_cancer = ? OR CAST(aux_clave_cancer AS TEXT) = ?", arrayOf(whereVal, value))
            }
        }

        val result = writableDatabase.delete(
            tableName,
            "$primaryKeyCol = ? OR CAST($primaryKeyCol AS TEXT) = ?",
            arrayOf(whereVal, value)
        )
        return result > 0
    }

    // Inserta un estudio de biometría hemática completa calculando automáticamente sus índices
    fun insertBiometria(
        clavePaciente: String,
        fecha: String,
        eritrocitos: Double,
        hemoglobina: Double,
        hematocrito: Double = 0.0,
        vcm: Double = 0.0,
        hcm: Double = 0.0,
        chcm: Double = 0.0,
        leucocitos: Double,
        neutrofilos: Double = 60.0,
        linfocitos: Double = 30.0,
        monocitos: Double = 6.0,
        eosinofilos: Double = 3.0,
        plaquetas: Double,
        sexo: String = "M",
        folio: String = "",
        tipoMuestra: String = "",
        responsable: String = "",
        observacionesFrotis: String = "",
        diagnosticoManual: String = ""
    ): Boolean {
        val numPac = clavePaciente.removePrefix("PAC-").trim().toIntOrNull() ?: clavePaciente
        
        // Fórmulas automáticas de laboratorio
        val calcHct = if (hematocrito > 0) hematocrito else (hemoglobina * 3.0)
        val calcVcm = if (vcm > 0) vcm else if (eritrocitos > 0) ((calcHct * 10.0) / eritrocitos) else 90.0
        val calcHcm = if (hcm > 0) hcm else if (eritrocitos > 0) ((hemoglobina * 10.0) / eritrocitos) else 30.0
        val calcChcm = if (chcm > 0) chcm else if (calcHct > 0) ((hemoglobina * 100.0) / calcHct) else 33.3

        val diag = if (diagnosticoManual.isNotBlank()) diagnosticoManual
        else calcularDiagnosticoHematologico(sexo, eritrocitos, hemoglobina, calcHct, calcVcm, calcHcm, calcChcm, leucocitos, neutrofilos, linfocitos, plaquetas)

        val values = ContentValues().apply {
            put("clave_paciente", numPac.toString())
            put("fecha", fecha)
            if (folio.isNotBlank()) put("folio", folio)
            put("eritrocitos", eritrocitos)
            put("hemoglobina", hemoglobina)
            put("hematocrito", calcHct)
            put("vcm", calcVcm)
            put("hcm", calcHcm)
            put("chcm", calcChcm)
            put("leucocitos", leucocitos)
            put("neutrofilos", neutrofilos)
            put("linfocitos", linfocitos)
            put("monocitos", monocitos)
            put("eosinofilos", eosinofilos)
            put("plaquetas", plaquetas)
            put("tipo_muestra", tipoMuestra)
            put("responsable", responsable)
            put("frotis", observacionesFrotis)
            put("diagnostico", diag)
        }
        val id = writableDatabase.insert(TABLE_BIOMETRIA, null, values)
        if (id != -1L && folio.isBlank()) {
            writableDatabase.execSQL("UPDATE $TABLE_BIOMETRIA SET folio = 'BH-00' || id_estudio WHERE id_estudio = ?", arrayOf(id))
        }
        return id != -1L
    }

    // Actualiza los parámetros de una biometría hemática recalculando sus índices
    fun updateBiometria(
        idEstudio: String,
        fecha: String,
        eritrocitos: Double,
        hemoglobina: Double,
        hematocrito: Double = 0.0,
        vcm: Double = 0.0,
        hcm: Double = 0.0,
        chcm: Double = 0.0,
        leucocitos: Double,
        neutrofilos: Double = 60.0,
        linfocitos: Double = 30.0,
        monocitos: Double = 6.0,
        eosinofilos: Double = 3.0,
        plaquetas: Double,
        sexo: String = "M",
        folio: String = "",
        tipoMuestra: String = "",
        responsable: String = "",
        observacionesFrotis: String = "",
        diagnosticoManual: String = ""
    ): Boolean {
        val calcHct = if (hematocrito > 0) hematocrito else (hemoglobina * 3.0)
        val calcVcm = if (vcm > 0) vcm else if (eritrocitos > 0) ((calcHct * 10.0) / eritrocitos) else 90.0
        val calcHcm = if (hcm > 0) hcm else if (eritrocitos > 0) ((hemoglobina * 10.0) / eritrocitos) else 30.0
        val calcChcm = if (chcm > 0) chcm else if (calcHct > 0) ((hemoglobina * 100.0) / calcHct) else 33.3

        val diag = if (diagnosticoManual.isNotBlank()) diagnosticoManual
        else calcularDiagnosticoHematologico(sexo, eritrocitos, hemoglobina, calcHct, calcVcm, calcHcm, calcChcm, leucocitos, neutrofilos, linfocitos, plaquetas)

        val values = ContentValues().apply {
            put("fecha", fecha)
            if (folio.isNotBlank()) put("folio", folio)
            put("eritrocitos", eritrocitos)
            put("hemoglobina", hemoglobina)
            put("hematocrito", calcHct)
            put("vcm", calcVcm)
            put("hcm", calcHcm)
            put("chcm", calcChcm)
            put("leucocitos", leucocitos)
            put("neutrofilos", neutrofilos)
            put("linfocitos", linfocitos)
            put("monocitos", monocitos)
            put("eosinofilos", eosinofilos)
            put("plaquetas", plaquetas)
            put("tipo_muestra", tipoMuestra)
            put("responsable", responsable)
            put("frotis", observacionesFrotis)
            put("diagnostico", diag)
        }
        val rows = writableDatabase.update(
            TABLE_BIOMETRIA,
            values,
            "id_estudio = ? OR CAST(id_estudio AS TEXT) = ?",
            arrayOf(idEstudio, idEstudio)
        )
        return rows > 0
    }

    // Elimina un estudio hematológico de la tabla biometria_hematica
    fun deleteBiometria(idEstudio: String): Boolean {
        val rows = writableDatabase.delete(
            TABLE_BIOMETRIA,
            "id_estudio = ? OR CAST(id_estudio AS TEXT) = ?",
            arrayOf(idEstudio, idEstudio)
        )
        return rows > 0
    }

    // Evalúa los parámetros hematológicos y genera un diagnóstico médico interpretativo
    fun calcularDiagnosticoHematologico(
        sexo: String,
        rbc: Double, hb: Double, hct: Double, vcm: Double, hcm: Double, chcm: Double,
        wbc: Double, neut: Double, linf: Double, plt: Double
    ): String {
        val hallazgos = mutableListOf<String>()
        val esMujer = sexo.equals("F", ignoreCase = true)

        // 1. Serie Roja ajustada por sexo
        val hbMinRef = if (esMujer) 12.0 else 13.8
        val hbMaxRef = if (esMujer) 15.5 else 17.5
        val rbcMaxRef = if (esMujer) 5.4 else 6.1

        if (hb < hbMinRef) {
            val tipoMorfologico = when {
                vcm < 80.0 -> context.getString(R.string.diag_db_microcytic)
                vcm > 100.0 -> context.getString(R.string.diag_db_macrocytic)
                else -> context.getString(R.string.diag_db_normocytic)
            }
            val tipoCromico = when {
                hcm < 27.0 || chcm < 32.0 -> context.getString(R.string.diag_db_hypochromic)
                else -> context.getString(R.string.diag_db_normochromic)
            }
            val paramSex = if (esMujer) context.getString(R.string.diag_db_params_female) else context.getString(R.string.diag_db_params_male)
            hallazgos.add(context.getString(R.string.diag_db_anemia_format, tipoMorfologico, tipoCromico, paramSex))
        } else if (rbc > rbcMaxRef || hb > hbMaxRef) {
            hallazgos.add(context.getString(R.string.diag_db_erythrocytosis))
        }

        // 2. Serie Blanca
        if (wbc > 11.0) {
            if (neut > 70.0) {
                hallazgos.add(context.getString(R.string.diag_db_leukocytosis_neutrophilia))
            } else if (linf > 45.0) {
                hallazgos.add(context.getString(R.string.diag_db_leukocytosis_lymphocytosis))
            } else {
                hallazgos.add(context.getString(R.string.diag_db_leukocytosis_reactive))
            }
        } else if (wbc < 4.5) {
            hallazgos.add(context.getString(R.string.diag_db_leukopenia))
        }

        // 3. Serie Plaquetaria
        if (plt < 150.0) {
            hallazgos.add(context.getString(R.string.diag_db_thrombocytopenia))
        } else if (plt > 450.0) {
            hallazgos.add(context.getString(R.string.diag_db_thrombocytosis))
        }

        return if (hallazgos.isEmpty()) {
            val sexStr = if (esMujer) context.getString(R.string.gender_female) else context.getString(R.string.gender_male)
            context.getString(R.string.diag_db_normal_format, sexStr)
        } else {
            hallazgos.joinToString(". ") + "."
        }
    }

    // Genera la siguiente clave sugerida para paciente
    fun getNextPatientId()  = getNextId(TABLE_PACIENTES,  "clave_paciente", "PAC")
    // Genera la siguiente clave sugerida para alergia
    fun getNextAllergyId()  = getNextId(TABLE_ALERGIAS,   "clave_alergia",  "ALG")
    // Genera la siguiente clave sugerida para neoplasia
    fun getNextCancerId()   = getNextId(TABLE_TIPOCANCER, "clave_cancer",   "CA")

    // Calcula el siguiente identificador numérico autoincrementable con prefijo
    private fun getNextId(tableName: String, column: String, prefix: String): String {
        var maxNum = 0
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT MAX(CAST(REPLACE(REPLACE(CAST($column AS TEXT), '$prefix-', ''), '$prefix', '') AS INTEGER)) FROM $tableName",
                null
            )
            if (cursor.moveToFirst()) {
                maxNum = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error al obtener el siguiente ID para $tableName", e)
        }
        return "$prefix-${maxNum + 1}"
    }

    // Registra un análisis de archivo CSV/TXT en la tabla de historial
    fun insertHistorialArchivo(record: FileAnalysisRecord): Long {
        val values = ContentValues().apply {
            put("nombre_archivo", record.nombreArchivo)
            put("nombre_copia", record.nombreCopia)
            put("fecha_analisis", record.fechaAnalisis)
            put("folio", record.folio)
            put("rbc", record.rbc)
            put("hb", record.hb)
            put("hct", record.hct)
            put("vcm", record.vcm)
            put("hcm", record.hcm)
            put("chcm", record.chcm)
            put("wbc", record.wbc)
            put("neut", record.neut)
            put("linf", record.linf)
            put("mono", record.mono)
            put("eos", record.eos)
            put("plt", record.plt)
            put("diagnostico", record.diagnostico)
            put("flag", record.flag)
            put("paciente_vinculado", record.pacienteVinculado)
        }
        return writableDatabase.insert(TABLE_HISTORIAL_ARCHIVOS, null, values)
    }

    // Consulta la lista cronológica de análisis importados
    fun getHistorialArchivos(): List<FileAnalysisRecord> {
        val list = mutableListOf<FileAnalysisRecord>()
        readableDatabase.rawQuery("SELECT * FROM $TABLE_HISTORIAL_ARCHIVOS ORDER BY id DESC", null).use { c ->
            if (c.moveToFirst()) {
                val colCopiaIdx = c.getColumnIndex("nombre_copia")
                do {
                    list.add(
                        FileAnalysisRecord(
                            id = c.getLong(c.getColumnIndexOrThrow("id")),
                            nombreArchivo = c.getString(c.getColumnIndexOrThrow("nombre_archivo")) ?: "",
                            nombreCopia = if (colCopiaIdx != -1) (c.getString(colCopiaIdx) ?: "") else "",
                            fechaAnalisis = c.getString(c.getColumnIndexOrThrow("fecha_analisis")) ?: "",
                            folio = c.getString(c.getColumnIndexOrThrow("folio")) ?: "",
                            rbc = c.getDouble(c.getColumnIndexOrThrow("rbc")),
                            hb = c.getDouble(c.getColumnIndexOrThrow("hb")),
                            hct = c.getDouble(c.getColumnIndexOrThrow("hct")),
                            vcm = c.getDouble(c.getColumnIndexOrThrow("vcm")),
                            hcm = c.getDouble(c.getColumnIndexOrThrow("hcm")),
                            chcm = c.getDouble(c.getColumnIndexOrThrow("chcm")),
                            wbc = c.getDouble(c.getColumnIndexOrThrow("wbc")),
                            neut = c.getDouble(c.getColumnIndexOrThrow("neut")),
                            linf = c.getDouble(c.getColumnIndexOrThrow("linf")),
                            mono = c.getDouble(c.getColumnIndexOrThrow("mono")),
                            eos = c.getDouble(c.getColumnIndexOrThrow("eos")),
                            plt = c.getDouble(c.getColumnIndexOrThrow("plt")),
                            diagnostico = c.getString(c.getColumnIndexOrThrow("diagnostico")) ?: "",
                            flag = c.getString(c.getColumnIndexOrThrow("flag")) ?: "NORMAL",
                            pacienteVinculado = c.getString(c.getColumnIndexOrThrow("paciente_vinculado")) ?: ""
                        )
                    )
                } while (c.moveToNext())
            }
        }
        return list
    }

    // Elimina un registro del historial de análisis de archivos
    fun deleteHistorialArchivo(id: Long): Boolean {
        return writableDatabase.delete(TABLE_HISTORIAL_ARCHIVOS, "id = ?", arrayOf(id.toString())) > 0
    }

    // Actualiza la información del paciente vinculado en un registro del historial
    fun updateHistorialPacienteVinculado(id: Long, pacienteInfo: String): Boolean {
        val cv = ContentValues().apply {
            put("paciente_vinculado", pacienteInfo)
        }
        return writableDatabase.update(TABLE_HISTORIAL_ARCHIVOS, cv, "id = ?", arrayOf(id.toString())) > 0
    }
}

data class FileAnalysisRecord(
    val id: Long = 0,
    val nombreArchivo: String,
    val nombreCopia: String = "",
    val fechaAnalisis: String,
    val folio: String,
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
    val diagnostico: String,
    val flag: String,
    val pacienteVinculado: String = ""
) : java.io.Serializable

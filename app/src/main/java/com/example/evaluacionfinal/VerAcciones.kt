package com.example.evaluacionfinal

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VerAcciones : AppCompatActivity() {

    private lateinit var tvusuacciones: TextView
    private lateinit var tvdistancia: TextView
    private lateinit var tvestadosensor: TextView
    private lateinit var tvdetalle: TextView
    private lateinit var tvestadoesp: TextView
    private lateinit var lvacciones: ListView
    private lateinit var btnvolacc: Button

    private var usu: Usuario? = null

    private val datosRef = FirebaseDatabase.getInstance().getReference("Datos")
    private val accionesQuery: Query = FirebaseDatabase.getInstance()
        .getReference("Acciones").orderByChild("fecha").limitToLast(30)

    private val historial = ArrayList<String>()
    private lateinit var adaptador: ArrayAdapter<String>

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "CL"))

    // Último estado recibido del ESP32 (se re-evalúa cada 3 s para detectar desconexión)
    private var estadoSensor: String? = null
    private var detecciones: Long = 0
    private var ultimaDeteccionSeg: Long? = null
    private var ultimaLecturaSeg: Long? = null

    private val handler = Handler(Looper.getMainLooper())
    private val refrescoEstado = object : Runnable {
        override fun run() {
            actualizarEstado()
            handler.postDelayed(this, 3000)
        }
    }

    private val datosListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val distancia = snapshot.child("distancia").getValue(Double::class.javaObjectType)
            estadoSensor = snapshot.child("estado").getValue(String::class.java)
            detecciones = snapshot.child("detecciones").getValue(Long::class.javaObjectType) ?: 0
            ultimaDeteccionSeg = snapshot.child("ultimaDeteccion").getValue(Long::class.javaObjectType)
            ultimaLecturaSeg = snapshot.child("ultimaLectura").getValue(Long::class.javaObjectType)

            tvdistancia.text =
                if (distancia != null && estadoSensor != "apagado")
                    String.format(Locale.US, "%.1f cm", distancia)
                else "-- cm"
            actualizarEstado()
        }

        override fun onCancelled(error: DatabaseError) {
            tvestadoesp.text = "Error de lectura: ${error.message}"
        }
    }

    private val accionesListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            historial.clear()
            // limitToLast entrega de la más antigua a la más nueva: se invierte para ver la última arriba
            for (x in snapshot.children.reversed()) {
                val fecha = x.child("fecha").getValue(Long::class.javaObjectType)
                val texto = fecha?.let { formatoFecha.format(Date(it)) } ?: "--"
                val nombre = x.child("nombre").getValue(String::class.java) ?: "?"
                val accion = x.child("accion").getValue(String::class.java) ?: "?"
                historial.add("$texto\n$nombre: $accion")
            }
            if (historial.isEmpty()) historial.add("Aún no hay registros")
            adaptador.notifyDataSetChanged()
        }

        override fun onCancelled(error: DatabaseError) {
            historial.clear()
            historial.add("Error al leer el historial: ${error.message}")
            adaptador.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ver_acciones)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvusuacciones = findViewById(R.id.tvusuacciones)
        tvdistancia = findViewById(R.id.tvdistancia)
        tvestadosensor = findViewById(R.id.tvestadosensor)
        tvdetalle = findViewById(R.id.tvdetalle)
        tvestadoesp = findViewById(R.id.tvestadoesp)
        lvacciones = findViewById(R.id.lvacciones)
        btnvolacc = findViewById(R.id.btnvolacc)

        adaptador = ArrayAdapter(this, android.R.layout.simple_list_item_1, historial)
        lvacciones.adapter = adaptador

        usu = intent.getParcelableExtra("usu", Usuario::class.java)
        tvusuacciones.text = "Bienvenido Usuario : " + usu?.nombre

        volver()
        desactivarFlecha()
    } // Cierra el "onCreate"

    override fun onStart() {
        super.onStart()
        datosRef.addValueEventListener(datosListener)
        accionesQuery.addValueEventListener(accionesListener)
        handler.post(refrescoEstado)
    }

    override fun onStop() {
        datosRef.removeEventListener(datosListener)
        accionesQuery.removeEventListener(accionesListener)
        handler.removeCallbacks(refrescoEstado)
        super.onStop()
    }

    /** Decide qué mensaje mostrar sobre el funcionamiento del sensor. */
    private fun actualizarEstado() {
        val ultima = ultimaLecturaSeg
        val ahoraSeg = System.currentTimeMillis() / 1000
        val enLinea = ultima != null && (ahoraSeg - ultima) <= 20

        val (mensaje, color) = when {
            ultima == null -> Pair("Esperando datos del ESP32...", Color.GRAY)
            !enLinea -> Pair("ESP32 SIN CONEXIÓN\nNo se puede verificar el sensor", Color.rgb(200, 0, 0))
            else -> when (estadoSensor) {
                "ok" -> Pair("✔ SENSOR FUNCIONANDO CORRECTAMENTE\nSin objetos cerca", Color.rgb(0, 140, 0))
                "detectando" -> Pair("✔ SENSOR ACTUANDO CORRECTAMENTE\nObjeto detectado", Color.rgb(230, 120, 0))
                "falla" -> Pair("⚠ FALLA DEL SENSOR\nNo recibe eco, revisa el cableado", Color.RED)
                "apagado" -> Pair("Sensor apagado desde la app", Color.GRAY)
                else -> Pair("Iniciando sensor...", Color.GRAY)
            }
        }
        tvestadosensor.text = mensaje
        tvestadosensor.setTextColor(color)

        val ultimaDeteccion = ultimaDeteccionSeg?.let { formatoFecha.format(Date(it * 1000)) } ?: "--"
        tvdetalle.text = "Detecciones: $detecciones   Última: $ultimaDeteccion"
        tvestadoesp.text = if (enLinea) "ESP32: en línea" else "ESP32: sin conexión"
    }

    fun volver() {
        btnvolacc.setOnClickListener { finish() }
    } // Cierra la funcion "volver"

    fun desactivarFlecha() {
        onBackPressedDispatcher.addCallback(this) { }
    } // Cierra la funcion "desactivarFlecha"

} // Cierra la clase "VerAcciones"

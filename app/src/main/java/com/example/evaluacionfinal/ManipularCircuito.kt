package com.example.evaluacionfinal

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

class ManipularCircuito : AppCompatActivity() {

    private lateinit var tvusumanipular: TextView
    private lateinit var tvestado: TextView
    private lateinit var btnon: Button
    private lateinit var btnoff: Button
    private lateinit var btnvolman: Button

    private lateinit var activoRef: DatabaseReference

    private var usu: Usuario? = null

    // Muestra en tiempo real lo que hay en Firebase (lo que lee el ESP32)
    private val estadoListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val activo = snapshot.getValue(Boolean::class.javaObjectType)
            tvestado.text = when (activo) {
                true -> "Estado del sensor: ENCENDIDO"
                false -> "Estado del sensor: APAGADO"
                null -> "Estado del sensor: sin datos"
            }
        }

        override fun onCancelled(error: DatabaseError) {
            tvestado.text = "Estado del sensor: error de lectura"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manipular_circuito)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvusumanipular = findViewById(R.id.tvusumanipular)
        tvestado = findViewById(R.id.tvestado)
        btnon = findViewById(R.id.btnon)
        btnoff = findViewById(R.id.btnoff)
        btnvolman = findViewById(R.id.btnvolman)

        activoRef = FirebaseDatabase.getInstance().reference.child("Datos").child("activo")

        usu = intent.getParcelableExtra("usu", Usuario::class.java)
        tvusumanipular.text = "Bienvenido Usuario :" + usu?.nombre

        btnon.setOnClickListener { cambiarEstado(true) }
        btnoff.setOnClickListener { cambiarEstado(false) }

        volver()
        desactivarFlecha()
    } // Cierra el "onCreate"

    override fun onStart() {
        super.onStart()
        activoRef.addValueEventListener(estadoListener)
    }

    override fun onStop() {
        activoRef.removeEventListener(estadoListener)
        super.onStop()
    }

    private fun cambiarEstado(encender: Boolean) {
        activoRef.setValue(encender)
            .addOnSuccessListener {
                registrarAccion(if (encender) "Sensor encendido" else "Sensor apagado")
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo enviar el comando", Toast.LENGTH_SHORT).show()
            }
    }

    /** Guarda quién hizo qué y cuándo (la hora la pone el servidor de Firebase). */
    private fun registrarAccion(accion: String) {
        val registro = mapOf(
            "rut" to (usu?.rut ?: ""),
            "nombre" to (usu?.nombre ?: ""),
            "accion" to accion,
            "fecha" to ServerValue.TIMESTAMP
        )
        FirebaseDatabase.getInstance().getReference("Acciones").push().setValue(registro)
    }

    fun volver() {
        btnvolman.setOnClickListener { finish() }
    } // Cierra la funcion "volver"

    fun desactivarFlecha() {
        onBackPressedDispatcher.addCallback(this) { }
    } // Cierra "desactivarFlecha"

} // Cierra la clase "ManipularCircuito"

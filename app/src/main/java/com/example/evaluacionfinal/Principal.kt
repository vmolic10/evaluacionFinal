package com.example.evaluacionfinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Principal : AppCompatActivity() {

    private lateinit var txtrut: EditText   // ahora recibe el CORREO (mismo id para no tocar el resto)
    private lateinit var txtcon: EditText
    private lateinit var btnini: Button
    private lateinit var btncre: Button

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_principal)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        txtrut = findViewById(R.id.txtrut)
        txtcon = findViewById(R.id.txtcon)
        btnini = findViewById(R.id.btnini)
        btncre = findViewById(R.id.btncre)

        botonLogin()
        desactivarFlecha()

        btncre.setOnClickListener {
            startActivity(Intent(this@Principal, CrearCuenta::class.java))
        }
    } // Cierra el "onCreate"

    fun botonLogin() {
        btnini.setOnClickListener {
            val correo = txtrut.text.toString().trim()
            val con = txtcon.text.toString().trim()

            if (correo.isEmpty() || con.isEmpty()) {
                Toast.makeText(this@Principal, "Debe ingresar Correo y Contraseña!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnini.isEnabled = false

            // Firebase Authentication valida las credenciales (la contraseña nunca queda en nuestra base de datos)
            auth.signInWithEmailAndPassword(correo, con)
                .addOnSuccessListener { resultado ->
                    val uid = resultado.user?.uid
                    if (uid == null) {
                        btnini.isEnabled = true
                        return@addOnSuccessListener
                    }

                    // El perfil (rut, nombre, apellido) se guarda bajo el uid del usuario
                    FirebaseDatabase.getInstance().getReference("Usuario").child(uid).get()
                        .addOnSuccessListener { perfil ->
                            btnini.isEnabled = true
                            val usu = Usuario(
                                perfil.child("rut").getValue(String::class.java) ?: "",
                                perfil.child("nombre").getValue(String::class.java) ?: "",
                                perfil.child("apellido").getValue(String::class.java) ?: "",
                                perfil.child("correo").getValue(String::class.java) ?: correo,
                                "" // la contraseña no se guarda ni se pasa entre pantallas
                            )
                            val intent = Intent(this@Principal, Menu::class.java)
                            intent.putExtra("usu", usu)
                            startActivity(intent)
                        }
                        .addOnFailureListener {
                            btnini.isEnabled = true
                            Toast.makeText(this@Principal, "No se pudo leer el perfil del usuario", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    btnini.isEnabled = true
                    txtcon.setText("")
                    Toast.makeText(this@Principal, "Correo o Contraseña incorrectos!", Toast.LENGTH_SHORT).show()
                }
        }
    } // Cierra el "botonLogin"

    fun desactivarFlecha() {
        onBackPressedDispatcher.addCallback(this) { }
    }
} // Cierra la clase Principal

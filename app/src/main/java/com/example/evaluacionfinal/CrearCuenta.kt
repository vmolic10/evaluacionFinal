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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase

class CrearCuenta : AppCompatActivity() {

    private lateinit var tvrutcrear: EditText
    private lateinit var tvnomcrear: EditText
    private lateinit var tvcorrcrear: EditText
    private lateinit var tvapecrear: EditText
    private lateinit var tvconcrear: EditText
    private lateinit var btnreg: Button

    private lateinit var btnvol: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crear_cuenta)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Asignar variables a los componentes del XML
        tvrutcrear = findViewById(R.id.tvrutcrear)
        tvnomcrear = findViewById(R.id.tvnomcrear)
        tvcorrcrear = findViewById(R.id.tvcorrcrear)
        tvapecrear = findViewById(R.id.tvapecrear)
        tvconcrear = findViewById(R.id.tvconcrear)
        btnreg = findViewById(R.id.btnreg)
        btnvol = findViewById(R.id.btnvol)

        btnreg.setOnClickListener { registrarUsuario() }
        btnvol.setOnClickListener { volverInicio() }

        desactivarFlecha()
    }

    private fun registrarUsuario() {
        val rut = normalizarRut(tvrutcrear.text.toString())
        val nom = tvnomcrear.text.toString().trim()
        val cor = tvcorrcrear.text.toString().trim()
        val ape = tvapecrear.text.toString().trim()
        val con = tvconcrear.text.toString().trim()

        if (rut.isEmpty() || nom.isEmpty() || cor.isEmpty() || ape.isEmpty() || con.isEmpty()) {
            Toast.makeText(this, "Debe completar todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cor).matches()) {
            Toast.makeText(this, "El correo no es válido", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase Authentication exige un mínimo de 6 caracteres
        if (con.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        btnreg.isEnabled = false
        val auth = FirebaseAuth.getInstance()

        // 1) Firebase Authentication crea la cuenta (guarda la contraseña cifrada, no en nuestra base de datos)
        auth.createUserWithEmailAndPassword(cor, con)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user?.uid
                if (uid == null) {
                    btnreg.isEnabled = true
                    return@addOnSuccessListener
                }

                // 2) El perfil se guarda bajo el uid, SIN contraseña
                val perfil = mapOf(
                    "rut" to rut,
                    "nombre" to nom,
                    "apellido" to ape,
                    "correo" to cor
                )
                FirebaseDatabase.getInstance().getReference("Usuario").child(uid).setValue(perfil)
                    .addOnSuccessListener {
                        btnreg.isEnabled = true
                        auth.signOut() // que inicie sesión desde la pantalla de login
                        Toast.makeText(this, "Cuenta creada. Ya puedes iniciar sesión", Toast.LENGTH_SHORT).show()
                        limpiarCampos()
                    }
                    .addOnFailureListener {
                        btnreg.isEnabled = true
                        Toast.makeText(this, "La cuenta se creó, pero no se pudo guardar el perfil", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { error ->
                btnreg.isEnabled = true
                val mensaje = if (error is FirebaseAuthUserCollisionException)
                    "Ya existe una cuenta con ese correo"
                else
                    "No se pudo crear la cuenta: ${error.localizedMessage}"
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
    }

    private fun limpiarCampos() {
        tvrutcrear.setText("")
        tvnomcrear.setText("")
        tvcorrcrear.setText("")
        tvapecrear.setText("")
        tvconcrear.setText("")
    }

    fun volverInicio() {
        val intent = Intent(this, Principal::class.java)
        startActivity(intent)
        finish()
    }

     fun desactivarFlecha() {
        onBackPressedDispatcher.addCallback(this) { }
    }
}

package com.example.evaluacionfinal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class Menu : AppCompatActivity() {

    private lateinit var tvusumenu: TextView
    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var btn3: Button
    private lateinit var btn4: Button

    private var usu: Usuario? = null






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvusumenu = findViewById(R.id.tvusumenu)
        btn1 = findViewById(R.id.btn1)
        btn2 = findViewById(R.id.btn2)
        btn3 = findViewById(R.id.btn3)
        btn4 = findViewById(R.id.btn4)

        usu = intent.getParcelableExtra("usu", Usuario::class.java)

        tvusumenu.setText("Bienvenido Usuario :" + usu?.nombre)

        Log.i("RUT", "${usu?.rut}")
        Log.i("NOMBRE", "${usu?.nombre}")
        Log.i("CORREO", "${usu?.correo}")
        Log.i("APELLIDO", "${usu?.apellido}")


        boton1()
        boton2()
        boton3()
        boton4()
        desactivarFlecha()

    }// Cierra el "onCreate"

    fun boton1(){
        btn1.setOnClickListener {
            var intent = Intent(this@Menu, ManipularCircuito::class.java)
            intent.putExtra("usu", usu)
            startActivity(intent)
        }



    } // Cierra la funcion boton1






    fun boton2(){
        btn2.setOnClickListener {
            var intent = Intent(this@Menu, VerAcciones::class.java)
            intent.putExtra("usu", usu)
            startActivity(intent)
        }

    } // Cierra la funcion boton2





    fun boton3(){
        btn3.setOnClickListener {
            var intent = Intent(this@Menu, VerPerfil::class.java)
            intent.putExtra("usu", usu)
            startActivity(intent)
        }
    } // Cierra la funcion boton3






    fun boton4(){
        btn4.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // cierra la sesión en Firebase
            var intent = Intent(this@Menu, Principal::class.java)
            startActivity(intent)
            finish()
        }
    } // Cierra la funcion boton4




    fun desactivarFlecha(){
        onBackPressedDispatcher.addCallback(this@Menu){
        }
    } // Cierra la funcion "desactivarFlecha".






} // Cierre la clase "Menu".
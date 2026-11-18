package com.example.evaluacionfinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.w3c.dom.Text

class VerPerfil : AppCompatActivity() {

    private lateinit var tvusuper: TextView

    private lateinit var tvusuperfil: TextView
    private lateinit var tvrutper: TextView
    private lateinit var tvnomper: TextView
    private lateinit var tvapeper: TextView
    private lateinit var tvcorper: TextView
    private lateinit var tvconper: TextView

    private lateinit var btnvolper: Button

    private var usu: Usuario? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ver_perfil)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        tvusuper = findViewById(R.id.tvusuper)
        tvusuperfil = findViewById(R.id.tvusuperfil)
        tvrutper = findViewById(R.id.tvrutper)
        tvnomper = findViewById(R.id.tvnomper)
        tvapeper = findViewById(R.id.tvapeper)
        tvcorper = findViewById(R.id.tvcorper)
        tvconper = findViewById(R.id.tvconper)
        btnvolper = findViewById(R.id.btnvolper)



        usu = intent.getParcelableExtra("usu", Usuario::class.java)

        tvusuper.setText("Bienvenido Usuario :" + usu?.nombre)

        tvrutper.setText(usu?.rut.toString())
        tvnomper.setText(usu?.nombre?.lowercase())
        tvapeper.setText(usu?.apellido?.lowercase())
        tvcorper.setText(usu?.correo?.lowercase())
        tvconper.setText("********")

        volver()
        desactivarFlecha()




    } // Cierra el "onCreate".

    fun volver() {
        btnvolper.setOnClickListener {
            finish() // vuelve al Menu que ya está abajo en la pila

        }

    } // Cierra la funcion "volver"

    fun desactivarFlecha(){
        onBackPressedDispatcher.addCallback(this){
        }
    } // Cierra la funcion "desactivarFlecha".



} // Cierra la clase "Perfil".
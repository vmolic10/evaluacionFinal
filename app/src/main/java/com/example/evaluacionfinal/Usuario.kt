package com.example.evaluacionfinal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Usuario(

    var rut: String,
    var nombre: String,
    var apellido: String,
    var correo: String,
    var contraseña: String

) : Parcelable

/**
 * Firebase no admite '.', '#', '$', '[', ']' ni '/' en las claves.
 * Un RUT escrito como "12.345.678-k" haría que la app se cerrara al guardarlo,
 * por eso se normaliza siempre igual (sin puntos ni espacios y en mayúscula).
 */
fun normalizarRut(texto: String): String =
    texto.trim().replace(".", "").replace(" ", "").uppercase()

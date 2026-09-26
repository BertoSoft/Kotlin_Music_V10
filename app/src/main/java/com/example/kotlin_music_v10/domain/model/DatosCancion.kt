package com.example.kotlin_music_v10.domain.model

import android.net.Uri

data class DatosCancion(
    val id: Int,
    val titulo: String,
    val duracion: Int,
    val ruta: String
)

package com.example.kotlin_music_v10.domain.model

import android.net.Uri

data class DatosCancion(
    val id: Long,
    val nombre: String,
    val duracion: Long,
    val ruta: String
)

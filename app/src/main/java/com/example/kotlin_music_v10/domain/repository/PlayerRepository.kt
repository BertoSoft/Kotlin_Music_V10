package com.example.kotlin_music_v10.domain.repository

import com.example.kotlin_music_v10.domain.model.DatosCancion

interface PlayerRepository {

    suspend fun getAllCanciones(): List<DatosCancion>;
}
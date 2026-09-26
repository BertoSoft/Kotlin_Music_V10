package com.example.kotlin_music_v10.domain.usecase

import com.example.kotlin_music_v10.domain.model.DatosCancion
import com.example.kotlin_music_v10.domain.repository.PlayerRepository

class PlayerUseCase(private  val repository: PlayerRepository) {

    suspend fun getAllCancionesUseCase(): List<DatosCancion>{
        return repository.getAllCanciones();
    }
}
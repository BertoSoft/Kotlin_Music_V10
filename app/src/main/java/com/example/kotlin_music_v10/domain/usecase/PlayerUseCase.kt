package com.example.kotlin_music_v10.domain.usecase

import com.example.kotlin_music_v10.domain.model.DatosCancion
import com.example.kotlin_music_v10.domain.repository.HomeRepository
import javax.inject.Inject

class PlayerUseCase @Inject constructor(
    private  val repository: HomeRepository
) {

    suspend fun getAllCancionesUseCase(): List<DatosCancion>?{
        return repository.getAllCanciones();
    }
}
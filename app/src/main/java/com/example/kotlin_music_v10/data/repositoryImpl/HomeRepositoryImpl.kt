package com.example.kotlin_music_v10.data.repositoryImpl

import com.example.kotlin_music_v10.data.source.HomeDataSource
import com.example.kotlin_music_v10.domain.model.DatosCancion
import com.example.kotlin_music_v10.domain.repository.HomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val dataSource: HomeDataSource
): HomeRepository {
    override suspend fun getAllCanciones(): List<DatosCancion>? {
        return withContext(Dispatchers.IO) {
            try {
                return@withContext dataSource.getAllCanciones()
            }
            catch (e: Exception){
                null
            }
        }
    }
}
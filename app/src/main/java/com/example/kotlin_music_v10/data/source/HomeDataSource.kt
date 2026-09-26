package com.example.kotlin_music_v10.data.source

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.kotlin_music_v10.domain.model.DatosCancion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class HomeDataSource @Inject constructor(
    @ApplicationContext private val contextoApp: Context
){

    suspend fun getAllCanciones(): List<DatosCancion>?{
        val listaCanciones = mutableListOf<DatosCancion>()

        // 1. Apuntamos a la base de datos de audio externa de Android
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        // 2.- Campos para leer de las canciones
        val camposCancion = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,   // 👈 CORRECCIÓN: Cambiado AUTHOR por ARTIST
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
            )

        // 3. Traemos toda la música indexada para evitar bloqueos del sistema
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val selectionArgs = null

        // 4.- Ordenamos por el titulo
        val orden = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            contextoApp.contentResolver.query(
                uri,
                camposCancion,
                selection,
                selectionArgs,
                orden
            )?.use{ cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val tituloCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistaCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val duracionCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                // 5. Iteramos por cada fila encontrada en las carpetas
                while (cursor.moveToNext()){
                    val id = cursor.getLong(idCol)
                    val titulo = cursor.getString(tituloCol) ?: "Título Desconocido"
                    val artista = cursor.getString(artistaCol) ?: "Artista Desconocido"
                    val duracion = cursor.getLong(duracionCol) ?: 0L
                    val rutaFisica = cursor.getString(dataCol) ?: ""

                    // Mapeamos al datos de dominio
                    listaCanciones.add(DatosCancion(
                        id = id,
                        titulo = titulo,
                        artista = artista,
                        duracion = duracion,
                        ruta = rutaFisica
                    )
                    )
                }
            }
            return listaCanciones
        }
        catch (e: Exception){
            return null
        }

    };
}
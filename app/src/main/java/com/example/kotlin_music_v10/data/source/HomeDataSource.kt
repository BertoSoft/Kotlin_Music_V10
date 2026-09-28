package com.example.kotlin_music_v10.data.source

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.kotlin_music_v10.domain.model.DatosCancion
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.processor.internal.definecomponent.codegen._dagger_hilt_android_internal_builders_ServiceComponentBuilder
import javax.inject.Inject

class HomeDataSource @Inject constructor(
    @ApplicationContext private val contextoApp: Context
){

    suspend fun getAllCanciones(): List<DatosCancion>?{
        val listaCanciones = mutableListOf<DatosCancion>()
        var id = 0;

        // 1.- Apuntamos a todos los archivos del almacenamiento externo
        val uri = MediaStore.Files.getContentUri("external")

        // 2.- Campos que necesitamos
        val campos = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.SIZE
        )

        // 2.5 Definimos nuestros límites de filtrado
        // Ejemplo: Más de 30 segundos (30,000 ms) y más de 1 MB (1,048,576 bytes)
        val duracionMinimaMs = 30000
        val tamanoMinimoBytes = 1024 * 1024

        // 3. Modificamos el filtro (selection)
        // Agrupamos las rutas con paréntesis para que el operador AND afecte a ambas carpetas por igual
        val seleccion = "(" +
                "${MediaStore.Files.FileColumns.DATA} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE ?" +
                ") AND ${MediaStore.Files.FileColumns.DURATION} >= ? " +
                "AND ${MediaStore.Files.FileColumns.SIZE} >= ?"

        // 4. Pasamos los argumentos en el mismo orden que los signos de interrogación '?'
        val seleccionArgumentos = arrayOf(
            "%/Download/%.mp3",
            "%/Music/%.mp3",
            duracionMinimaMs.toString(),
            tamanoMinimoBytes.toString()
        )

        // 4. Ordenamos alfabéticamente por el nombre del archivo
        val orden = "${MediaStore.Files.FileColumns.DISPLAY_NAME} ASC"

        try {
            contextoApp.contentResolver.query(
                uri,
                campos,
                seleccion,
                seleccionArgumentos,
                orden
            )?.use{ cursor ->
                val nombreCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val rutaCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val duracionCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)

                while (cursor.moveToNext()){
                    val nombreSufijo = cursor.getString(nombreCol)
                    val ruta = cursor.getString(rutaCol)
                    val duracion = cursor.getLong(duracionCol)

                    val nombre = nombreSufijo.substringBeforeLast(".")
                    // Aqui iria duracion em minitos:segundos


                    android.util.Log.d("MUSICA_TEST", "Encontrado en MediaStore: $nombre en ruta: $ruta")

                    listaCanciones.add(DatosCancion(
                        id = id,
                        nombre = nombre,
                        duracion = duracion,
                        ruta = ruta
                    ))
                    id++;
                }
            }
            return listaCanciones
        }
        catch (e: Exception){
            return null
        }
    }
}
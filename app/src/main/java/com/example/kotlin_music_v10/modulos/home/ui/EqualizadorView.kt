package com.example.kotlin_music_v10.modulos.home.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

class EqualizadorView @JvmOverloads constructor(
    miContexto: Context,
    atributo: AttributeSet? = null,
    defEstiloAtributo: Int = 0
) : View(miContexto, atributo, defEstiloAtributo) {

    // Numero de barras
    private val numeroBarras = 32

    // pincel
    private val pincel: Paint = Paint().apply {
        color = Color.parseColor("#1DB954") // Color verde estilo moderno / Spotify
        style = Paint.Style.FILL
        isAntiAlias = true // Suaviza los bordes para evitar dientes de sierra
    }

    // ALtura de las barras entre 0.0f y 1.0f
    private val alturaBarras = FloatArray(numeroBarras);

    // Radio para redondear las esquinas superiores
    private val radioBarras = 12.0f

    //2.- funcion que recibe los datos
    fun actualizarEspectro(datosFFT: ByteArray?) {
        if (datosFFT == null || datosFFT.isEmpty()) return

        val binesPorBarra: Int = datosFFT.size / numeroBarras
        for (i in 0 until numeroBarras) {
            val indice = (i * binesPorBarra)
            if (indice < datosFFT.size) {
                val valorAbs = abs(datosFFT[indice].toInt())
                alturaBarras[i] = (valorAbs % 100) / 100f
            }
        }
        // Redibujamos el view
        invalidate()
    }

    // 3.- Renderizado de lienzo
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (height == 0 || width == 0) return

        val ancho = width.toFloat()
        val alto = height.toFloat()
        val espacioEntreBarras = 8.0f

        // Ancho de las barras
        val anchoBarra = (ancho - (espacioEntreBarras * (numeroBarras - 1))) / numeroBarras

        // Dibujamos las barras secuencialmente de Izda a Decha
        for (i in 0 until numeroBarras) {
            val izda = i * (anchoBarra + espacioEntreBarras)
            val derecha = izda + anchoBarra
            val altura = alturaBarras[i] * alto

            // Garantizamos una altura mínima (ej. 10 píxeles) para que el ecualizador
            // no desaparezca del todo si la música se queda en silencio temporalmente
            val arriba = (alto - altura).coerceAtMost(alto - 10f)
            val abajo = alto

            // Dibujamos rectángulos con bordes superiores redondeados (drawRoundRect)
            canvas.drawRoundRect(
                izda,
                arriba,
                derecha,
                abajo,
                radioBarras, // Radio horizontal del borde
                radioBarras, // Radio vertical del borde
                pincel
            )
        }
    }
}
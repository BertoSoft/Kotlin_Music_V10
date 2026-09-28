package com.example.kotlin_music_v10.modulos.home.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow

class EqualizadorView @JvmOverloads constructor(
    miContexto: Context,
    atributo: AttributeSet? = null,
    defEstiloAtributo: Int = 0
) : View(miContexto, atributo, defEstiloAtributo) {

    private val numeroBarras = 32
    private val pincel: Paint = Paint().apply {
        color = Color.parseColor("#1DB954") // Verde Spotify
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val alturaBarras = FloatArray(numeroBarras)
    private val radioBarras = 12.0f

    // Calibración elástica idónea para una tasa de refresco de 30 FPS
    // Hace que la barra suba instantáneamente con el golpe de música, pero baje con una caída tersa y armónica
    private val factorCaidaSuave = 0.22f

    fun actualizarEspectro(datosFFT: ByteArray?) {
        if (datosFFT == null || datosFFT.isEmpty()) return

        // 1. Extraemos la mitad física del búfer FFT
        val binesMitadFisica = datosFFT.size / 2

        // CALIBRACIÓN DE DETECCIÓN: Reducimos ligeramente el recorte (a un 85%)
        // para asegurarnos de capturar todo el rango dinámico real del audio digital
        val binesMusicalesUtiles = (binesMitadFisica * 0.85f).toInt().coerceAtLeast(numeroBarras)

        val magnitudesDb = FloatArray(binesMusicalesUtiles)

        for (i in 0 until binesMusicalesUtiles) {
            val baseIndex = i * 2
            if (baseIndex + 1 < datosFFT.size) {
                val r = datosFFT[baseIndex].toFloat()
                val j = datosFFT[baseIndex + 1].toFloat()

                val amplitud = kotlin.math.hypot(r, j)

                if (amplitud > 1.0f) {
                    magnitudesDb[i] = 10f * kotlin.math.log10(amplitud)
                } else {
                    magnitudesDb[i] = 0f
                }
            }
        }

        // 2. REPARACIÓN CRÍTICA: Distribución más lineal y compacta (Aplanamos la curva exponencial)
        for (i in 0 until numeroBarras) {
            val fraccionProgresiva = i.toFloat() / numeroBarras

            // Bajamos la potencia a 1.4f para que los bines de frecuencias medias-altas
            // se desplacen hacia la derecha, obligando a TODAS las barras a bailar
            val indexInicio = (fraccionProgresiva.pow(1.4f) * binesMusicalesUtiles).toInt().coerceIn(0, binesMusicalesUtiles - 1)
            val indexFin = (((i + 1).toFloat() / numeroBarras).pow(1.4f) * binesMusicalesUtiles).toInt().coerceIn(indexInicio + 1, binesMusicalesUtiles)

            // Buscamos el impacto máximo de esa frecuencia (Peak Detection) para los golpes limpios
            var magnifiedMax = 0f
            for (bin in indexInicio until indexFin) {
                if (magnitudesDb[bin] > magnifiedMax) {
                    magnifiedMax = magnitudesDb[bin]
                }
            }

            // 3. Ajustamos tu ecualización por tramos favorita con más ganancia progresiva
            val gananciaProgresivaCompensada = when {
                i < 8 -> 0.60f        // Graves controlados
                i in 8..21 -> 0.75f    // Medios subidos para dar más presencia
                else -> {              // Agudos (Aumentamos la base para forzar el movimiento a la derecha)
                    val factorAgudo = (i - 22).toFloat() / (numeroBarras - 1 - 22)
                    0.80f + (2.70f * factorAgudo)
                }
            }

            val dbEcualizados = magnifiedMax * gananciaProgresivaCompensada

            // Reducimos los umbrales mínimos de descarte para que las barras de la derecha sean hiper-sensibles
            val dbMinimosDescarte = 2f
            val dbMaximosTecho = 26f

            val dbLimpios = (dbEcualizados - dbMinimosDescarte).coerceAtLeast(0f)
            val alturaNormalizada = (dbLimpios / (dbMaximosTecho - dbMinimosDescarte)).coerceIn(0f, 1f)

            // 4. Respuesta de impacto inmediato y caída tersa
            if (alturaNormalizada >= alturaBarras[i]) {
                alturaBarras[i] = alturaNormalizada
            } else {
                alturaBarras[i] -= (alturaBarras[i] - alturaNormalizada) * 0.18f
            }
        }

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
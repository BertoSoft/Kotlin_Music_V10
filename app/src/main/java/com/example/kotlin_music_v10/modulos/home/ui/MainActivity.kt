package com.example.kotlin_music_v10.modulos.home.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlin_music_v10.databinding.ActivityMainBinding
import com.example.kotlin_music_v10.modulos.home.ui.adapter.HomeAdapter
import com.example.kotlin_music_v10.modulos.home.ui.extensions.toMinSeg
import com.example.kotlin_music_v10.modulos.home.viewmodel.EstadoPlayer
import com.example.kotlin_music_v10.modulos.home.viewmodel.HomeUiEstado
import com.example.kotlin_music_v10.modulos.home.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()
    lateinit var binding: ActivityMainBinding
    private  val miAdaptador by lazy { HomeAdapter() }
    private var exoPlayer: ExoPlayer? = null
    private var jobProgreso: kotlinx.coroutines.Job? = null
    private var visualizadorEQ: android.media.audiofx.Visualizer? = null
    private var rutaCancionActual: String? = null
    private var usuarioTocaBarraDesplazamiento = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUi()
    }

    private fun initUi() {
        verificarYPedirPermisos()
        initRv()
        initPlayer()
        initListeners()
        initObservers()
    }

    private fun initRv() {
        with(binding.rvCanciones){
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = miAdaptador
            setHasFixedSize(true)
        }

        miAdaptador.onCancionClick = { cancion ->
                viewModel.itemClick(cancion)
            }

    }

    // Inicia el exoPlayer
    @OptIn(UnstableApi::class)
    private fun initPlayer() {
        // Definimos que el audio es estrictamente para reproducción de música (Media)
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(this).build().apply {
            volume = 1.0f
            setAudioAttributes(audioAttributes, true)
            // Listener para detectar cuándo termina la canción
            addListener(object: androidx.media3.common.Player.Listener{
                override fun onPlaybackStateChanged(playbackState: Int){
                    if(playbackState == androidx.media3.common.Player.STATE_ENDED){
                        viewModel.cancionTerminada();
                    }
                }
            })

            // 2. SOLUCIÓN COMPATIBLE: Añadimos el listener de analíticas exclusivo para el canal de audio hardware
            addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
                override fun onAudioSessionIdChanged(
                    eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                    audioSessionId: Int
                ) {
                    // Ignoramos la constante de validación estática y vinculamos si es un ID real de sonido (mayor a 0)
                    if (audioSessionId > 0) {
                        dibujarVistaEq(audioSessionId)
                    }
                }


            })
        }
    }

    private var ultimoTimestampRenderizado = 0L

    private fun dibujarVistaEq(audioSessionId: Int) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            visualizadorEQ?.release()

            visualizadorEQ = android.media.audiofx.Visualizer(audioSessionId).apply {
                captureSize = 512

                setDataCaptureListener(object :
                    android.media.audiofx.Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: android.media.audiofx.Visualizer?, waveform: ByteArray?, samplingRate: Int) {}

                    override fun onFftDataCapture(v: android.media.audiofx.Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        val tiempoActual = System.currentTimeMillis()

                        // OPTIMIZACIÓN CRÍTICA: Captura sincronizada a 30 FPS estables (cada 33ms)
                        // Esto elimina por completo el desfase y el comportamiento desacompasado
                        if (tiempoActual - ultimoTimestampRenderizado >= 33) {
                            ultimoTimestampRenderizado = tiempoActual

                            if (fft != null && exoPlayer?.isPlaying == true) {
                                runOnUiThread {
                                    binding.spectrumVisualizer.actualizarEspectro(fft)
                                }
                            }
                        }
                    }
                }, android.media.audiofx.Visualizer.getMaxCaptureRate() / 2, false, true) // Reducimos el muestreo nativo a la mitad

                enabled = true
            }
        }
        catch (e: Exception){
            android.util.Log.e("FFT_ERROR", "No se pudo encender el visualizador físico: ${e.message}")
        }
    }

    private fun initListeners() {
        with(binding){
            btnPlay.setOnClickListener {
                viewModel.btnPlayClick()
            }

            seekBarProgress.setOnSeekBarChangeListener(object: android.widget.SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(
                    p0: SeekBar?,
                    p1: Int,        // Progress
                    p2: Boolean     //fromUser
                ) {
                    if(p2){
                        txtProgreso.text = p1.toLong().toMinSeg()
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                    usuarioTocaBarraDesplazamiento = true

                    // Si esta sonando pausamos
                    if(exoPlayer?.isPlaying == true){
                        viewModel.btnPlayClick()
                    }
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    usuarioTocaBarraDesplazamiento = false

                    val nuevoProgreso = p0?.progress?.toLong()

                    if(nuevoProgreso != null){
                        exoPlayer?.seekTo(nuevoProgreso)

                        // 2. Si el reproductor estaba pausado o cambió de estado, aseguramos que continúe en Play
                        if (viewModel.estado.value.estadoPlayer != EstadoPlayer.Play) {
                            viewModel.btnPlayClick()
                        }
                    }
                }
            })

            btnAtras.setOnClickListener {
                viewModel.btnAtrasClick()
            }

            btnAdelante.setOnClickListener {
                viewModel.btnAdelanteClick()
            }
        }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.estado.collect { estado ->
                    gestionarPlayer(estado);
                    dibujaUi(estado)
                }
            }
        }
    }

    private fun dibujaUi(estado: HomeUiEstado) {
        with(binding){

            // Nombre Cancion
            if(estado.cancionActual != null){
                txtCancion.text = estado.cancionActual.nombre
                txtCancion.tag = estado.cancionActual.ruta
                txtDuracion.text = estado.cancionActual.duracion.toMinSeg()
                seekBarProgress.max = estado.cancionActual.duracion.toInt()
            }

            //RecyclerView
            // OPTIMIZACIÓN 3: Evitar asignaciones redundantes al adaptador si la lista no cambia
            if (miAdaptador.currentList != estado.listaCanciones) {
                miAdaptador.submitList(estado.listaCanciones)
            }

            //Boton Play
            if(estado.estadoPlayer == EstadoPlayer.Play){
                btnPlay.setImageResource(android.R.drawable.ic_media_pause)
            }
            else if(estado.estadoPlayer == EstadoPlayer.Pause ||
                estado.estadoPlayer == EstadoPlayer.Stop){
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
            }
        }
    }

    // Gestiona el comportamiento de exoPlayer segun el estado
    fun gestionarPlayer(estado: HomeUiEstado) {
        val cancionEstado = estado.cancionActual ?: return

        // OPTIMIZACIÓN 1 CORREGIDA: Control por código nativo, blindado contra recargas repetitivas
        if (rutaCancionActual != cancionEstado.ruta) {
            // Solo entramos aquí si de verdad es una canción NUEVA
            rutaCancionActual = cancionEstado.ruta
            exoPlayer?.stop()
            val mediaItem = MediaItem.Builder()
                .setMediaId(cancionEstado.ruta) // Usamos la ruta como ID único en el reproductor
                .setUri(cancionEstado.ruta)
                .build()

            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
        }

        val estaPlayExoPlayer = exoPlayer?.isPlaying ?: false

        when(estado.estadoPlayer){
            EstadoPlayer.Play -> {
                if(!estaPlayExoPlayer) exoPlayer?.play()
                iniciarJobProgreso()
            }
            EstadoPlayer.Pause ->{
                if(estaPlayExoPlayer) exoPlayer?.pause()
                detenerJobProgreso()
            }
            EstadoPlayer.Stop -> {
                exoPlayer?.stop()
                exoPlayer?.clearMediaItems()
                rutaCancionActual = null // Reseteamos la ruta de control
                detenerJobProgreso()
            }
        }
    }

    // Usa corroutina fuera de main para actualizar el progreso en tiempo real
    private fun iniciarJobProgreso(){
        // Si ya existe un job activo corriendo, no creamos otro para evitar duplicados
        if (jobProgreso?.isActive == true) return

        jobProgreso = lifecycleScope.launch(Dispatchers.Default) {
            while (true){
                // Consultamos de forma segura en el hilo principal
                val (posActual, estaReproduciendo, estadoPlayer) = withContext(Dispatchers.Main){
                    Triple(
                        exoPlayer?.currentPosition ?: 0L,
                        exoPlayer?.isPlaying == true,
                        viewModel.estado.value.estadoPlayer
                    )
                }

                // SI EL USUARIO DIO PAUSA O STOP MANUAL: Aquí sí destruimos la corrutina de forma segura
                if (estadoPlayer != EstadoPlayer.Play) {
                    break
                }

                // SI ESTÁ CAMBIANDO DE CANCIÓN (Temporeramente no suena):
                // No matamos el bucle, solo esperamos a la siguiente vuelta usando 'continue'
                if (!estaReproduciendo) {
                    delay(100.milliseconds)
                    continue
                }

                // Si el usuario no está arrastrando el dedo, actualizamos la interfaz de forma fluida
                if (!usuarioTocaBarraDesplazamiento) {
                    withContext(Dispatchers.Main){
                        binding.txtProgreso.text = posActual.toMinSeg()
                        binding.seekBarProgress.progress = posActual.toInt()
                    }
                }

                delay(250.milliseconds)
            }
        }
    }

    // Detiene la coroutina de progreso en tiempo real
    private fun detenerJobProgreso(){
        jobProgreso?.cancel()
        jobProgreso = null
    }

    // Cambiamos el contrato para pedir el permiso de audio físico de forma explícita
    private val solicitarPermisosAudio = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            // Si el usuario lo acepta con la música corriendo, reiniciamos el player para enganchar el hardware
            exoPlayer?.audioSessionId?.let { id -> if(id > 0) dibujarVistaEq(id) }
        }
    }

    private fun verificarYPedirPermisos() {
        // PERMISO CRÍTICO PARA EL RECOLECTOR FFT: RECORD_AUDIO
        val permisoVisualizador = Manifest.permission.RECORD_AUDIO
        if (checkSelfPermission(permisoVisualizador) != PackageManager.PERMISSION_GRANTED) {
            solicitarPermisosAudio.launch(permisoVisualizador)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        detenerJobProgreso()
        exoPlayer?.release()
        exoPlayer = null
    }
}
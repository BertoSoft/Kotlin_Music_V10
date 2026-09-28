package com.example.kotlin_music_v10.modulos.home.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlin_music_v10.databinding.ActivityMainBinding
import com.example.kotlin_music_v10.modulos.home.ui.adapter.HomeAdapter
import com.example.kotlin_music_v10.modulos.home.ui.extensions.toMinSeg
import com.example.kotlin_music_v10.modulos.home.viewmodel.EstadoPlayer
import com.example.kotlin_music_v10.modulos.home.viewmodel.HomeUiEstado
import com.example.kotlin_music_v10.modulos.home.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()
    lateinit var binding: ActivityMainBinding
    private  val miAdaptador by lazy { HomeAdapter() }
    private var exoPlayer: ExoPlayer? = null
    private var jobProgreso: kotlinx.coroutines.Job? = null

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
                    // Si esta sonando pausamos
                    if(exoPlayer?.isPlaying == true){
                        viewModel.btnPlayClick()
                    }
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    val nuevoProgreso = p0?.progress?.toLong()

                    if(nuevoProgreso != null){
                        exoPlayer?.seekTo(nuevoProgreso)
                        viewModel.refrescaProgreso(nuevoProgreso)
                        viewModel.btnPlayClick()
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
            }

            //RecyclerView
            miAdaptador.submitList(estado.listaCanciones)

            //Boton Play
            if(estado.estadoPlayer == EstadoPlayer.Play){
                btnPlay.setImageResource(android.R.drawable.ic_media_pause)
            }
            else if(estado.estadoPlayer == EstadoPlayer.Pause ||
                estado.estadoPlayer == EstadoPlayer.Stop){
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
            }

            // Barra desplazamiento
            if(estado.cancionActual != null){
                seekBarProgress.max = estado.cancionActual.duracion.toInt()
                seekBarProgress.progress = estado.progreso.toInt()

                txtProgreso.text = estado.progreso.toMinSeg()
                txtDuracion.text = estado.cancionActual.duracion.toMinSeg()
            }
        }
    }

    fun gestionarPlayer(estado: HomeUiEstado) {
        val cancionEstado = estado.cancionActual ?: return
        val rutaTxtCancionActual = binding.txtCancion.tag as? String

        if(rutaTxtCancionActual != estado.cancionActual.ruta){
            // Solo entramos aquí si de verdad es una canción NUEVA
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
                if(jobProgreso == null || jobProgreso?.isActive == false){
                    iniciarJobProgreso()
                }
            }
            EstadoPlayer.Pause ->{
                if(estaPlayExoPlayer) exoPlayer?.pause()
                detenerJobProgreso()
            }
            EstadoPlayer.Stop -> {
                exoPlayer?.stop()
                exoPlayer?.clearMediaItems()
                detenerJobProgreso()
            }
        }
    }

    private fun iniciarJobProgreso(){
        // Si  ya existe un job lo anulamos
        jobProgreso?.cancel()

        jobProgreso = lifecycleScope.launch {
            while (true){
                val posActual = exoPlayer?.currentPosition ?: 0L
                viewModel.refrescaProgreso(posActual)

                // Espera un quinto de segundo antes de volver a preguntar
                kotlinx.coroutines.delay(200.milliseconds)
            }
        }
    }

    private fun detenerJobProgreso(){
        jobProgreso?.cancel()
        jobProgreso = null
    }

    private val solicitarPermisosAudio = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            // Llama a tu viewModel para que cargue la música
            // viewModel.obtenerCanciones()
        } else {
            // Maneja el caso de permiso denegado si lo deseas
        }
    }

    private fun verificarYPedirPermisos() {
        val permisoAudio = Manifest.permission.READ_MEDIA_AUDIO

        if (checkSelfPermission(permisoAudio) == PackageManager.PERMISSION_GRANTED) {
            // Ya tienes acceso, puedes ordenar la carga de música de inmediato
            // viewModel.obtenerCanciones()
        } else {
            // Lanza directamente la solicitud sin verificar versiones de Android
            solicitarPermisosAudio.launch(permisoAudio)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}
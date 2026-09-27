package com.example.kotlin_music_v10.modulos.home.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlin_music_v10.databinding.ActivityMainBinding
import com.example.kotlin_music_v10.domain.model.DatosCancion
import com.example.kotlin_music_v10.modulos.home.ui.adapter.HomeAdapter
import com.example.kotlin_music_v10.modulos.home.viewmodel.EstadoPlayer
import com.example.kotlin_music_v10.modulos.home.viewmodel.HomeUiEstado
import com.example.kotlin_music_v10.modulos.home.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()
    lateinit var binding: ActivityMainBinding
    private  val miAdaptador by lazy { HomeAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUi()
    }

    private fun initUi() {
        verificarYPedirPermisos()
        initRv()
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

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.estado.collect { estado ->
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
        }
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
}
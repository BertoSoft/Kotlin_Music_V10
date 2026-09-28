package com.example.kotlin_music_v10.modulos.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_music_v10.domain.model.DatosCancion
import com.example.kotlin_music_v10.domain.usecase.PlayerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EstadoPlayer {Stop, Play, Pause}

data class HomeUiEstado(
    val listaCanciones: List<DatosCancion>? = null,
    val cancionActual: DatosCancion? = null,
    val proximaCancion: DatosCancion? = null,
    val estadoPlayer: EstadoPlayer = EstadoPlayer.Stop,
    val isCargando: Boolean = false,
    val msgError: String? = null
) {
    // Necesario para ByteArray de datosFFT
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HomeUiEstado

        if (listaCanciones != other.listaCanciones) return false
        if (cancionActual != other.cancionActual) return false
        if (proximaCancion != other.proximaCancion) return false
        if (estadoPlayer != other.estadoPlayer) return false
        // Se agregaron los nuevos campos al equals
        if (isCargando != other.isCargando) return false
        if (msgError != other.msgError) return false

        return true
    }
    override fun hashCode(): Int {
        var result = listaCanciones?.hashCode() ?: 0
        result = 31 * result + (cancionActual?.hashCode() ?: 0)
        result = 31 * result + (proximaCancion?.hashCode() ?: 0)
        result = 31 * result + estadoPlayer.hashCode()
        // Se agregaron los nuevos campos al hashCode
        result = 31 * result + isCargando.hashCode()
        result = 31 * result + (msgError?.hashCode() ?: 0)
        return result
    }
}

@HiltViewModel
class HomeViewModel@Inject constructor(
    private val playerUseCase: PlayerUseCase
): ViewModel() {

    private val _estado = MutableStateFlow<HomeUiEstado>(HomeUiEstado())
    val estado: StateFlow<HomeUiEstado> = _estado.asStateFlow()

    init{
        getListaCanciones();
    }

    fun itemClick(cancion: DatosCancion){
        _estado.update { estado ->
            estado.copy(
                cancionActual = cancion,
                estadoPlayer = EstadoPlayer.Play
            )
        }
    }

    fun cancionTerminada(){
        val tamanoLista = _estado.value.listaCanciones?.count() ?: 0
        if(tamanoLista > 1){
            btnAdelanteClick()
        }
        else{
            _estado.update { estado ->
                estado.copy(
                    estadoPlayer = EstadoPlayer.Stop
                )
            }
        }

    }

    fun btnPlayClick(){
        _estado.update { estado ->
            if(estado.cancionActual == null) return@update estado

            val nuevoEstadoPlayer = if(estado.estadoPlayer == EstadoPlayer.Play){
                EstadoPlayer.Pause
            }
            else{
                EstadoPlayer.Play
            }
            estado.copy(estadoPlayer = nuevoEstadoPlayer)
        }
    }

    fun btnAdelanteClick(){
        val tamanoLista = _estado.value.listaCanciones?.count() ?: 0
        var idActual = _estado.value.cancionActual?.id ?: -1

        if(idActual < 0 || _estado.value.listaCanciones == null) return

        idActual++
        if(idActual < tamanoLista){
            // saltamos una cancion
            _estado.update { estado ->
                estado.copy(
                    cancionActual = getCancionFromId(idActual),
                    estadoPlayer = EstadoPlayer.Play

                )
            }
        }
        else{
            // volvemos a la primera
            _estado.update { estado ->
                estado.copy(
                    cancionActual = getCancionFromId(0),
                    estadoPlayer = EstadoPlayer.Play
                )
            }
        }
    }

    fun btnAtrasClick(){
        val tamanoLista = _estado.value.listaCanciones?.count() ?: 0
        var idActual = _estado.value.cancionActual?.id ?: -1

        if(idActual < 0 || _estado.value.listaCanciones == null) return

        idActual--
        if(idActual < 0){
            // saltamos a la ultima
            val ultima = _estado.value.listaCanciones?.count() ?: -1
            _estado.update { estado ->
                estado.copy(
                    cancionActual = getCancionFromId(ultima - 1),
                    estadoPlayer = EstadoPlayer.Play
                )
            }
        }
        else{
            // saltamos una atras
            _estado.update { estado ->
                estado.copy(
                    cancionActual = getCancionFromId(idActual),
                    estadoPlayer = EstadoPlayer.Play
                )
            }
        }
    }

    fun getCancionFromId(id: Int): DatosCancion?{
        return _estado.value.listaCanciones?.find { it.id == id }
    }

    // Funciones con curoutinas
    fun getListaCanciones(){
        viewModelScope.launch {
           _estado.update { estado ->
               estado.copy(isCargando = true)
           }
            try {
                val lista = playerUseCase.getAllCancionesUseCase()
                if(!lista.isNullOrEmpty()){
                    _estado.update { estado ->
                        estado.copy(
                            isCargando = false,
                            listaCanciones = lista,
                            msgError = null
                        )
                    }
                }
                else{
                    _estado.update { estado ->
                        estado.copy(
                            isCargando = false,
                            msgError = "Lista de Canciones Vacía."
                        )
                    }
                }
            }
            catch (e: Exception){
                _estado.update { estado ->
                    estado.copy(
                        isCargando = false,
                        msgError = "Error: ${e.message}"
                    )
                }
            }
        }
    }
}
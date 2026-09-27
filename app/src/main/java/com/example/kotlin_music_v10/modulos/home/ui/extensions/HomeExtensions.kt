package com.example.kotlin_music_v10.modulos.home.ui.extensions

import java.util.Locale

fun Long.toMinSeg(): String{
    if(this <= 0L) return "00:00"

    val segTotales = this/1000

    val minutos = segTotales / 60
    val segundos = segTotales % 60

    return String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos)
}
package com.example.kotlin_music_v10.modulos.home.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlin_music_v10.databinding.ItemMainBinding
import com.example.kotlin_music_v10.domain.model.DatosCancion
import com.example.kotlin_music_v10.modulos.home.ui.extensions.toMinSeg

class HomeAdapter(
    var onCancionClick: ((DatosCancion) -> Unit)? = null // Variable accesible desde la mainActivity
): ListAdapter<DatosCancion, HomeAdapter.HomeViewHolder>(diffCallBack) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HomeViewHolder {
        val binding = ItemMainBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HomeViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: HomeViewHolder,
        position: Int
    ) {
        val item = getItem(position)
        holder.render(item)
    }

    inner class HomeViewHolder(
        private val binding : ItemMainBinding
    ): RecyclerView.ViewHolder(binding.root){

        fun render(cancion: DatosCancion){

            binding.txtCancion.text = cancion.nombre
            val txt = cancion.duracion.toMinSeg()
            binding.txtDuracion.text = cancion.duracion.toMinSeg()

            binding.imgPlay.setOnClickListener {
                onCancionClick?.invoke(cancion)
            }
        }
    }

    companion object diffCallBack: DiffUtil.ItemCallback<DatosCancion>(){
        override fun areItemsTheSame(
            oldItem: DatosCancion,
            newItem: DatosCancion
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: DatosCancion,
            newItem: DatosCancion
        ): Boolean {
            return oldItem == newItem
        }

    }
}
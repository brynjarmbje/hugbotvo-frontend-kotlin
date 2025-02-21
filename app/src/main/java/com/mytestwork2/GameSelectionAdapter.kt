package com.mytestwork2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mytestwork2.models.GameOption

class GameSelectionAdapter(
    private val gameOptions: List<GameOption>,
    private val onItemClicked: (GameOption) -> Unit
) : RecyclerView.Adapter<GameSelectionAdapter.GameOptionViewHolder>() {

    inner class GameOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gameName: TextView = itemView.findViewById(R.id.gameOptionName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameOptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_option, parent, false)
        return GameOptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameOptionViewHolder, position: Int) {
        val option = gameOptions[position]
        holder.gameName.text = option.name
        holder.itemView.setOnClickListener { onItemClicked(option) }
    }

    override fun getItemCount(): Int = gameOptions.size
}

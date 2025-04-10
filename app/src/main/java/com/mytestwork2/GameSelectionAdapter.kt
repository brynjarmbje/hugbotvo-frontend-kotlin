package com.mytestwork2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mytestwork2.models.GameOption

class GameSelectionAdapter(
    private val gameOptions: List<GameOption>,
    private val onItemClicked: (GameOption) -> Unit
) : RecyclerView.Adapter<GameSelectionAdapter.GameOptionViewHolder>() {

    inner class GameOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gameTitle: TextView = itemView.findViewById(R.id.gameTitle)
        val gameBackground: ImageView = itemView.findViewById(R.id.gameBackground)
        val gameCard: MaterialCardView = itemView.findViewById(R.id.gameCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameOptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_selection, parent, false)
        return GameOptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameOptionViewHolder, position: Int) {
        val option = gameOptions[position]
        val context = holder.itemView.context

        // If game id is less than 4, show points; otherwise, just show the game name.
        if (option.id < 4) {
            holder.gameTitle.text = "${option.name} - ${option.points} stig"
        } else {
            holder.gameTitle.text = option.name
        }

        // For the Shake game (id == 4), adjust visual style based on enabled state.
        if (option.id == 4 && !option.enabled) {
            // Indicate that Shake is locked—for example, append a note and gray out the text.
            holder.gameTitle.text = "${option.name} (Lok lok og lás)"
            holder.gameTitle.setTextColor(ContextCompat.getColor(context, R.color.gray))
        } else {
            // Otherwise, normal text color (using white or any other color defined)
            holder.gameTitle.setTextColor(ContextCompat.getColor(context, R.color.onPrimary))
        }

        // Set the gradient background based on game id.
        val gradientRes = when (option.id) {
            1 -> R.drawable.gradient_letters
            2 -> R.drawable.gradient_numbers
            3 -> R.drawable.gradient_locate
            4 -> R.drawable.gradient_shake
            else -> R.drawable.gradient_letters
        }
        holder.gameBackground.setImageResource(gradientRes)

        // Click animation (scale in/out).
        holder.gameCard.setOnClickListener {
            holder.gameCard.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    holder.gameCard.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    // If the option is disabled, show a toast.
                    if (option.id == 4 && !option.enabled) {
                        Toast.makeText(context, "Náðu 50 stigum fyrst!", Toast.LENGTH_SHORT).show()
                    } else {
                        onItemClicked(option)
                    }
                }
                .start()
        }
    }

    override fun getItemCount(): Int = gameOptions.size
}
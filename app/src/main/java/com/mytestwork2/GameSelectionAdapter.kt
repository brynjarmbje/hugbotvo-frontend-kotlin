package com.mytestwork2

import android.content.Context
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
        val gameNameTextView: TextView = itemView.findViewById(R.id.gameNameTextView)
        val gamePointsTextView: TextView = itemView.findViewById(R.id.gamePointsTextView)
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

        // For games with id less than 4, set game name and points.
        if (option.id < 4) {
            holder.gameNameTextView.text = option.name
            holder.gamePointsTextView.text = "${option.points} stig"
        } else {
            // For the Shake game (id == 4)
            holder.gameNameTextView.text = option.name
            if (!option.enabled) {
                // If the Shake game is locked, adjust the visuals.
                holder.gameNameTextView.text = "${option.name}"
                holder.gameNameTextView.setTextColor(ContextCompat.getColor(context, R.color.gray))
                holder.gamePointsTextView.text = "Lok lok og lás"  // e.g., display "Locked" or any message
                holder.gamePointsTextView.setTextColor(ContextCompat.getColor(context, R.color.gray))
            } else {
                // If unlocked, use the default color and display the local high score.
                holder.gameNameTextView.setTextColor(ContextCompat.getColor(context, R.color.onPrimary))
                holder.gamePointsTextView.setTextColor(ContextCompat.getColor(context, R.color.onPrimary))
                val prefs = context.getSharedPreferences("shake_game_prefs", Context.MODE_PRIVATE)
                val highScore = prefs.getFloat("met", 0f)
                holder.gamePointsTextView.text = "Met: ${"%.2f".format(highScore)} sekúndur"
            }
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
            holder.gameCard.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                .withEndAction {
                    holder.gameCard.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    // If the option is disabled, show a toast.
                    if (option.id == 4 && !option.enabled) {
                        Toast.makeText(context, "Náðu 20 stigum fyrst!", Toast.LENGTH_SHORT).show()
                    } else {
                        onItemClicked(option)
                    }
                }.start()
        }
    }

    override fun getItemCount(): Int = gameOptions.size
}
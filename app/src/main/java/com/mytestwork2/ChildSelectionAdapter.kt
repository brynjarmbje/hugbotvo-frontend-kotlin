package com.mytestwork2

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mytestwork2.models.Child

class ChildSelectionAdapter(
    private val children: List<Child>,
    private val selectedChildren: Set<Long>,
    private val onItemClick: (Long) -> Unit
) : RecyclerView.Adapter<ChildSelectionAdapter.ChildViewHolder>() {

    inner class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val childName: TextView = itemView.findViewById(R.id.childName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_child_selection, parent, false)
        return ChildViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        val child = children[position]
        holder.childName.text = child.name

        val isSelected = selectedChildren.contains(child.id)
        Log.d("ChildSelectionAdapter", "Child id: ${child.id} selected: $isSelected")

        // Cast the root view to MaterialCardView.
        val cardView = holder.itemView as com.google.android.material.card.MaterialCardView
        val context = holder.itemView.context
        if (isSelected) {
            // Set selected background color or stroke.
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.selected_child_background))
            cardView.strokeWidth = 4 // For example, 4px stroke for selected state.
            cardView.strokeColor = ContextCompat.getColor(context, R.color.selected_child_background)
        } else {
            // Reset to default background.
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.default_child_background))
            cardView.strokeWidth = 0
        }

        holder.itemView.setOnClickListener { child.id?.let { onItemClick(it) } }
    }

    override fun getItemCount(): Int = children.size
}

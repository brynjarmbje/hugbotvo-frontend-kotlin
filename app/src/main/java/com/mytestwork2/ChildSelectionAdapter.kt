package com.mytestwork2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        // Change background color if selected
        if (selectedChildren.contains(child.id)) {
            holder.itemView.setBackgroundResource(android.R.color.holo_blue_light)
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent)
        }
        holder.itemView.setOnClickListener { child.id?.let { it1 -> onItemClick(it1) } }
    }

    override fun getItemCount(): Int = children.size
}

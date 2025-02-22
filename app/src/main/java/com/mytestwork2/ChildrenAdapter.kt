package com.mytestwork2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mytestwork2.models.Child

class ChildrenAdapter(
    private val children: List<Child>,
    private val onItemClick: (Child) -> Unit
) : RecyclerView.Adapter<ChildrenAdapter.ChildViewHolder>() {

    inner class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val childNameText: TextView = itemView.findViewById(R.id.childNameText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_child, parent, false)
        return ChildViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        val child = children[position]
        holder.childNameText.text = child.name
        holder.itemView.setOnClickListener { onItemClick(child) }
    }

    override fun getItemCount(): Int = children.size
}

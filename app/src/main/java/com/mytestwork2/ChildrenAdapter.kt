package com.mytestwork2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mytestwork2.models.Child

class ChildrenAdapter(
    private val children: List<Child>,
    private val onClick: (Child) -> Unit
) : RecyclerView.Adapter<ChildrenAdapter.ChildViewHolder>() {

    inner class ChildViewHolder(val button: MaterialButton) : RecyclerView.ViewHolder(button)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_child, parent, false) as MaterialButton
        return ChildViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        val child = children[position]
        holder.button.text = child.name
        holder.button.setOnClickListener { onClick(child) }
    }

    override fun getItemCount(): Int = children.size
}


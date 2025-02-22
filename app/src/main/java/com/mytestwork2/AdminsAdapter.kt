package com.mytestwork2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mytestwork2.models.Admin

class AdminsAdapter(
    private val admins: List<Admin>,
    private val onItemClick: (Admin) -> Unit
) : RecyclerView.Adapter<AdminsAdapter.AdminViewHolder>() {

    inner class AdminViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val adminNameText: TextView = itemView.findViewById(R.id.adminNameText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin, parent, false)
        return AdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        val admin = admins[position]
        holder.adminNameText.text = admin.username
        holder.itemView.setOnClickListener { onItemClick(admin) }
    }

    override fun getItemCount(): Int = admins.size
}

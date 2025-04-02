package com.mytestwork2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mytestwork2.models.SessionSummary
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class SessionAdapter(private val sessions: List<SessionSummary>) :
    RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    inner class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textPoints: TextView = itemView.findViewById(R.id.textPoints)
        val textCorrectAnswers: TextView = itemView.findViewById(R.id.textCorrectAnswers)
        val textAccuracy: TextView = itemView.findViewById(R.id.textAccuracy)
        val textStartTime: TextView = itemView.findViewById(R.id.textStartTime)
        val textEndTime: TextView = itemView.findViewById(R.id.textEndTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.session_item, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        // Assuming you have a helper function to get game name
        holder.textPoints.text = "Stig: ${session.points}"
        holder.textCorrectAnswers.text = "Rétt svör: ${session.correctAnswers}"
        holder.textAccuracy.text = "Nákvæmni: ${session.accuracy.toInt()}%"
        holder.textStartTime.text = "Upphaf: ${formatDateTime(session.startTime)}"
        holder.textEndTime.text = "Lok: ${formatDateTime(session.endTime) ?: "Í gangi"}"
    }

    private fun formatDateTime(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return "N/A"
        return try {
            val zonedDateTime = ZonedDateTime.parse(isoString)
            // Choose your desired pattern. For example: "dd.MM.yyyy HH:mm" or "yyyy-MM-dd HH:mm"
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
            zonedDateTime.format(formatter)
        } catch (e: Exception) {
            // Fallback if parsing fails
            isoString
        }
    }


    override fun getItemCount() = sessions.size
}

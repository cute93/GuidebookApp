package com.example.guidebook.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.guidebook.R
import com.example.guidebook.models.UserNote

class UserPanelAdapter(
    private val panels: List<PanelItem>,
    private val onWriteClick: (PanelItem) -> Unit
) : RecyclerView.Adapter<UserPanelAdapter.PanelViewHolder>() {

    data class PanelItem(
        val userId: String,
        val userName: String,
        val role: String,
        val note: UserNote?
    )

    inner class PanelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPanelName)
        val ivNote: ImageView = view.findViewById(R.id.ivNotePreview)
        val btnWrite: Button = view.findViewById(R.id.btnWrite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_panel, parent, false)
        return PanelViewHolder(view)
    }

    override fun onBindViewHolder(holder: PanelViewHolder, position: Int) {
        val item = panels[position]
        val label = if (item.role == "teacher") "Teacher's thoughts" else "student thoughts"
        holder.tvName.text = "${label}\n${item.userName}"

        if (item.note?.noteImageUrl?.isNotEmpty() == true) {
            Glide.with(holder.ivNote)
                .load(item.note.noteImageUrl)
                .into(holder.ivNote)
        } else {
            holder.ivNote.setImageResource(android.R.color.darker_gray)
        }
        holder.btnWrite.setOnClickListener { onWriteClick(item) }
    }

    override fun getItemCount() = panels.size
}

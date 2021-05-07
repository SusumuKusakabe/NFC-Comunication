package com.uphyca.nfcchat.shared

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.io.Serializable
import java.util.UUID

data class ChatData(
    val id: String = UUID.randomUUID().toString(),
    val isMe: Boolean,
    val text: String
) : Serializable

class ChatAdapter : ListAdapter<ChatData, ViewHolder>(itemCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val textView: TextView = view.findViewById(android.R.id.text1)

    fun bind(chatData: ChatData) {
        textView.text = (if (chatData.isMe) "自分: " else "相手: ") + chatData.text
    }

    companion object {
        fun create(parent: ViewGroup): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ViewHolder(inflater.inflate(android.R.layout.simple_list_item_1, parent, false))
        }
    }
}

private val itemCallback = object : DiffUtil.ItemCallback<ChatData>() {
    override fun areItemsTheSame(oldItem: ChatData, newItem: ChatData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChatData, newItem: ChatData): Boolean {
        return oldItem == newItem
    }
}

package com.htanh.appvk.ui.learning

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.htanh.appvk.R

data class LearningListItem(
    val id: String,
    val title: String,
    val description: String,
    val metadata: String,
    val isNavigable: Boolean
)

class LearningListAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<LearningListItem, LearningListAdapter.LearningViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LearningViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_learning, parent, false)
        return LearningViewHolder(view)
    }

    override fun onBindViewHolder(holder: LearningViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener {
            if (item.isNavigable) onItemClick(item.id)
        }
    }

    class LearningViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvLearningItemTitle)
        private val description: TextView = itemView.findViewById(R.id.tvLearningItemDescription)
        private val metadata: TextView = itemView.findViewById(R.id.tvLearningItemMetadata)

        fun bind(item: LearningListItem) {
            title.text = item.title
            description.text = item.description
            description.visibility = if (item.description.isBlank()) View.GONE else View.VISIBLE
            metadata.text = item.metadata
            itemView.isClickable = item.isNavigable
            itemView.isFocusable = item.isNavigable
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LearningListItem>() {
            override fun areItemsTheSame(oldItem: LearningListItem, newItem: LearningListItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: LearningListItem, newItem: LearningListItem) =
                oldItem == newItem
        }
    }
}

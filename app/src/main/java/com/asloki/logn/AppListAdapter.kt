package com.asloki.logn

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(private val context: Context) : ListAdapter<AppInfo, AppListAdapter.ViewHolder>(AppDiffCallback()) {

    private val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = getItem(position)
        holder.bind(app)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        private val appName: TextView = itemView.findViewById(R.id.appName)
        private val appCheckbox: CheckBox = itemView.findViewById(R.id.appCheckbox)

        fun bind(app: AppInfo) {
            appName.text = app.name
            appIcon.setImageDrawable(context.packageManager.getApplicationIcon(app.packageName))

            // Remove the listener before setting the checked state
            appCheckbox.setOnCheckedChangeListener(null)
            appCheckbox.isChecked = app.isSelected

            appCheckbox.setOnCheckedChangeListener { _, isChecked ->
                app.isSelected = isChecked
                sharedPreferences.edit().putBoolean(app.packageName, isChecked).apply()
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                    notifyItemChanged(position)
                }
            }
        }
    }
}

class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
    override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
        return oldItem.packageName == newItem.packageName
    }

    override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
        return oldItem == newItem
    }
}
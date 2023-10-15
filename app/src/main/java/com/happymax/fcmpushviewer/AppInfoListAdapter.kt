package com.happymax.fcmpushviewer

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppInfoListAdapter(val list: List<AppInfo>) :
    RecyclerView.Adapter<AppInfoListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val appIconImageView: ImageView = view.findViewById(R.id.AppIconImageView)
        val appNameTextView: TextView = view.findViewById(R.id.AppNameTextView)
        val packageNameTextView: TextView = view.findViewById(R.id.PackageNameTextView)
        val AndroidIcon:ImageView = view.findViewById(R.id.AndroidIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.applist_item, parent, false)
        val viewHolder = ViewHolder(view)
        viewHolder.itemView.setOnClickListener {
            try {
                val position = viewHolder.adapterPosition
                val packageName = list[position].packageName
                val intent = Intent()
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.setData(Uri.parse("package:" + packageName))
                view.context.startActivity(intent)
            }catch (e:Exception){
                e.printStackTrace()
                throw e
            }
        }

        return  viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appInfo = list[position]
        holder.appNameTextView.text = appInfo.appName
        holder.packageNameTextView.text = appInfo.packageName
        holder.appIconImageView.setImageDrawable(appInfo.icon)
        if(appInfo.systemApp)
            holder.AndroidIcon.visibility = View.VISIBLE
        else
            holder.AndroidIcon.visibility = View.INVISIBLE
    }

    override fun getItemCount(): Int {
        return list.size
    }
}
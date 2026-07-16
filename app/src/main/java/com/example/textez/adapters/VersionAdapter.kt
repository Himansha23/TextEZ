package com.example.textez.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.textez.R
import com.example.textez.models.Version
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VersionAdapter(
    private val versions: List<Version>,
    private val onVersionClick:
        (Version) -> Unit
) : RecyclerView.Adapter<
        VersionAdapter.VersionViewHolder
        >() {

    class VersionViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val txtVersionNumber: TextView =
            itemView.findViewById(
                R.id.txtVersionNumber
            )

        val txtVersionName: TextView =
            itemView.findViewById(
                R.id.txtVersionName
            )

        val txtVersionDate: TextView =
            itemView.findViewById(
                R.id.txtVersionDate
            )

        val txtVersionType: TextView =
            itemView.findViewById(
                R.id.txtVersionType
            )
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VersionViewHolder {

        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.item_version,
                    parent,
                    false
                )

        return VersionViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: VersionViewHolder,
        position: Int
    ) {
        val version =
            versions[position]

        val context =
            holder.itemView.context

        holder.txtVersionNumber.text =
            context.getString(
                R.string.version_number_format,
                version.versionNumber
            )

        holder.txtVersionName.text =
            version.versionName

        holder.txtVersionType.text =
            if (version.isBaseVersion) {
                context.getString(
                    R.string.base_version
                )
            } else {
                context.getString(
                    R.string.delta_version
                )
            }

        val formatter =
            SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
            )

        holder.txtVersionDate.text =
            formatter.format(
                Date(version.createdAt)
            )

        holder.itemView
            .setOnClickListener {

                onVersionClick(version)
            }
    }

    override fun getItemCount(): Int {
        return versions.size
    }
}
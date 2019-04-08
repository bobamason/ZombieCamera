package net.masonapps.zombiecamera.settings

import android.content.ContentResolver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_asset.view.*
import net.masonapps.zombiecamera.R
import net.masonapps.zombiecamera.TextureAsset

class AssetAdapter<T : TextureAsset>(private val list: List<T>, private val listener: ((T) -> Unit)?) :
    RecyclerView.Adapter<AssetAdapter.ViewHolder>() {

    var selectedItemPosition = 0
    set(value) {
        notifyItemChanged(field)
        notifyItemChanged(value)
        field = value
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_asset,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        
        holder.itemView.isActivated = position == selectedItemPosition
        
        holder.itemView.setOnClickListener {
            listener?.invoke(item)
            selectedItemPosition = holder.adapterPosition
        }

        Glide.with(holder.previewImageView)
            .load("${ContentResolver.SCHEME_FILE}:///android_asset/${item.assetPath}")
            .into(holder.previewImageView)

        holder.previewImageView.contentDescription = item.assetPath
    }
    
    fun setSelectedItem(item: T){
        val i = list.indexOf(item)
        if(i != -1) selectedItemPosition = i
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val previewImageView: ImageView = itemView.previewImageView
    }
}
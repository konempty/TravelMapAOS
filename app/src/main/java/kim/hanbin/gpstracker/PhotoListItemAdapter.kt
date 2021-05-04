package kim.hanbin.gpstracker

import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileNotFoundException

class PhotoListItemAdapter(
    val list: List<BaseData>,
    val context: Context,
    val isActivity: Boolean,
    val listener: (ViewHolder) -> Unit
) :
    RecyclerView.Adapter<PhotoListItemAdapter.ViewHolder>() {

    class ViewHolder internal constructor(
        itemView: View,
        listener: (ViewHolder) -> Unit
    ) :
        RecyclerView.ViewHolder(itemView) {
        var img: ImageView = itemView.findViewById(R.id.image)
        lateinit var item: BaseData

        init {
            itemView.setOnClickListener { listener(this) }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context: Context = parent.context
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val view: View = inflater.inflate(R.layout.photo_list_item, parent, false)

        return ViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val it = list[position]

        holder.item = it
        if (holder.item.isVideo == true) {
            holder.itemView.findViewById<ImageView>(R.id.playBtn).visibility = View.VISIBLE
        } else {
            holder.itemView.findViewById<ImageView>(R.id.playBtn).visibility = View.GONE
        }
        try {

            if (it.bitmap == null)
                it.bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        it.uri, Size(640, 640), null
                    )
                } else {
                    MediaStore.Images.Thumbnails.getThumbnail(
                        context.contentResolver, it.uri.lastPathSegment!!.toLong(),
                        MediaStore.Images.Thumbnails.MINI_KIND, null
                    )
                }
            holder.img.setImageBitmap(it.bitmap)
        } catch (e: FileNotFoundException) {

            CoroutineScope(Dispatchers.IO).launch {
                if (it is EventData) {

                    val db = InnerDB.getInstance(context)
                    db.delete(it.id!!)
                } else if (it is PhotoData) {
                    val db = InnerDB.getPhotoInstance(context)
                    db.delete(it.id!!)
                }

            }
        } catch (e: Exception) {
        }
    }

    override fun getItemCount(): Int {
        if (list.isEmpty() && isActivity)
            (context as Activity).finish()
        return list.size
    }

}
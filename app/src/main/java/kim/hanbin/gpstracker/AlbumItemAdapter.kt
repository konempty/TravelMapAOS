package kim.hanbin.gpstracker

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.FileNotFoundException

class AlbumItemAdapter(
    val map: MutableMap<String, MutableList<BaseData>>, val context: Context,
    val listener: (ViewHolder) -> Unit
) :
    RecyclerView.Adapter<AlbumItemAdapter.ViewHolder>() {


    class ViewHolder internal constructor(
        itemView: View,
        val listener: (ViewHolder) -> Unit
    ) :
        RecyclerView.ViewHolder(itemView) {
        var title: TextView = itemView.findViewById(R.id.title)
        var img: ImageView = itemView.findViewById(R.id.image)
        var path: String = ""

        init {
            itemView.setOnClickListener {
                listener(this)

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context: Context = parent.context
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val view: View = inflater.inflate(R.layout.album_item, parent, false)


        return ViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var i = 0

        for (entry in map) {
            if (i == position) {
                val splts = entry.key.split("/")

                setView(holder, splts[splts.size - 2], entry.value.size)

                if (entry.value.isEmpty()) {
                    map.remove(entry.key)
                    return
                }
                entry.value.sortByDescending { (it as PhotoData).modifyTime }
                val it = entry.value.first()


                holder.itemView.findViewById<ImageView>(R.id.playBtn).visibility =
                    if (it.isVideo == true) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                try {

                    if (it.bitmap == null) {
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
                    }
                    holder.img.setImageBitmap(it.bitmap)
                } catch (e: FileNotFoundException) {

                    CoroutineScope(Dispatchers.IO).launch {

                        val db = InnerDB.getInstance(context)
                        db.delete((it as PhotoData).id!!)
                        entry.value.remove(it)
                        if (entry.value.size == 0)
                            PhotoService.imageListMap.remove(entry.key)
                        MainScope().launch {
                            delay(100)
                            notifyDataSetChanged()
                        }


                    }
                } catch (e: Exception) {

                }
                holder.path = entry.key
                return
            }
            i++
        }

    }

    override fun getItemCount(): Int {
        return map.size
    }

    fun setView(holder: ViewHolder, name: String, count: Int) {
        val countText = "($count)"
        val text = "$name $countText"

        val builder = SpannableStringBuilder(text)


        val idx = text.indexOf(countText)
        builder.setSpan(
            ForegroundColorSpan(Color.parseColor("#9b9b9b")),
            idx,
            idx + countText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        holder.title.text = builder
    }
}
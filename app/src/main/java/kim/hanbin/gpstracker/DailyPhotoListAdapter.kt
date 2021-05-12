package kim.hanbin.gpstracker

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class DailyPhotoListAdapter(val parent: ViewGroup, var map: Map<Date, List<BaseData>>) :
    BaseAdapter() {
    private val listViewItemList: ArrayList<List<BaseData>> = ArrayList()
    val sdf = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
    var keys = map.keys.sortedDescending()
    val context: Context = parent.context
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater


    override fun getCount(): Int {
        return keys.size
    }

    // position에 위치한 데이터를 화면에 출력하는데 사용될 View를 리턴. : 필수 구현
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val convertView: View = getConvertView(position, convertView)

        /*recyclerView.adapter = PhotoListItemAdapter(listViewItem, context, false) {
            val item = it.item
            PhotoListActivity.photoList =
                PhotoService.imageList.sortedByDescending { (it as PhotoData).addedTime }
                    .toMutableList()
            context.startActivity(
                Intent(context, PhotoActivity::class.java).putExtra("id", (item as PhotoData).id)
                    .putExtra("isFromTracking", false)
            )
        }*/

        return convertView
    }

    // 지정한 위치(position)에 있는 데이터와 관계된 아이템(row)의 ID를 리턴. : 필수 구현
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // 지정한 위치(position)에 있는 데이터 리턴 : 필수 구현
    override fun getItem(position: Int): Any {
        return listViewItemList[position]
    }

    fun getConvertView(position: Int, view: View?): View {
        val k = keys[position]

        var convertView = view
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.daily_photo_list_item, parent, false)


        }
        val listViewItem = listViewItemList[position]


        // 화면에 표시될 View(Layout이 inflate된)으로부터 위젯에 대한 참조 획득
        val datetv = convertView!!.findViewById<TextView>(R.id.date)
        val constraintLayout =
            convertView.findViewById<ConstraintLayout>(R.id.constraintLayout)
        constraintLayout.removeAllViews()
        /* for (view in array) {
        view.visibility = View.GONE
    }*/
        datetv.text = sdf.format(k)

        var count = 0
        var prevView: View = constraintLayout
        for (item in listViewItem) {
            val imageView = ImageView(context)
            imageView.id = ViewCompat.generateViewId()
            imageView.setOnClickListener {

                PhotoListActivity.photoList =
                    PhotoService.imageList.sortedByDescending { (it as PhotoData).modifyTime }
                        .toMutableList()
                context.startActivity(
                    Intent(context, PhotoActivity::class.java).putExtra(
                        "id",
                        (item as PhotoData).id
                    )
                        .putExtra("isFromTracking", false)
                )
            }
            imageView.visibility = View.VISIBLE
            imageView.setImageDrawable(null)
            /*
       app:layout_constraintDimensionRatio="H3,1"
       app:layout_constraintLeft_toLeftOf="parent"
       app:layout_constraintTop_toTopOf="parent"
       app:layout_constraintWidth_percent="0.3"*/
            val layoutParams = ConstraintLayout.LayoutParams(0, 0)
            imageView.layoutParams = layoutParams
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams.dimensionRatio = "H,1:1"
            layoutParams.matchConstraintPercentWidth = 0.32F
            if (count == 0) {
                layoutParams.topToTop = prevView.id
                layoutParams.leftToLeft = prevView.id
            } else {
                val prevParam = prevView.layoutParams as ConstraintLayout.LayoutParams
                when (count % 3) {
                    0 -> {
                        layoutParams.topMargin = 10
                        layoutParams.topToBottom = prevView.id
                        layoutParams.leftToLeft = constraintLayout.id
                    }
                    1 -> {
                        layoutParams.topToTop = prevView.id
                        layoutParams.leftToRight = prevView.id
                        prevParam.rightToLeft = imageView.id

                    }
                    2 -> {

                        layoutParams.topToTop = prevView.id
                        layoutParams.leftToRight = prevView.id
                        layoutParams.rightToRight = constraintLayout.id
                        prevParam.rightToLeft = imageView.id

                    }


                }
            }
            prevView = imageView
            constraintLayout.addView(imageView, count)
            CoroutineScope(Dispatchers.IO).launch {
                try {

                    if (item.bitmap == null)
                        item.bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            context.contentResolver.loadThumbnail(
                                item.uri, Size(640, 640), null
                            )
                        } else {
                            MediaStore.Images.Thumbnails.getThumbnail(
                                context.contentResolver,
                                item.uri.lastPathSegment!!.toLong(),
                                MediaStore.Images.Thumbnails.MINI_KIND,
                                null
                            )
                        }
                    MainScope().launch { imageView.setImageBitmap(item.bitmap) }


                } catch (e: FileNotFoundException) {

                    if (item is EventData) {

                        val db = InnerDB.getInstance(context)
                        db.delete(item.id!!)
                    } else if (item is PhotoData) {
                        val db = InnerDB.getPhotoInstance(context)
                        db.delete(item.id!!)
                    }


                } catch (e: Exception) {
                }

            }
            count++
        }

        val tri = (count / 3) * 3 + if (count % 3 == 0) 0 else 3
        for (i in count until tri) {
            val imageView = ImageView(context)
            imageView.id = ViewCompat.generateViewId()

            val layoutParams = ConstraintLayout.LayoutParams(0, 0)
            imageView.layoutParams = layoutParams
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams.dimensionRatio = "H,1:1"
            layoutParams.matchConstraintPercentWidth = 0.32F
            if (count == 0) {
                layoutParams.topToTop = prevView.id
                layoutParams.leftToLeft = prevView.id
            } else {
                val prevParam = prevView.layoutParams as ConstraintLayout.LayoutParams
                when (count % 3) {
                    0 -> {
                        layoutParams.topToBottom = prevView.id
                        layoutParams.leftToLeft = constraintLayout.id
                    }
                    1 -> {
                        layoutParams.topToTop = prevView.id
                        layoutParams.leftToRight = prevView.id
                        prevParam.rightToLeft = imageView.id

                    }
                    2 -> {

                        layoutParams.topToTop = prevView.id
                        layoutParams.leftToRight = prevView.id
                        layoutParams.rightToRight = constraintLayout.id
                        prevParam.rightToLeft = imageView.id

                    }


                }
            }
            prevView = imageView
            constraintLayout.addView(imageView, count)
            count++
        }

        return convertView
    }

    init {
        for (k in keys)
            listViewItemList.add(map[k]!!.sortedByDescending { (it as PhotoData).modifyTime })
    }
}
package kim.hanbin.gpstracker

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView


class DailyPhotoListAdapter(val map: Map<String, List<BaseData>>) : BaseAdapter() {
    private val listViewItemList: ArrayList<List<BaseData>> = ArrayList()
    var keys: List<String> = ArrayList()


    override fun getCount(): Int {
        return keys.size
    }

    // position에 위치한 데이터를 화면에 출력하는데 사용될 View를 리턴. : 필수 구현
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView: View? = convertView
        val context: Context = parent.context

        // "listview_item" Layout을 inflate하여 convertView 참조 획득.
        if (convertView == null) {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.daily_photo_list_item, parent, false)
        }

        // 화면에 표시될 View(Layout이 inflate된)으로부터 위젯에 대한 참조 획득
        val datetv = convertView!!.findViewById<TextView>(R.id.date)
        val recyclerView = convertView.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 3)

        val listViewItem = listViewItemList[position].sortedByDescending { (it as PhotoData).addedTime }
        recyclerView.isNestedScrollingEnabled = false
        datetv.text = keys[position]
        recyclerView.adapter = PhotoListItemAdapter(listViewItem, context, false) {
            val item = it.item
            PhotoListActivity.photoList =
                PhotoService.imageList.sortedByDescending { (it as PhotoData).addedTime }
            context.startActivity(
                Intent(context, PhotoActivity::class.java).putExtra("id", (item as PhotoData).id)
            )
        }

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

    init {
        keys = map.keys.sortedDescending()
        for (k in keys) {
            listViewItemList.add(map[k]!!)
        }
    }
}
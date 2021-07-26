package kim.hanbin.gpstracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import androidx.fragment.app.Fragment
import kim.hanbin.gpstracker.databinding.FragmentPhotoListBinding


class DailyPhotoListFragment : Fragment() {


    private var mBinding: FragmentPhotoListBinding? = null
    private val binding get() = mBinding!!
    val listView by lazy { binding.list }
    var adapter: DailyPhotoListAdapter? = null
    var position = 0

    companion object {
        var instance: DailyPhotoListFragment? = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        instance = this
        mBinding = FragmentPhotoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun refresh() {
        //adapter = DailyPhotoListAdapter(listView, PhotoService.imageListDailyMap)
        if (adapter != null) {
            adapter!!.notifyDataSetChanged(true)
        } else {

            adapter = DailyPhotoListAdapter(listView, PhotoService.imageListDailyMap)
            listView.adapter = adapter
            listView.setOnScrollListener(object : AbsListView.OnScrollListener {
                override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
                    // TODO Auto-generated method stub
                }

                override fun onScroll(
                    view: AbsListView?, firstVisibleItem: Int,
                    visibleItemCount: Int, totalItemCount: Int
                ) {
                    position = firstVisibleItem
                }
            })
        }
        //listView.adapter = adapter
        // listView.setSelection(position)

    }

    fun refreshHeight() {
        listView.setSelection(position)
    }


}
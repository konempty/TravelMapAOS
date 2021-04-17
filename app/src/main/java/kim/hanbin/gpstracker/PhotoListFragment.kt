package kim.hanbin.gpstracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.fragment.app.Fragment
import kim.hanbin.gpstracker.databinding.FragmentPhotoListBinding
import java.lang.Exception

class PhotoListFragment : Fragment() {


    private var mBinding: FragmentPhotoListBinding? = null
    private val binding get() = mBinding!!

    companion object {
        var instance: PhotoListFragment? = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        instance = this
        mBinding = FragmentPhotoListBinding.inflate(inflater, container, false)
        binding.list.adapter = DailyPhotoListAdapter(PhotoService.imageListDailyMap)

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun refresh() {
        binding.list.adapter = DailyPhotoListAdapter(PhotoService.imageListDailyMap)
    }


}
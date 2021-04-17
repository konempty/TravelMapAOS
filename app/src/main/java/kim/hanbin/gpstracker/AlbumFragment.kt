package kim.hanbin.gpstracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import kim.hanbin.gpstracker.databinding.FragmentAlbumBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class AlbumFragment : Fragment() {
    companion object {
        var instance: AlbumFragment? = null
    }

    private var mBinding: FragmentAlbumBinding? = null
    private val binding get() = mBinding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentAlbumBinding.inflate(inflater, container, false)

        binding.recyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.recyclerView.adapter = AlbumItemAdapter(PhotoService.imageListMap, context!!) {
            startActivity(
                Intent(
                    context,
                    PhotoListActivity::class.java
                ).putExtra("name", it.path)
            )

        }
        instance = this
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun refresh() {

            binding.recyclerView.adapter?.notifyDataSetChanged()

    }

}
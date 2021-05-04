package kim.hanbin.gpstracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import kim.hanbin.gpstracker.databinding.ActivityPhotoListBinding

class PhotoListActivity : AppCompatActivity() {
    companion object {
        lateinit var photoList: MutableList<BaseData>
    }

    private var mBinding: ActivityPhotoListBinding? = null
    private val binding get() = mBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityPhotoListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val name = intent.getStringExtra("name")
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        if (name == null) {
            photoList = MapsActivity.clusterList
        } else if (name == "\\") {
            photoList = MapFragment.clusterList
        } else {
            photoList = PhotoService.imageListMap[name]!!
        }
        binding.recyclerView.adapter = PhotoListItemAdapter(photoList, this, true) {
            val item = it.item
            val id = if (item is EventData) {
                item.pictureId
            } else if (item is PhotoData) {
                item.id
            } else {
                0
            }
            startActivity(
                Intent(this, PhotoActivity::class.java).putExtra("id", id)
                    .putExtra("isFromTracking", name == null)
            )
        }
        //val splts = name!!.split("/")

        //binding.title_tv.text = splts[splts.size - 2]
        binding.back.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        binding.recyclerView.adapter?.notifyDataSetChanged()
    }


}
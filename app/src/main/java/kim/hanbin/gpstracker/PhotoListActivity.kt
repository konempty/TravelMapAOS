package kim.hanbin.gpstracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import kim.hanbin.gpstracker.databinding.ActivityPhotoListBinding

class PhotoListActivity : AppCompatActivity() {
    companion object {
        lateinit var photoList: MutableList<EventData>
    }

    private var mBinding: ActivityPhotoListBinding? = null
    private val binding get() = mBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityPhotoListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //  val name = intent.getStringExtra("name")
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        // if (name == "Gallery Place/") {
        photoList = MapsActivity.clusterList
        /* } else {
             photoList = PictureService.imageListMap[name]!!
         }*/
        binding.recyclerView.adapter = PhotoListItemAdapter(photoList, this) {
            startActivity(
                Intent(this, PhotoActivity::class.java).putExtra("id", it.item.id)
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
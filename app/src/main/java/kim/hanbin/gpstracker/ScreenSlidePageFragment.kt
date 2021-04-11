package kim.hanbin.gpstracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide

class ScreenSlidePageFragment : Fragment {

    var position = 0
    lateinit var img: TouchImageView

    constructor(p: Int) : super() {
        position = p
    }

    constructor()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (savedInstanceState != null) {
            position = savedInstanceState.getInt("position")
        }
        val v = inflater.inflate(R.layout.fragment_screen_slide_page, container, false)
        val item = PhotoListActivity.photoList[position]
        img = v.findViewById<TouchImageView>(R.id.img)
        Glide.with(img)
            .load(item.uri)
            .into(img)
        if (item.isVideo!!) {
            v.findViewById<LinearLayout>(R.id.playBtn).visibility = View.VISIBLE
            v.findViewById<LinearLayout>(R.id.playBtn).setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(item.uri, "video/*")
                startActivity(intent)
            }
        }
        return v
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("position", position)
        super.onSaveInstanceState(outState)
    }
}
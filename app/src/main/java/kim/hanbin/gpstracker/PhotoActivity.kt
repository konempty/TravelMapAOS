package kim.hanbin.gpstracker

import android.content.IntentSender
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.MutableLiveData
import kim.hanbin.gpstracker.databinding.ActivityPhotoBinding

class PhotoActivity : AppCompatActivity() {

    companion object {
        var instance: PhotoActivity? = null
    }

    var isMenuClicked = false
    private val _permissionNeededForDelete = MutableLiveData<IntentSender?>()
    private var mBinding: ActivityPhotoBinding? = null
    private val binding get() = mBinding!!

    //lateinit var uri: Uri
    // var photo_id: Long = 0
    val DELETE_PERMISSION_REQUEST = 10

    // private var mScaleGestureDetector: ScaleGestureDetector? = null
    // private var mScaleFactor = 1.0f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val items = arrayOf("Delete")
        instance = this

        val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
        val id = intent.getLongExtra("id", 0)
        binding.pager.adapter = pagerAdapter
        for (i in 0 until PhotoListActivity.photoList.size) {
            if (PhotoListActivity.photoList[i].id == id) {
                binding.pager.currentItem = i
                break
            }
        }
        binding.back.setOnClickListener {
            finish()
        }
        /*menu.setOnClickListener {
            isMenuClicked = true
            spinner.performClick()
        }

        share.setOnClickListener {
            val share = Intent(Intent.ACTION_SEND)
            share.type = "image/jpeg"

            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "공유")
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            val item = PhotoListActivity.photoList[pager.currentItem]

            share.putExtra(Intent.EXTRA_STREAM, item.uri)
            startActivity(Intent.createChooser(share, "Share Image"))
        }*/
        //img.setImageURI(uri)
        //mScaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        _permissionNeededForDelete.observe(this, { intentSender ->
            intentSender?.let {
                // On Android 10+, if the app doesn't have permission to modify
                // or delete an item, it returns an `IntentSender` that we can
                // use here to prompt the user to grant permission to delete (or modify)
                // the image.
                startIntentSenderForResult(
                    intentSender,
                    DELETE_PERMISSION_REQUEST,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            }
        })
    }

    /*override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            DeleteImg()
        }
    }

    fun DeleteImg() {
        try {
            val item = PhotoListActivity.photoList[pager.currentItem]

            application.contentResolver.delete(
                item.uri,
                "${MediaStore.Images.Media._ID} = ?",
                arrayOf(item.id.toString())
            )
            GlobalScope.async {

                val db = InnerDB.getInstance(this@PhotoActivity)
                db.delete(item.id!!)

                synchronized(PictureService.imageListMap) {
                    for (entry in PictureService.imageListMap) {
                        for (it in entry.value) {
                            if (it.id == item.id!!) {
                                entry.value.remove(it)
                                if (entry.value.size == 0)
                                    PictureService.imageListMap.remove(entry.key)
                                PictureService.myPlace = db.getMyPlace()
                                PictureService.galleryPlace = db.getGaleryPlace()
                                finish()
                            }
                        }
                    }
                }
            }
        } catch (securityException: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recoverableSecurityException =
                    securityException as? RecoverableSecurityException
                        ?: throw securityException

                // Signal to the Activity that it needs to request permission and
                // try the delete again if it succeeds.
                _permissionNeededForDelete.postValue(
                    recoverableSecurityException.userAction.actionIntent.intentSender
                )
            } else {
                throw securityException
            }
        }

    }*/

    /*private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            mScaleFactor *= scaleGestureDetector.scaleFactor
            mScaleFactor = 1f.coerceAtLeast(mScaleFactor.coerceAtMost(5.0f))
            img.scaleX = mScaleFactor
            img.scaleY = mScaleFactor
            return true
        }
    }*/

    private inner class ScreenSlidePagerAdapter(val fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int = PhotoListActivity.photoList.size

        override fun getItem(position: Int): Fragment {
            for (f in fm.fragments) {
                try {

                    (f as ScreenSlidePageFragment).img.resetScale()
                } catch (e: Exception) {

                }
            }
            return ScreenSlidePageFragment(position)
        }
    }

}
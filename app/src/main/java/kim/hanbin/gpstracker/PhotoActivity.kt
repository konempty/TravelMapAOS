package kim.hanbin.gpstracker

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.MutableLiveData
import kim.hanbin.gpstracker.databinding.ActivityPhotoBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    val isFromTracking by lazy { intent.getBooleanExtra("isFromTracking", false) }

    // private var mScaleFactor = 1.0f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val items = arrayOf("삭제")
        instance = this
        binding.spinner.adapter =
            ArrayAdapter(this, R.layout.custom_spinner_dropdown_item, items)
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                when (p2) {
                    0 -> {
                        if (isMenuClicked) {
                            val dialog =
                                AlertDialog.Builder(this@PhotoActivity, R.style.MyDialogTheme)
                                    .setTitle("이 항목을 지우시겠습니까?")
                                    .setNegativeButton("아니오") { dialogInterface: DialogInterface, i: Int ->
                                    }
                                    .setPositiveButton("예") { dialogInterface: DialogInterface, i: Int ->
                                        if (isFromTracking) {
                                            val item =
                                                PhotoListActivity.photoList[binding.pager.currentItem] as EventData
                                            AlertDialog.Builder(this@PhotoActivity)
                                                .setTitle("이 항목을 여행기록에서만 지우시겠습니까?")
                                                .setNegativeButton("기기에서 완전삭제") { dialogInterface: DialogInterface, i: Int ->
                                                    DeleteImg()
                                                    DeletefromTrackingMap(item)
                                                }
                                                .setPositiveButton("여행기록에서만 삭제") { dialogInterface: DialogInterface, i: Int ->
                                                    DeletefromTrackingMap(item)
                                                    DeleteTrackingLog(item.pictureId!!)
                                                    finish()
                                                }.create().show()
                                        } else {

                                            DeleteImg()
                                        }
                                    }
                                    .setMessage("삭제한뒤 복구는 불가능 합니다. 정말로 이 항목을 지우시겠습니까?")
                                    .create()
                            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            dialog.show()

                        }
                    }
                    else -> {
                    }
                }
                isMenuClicked = false
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                isMenuClicked = false
            }
        }
        val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
        val id = intent.getLongExtra("id", 0)
        binding.pager.adapter = pagerAdapter
        for (i in 0 until PhotoListActivity.photoList.size) {
            val item = PhotoListActivity.photoList[i]
            val item_id = if (item is EventData) {
                item.pictureId
            } else if (item is PhotoData) {
                item.id
            } else {
                0
            }

            if (item_id == id) {
                binding.pager.currentItem = i
                break
            }
        }
        binding.back.setOnClickListener {
            finish()
        }
        binding.menu.setOnClickListener {
            isMenuClicked = true
            binding.spinner.performClick()
        }

        binding.share.setOnClickListener {
            val share = Intent(Intent.ACTION_SEND)


            val item = PhotoListActivity.photoList[binding.pager.currentItem]
            share.type = if (item.isVideo == true) "video/*" else "image/*"

            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "공유")
            values.put(MediaStore.Images.Media.MIME_TYPE, share.type)

            share.putExtra(Intent.EXTRA_STREAM, item.uri)
            startActivity(Intent.createChooser(share, "Share Image"))
        }
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
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

    fun DeletefromTrackingMap(item: EventData) {

        PhotoListActivity.photoList.remove(item)
        MapsActivity.instance.initCluster()

    }

    fun DeleteTrackingLog(id: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val db =
                InnerDB.getInstance(this@PhotoActivity)
            db.deletePicture(id)
        }
    }

    fun DeleteImg() {
        try {
            val item = PhotoListActivity.photoList[binding.pager.currentItem]
            val id = if (item is PhotoData) {
                item.id
            } else if (item is EventData) {
                item.pictureId
            } else {
                0
            }
            application.contentResolver.delete(
                item.uri,
                "${MediaStore.Images.Media._ID} = ?",
                arrayOf(id.toString())
            )
            CoroutineScope(Dispatchers.IO).launch {

                val db = InnerDB.getPhotoInstance(this@PhotoActivity)
                db.delete(id!!)
                DeleteTrackingLog(id)
                synchronized(PhotoService.imageListMap) {
                    val it1 = PhotoService.imageListMap.iterator()
                    while (it1.hasNext()) {
                        val entry = it1.next()
                        val it2 = entry.value.iterator()
                        while (it2.hasNext()) {
                            val item = it2.next()
                            val id2 = if (item is PhotoData) {
                                item.id
                            } else if (item is EventData) {
                                item.pictureId
                            } else {
                                0
                            }
                            if (id2 == id) {
                                it2.remove()
                                if (entry.value.size == 0)
                                    it1.remove()
                                val it3 = PhotoService.imageListDailyMap.iterator()
                                while (it3.hasNext()) {
                                    val entry2 = it3.next()
                                    val it4 = entry2.value.iterator()
                                    while (it4.hasNext()) {
                                        val item = it4.next()
                                        val id3 = if (item is PhotoData) {
                                            item.id
                                        } else if (item is EventData) {
                                            item.pictureId
                                        } else {
                                            0
                                        }
                                        if (id3 == id) {

                                            it4.remove()
                                            if (entry2.value.size == 0)
                                                it3.remove()
                                            PhotoService.galleryPlace = db.getGaleryPlace()
                                            launch(Dispatchers.Main) {
                                                AlbumFragment.instance?.refresh()
                                                MapFragment.instance?.refresh()
                                                DailyPhotoListFragment.instance?.refresh()
                                                finish()
                                            }
                                            return@launch
                                        }
                                    }
                                }
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

    }

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
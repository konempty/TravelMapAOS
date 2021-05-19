package kim.hanbin.gpstracker

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.Semaphore

class PhotoService : Service() {
    companion object {

        val imageListMap = mutableMapOf<String, MutableList<BaseData>>()
        val imageListDailyMap = mutableMapOf<Date, MutableList<BaseData>>()
        val imageList = Collections.synchronizedList(arrayListOf<BaseData>())
        var galleryPlace: List<PhotoData> = arrayListOf()
        var isRunning = false

        fun refresh() {
            MainScope().launch {
                AlbumFragment.instance?.refresh()
                DailyPhotoListFragment.instance?.refresh()
            }
        }

        var loadComplete = false
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    var photoList = arrayListOf<PhotoData>()
    var tmpList = arrayListOf<PhotoData>()
    val thread: Thread = Thread {
        try {
            Thread.sleep(100)
            val sem = Semaphore(10)

            while (tmpList.isNotEmpty()) {
                val it = tmpList.removeFirst()
                sem.acquire()
                contentResolver.openInputStream(it.uri)?.use { stream ->

                    ExifInterface(stream).run {
                        // If lat/long is null, fall back to the coordinates (0, 0).


                        if (latLong != null) {

                            it.isLoc = true
                            it.lat = latLong!![0]
                            it.lng = latLong!![1]
                        }
                        stream.close()
                        photoList.add(it)
                        sem.release()
                    }
                }

            }
            sem.acquire(10)
            db.singleTransaction(photoList)

            loadComplete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        CoroutineScope(Dispatchers.IO).launch {
            delay(100)
            init()

        }
        return START_STICKY
    }

    val db: PhotoDataDao by lazy { InnerDB.getPhotoInstance(this) }

    fun init() {
        loadComplete = false
        val projection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.MediaColumns.RELATIVE_PATH
                )
            } else {
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.LATITUDE,
                    MediaStore.Images.Media.LONGITUDE,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.DATA
                )
            }

// Show only videos that are at least 5 minutes in duration.
// Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} ASC"
        val max = db.getMax()
        var maxid = max.id
        if (maxid == null)
            maxid = 0
        var maxModify = max.modifyTime
        if (maxModify == null)
            maxModify = 0
        val select =
            "(${MediaStore.Images.Media._ID} > $maxid or ${MediaStore.Images.Media.DATE_MODIFIED} > $maxModify) and (${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} or ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
        val query = applicationContext.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            select,
            null,
            sortOrder
        )
        val list = db.getAll()
        imageListMap.clear()
        imageListDailyMap.clear()
        imageList.clear()
        for (photo in list) {
            val parent = photo.path

            val date = Date(photo.modifyTime!! / 86400 * 86400000) // 날짜로 구별할수 있도록 시분초를 없앤다
            if (!imageListMap.containsKey(parent)) {
                imageListMap[parent!!] = mutableListOf()
            }
            if (!imageListDailyMap.containsKey(date)) {
                imageListDailyMap[date] = mutableListOf()
            }

            imageListMap[parent]!! += photo
            imageListDailyMap[date]!! += photo
            imageList += photo

        }
        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val latColumn =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) -1 else cursor.getColumnIndexOrThrow(
                    MediaStore.Images.Media.LATITUDE
                )
            val longColumn =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) -1 else cursor.getColumnIndexOrThrow(
                    MediaStore.Images.Media.LONGITUDE
                )
            val modifyColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Images.Media.DATE_MODIFIED
            )
            val pathColumn =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.RELATIVE_PATH
                ) else cursor.getColumnIndexOrThrow(
                    MediaStore.Images.Media.DATA
                )
            val mediaColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Files.FileColumns.MEDIA_TYPE
            )
            thread.start()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val modify = cursor.getLong(modifyColumn)
                // Get values of columns for a given video.

                var latLong: DoubleArray? = null
                val path =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cursor.getString(pathColumn) + name else cursor.getString(
                        pathColumn
                    )
                val parent = path.replace(name, "")
                val media =
                    cursor.getInt(mediaColumn) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

                //Thread {
                // Stores column values and the contentUri in a local object
                // that represents the media file.

                val date = Date(modify / 86400 * 86400000)// 날짜로 구별할수 있도록 시분초를 없앤다

                if (!imageListMap.containsKey(parent) || imageListMap[parent] == null) {
                    imageListMap[parent] = mutableListOf()
                }
                if (!imageListDailyMap.containsKey(date) || imageListDailyMap[date] == null) {
                    imageListDailyMap[date] = mutableListOf()
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    latLong = DoubleArray(2)

                    latLong[0] = cursor.getDouble(latColumn)
                    latLong[1] = cursor.getDouble(longColumn)
                    var photodata: PhotoData
                    if (latLong[0] != 0.0 || latLong[1] != 0.0) {

                        photodata = PhotoData(
                            id,
                            name,
                            parent,
                            media,
                            modify,
                            true,
                            latLong[0],
                            latLong[1]
                        )
                        photoList.add(photodata)
                    } else {
                        photodata = PhotoData(
                            id,
                            name,
                            parent,
                            media,
                            modify,
                            false,
                            0.0,
                            0.0
                        )
                        photoList.add(photodata)

                    }
                    try {

                        imageListMap[parent]!! += photodata
                        imageListDailyMap[date]!! += photodata
                        imageList += photodata
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    val photodata =
                        PhotoData(id, name, parent, media, modify, false, 0.0, 0.0)

                    tmpList.add(photodata)
                    imageListMap[parent]!! += photodata
                    imageListDailyMap[date]!! += photodata
                    imageList += photodata

                }


// Get location data using the Exifinterface library.
// Exception occurs if ACCESS_MEDIA_LOCATION permission isn't granted.


//}.start()
            }
            cursor.close()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

                val tmplist = photoList.clone() as List<PhotoData>
                db.singleTransaction(tmplist)

                loadComplete()
            }
            refresh()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (thread.isAlive)
            thread.interrupt()
        isRunning = false
    }

    fun loadComplete() {
        loadComplete = true
        galleryPlace = db.getGaleryPlace()
        MapFragment.instance?.refresh()
        isRunning = false
        stopSelf()
    }

}
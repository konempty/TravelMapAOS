package kim.hanbin.gpstracker

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Semaphore

class GPSTrackingService : Service() {
    // ...
    val semaphore = Semaphore(1) //이미지 정리중에 연속으로 이미지 정리 요청 방지
    val semaphore2 = Semaphore(1) //이미지 정리가 끝난후 gps정보 저장

    companion object {
        var isStopSelf = false
        var locationListenerGPS: LocationListener? = null
        var locationListenerNet: LocationListener? = null
        var locationManager: LocationManager? = null
        var lastTime = 0L
        var instance: GPSTrackingService? = null
    }

    lateinit var db: EventDao
    lateinit var lastLocation: Location
    var photoList = arrayListOf<EventData>()
    var tmpList = arrayListOf<EventData>()


    fun findNewPicture() {
        CoroutineScope(Dispatchers.IO).launch {
            val projection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Files.FileColumns.MEDIA_TYPE,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.DATE_TAKEN,
                        MediaStore.MediaColumns.RELATIVE_PATH
                    )
                } else {
                    arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Files.FileColumns.MEDIA_TYPE,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.DATE_TAKEN,
                        MediaStore.Images.Media.LATITUDE,
                        MediaStore.Images.Media.LONGITUDE,
                        MediaStore.Images.Media.DATA
                    )
                }
            var isNotProcess = true
// Show only videos that are at least 5 minutes in duration.
// Display videos in alphabetical order based on their display name.
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} ASC"

            val select =
                "${MediaStore.Images.Media.DATE_TAKEN} > $lastTime and (${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} or ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
            val query = applicationContext.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                select,
                null,
                sortOrder
            )
            query?.use { cursor ->
                // Cache column indices.
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val addedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

                val latColumn =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) -1 else cursor.getColumnIndexOrThrow(
                        MediaStore.Images.Media.LATITUDE
                    )
                val longColumn =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) -1 else cursor.getColumnIndexOrThrow(
                        MediaStore.Images.Media.LONGITUDE
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
                val trackingNum = db.getTrackingNum()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    // Get values of columns for a given video.

                    var latLong: DoubleArray? = null
                    val path =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cursor.getString(
                            pathColumn
                        ) + name else cursor.getString(
                            pathColumn
                        )
                    val parent = path.replace(name, "")
                    val media =
                        cursor.getInt(mediaColumn) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    val addedDate = cursor.getLong(addedColumn)
                    //Thread {
                    // Stores column values and the contentUri in a local object
                    // that represents the media file.


                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        latLong = DoubleArray(2)

                        latLong[0] = cursor.getDouble(latColumn)
                        latLong[1] = cursor.getDouble(longColumn)
                        var eventdata: EventData
                        if (latLong[0] != 0.0 || latLong[1] != 0.0) {

                            eventdata = EventData(
                                trackingNum,
                                5,
                                latLong[0],
                                latLong[1],
                                id,
                                name,
                                parent,
                                media,
                                Date(addedDate)
                            )
                        } else {

                            eventdata = EventData(
                                trackingNum,
                                5,
                                lastLocation.latitude,
                                lastLocation.longitude,
                                id,
                                name,
                                parent,
                                media,
                                Date(addedDate)
                            )

                        }
                        photoList.add(eventdata)
                    } else {
                        val photodata =
                            EventData(
                                trackingNum,
                                5,
                                id,
                                name,
                                parent,
                                media,
                                Date(addedDate)
                            )

                        tmpList.add(photodata)
                        if (isNotProcess) {
                            processData()
                            isNotProcess = false
                        }

                    }


// Get location data using the Exifinterface library.
// Exception occurs if ACCESS_MEDIA_LOCATION permission isn't granted.


//}.start()
                }
                cursor.close()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

                    val tmplist = photoList.clone() as List<EventData>
                    db.singleTransaction(tmplist)
                    semaphore2.release()

                }
            }
            lastTime = System.currentTimeMillis()
            semaphore.release()
        }
    }

    fun processData() {
        Thread {
            try {
                Thread.sleep(1000)
                val sem = Semaphore(10)

                while (tmpList.isNotEmpty()) {
                    val it = tmpList.removeFirst()
                    sem.acquire()
                    contentResolver.openInputStream(it.uri)?.use { stream ->

                        ExifInterface(stream).run {
                            // If lat/long is null, fall back to the coordinates (0, 0).


                            if (latLong != null) {

                                it.lat = latLong!![0]
                                it.lng = latLong!![1]
                            } else {

                                it.lat = lastLocation.latitude
                                it.lng = lastLocation.longitude
                            }
                            stream.close()
                            photoList.add(it)
                            sem.release()
                        }
                    }

                }
                sem.acquire(10)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            db.singleTransaction(photoList)
            semaphore2.release()

        }.start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        isStopSelf = false
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Thread {
                Thread.sleep(100)
                realStopSelf()
            }.start()
        } else {
            initializeNotification()
            instance = this
            db = InnerDB.getInstance(this)
            val intervalTime = MyPreference.trackingTime
            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
            locationListenerGPS = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    semaphore.acquire()
                    if (System.currentTimeMillis() - lastTime > intervalTime - 10) {
                        if (MyPreference.trackingState == 2) {
                            lastLocation = location
                            semaphore2.acquire()
                            CoroutineScope(Dispatchers.IO).launch {

                                val lat: Double = location.latitude
                                val lng: Double = location.longitude
                                Log.i("tag", "lat $lat lon $lng")
                                db.insert(
                                    EventData(
                                        db.getTrackingNum()!!,
                                        3,
                                        lat,
                                        lng,
                                        Date()
                                    )
                                )
                                semaphore2.release()


                            }

                            findNewPicture()
                        } else {
                            semaphore.release()
                            lastTime = System.currentTimeMillis()
                            realStopSelf()
                        }
                    } else {

                        semaphore.release()
                    }
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {

                }

                override fun onProviderEnabled(provider: String) {
                }

                override fun onProviderDisabled(provider: String) {
                }
            }
            locationListenerNet = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    semaphore.acquire()
                    if (System.currentTimeMillis() - lastTime > intervalTime - 10) {
                        if (MyPreference.trackingState == 2) {
                            lastLocation = location
                            semaphore2.acquire()
                            CoroutineScope(Dispatchers.IO).launch {

                                val lat: Double = location.latitude
                                val lng: Double = location.longitude
                                Log.i("tag", "lat $lat lon $lng")
                                db.insert(
                                    EventData(
                                        db.getTrackingNum()!!,
                                        3,
                                        lat,
                                        lng,
                                        Date()
                                    )
                                )
                                semaphore2.release()

                            }

                            findNewPicture()
                        } else {
                            lastTime = System.currentTimeMillis()
                            realStopSelf()
                            semaphore.release()
                        }
                    } else {

                        semaphore.release()
                    }
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {

                }

                override fun onProviderEnabled(provider: String) {
                }

                override fun onProviderDisabled(provider: String) {
                }
            }

            if (locationManager!!
                    .isProviderEnabled(LocationManager.GPS_PROVIDER)
            ) {

                locationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    intervalTime,
                    20f,
                    locationListenerGPS!!
                )
            }
            if (locationManager!!
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            ) {
                locationManager!!.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    intervalTime,
                    20f,
                    locationListenerNet!!
                )
            }
            val locationProvider = LocationManager.GPS_PROVIDER
            val lastKnownLocation = locationManager!!.getLastKnownLocation(locationProvider)
            CoroutineScope(Dispatchers.IO).launch {
                val data = db.getLasTime()

                lastTime = data.time!!.time
                if (lastKnownLocation != null) {
                    val lng = lastKnownLocation.latitude
                    val lat = lastKnownLocation.longitude
                    Log.i("tag", "lat $lat lon $lng")
                    lastLocation = lastKnownLocation
                    findNewPicture()
                }
            }

        }
        return START_STICKY
    }

    fun realStopSelf() {
        if (locationManager != null) {
            locationManager!!.removeUpdates(locationListenerGPS!!)
            locationManager!!.removeUpdates(locationListenerNet!!)
        }
        isStopSelf = true
        instance = null
        stopSelf()
    }

    fun initializeNotification() {
        val builder = NotificationCompat.Builder(this, "2")
        builder.setSmallIcon(R.mipmap.ic_launcher)
        val style = NotificationCompat.BigTextStyle()
        style.bigText("앱을 실행하려면 눌러주세요.")
        style.setBigContentTitle(null)
        style.setSummaryText("TravelMap이 열심히 트래킹하고 있어요!")
        builder.setContentText(null)
        builder.setContentTitle("TravelMap실행중")
        builder.setOngoing(true)
        builder.setStyle(style)
        builder.setWhen(0)
        builder.setShowWhen(false)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        builder.setContentIntent(pendingIntent)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    "2",
                    "tracking notification",
                    NotificationManager.IMPORTANCE_NONE
                )
            )
        }
        val notification = builder.build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (locationManager != null) {
            locationManager!!.removeUpdates(locationListenerGPS!!)
            locationManager!!.removeUpdates(locationListenerNet!!)
        }
        findNewPicture()
        instance = null
        if (!isStopSelf) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.SECOND, 1)
            val intent = Intent(this, AlarmReceiver::class.java)
            val sender = PendingIntent.getBroadcast(this, 1234, intent, 0)
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                //API 19 미만
                alarmManager[AlarmManager.RTC_WAKEUP, calendar.timeInMillis] = sender
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                //API 19 이상 API 23미만
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, sender)
            } else {
                //API 23 이상
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    sender
                )
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        if (locationManager != null) {
            locationManager!!.removeUpdates(locationListenerGPS!!)
            locationManager!!.removeUpdates(locationListenerNet!!)
        }


        findNewPicture()
        instance = null
        if (!isStopSelf) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.SECOND, 1)
            val intent = Intent(this, AlarmReceiver::class.java)
            val sender = PendingIntent.getBroadcast(this, 1234, intent, 0)
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {

                //API 19 미만
                alarmManager[AlarmManager.RTC_WAKEUP, calendar.timeInMillis] = sender
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                //API 19 이상 API 23미만
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, sender)
            } else {
                //API 23 이상
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    sender
                )
            }
        }
    }
}
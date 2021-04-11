package kim.hanbin.gpstracker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.view.View
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ktx.utils.sphericalDistance
import kim.hanbin.gpstracker.databinding.ActivityMapsBinding
import kotlinx.coroutines.*
import java.io.FileNotFoundException
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    private var mBinding: ActivityMapsBinding? = null
    private val binding get() = mBinding!!
    private lateinit var mMap: GoogleMap
    private lateinit var clusterManager: ClusterManager<MyItem>
    var trackingNum: Int = 0
    lateinit var db: EventDao
    var zoomlevel = 0
    var angle = 0f
    var nextIdx = 0
    var lastLoc: LatLng? = null
    var isDiscrete = true
    var isAuto = true
    var isPause = true
    val zoomLevels = arrayOf(20f, 18f, 16f)
    var speed = 1
    lateinit var centerMarker: Marker
    var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    companion object {

        val clusterList = mutableListOf<EventData>()
    }


    lateinit var eventList: List<EventData>
    var trackingLogs = arrayListOf<EventData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        db = InnerDB.getInstance(this@MapsActivity)
        trackingNum = intent.getIntExtra("trackingNum", 0)
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomNavigationContainer)


        bottomSheetBehavior!!.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    // do stuff when the drawer is expanded
                }
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    // do stuff when the drawer is collapsed
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // do stuff during the actual drag event for example
                // animating a background color change based on the offset

                // or for example hidding or showing a fab

            }
        })
        binding.root
        binding.root.post {
            val param = binding.bottomNavigationContainer.layoutParams
            param.height = (binding.root.height * 0.8).toInt()
            binding.bottomNavigationContainer.layoutParams = param



            bottomSheetBehavior!!.peekHeight = binding.top.height
            /*param = margin.layoutParams
            param.height = top.height
            margin.layoutParams = param*/
        }
        bottomSheetBehavior!!.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {

                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    //advertiseList.setSelectionAfterHeaderView()
                }
            }

            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) {

            }
        })
        binding.startBtn.setOnClickListener {
            isPause = !isPause
            if (lastLoc == null) {
                binding.startBtn.setImageResource(R.drawable.ic_baseline_pause_96)
                nextTracking()
            } else if (isPause) {
                binding.startBtn.setImageResource(R.drawable.ic_baseline_play_arrow_96)
                pauseAnimation()
            } else {
                binding.startBtn.setImageResource(R.drawable.ic_baseline_pause_96)
                resumeAnimation()
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        clusterManager = ClusterManager(this, mMap)
        mMap.setOnCameraIdleListener(clusterManager)
        mMap.setOnMarkerClickListener(clusterManager)

        clusterManager.setOnClusterClickListener {
            clusterList.clear()
            for (it in it.items.sortedWith(compareBy<MyItem> { it.item.time })) {
                clusterList.add(it.item)
            }
            clusterList.sortByDescending { it.time }
            startActivity(
                Intent(
                    this,
                    PhotoListActivity::class.java
                ).putExtra("name", "Gallery Place/")
            )
            true
        }
        clusterManager.setOnClusterItemClickListener {
            clusterList.clear()
            clusterList.add(it.item)
            startActivity(
                Intent(
                    this,
                    PhotoListActivity::class.java
                ).putExtra("name", "Gallery Place/")
            )
            true
        }
        initCluster()
        clusterManager.renderer =
            CustomMapClusterRenderer(this, mMap, clusterManager, binding)

        mMap.setOnCameraMoveListener {
            //println(mMap!!.cameraPosition.zoom)

            centerMarker.position = mMap.cameraPosition.target
        }
        val uiSetting = mMap.uiSettings
        uiSetting.isTiltGesturesEnabled = false
        uiSetting.isRotateGesturesEnabled = false
        uiSetting.isZoomGesturesEnabled = false
        uiSetting.isScrollGesturesEnabled = false
        uiSetting.isCompassEnabled = false
        //mMap.loa
    }

    fun initCluster() {

        clusterManager.clearItems()
        var i = 0
        var polylineOptions = PolylineOptions()

        GlobalScope.launch {
            eventList = db.getTrackingLog(trackingNum)
            MainScope().launch {
                for (item in eventList) {
                    if (item.eventNum == 3) {
                        centerMarker = mMap.addMarker(
                            MarkerOptions()
                                .position(item.latLng)
                        )
                        val camera =
                            CameraPosition.Builder().target(item.latLng).tilt(90F)
                                .zoom(zoomLevels[1])


                        mMap.moveCamera(
                            CameraUpdateFactory.newCameraPosition(camera.build())
                        )
                        break
                    }
                }
            }
            delay(200)


            for (item in eventList) {
                when (item.eventNum) {
                    1 -> {
                        MainScope().launch {
                            mMap.addPolyline(polylineOptions)
                            polylineOptions = PolylineOptions()
                        }.join()
                        trackingLogs.add(item)
                    }
                    3 -> {
                        polylineOptions.add(item.latLng)
                        trackingLogs.add(item)
                    }
                    4 -> trackingLogs.add(item)
                    5 -> {
                        val item2 = MyItem(item)
                        try {

                            if (item.bitmap == null) {
                                item.bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    contentResolver.loadThumbnail(
                                        item.uri, Size(640, 640), null
                                    )
                                } else {
                                    MediaStore.Images.Thumbnails.getThumbnail(
                                        contentResolver, item.uri.lastPathSegment!!.toLong(),
                                        MediaStore.Images.Thumbnails.MINI_KIND, null
                                    )
                                }
                            }
                            clusterManager.addItem(
                                item2
                            )
                        } catch (e: FileNotFoundException) {

                            GlobalScope.async {

                                db.delete(item.id!!)


                            }
                        } catch (e: Exception) {

                        }
                    }
                }

            }

            launch(Dispatchers.Main) {

                mMap.addPolyline(polylineOptions)
                polylineOptions = PolylineOptions()
                clusterManager.cluster()
            }
        }


    }

    fun nextTracking() {
        if (nextIdx == trackingLogs.size) {
            lastLoc = null
            isPause = true
            nextIdx = 0
            binding.startBtn.setImageResource(R.drawable.ic_baseline_play_arrow_96)
            return
        }
        val item = trackingLogs[nextIdx]

        nextIdx++
        when (item.eventNum) {
            1 -> {
                isDiscrete = true
                nextTracking()
            }
            3 -> {
                val animateTime =

                    if (isDiscrete) {
                        isDiscrete = false
                        1

                    } else {
                        3000 / speed
                    }
                if (!isPause) {
                    MainScope().launch {
                        if (isAuto && lastLoc != null) {
                            delay(100)

                            angle = bearing(
                                lastLoc!!.latitude,
                                lastLoc!!.longitude,
                                item.latLng.latitude,
                                item.latLng.longitude
                            )
                            mMap.animateCamera(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder().target(lastLoc).bearing(angle)
                                        .zoom(zoomLevels[zoomlevel]).tilt(90F).build()
                                ),
                                1000 / speed,
                                null
                            )

                            delay((500 / speed).toLong())
                        }
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder().target(item.latLng).bearing(angle)
                                .zoom(zoomLevels[zoomlevel]).tilt(90F).build()
                        ),

                            animateTime,
                            object :
                                GoogleMap.CancelableCallback {
                                override fun onFinish() {
                                    lastLoc = item.latLng
                                    nextTracking()
                                }

                                override fun onCancel() {

                                }
                            })


                    }

                }
            }
            4 -> {
                zoomlevel = item.trackingSpeed!!
                nextTracking()
            }
        }
    }

    inner class MyItem(
        val item: EventData
    ) : ClusterItem {


        override fun getPosition(): LatLng {
            return item.latLng
        }

        override fun getTitle(): String {
            return item.name!!
        }

        override fun getSnippet(): String {
            return item.name!!
        }

    }

    private class CustomMapClusterRenderer(
        context: Context,
        map: GoogleMap,
        clusterManager: ClusterManager<MyItem>,
        val binding: ActivityMapsBinding
    ) :
        DefaultClusterRenderer<MyItem>(context, map, clusterManager) {
        val view = binding.markerView
        override fun shouldRenderAsCluster(cluster: Cluster<MyItem>): Boolean {
            //start clustering if 2 or more items overlap
            return cluster.size > 1
        }

        override fun onBeforeClusterRendered(
            cluster: Cluster<MyItem>,
            markerOptions: MarkerOptions
        ) {
            // Draw multiple people.
            // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
            markerOptions.icon(getClusterIcon(cluster))
        }

        override fun onClusterUpdated(cluster: Cluster<MyItem>, marker: Marker) {
            // Same implementation as onBeforeClusterRendered() (to update cached markers)
            marker.setIcon(getClusterIcon(cluster))

        }

        private fun getClusterIcon(cluster: Cluster<MyItem>): BitmapDescriptor? {
            val markerItem: MyItem =
                cluster.items.sortedWith(compareBy<MyItem> { it.item.time }).last() as MyItem

            binding.image.setImageBitmap(markerItem.item.bitmap)
            binding.count10.visibility = View.INVISIBLE
            binding.count100.visibility = View.INVISIBLE
            binding.count10000.visibility = View.INVISIBLE
            when (cluster.size.toString().length) {
                0, 1, 2 -> {
                    binding.count10.visibility = View.VISIBLE
                    binding.count10.text = cluster.size.toString()
                }
                3, 4 -> {
                    binding.count100.visibility = View.VISIBLE
                    binding.count100.text = cluster.size.toString()
                }
                else -> {

                    binding.count10000.visibility = View.VISIBLE
                    if (cluster.size > 999999)
                        binding.count10000.text = "999999"
                    else
                        binding.count10000.text = cluster.size.toString()
                }

            }
            // println(cluster.size)
            return BitmapDescriptorFactory.fromBitmap(loadBitmapFromView(view))
        }


        override fun onBeforeClusterItemRendered(
            item: MyItem,
            markerOptions: MarkerOptions
        ) {
            val markerItem: MyItem = item
            binding.image.setImageBitmap(markerItem.item.bitmap)
            binding.count10.visibility = View.INVISIBLE
            binding.count100.visibility = View.INVISIBLE
            binding.count10000.visibility = View.INVISIBLE
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(loadBitmapFromView(view)))
        }

        override fun onClusterItemUpdated(item: MyItem, marker: Marker) {
            // Same implementation as onBeforeClusterItemRendered() (to update cached markers)

            val markerItem: MyItem = item
            binding.image.setImageBitmap(markerItem.item.bitmap)
            binding.count10.visibility = View.INVISIBLE
            binding.count100.visibility = View.INVISIBLE
            binding.count10000.visibility = View.INVISIBLE
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(loadBitmapFromView(view)))
        }

        fun loadBitmapFromView(view: View): Bitmap? {
            val returnedBitmap =
                Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(returnedBitmap)
            val bgDrawable = view.background
            if (bgDrawable != null) bgDrawable.draw(canvas) else canvas.drawColor(Color.TRANSPARENT)
            view.draw(canvas)
            return returnedBitmap
        }
    }

    fun bearing(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Float {
        // 현재 위치 : 위도나 경도는 지구 중심을 기반으로 하는 각도이기 때문에 라디안 각도로 변환한다.
        val Cur_Lat_radian = latitude1 * (Math.PI / 180)
        val Cur_Lon_radian = longitude1 * (Math.PI / 180)


        // 목표 위치 : 위도나 경도는 지구 중심을 기반으로 하는 각도이기 때문에 라디안 각도로 변환한다.
        val Dest_Lat_radian = latitude2 * (Math.PI / 180)
        val Dest_Lon_radian = longitude2 * (Math.PI / 180)

        // radian distance
        var radian_distance = 0.0
        radian_distance = acos(
            sin(Cur_Lat_radian) * sin(Dest_Lat_radian)
                    + cos(Cur_Lat_radian) * cos(Dest_Lat_radian) * cos(Cur_Lon_radian - Dest_Lon_radian)
        )

        // 목적지 이동 방향을 구한다.(현재 좌표에서 다음 좌표로 이동하기 위해서는 방향을 설정해야 한다. 라디안값이다.
        val radian_bearing = acos(
            (sin(Dest_Lat_radian) - sin(Cur_Lat_radian)
                    * cos(radian_distance)) / (cos(Cur_Lat_radian) * sin(
                radian_distance
            ))
        ) // acos의 인수로 주어지는 x는 360분법의 각도가 아닌 radian(호도)값이다.
        var true_bearing: Double
        if (sin(Dest_Lon_radian - Cur_Lon_radian) < 0) {
            true_bearing = radian_bearing * (180 / Math.PI)
            true_bearing = 360 - true_bearing
        } else {
            true_bearing = radian_bearing * (180 / Math.PI)
        }
        return true_bearing.toFloat()
    }


    fun pauseAnimation() {
        isPause = true
        mMap.stopAnimation()
    }

    fun resumeAnimation() {
        isPause = false
        var destLoc: LatLng? = null
        for (i in nextIdx - 1 until trackingLogs.size) {
            if (trackingLogs[i].eventNum == 3) {
                destLoc = trackingLogs[i].latLng
                break;
            }
        }
        if (destLoc != null) {
            MainScope().launch {
                val orgDist = lastLoc!!.sphericalDistance(destLoc)
                val currDist = mMap.cameraPosition.target.sphericalDistance(destLoc)
                var animateTime =
                    if (isDiscrete) {
                        isDiscrete = false
                        1

                    } else {
                        3000 / speed
                    }
                var ratio = currDist / orgDist

                if (isAuto && ratio > 0.5) {

                    angle = bearing(
                        lastLoc!!.latitude,
                        lastLoc!!.longitude,
                        destLoc.latitude,
                        destLoc.longitude
                    )
                    ratio -= 0.5
                    val time = (1000 / speed * ratio / 0.5).toLong()
                    mMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder().target(destLoc).bearing(angle)
                                .zoom(zoomLevels[zoomlevel]).tilt(90F).build()
                        ),
                        time.toInt(),
                        null
                    )

                    delay(time - 500 / speed)
                } else {
                    animateTime = (animateTime * currDist / orgDist / 0.5).toInt()
                }
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(destLoc).bearing(angle)
                        .zoom(zoomLevels[zoomlevel]).tilt(90F).build()
                ),

                    animateTime,
                    object :
                        GoogleMap.CancelableCallback {
                        override fun onFinish() {
                            lastLoc = destLoc
                            nextTracking()
                        }

                        override fun onCancel() {

                        }
                    })
            }
        }
    }
}

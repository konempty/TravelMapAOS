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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import kim.hanbin.gpstracker.databinding.ActivityMapsBinding
import kotlinx.coroutines.*
import java.io.FileNotFoundException

class MapFragment : Fragment(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null

    private lateinit var clusterManager: ClusterManager<MyItem>

    companion object {
        var instance: MapFragment? = null
        val clusterList = mutableListOf<BaseData>()
    }

    private var mBinding: ActivityMapsBinding? = null
    private val binding get() = mBinding!!


    var beforeList: List<BaseData>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        mBinding = ActivityMapsBinding.inflate(inflater, container, false)
        binding.bottomNavigationContainer.visibility = View.GONE
        instance = this

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this@MapFragment)
        return binding.root
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Add a marker in Sydney and move the camera
        clusterManager = ClusterManager(context!!, mMap)

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap!!.setOnCameraIdleListener(clusterManager)
        mMap!!.setOnMarkerClickListener(clusterManager)
        clusterManager.setOnClusterClickListener {
            clusterList.clear()
            for (it in it.items.sortedWith(compareBy<MyItem> { it.item.modifyTime })) {
                clusterList.add(it.item)
            }
            clusterList.sortByDescending { (it as PhotoData).modifyTime }
            startActivity(
                Intent(
                    context,
                    PhotoListActivity::class.java
                ).putExtra("name", "\\")
            )
            true
        }
        clusterManager.setOnClusterItemClickListener {
            clusterList.clear()
            clusterList.add(it.item)
            startActivity(
                Intent(
                    context,
                    PhotoListActivity::class.java
                ).putExtra("name", "\\")
            )
            true
        }
        initCluster()

        clusterManager.renderer =
    CustomMapClusterRenderer(context!!, mMap!!, clusterManager, binding.markerView)


    }


    fun initCluster() {
        if (PhotoService.loadComplete)
            MainScope().launch {
                binding.cover.visibility = View.GONE
            }
        clusterManager.clearItems()
        var i = 0
        CoroutineScope(Dispatchers.IO).launch {
            if (PhotoService.galleryPlace.isNotEmpty())
                MainScope().launch {

                    mMap?.moveCamera(CameraUpdateFactory.newLatLng(PhotoService.galleryPlace.last().latLng))

                }
            for (item in PhotoService.galleryPlace) {
                if (item.latLng != null) {

                    val item2 = MyItem(
                        item
                    )
                    try {

                        if (item.bitmap == null) {
                            item.bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                context?.contentResolver!!.loadThumbnail(
                                    item.uri, Size(640, 640), null
                                )
                            } else {
                                MediaStore.Images.Thumbnails.getThumbnail(
                                    context?.contentResolver, item.uri.lastPathSegment!!.toLong(),
                                    MediaStore.Images.Thumbnails.MINI_KIND, null
                                )
                            }
                        }
                        clusterManager.addItem(
                            item2
                        )
                    } catch (e: FileNotFoundException) {

                        CoroutineScope(Dispatchers.IO).launch {

                            val db = InnerDB.getPhotoInstance(context!!)
                            db.delete(item.id!!)

                            PhotoService.galleryPlace = db.getGaleryPlace()
                            PhotoService.refresh()


                        }
                        return@launch
                    } catch (e: Exception) {

                    }
                    if (i % 100 == 0)
                        MainScope().launch {

                            clusterManager.cluster()
                        }
                    i++
                }
            }
            beforeList = PhotoService.galleryPlace


            MainScope().launch {

                clusterManager.cluster()
            }
        }

    }

    private class CustomMapClusterRenderer(
        context: Context,
        map: GoogleMap,
        clusterManager: ClusterManager<MyItem>,
        val view: View
    ) :
        DefaultClusterRenderer<MyItem>(context, map, clusterManager) {
        val count10 = view.findViewById<TextView>(R.id.count10)
        val count100 = view.findViewById<TextView>(R.id.count100)
        val count10000 = view.findViewById<TextView>(R.id.count10000)
        val image = view.findViewById<ImageView>(R.id.image)

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
                cluster.items.sortedWith(compareBy<MyItem> { it.item.modifyTime }).last() as MyItem


            image.setImageBitmap(markerItem.item.bitmap)

            count10.visibility = View.INVISIBLE
            count100.visibility = View.INVISIBLE
            count10000.visibility = View.INVISIBLE
            when (cluster.size.toString().length) {
                0, 1, 2 -> {
                    count10.visibility = View.VISIBLE
                    count10.text = cluster.size.toString()
                }
                3, 4 -> {
                    count100.visibility = View.VISIBLE
                    count100.text = cluster.size.toString()
                }
                else -> {

                    count10000.visibility = View.VISIBLE
                    if (cluster.size > 999999)
                        count10000.text = "999999"
                    else
                        count10000.text = cluster.size.toString()
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
            image.setImageBitmap(markerItem.item.bitmap)
            count10.visibility = View.INVISIBLE
            count100.visibility = View.INVISIBLE
            count10000.visibility = View.INVISIBLE
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(loadBitmapFromView(view)))
        }

        override fun onClusterItemUpdated(item: MyItem, marker: Marker) {
            // Same implementation as onBeforeClusterItemRendered() (to update cached markers)

            val markerItem: MyItem = item
            image.setImageBitmap(markerItem.item.bitmap)
            count10.visibility = View.INVISIBLE
            count100.visibility = View.INVISIBLE
            count10000.visibility = View.INVISIBLE
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

    override fun onResume() {
        super.onResume()
        if (mMap != null && beforeList != PhotoService.galleryPlace) {
            initCluster()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PhotoService.galleryPlace = arrayListOf()
        instance = null
    }

    fun refresh() {
        if (mMap != null) {
            initCluster()
        }
    }

    inner class MyItem(
        val item: PhotoData
    ) : ClusterItem {


        override fun getPosition(): LatLng {
            return item.latLng!!
        }

        override fun getTitle(): String {
            return item.name!!
        }

        override fun getSnippet(): String {
            return item.name!!
        }

    }

}
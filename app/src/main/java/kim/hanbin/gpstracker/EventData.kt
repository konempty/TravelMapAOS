package kim.hanbin.gpstracker

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import java.util.*

@Entity
data class EventData(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    var trackingNum: Int?,
    var eventNum: Int,
    var lat: Double?,
    var lng: Double?,
    val pictureId: Long?,
    val name: String?,
    val path: String?,
    val isVideo: Boolean?,
    var trackingSpeed: Int?,
    var time: Date
) {
    val latLng: LatLng
        get() {
            return LatLng(lat!!, lng!!)
        }
    val uri: Uri
        get() {
            val uri = if (isVideo == true) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            var photoUri: Uri = Uri.withAppendedPath(
                uri,
                pictureId.toString()
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                photoUri = MediaStore.setRequireOriginal(photoUri)
            }
            return photoUri
        }

    @Ignore
    var bitmap: Bitmap? = null

    //Q미만의 기기의 사진 데이터
    constructor(
        trackingNum: Int?,
        eventNum: Int,
        lat: Double?,
        lng: Double?,
        pictureId: Long?,
        name: String?,
        path: String?,
        isVideo: Boolean?,
        time: Date
    ) : this(null, trackingNum, eventNum, lat, lng, pictureId, name, path, isVideo, null, time)

    //Q이상의 기기의 사진 데이터
    constructor(
        trackingNum: Int?,
        eventNum: Int,
        pictureId: Long?,
        name: String?,
        path: String?,
        isVideo: Boolean?,
        time: Date
    ) : this(null, trackingNum, eventNum, null, null, pictureId, name, path, isVideo, null, time)

    //좌표데이터
    constructor(
        trackingNum: Int?,
        eventNum: Int, lat: Double?,
        lng: Double?,
        time: Date
    ) : this(null, trackingNum, eventNum, lat, lng, null, null, null, null, null, time)

    //트래킹 시작,종료, 일시정지
    constructor(
        trackingNum: Int?, eventNum: Int,
        time: Date
    ) : this(null, trackingNum, eventNum, null, null, null, null, null, null, null, time)

    //Q미만의 기기의 사진 데이터
    constructor(
        trackingNum: Int?, eventNum: Int,
        trackingSpeed: Int?,
        time: Date
    ) : this(null, trackingNum, eventNum, null, null, null, null, null, null, trackingSpeed, time)

}
/*
* eventNum
* 0 : 트래킹 시작
* 1 : 트래킹 일시중지
* 2 : 트래킹 마침
* 3 : 위치데이터 수신
* 4 : 트래킹 속도 변경
* 5 : 이미지 등록
* */
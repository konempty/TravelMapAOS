package kim.hanbin.gpstracker

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import java.util.*


abstract class BaseData {

    open var name: String? = null
    var path: String? = null
    var isVideo: Boolean? = null
    var lat: Double? = null
    var lng: Double? = null
    val latLng: LatLng?
        get() {
            if (lat == null || lng == null)
                return null
            return LatLng(lat!!, lng!!)
        }
    abstract val uri: Uri


    @Ignore
    var bitmap: Bitmap? = null
}

@Entity
data class PhotoData(
    @PrimaryKey val id: Long?,
    val modifyTime: Long?,
    var isLoc: Boolean?
) : BaseData() {

    constructor(
        id: Long?,
        name: String?,
        path: String?,
        isVideo: Boolean?,
        modifyTime: Long?,
        isLoc: Boolean?,
        lat: Double?,
        lng: Double?
    ) : this(id, modifyTime, isLoc) {
        super.name = name
        super.path = path
        super.isVideo = isVideo
        super.lat = lat
        super.lng = lng
    }

    override val uri: Uri
        get() {
            val uri = if (isVideo == true) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            var photoUri: Uri = Uri.withAppendedPath(
                uri,
                id.toString()
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                photoUri = MediaStore.setRequireOriginal(photoUri)
            }
            return photoUri
        }
}

/*
* eventNum
* 0 : 트래킹 시작/재시작
* 1 : 트래킹 일시중지
* 2 : 트래킹 마침
* 3 : 위치데이터 수신
* 4 : 트래킹 속도 변경
* 5 : 이미지 등록
* */
@Entity
data class EventData(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    var trackingNum: Int?,
    var eventNum: Int?,
    val pictureId: Long?,
    var trackingSpeed: Int?,
    var time: Date?
) : BaseData() {

    constructor(
        id: Long? = null,
        trackingNum: Int?,
        eventNum: Int?,
        lat: Double?,
        lng: Double?,
        pictureId: Long?,
        name: String?,
        path: String?,
        isVideo: Boolean?,
        trackingSpeed: Int?,
        time: Date
    ) : this(id, trackingNum, eventNum, pictureId, trackingSpeed, time) {
        super.name = name
        super.path = path
        super.isVideo = isVideo
        super.lat = lat
        super.lng = lng
    }

    override val uri: Uri
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


    //Q미만의 기기의 사진 데이터
    constructor(
        trackingNum: Int?,
        eventNum: Int?,
        lat: Double?,
        lng: Double?,
        pictureId: Long?,
        name: String,
        path: String?,
        isVideo: Boolean?,
        time: Date
    ) : this(null, trackingNum, eventNum, lat, lng, pictureId, name, path, isVideo, null, time)

    //Q이상의 기기의 사진 데이터
    constructor(
        trackingNum: Int?,
        eventNum: Int?,
        pictureId: Long?,
        name: String,
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

    //트래킹 시작, 일시정지
    constructor(
        trackingNum: Int, eventNum: Int,
        time: Date
    ) : this(null, trackingNum, eventNum, null, null, null, null, null, null, null, time)

    //트래킹 종료
    constructor(
        trackingNum: Int, eventNum: Int,
        name: String,
        time: Date
    ) : this(null, trackingNum, eventNum, null, null, null, name, null, null, null, time)

    //Q미만의 기기의 사진 데이터
    constructor(
        trackingNum: Int, eventNum: Int,
        trackingSpeed: Int?,
        time: Date
    ) : this(null, trackingNum, eventNum, null, null, null, null, null, null, trackingSpeed, time)

    //getLasTime() 합수에서 리턴값
    constructor(
        eventNum: Int,
        time: Date
    ) : this(null, 0, eventNum, null, null, null, null, null, null, null, time)

}

@Entity(indices = [Index(value = ["trackingID"], unique = true)])
data class TrackingInfo(
    @PrimaryKey val id: Int? = null,
    val userID: Long? = null,
    val trackingID: Long? = null,
    val isFriendShare: Boolean? = null
)

class TrackingListData(
    val id: Long,
    val trackingNum: Int,
    val name: String,
    val userID: Long?,
    val trackingID: Long?,
    val isFriendShare: Boolean?,
    val startTime: Date,
    val endTime: Date
)

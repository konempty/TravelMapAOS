package kim.hanbin.gpstracker

data class JsonData(
    val eventNum: Int,
    val trackingSpeed: Int?,
    val lat: Double?,
    val lng: Double?,
    val data: String?,
    val time: String
)

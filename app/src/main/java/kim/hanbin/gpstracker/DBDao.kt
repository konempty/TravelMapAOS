package kim.hanbin.gpstracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface EventDao {
    @Query("SELECT * FROM eventdata")
    fun getAll(): List<EventData>

    @Insert
    fun insert(event: EventData)

    @Query("DELETE from eventdata where id = :id")
    fun delete(id: Long)

    @Query("DELETE from eventdata")
    fun delete1()

    @Query("DELETE from TrackingInfo")
    fun delete2()

    @Query("DELETE from eventdata where pictureId = :id")
    fun deletePicture(id: Long)


    @Query("DELETE from eventdata where trackingNum = :trackingNum")
    fun deleteLogs(trackingNum: Int)

    @Query("DELETE from trackinginfo where id = :trackingNum")
    fun deleteTrackingInfo(trackingNum: Int)


    @Query("SELECT count(*) FROM eventdata")
    fun getCount(): Int


    @Query("SELECT eventNum,time FROM eventdata WHERE eventNum <> 4 ORDER BY id DESC LIMIT 1 ")
    fun getLasTime(): EventData

    @Query("SELECT max(trackingNum) FROM eventdata")
    fun getTrackingNum(): Int?

    @Query("SELECT max(id) FROM trackinginfo where trackingID = :trackingID")
    fun getTrackingInfo(trackingID: Long): Int?

    @Insert
    fun singleTransaction(dataList: List<EventData>)
    /*
    *
    val userID: Long?,
    val trackingID: Long?,
    val isFriendShare: Boolean?,
    * */

    @Query("SELECT e.trackingNum, name,a.startTime,a.endTime,e.id,t.userID,t.trackingID,t.isFriendShare FROM eventdata e left join trackinginfo t on e.trackingNum = t.id,(Select trackingNum, min(time) as startTime, max(time) as endTime FROM eventdata group by trackingNum) a on eventNum = 2 and e.trackingNum = a.trackingNum ")
    fun getAllTrackList(): List<TrackingListData>


    @Query("SELECT * FROM eventdata where trackingNum = :num")
    fun getTrackingLog(num: Int): List<EventData>

    @Query("UPDATE eventdata SET name = :name WHERE id = :id")
    fun updateTrckingData(name: String, id: Long)

    @Insert
    fun insert(data: TrackingInfo)

    @Insert
    fun insertShareData(data: TrackingInfo, dataList: List<EventData>)
}

@Dao
interface PhotoDataDao {
    @Query("SELECT * FROM photodata order by modifyTime desc")
    fun getAll(): List<PhotoData>

    @Query("SELECT max(id) as id,max(modifyTime) as modifyTime FROM photodata")
    fun getMax(): PhotoData

    @Insert
    fun insert(data: PhotoData)

    @Query("DELETE from photodata where id = :id")
    fun delete(id: Long)

    @Query("SELECT * FROM photodata where isLoc = 1 order by modifyTime")
    fun getGaleryPlace(): List<PhotoData>


    @Transaction
    fun singleTransaction(dataList: List<PhotoData>) {
        dataList.forEach {
            delete(it.id!!)
            insert(it)
        }

    }

    @Transaction
    fun singleTransactionUpdate(dataList: List<PhotoData>) {
        dataList.forEach {
            delete(it.id!!)
            insert(it)
        }
    }

}
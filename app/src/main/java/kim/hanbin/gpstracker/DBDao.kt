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


    @Query("DELETE from eventdata where trackingNum = :trackingNum")
    fun deleteLogs(trackingNum: Int)


    @Query("SELECT count(*) FROM eventdata")
    fun getCount(): Int


    @Query("SELECT eventNum,time FROM eventdata WHERE eventNum <> 4 ORDER BY id DESC LIMIT 1 ")
    fun getLasTime(): EventData

    @Query("SELECT max(trackingNum) FROM eventdata")
    fun getTrackingNum(): Int?

    @Transaction
    fun singleTransaction(dataList: List<EventData>) {
        dataList.forEach {
            insert(it)
        }
    }

    @Query("SELECT a.trackingNum, name,b.startTime,b.endTime,id FROM eventdata a,(Select trackingNum, min(time) as startTime, max(time) as endTime FROM eventdata group by trackingNum) b on eventNum = 2 and a.trackingNum = b.trackingNum ")
    fun getAllTrackList(): List<TrackingListData>


    @Query("SELECT * FROM eventdata where trackingNum = :num")
    fun getTrackingLog(num: Int): List<EventData>

    @Query("UPDATE eventdata SET name = :name WHERE id = :id")
    fun updateTrckingData(name: String, id: Long)


}

@Dao
interface PhotoDataDao {
    @Query("SELECT * FROM photodata order by addedTime desc")
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

    /* @Query("select *,count(*) as cnt from Waybill where waybillnum = :waybillnum")
     fun findOverlap(waybillnum: String): CountObject*/
}
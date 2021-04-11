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

    @Query("SELECT trackingNum FROM eventdata where eventNum = 2")
    fun getAllTrackList(): List<Int>


    @Query("SELECT * FROM eventdata where trackingNum = :num")
    fun getTrackingLog(num: Int): List<EventData>

}
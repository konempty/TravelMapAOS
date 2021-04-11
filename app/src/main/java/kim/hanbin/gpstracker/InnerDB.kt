package kim.hanbin.gpstracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [EventData::class], version = 1)
@TypeConverters(Converters::class)
abstract class InnerDB : RoomDatabase() {
    abstract fun eventDao(): EventDao?

    companion object {
        private var inst: EventDao? = null
        fun getInstance(context: Context): EventDao {
            if (inst == null) {
                inst = Room.databaseBuilder(context, InnerDB::class.java, "My bee").build()
                    .eventDao()
            }
            return inst!!
        }
    }
}

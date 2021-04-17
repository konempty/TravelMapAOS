package kim.hanbin.gpstracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [EventData::class, PhotoData::class], version = 1)
@TypeConverters(Converters::class)
abstract class InnerDB : RoomDatabase() {
    abstract fun eventDao(): EventDao?
    abstract fun photoDataDao(): PhotoDataDao?

    companion object {
        private var Eventinst: EventDao? = null
        private var Photoinst: PhotoDataDao? = null

        fun getInstance(context: Context): EventDao {
            if (Eventinst == null) {
                Eventinst = Room.databaseBuilder(context, InnerDB::class.java, "Travel Map").build()
                    .eventDao()
            }
            return Eventinst!!
        }

        fun getPhotoInstance(context: Context): PhotoDataDao {
            if (Photoinst == null) {
                Photoinst = Room.databaseBuilder(context, InnerDB::class.java, "Travel Map").build()
                    .photoDataDao()
            }
            return Photoinst!!
        }
    }
}

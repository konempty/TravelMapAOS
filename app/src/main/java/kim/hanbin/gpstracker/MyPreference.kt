package kim.hanbin.gpstracker

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity

class MyPreference {
    companion object {
        lateinit var preference: SharedPreferences
        fun initPreference(ctx: Context) {
            preference = ctx.getSharedPreferences("TravelMap", AppCompatActivity.MODE_PRIVATE)
        }

        var trackingState: Int
            get() {
                return preference.getInt("trackingState", 0)
            }
            set(i) {
                preference.edit().putInt("trackingState", i).remove("trackingTime").apply()
            }
        /*trackingState
        * 0 : 정지
        * 1 : 일시정지
        * 2 : 기록중
        * */

        var trackingTime: Long
            get() {
                return preference.getLong("trackingTime", 300000)
            }
            set(v) {
                preference.edit().putLong("trackingTime", v).apply()
            }
    }


}
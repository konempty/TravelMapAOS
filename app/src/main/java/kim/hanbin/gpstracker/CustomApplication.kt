package kim.hanbin.gpstracker

import android.content.Context
import androidx.multidex.MultiDexApplication
import androidx.startup.AppInitializer
import com.facebook.appevents.AppEventsLogger
import net.danlew.android.joda.JodaTimeInitializer


class CustomApplication : MultiDexApplication() {

    companion object {
        private lateinit var context: Context


        fun getAppContext(): Context {
            return context
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppInitializer.getInstance(this).initializeComponent(JodaTimeInitializer::class.java)
        //FirebaseApp.initializeApp(this)
        AppEventsLogger.activateApp(this);
        context = applicationContext;
    }
}
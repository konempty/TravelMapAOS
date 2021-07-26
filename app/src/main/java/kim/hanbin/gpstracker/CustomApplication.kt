package kim.hanbin.gpstracker

import androidx.multidex.MultiDexApplication
import androidx.startup.AppInitializer
import net.danlew.android.joda.JodaTimeInitializer


class CustomApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        AppInitializer.getInstance(this).initializeComponent(JodaTimeInitializer::class.java)

    }
}
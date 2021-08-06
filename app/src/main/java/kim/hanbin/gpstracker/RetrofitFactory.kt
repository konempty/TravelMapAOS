package kim.hanbin.gpstracker

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitFactory {
    //https://kyome.tistory.com/150
    companion object {
        private val SERVER_API_URL = "https://124.54.119.156/"
        val retrofit: RetroServce by lazy {
            val gson = GsonBuilder().setLenient().create()
            Retrofit.Builder()
                .baseUrl(SERVER_API_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(createOkHttpClient())
                .build()
                .create(RetroServce::class.java)
        }

        private fun createOkHttpClient(): OkHttpClient {
            val helper: SelfSigningHelper = SelfSigningHelper.getInstance()
            val builder = OkHttpClient.Builder()
            helper.setSSLOkHttp(builder)
            return builder.build()
        }
    }
}
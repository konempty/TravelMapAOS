package kim.hanbin.gpstracker

import ProgressResponseBody
import com.google.gson.GsonBuilder
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.CookieManager
import java.net.URLDecoder


class RetrofitFactory {
    companion object {
        private val SERVER_API_URL = "https://119.69.202.23/"
        val retrofit: RetroService by lazy {
            val gson = GsonBuilder().setLenient().create()
            Retrofit.Builder()
                .baseUrl(SERVER_API_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(createOkHttpClient())
                .build()
                .create(RetroService::class.java)
        }

        private fun createOkHttpClient(): OkHttpClient {
            val helper: SelfSigningHelper = SelfSigningHelper.getInstance()
            val builder = OkHttpClient.Builder()
            helper.setSSLOkHttp(builder).cookieJar(JavaNetCookieJar(CookieManager()))
                .addInterceptor(Interceptor { chain ->
                    val original: Request = chain.request()

                    // 헤더를 자유 자재로 변경
                    val builder: Request.Builder = original.newBuilder()
                    builder.addHeader("Accept", "application/json; charset=utf-8")
                    builder.method(original.method, original.body)
                    val request: Request = builder.build()
                    val response: Response = chain.proceed(request)


                    // 아래 소스는 response로 오는 데이터가 URLEncode 되어 있을 때

                    // URLDecode 하는 소스 입니다.
                    response.newBuilder()
                        .body(
                            URLDecoder.decode(response.body!!.string(), "utf-8")
                                .toResponseBody(response.body!!.contentType())
                        )
                        .build()
                })




            return builder.build()
        }



        fun getDownloadRetrofit(listener: (Float) -> Unit): RetroService {
            val gson = GsonBuilder().setLenient().create()
            return Retrofit.Builder()
                .baseUrl(SERVER_API_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(createOkHttpProgressClient(listener))
                .build()
                .create(RetroService::class.java)
        }

        private fun createOkHttpProgressClient(onAttachmentDownloadUpdate: (Float) -> Unit): OkHttpClient {
            val helper: SelfSigningHelper = SelfSigningHelper.getInstance()
            val builder = OkHttpClient.Builder()

            helper.setSSLOkHttp(builder).cookieJar(JavaNetCookieJar(CookieManager())).addInterceptor { chain ->
                val originalResponse =
                    chain.proceed(chain.request())
                originalResponse.newBuilder().body(
                    ProgressResponseBody(
                        originalResponse.body!!,
                        onAttachmentDownloadUpdate
                    )
                ).build()
            }
            return builder.build()
        }


    }

}
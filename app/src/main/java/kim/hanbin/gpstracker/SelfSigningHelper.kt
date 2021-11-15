package kim.hanbin.gpstracker

import android.util.Log
import okhttp3.OkHttpClient
import java.io.IOException
import java.io.InputStream
import java.security.*
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


class SelfSigningHelper private constructor() {
    private var sslContext: SSLContext? = null
    private var tmf: TrustManagerFactory? = null


    companion object {

        private val SingleInstance: SelfSigningHelper by lazy { SelfSigningHelper() }
        fun getInstance(): SelfSigningHelper {
            return SingleInstance
        }
    }


    fun setUp() {
        val cf: CertificateFactory
        val ca: Certificate
        val caInput: InputStream
        try {
            cf = CertificateFactory.getInstance("X.509")
            // Application을 상속받는 클래스에
// Context 호출하는 메서드 ( getAppContext() )를
// 생성해 놓았음

            caInput = CustomApplication.getAppContext().resources
                .openRawResource(R.raw.travelmap)
            ca = cf.generateCertificate(caInput)
            //println("ca=" + (ca as X509Certificate).subjectDN)
            // Create a KeyStore containing our trusted CAs
            val keyStoreType: String = KeyStore.getDefaultType()
            val keyStore: KeyStore = KeyStore.getInstance(keyStoreType)
            keyStore.load(null, null)
            keyStore.setCertificateEntry("ca", ca)
            // Create a TrustManager that trusts the CAs in our KeyStore
            val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
            tmf = TrustManagerFactory.getInstance(tmfAlgorithm)
            tmf!!.init(keyStore)
            // Create an SSLContext that uses our TrustManager
            sslContext = SSLContext.getInstance("TLS")
            sslContext!!.init(null, tmf!!.trustManagers, SecureRandom())
            caInput.close()
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        } catch (e: CertificateException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
    }

    fun setSSLOkHttp(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        builder.sslSocketFactory(
            getInstance().sslContext!!.socketFactory,
            getInstance().tmf!!.trustManagers[0] as X509TrustManager
        )
        builder.hostnameVerifier { hostname, session ->
            if (hostname!!.contentEquals("119.69.202.23")) {
                Log.d("test", "Approving certificate for host $hostname")
                return@hostnameVerifier true
            } else {
                Log.d("test", "fail $hostname")
                return@hostnameVerifier false
            }
        }
        return builder
    }

    init {
        setUp()
    }
}
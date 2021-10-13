package kim.hanbin.gpstracker

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import kim.hanbin.gpstracker.RetrofitFactory.Companion.retrofit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.nio.charset.Charset
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec


class SchemeActivity : AppCompatActivity() {

    val loginResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            next()
        } else {
            Toast.makeText(this, "여행기록을 공유하기위해선 로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
        }

    }

    val mAuth: FirebaseAuth by lazy { Firebase.auth }

    val id: Int by lazy { intent.getStringExtra("id")!!.toInt() }
    val salt: ByteArray? by lazy {
        val saltStr = intent.getStringExtra("salt")
        saltStr?.let { Base64.decode(saltStr, Base64.NO_WRAP) }
    }

    val filename =
        "compressedFile"
    val outputDir: File by lazy { cacheDir } // context being the Activity pointer


    private lateinit var popup: ProgressPopup
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheme)
        if (id == -1) {
            Toast.makeText(this, "에러가 발생했습니다. 나중에 다시시도해주세요", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (mAuth.currentUser == null) {
            loginResult.launch(Intent(this, LoginActivity::class.java))
        } else
            next()

    }

    fun next() {
        showProgress()
        mAuth.currentUser!!.getIdToken(true)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val token = it.result.token!!
                    val res: Call<JsonObject> =
                        retrofit
                            .login(token)

                    res.enqueue(object : Callback<JsonObject?> {
                        override fun onResponse(
                            call: Call<JsonObject?>?,
                            response: Response<JsonObject?>
                        ) {
                            hideProgress()
                            val json = response.body()!!

                            if (json.get("success").asBoolean) {
                                val nickname = json.get("result").asString
                                Toast.makeText(
                                    this@SchemeActivity,
                                    "${nickname}님 환영합니다!",
                                    Toast.LENGTH_LONG
                                ).show()

                                downloadFile()
                            } else {

                                when (json.get("result").asString) {
                                    "noUID" -> {

                                        Toast.makeText(
                                            this@SchemeActivity,
                                            "닉네임을 설정해주세요!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        val dialog = NicknameDialog(this@SchemeActivity)
                                        dialog.setOkListener {
                                            val res2: Call<JsonObject> =
                                                retrofit
                                                    .register(token, dialog.nickname)
                                            res2.enqueue(object : Callback<JsonObject> {
                                                override fun onResponse(
                                                    call: Call<JsonObject>,
                                                    response: Response<JsonObject>
                                                ) {
                                                    Toast.makeText(
                                                        this@SchemeActivity,
                                                        "${dialog.nickname}님 환영합니다!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    downloadFile()
                                                }

                                                override fun onFailure(
                                                    call: Call<JsonObject>,
                                                    t: Throwable
                                                ) {
                                                    Toast.makeText(
                                                        this@SchemeActivity,
                                                        "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    finish()
                                                }
                                            })
                                        }.show()
                                    }
                                    "invalidUser" -> {
                                        Toast.makeText(
                                            this@SchemeActivity,
                                            "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        finish()
                                    }
                                }
                            }

                        }

                        override fun onFailure(call: Call<JsonObject?>?, t: Throwable?) {
                            hideProgress()
                            Toast.makeText(
                                this@SchemeActivity,
                                "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()

                        }
                    })

                } else {
                    hideProgress()
                    Toast.makeText(this, "문제가 발생했습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

    }

    private fun downloadFile() {
        val res2: Call<ResponseBody> =
            retrofit
                .fileDownload(id)

        res2.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                if (response.isSuccessful) {
                    Log.d(this.javaClass.simpleName, "server contacted and has file")
                    CoroutineScope(Dispatchers.IO).launch {
                        val file =
                            writeResponseBodyToDisk(response.body()!!)

                        try {
                            if (file != null) {
                                if (salt != null) {

                                    launch(Dispatchers.Main) {
                                        processData(file)
                                    }
                                } else {
                                    parse(file)
                                }
                            } else {
                                finish()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            launch(Dispatchers.Main) {
                                Toast.makeText(
                                    this@SchemeActivity,
                                    "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        }
                    }


                } else {
                    Log.d(this.javaClass.simpleName, "server contact failed")
                    finish()
                }
            }

            override fun onFailure(
                call: Call<ResponseBody?>,
                t: Throwable
            ) {
                Log.e(this.javaClass.simpleName, "error")
                finish()
            }
        })
    }

    private fun writeResponseBodyToDisk(body: ResponseBody): File? {
        return try {
            // todo change the file location/name according to your needs

            val outputFile: File = File.createTempFile(
                filename, ".json", outputDir
            )
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val fileReader = ByteArray(4096)
                val fileSize = body.contentLength()
                var fileSizeDownloaded: Long = 0
                inputStream = body.byteStream()
                outputStream = FileOutputStream(outputFile)
                while (true) {
                    val read: Int = inputStream.read(fileReader)
                    if (read == -1) {
                        break
                    }
                    outputStream.write(fileReader, 0, read)
                    fileSizeDownloaded += read.toLong()
                    Log.d(
                        this.javaClass.simpleName,
                        "file download: $fileSizeDownloaded of $fileSize"
                    )
                }
                outputStream.flush()
                //copyFileToDownloads(this,outputFile, "$filename.json",outputFile.length())
                outputFile
            } catch (e: IOException) {
                null
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            null
        }
    }

    /*fun copyFileToDownloads(
        context: Context,
        downloadedFile: File,
        fileName: String,
        fileSize: Long
    ): Uri? {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    "application/json"
                )
                put(MediaStore.MediaColumns.SIZE, fileSize)
            }
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            val DOWNLOAD_DIR =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val authority = "${context.packageName}.provider"
            val destinyFile = File(DOWNLOAD_DIR, fileName)
            FileProvider.getUriForFile(context, authority, destinyFile)
        }?.also { downloadedUri ->
            resolver.openOutputStream(downloadedUri).use { outputStream ->
                val brr = ByteArray(1024)
                var len: Int
                val bufferedInputStream =
                    BufferedInputStream(FileInputStream(downloadedFile.absoluteFile))
                while ((bufferedInputStream.read(brr, 0, brr.size).also { len = it }) != -1) {
                    outputStream?.write(brr, 0, len)
                }
                outputStream?.flush()
                bufferedInputStream.close()
            }
        }
    }*/

    fun parse(file: File) {
        //create JsonReader object and pass it the json file,json source or json text.
        val fis = FileInputStream(file)
        try {
            JsonReader(
                InputStreamReader(
                    fis,
                    Charset.forName("UTF-8")
                )
            ).use { jsonReader ->

                val gson = GsonBuilder().setLenient().create()
                jsonReader.isLenient = true
                jsonReader.beginArray() //start of json array
                var numberOfRecords = 0
                while (jsonReader.hasNext()) { //next json array element
                    val document: JsonData = gson.fromJson(jsonReader, JsonData::class.java)
                    //do something real
//                System.out.println(document);
                    numberOfRecords++
                    if (numberOfRecords % 100 == 0) {
                        println(document)
                    }
                }
                jsonReader.endArray()
                println("Total Records Found : $numberOfRecords")
            }
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        fis.close()

        file.delete()
        finish()
    }

    fun processData(file: File) {
        val dialog = PasswordDialog(this@SchemeActivity)

        dialog.setOkListener {
            CoroutineScope(Dispatchers.IO).launch {

                try {
                    parse(getDecryptFile(file, dialog.password))
                } catch (e: IOException) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            this@SchemeActivity,
                            "비밀번호가 잘못되었습니다. 확인후 다시 시도해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                        processData(file)
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            this@SchemeActivity,
                            "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                        e.printStackTrace()
                    }
                }
            }
        }.show()
    }

    fun getDecryptFile(file: File, pswd: String): File {
        val iv = "9362469649674046"

        val spec = PBEKeySpec(pswd.toCharArray(), salt, 1000, 32 * 8)
        val key: SecretKey =
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        val aes: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        aes.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv.toByteArray()))

        val fis = FileInputStream(file)
        val outputFile = File.createTempFile(
            filename, ".enc", outputDir
        )
        val fos = FileOutputStream(outputFile)


        // Create cipher
        // Create cipher
        // Wrap the output stream
        // Wrap the output stream
        val cos = CipherOutputStream(fos, aes)
        // Write bytes
        // Write bytes
        var b: Int
        val d = ByteArray(2048)
        var count = 0
        while (fis.read(d).also { b = it } != -1) {
            cos.write(d, 0, b)
            println(count)
            if (++count % 10 == 0) {
                cos.flush()
            }
        }
        // Flush and close streams.
        // Flush and close streams.
        cos.flush()
        cos.close()
        fis.close()

        file.delete()

        return outputFile
    }


    private fun showProgress() {
        popup = ProgressPopup(this)
        try {
            popup.show()
        } catch (e: IllegalArgumentException) {
            Log.e(this.javaClass.simpleName, "showProgress IllegalArgumentException")
        } catch (e: RuntimeException) {
            Log.e(this.javaClass.simpleName, "showProgress RuntimeException")
        } catch (e: Exception) {
            Log.e(this.javaClass.simpleName, "showProgress Exception")
        }
    }


    private fun hideProgress() {
        try {
            popup.dismiss()
        } catch (e: IllegalArgumentException) {
            Log.e(this.javaClass.simpleName, "hideProgress IllegalArgumentException")
        } catch (e: RuntimeException) {
            Log.e(this.javaClass.simpleName, "hideProgress RuntimeException")
        } catch (e: Exception) {
            Log.e(this.javaClass.simpleName, "hideProgress Exception")
        }
    }
}
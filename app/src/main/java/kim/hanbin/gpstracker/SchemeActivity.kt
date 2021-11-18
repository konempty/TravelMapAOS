package kim.hanbin.gpstracker

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import kim.hanbin.gpstracker.databinding.ActivitySchemeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.net.URLDecoder
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec


class SchemeActivity : AppCompatActivity() {
    private var mBinding: ActivitySchemeBinding? = null
    private val binding get() = mBinding!!
    val relativeLocation = Environment.DIRECTORY_DCIM + File.separator + "TravelMap"

    val loginResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            next()
        } else {
            Toast.makeText(this, "여행기록을 다운로드하기위해선 로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
        }

    }
    val db by lazy { InnerDB.getInstance(this) }

    val mAuth: FirebaseAuth by lazy { Firebase.auth }

    val trackingId: Long by lazy { intent.getStringExtra("id")!!.toLong() }
    val userId: Long by lazy { intent.getStringExtra("userID")!!.toLong() }
    val nickname: String by lazy {
        URLDecoder.decode(
            intent.getStringExtra("nickname")!!.toString(),
            "UTF-8"
        )
    }
    val shareNum: Int by lazy { intent.getStringExtra("shareNum")!!.toInt() }
    val filename: String by lazy {
        URLDecoder.decode(
            intent.getStringExtra("trackingName")!!.toString(), "UTF-8"
        )
    }
    val salt: ByteArray? by lazy {
        val saltStr = intent.getStringExtra("salt")
        saltStr?.let { Base64.decode(saltStr, Base64.NO_WRAP) }
    }
    val retrofit: RetroService by lazy {
        RetrofitFactory.getDownloadRetrofit {
            runOnUiThread {
                val param = binding.progress.layoutParams as LinearLayout.LayoutParams
                param.weight = it
                binding.progress.layoutParams = param
                binding.percent.text = String.format("%.1f%%", it * 100)
            }
        }
    }

    val outputDir: File by lazy { cacheDir } // context being the Activity pointer


    private lateinit var popup: ProgressPopup
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivitySchemeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (trackingId == -1L) {
            Toast.makeText(this, "에러가 발생했습니다. 나중에 다시시도해주세요", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        CoroutineScope(Dispatchers.IO).launch {
/*db.delete1()
            db.delete2()*/
            if (db.getTrackingInfo(trackingId) != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    AlertDialog.Builder(this@SchemeActivity, R.style.MyDialogTheme)
                        .setTitle("이미 존재하는 여행기록")
                        .setMessage("이미 다운받은 여행기록입니다. 여행리스트 화면에서 확인해주세요")
                        .setNeutralButton(
                            "확인"
                        ) { dialogInterface, i ->
                            finish()
                        }.create().show()
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    if (mAuth.currentUser == null) {
                        loginResult.launch(Intent(this@SchemeActivity, LoginActivity::class.java))
                    } else
                        next()
                }
            }


        }

        binding.cancelButton.setOnClickListener {
            finish()
        }

    }


    fun next() {
        retrofit.loginCheck().enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.body() == "Logedin") {
                    if (shareNum == 1)
                        checkPermission()
                    else
                        downloadFile()
                } else {
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
                                            /*val nickname = json.get("result").asString
                                            Toast.makeText(
                                                this@SchemeActivity,
                                                "${nickname}님 환영합니다!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            MainActivity.instance?.setNickname(nickname)*/
                                            if (shareNum == 1)
                                                checkPermission()
                                            else
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
                                                                /*Toast.makeText(
                                                                    this@SchemeActivity,
                                                                    "${dialog.nickname}님 환영합니다!",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                                MainActivity.instance?.setNickname(
                                                                    dialog.nickname
                                                                )*/
                                                                if (shareNum == 1)
                                                                    checkPermission()
                                                                else
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

                                    override fun onFailure(
                                        call: Call<JsonObject?>?,
                                        t: Throwable?
                                    ) {
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
                                Toast.makeText(
                                    this@SchemeActivity,
                                    "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        }
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Toast.makeText(
                    this@SchemeActivity,
                    "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        })


    }

    fun checkPermission() {
        showProgress()
        retrofit.checkPermission(userId).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                val result = response.body()!!
                hideProgress()
                when (result) {
                    "true" -> {
                        downloadFile()
                    }
                    "false" -> {
                        AlertDialog.Builder(this@SchemeActivity, R.style.MyDialogTheme)
                            .setTitle("친구공개")
                            .setMessage("해당 여행 기록은 친구에게만 공개되어있습니다. ${nickname}님에게 친구신청 하시겠습니까?")
                            .setPositiveButton("예") { dialg, i ->
                                showProgress()
                                retrofit.addFriendRequest(userId)
                                    .enqueue(object : Callback<String> {
                                        override fun onResponse(
                                            call: Call<String>,
                                            response: Response<String>
                                        ) {
                                            hideProgress()
                                            val result = response.body()!!
                                            when (result) {
                                                "alreadyRequested", "success" -> {
                                                    Toast.makeText(
                                                        this@SchemeActivity,
                                                        "신청되었습니다. 친구신청 수락 되면 다시 시도해주세요.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                else -> {

                                                    Toast.makeText(
                                                        this@SchemeActivity,
                                                        "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    MainActivity.instance?.logout()
                                                }
                                            }
                                            finish()
                                        }

                                        override fun onFailure(call: Call<String>, t: Throwable) {
                                            Toast.makeText(
                                                this@SchemeActivity,
                                                "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            hideProgress()
                                        }
                                    })
                            }.setNegativeButton("아니요") { dialg, i ->
                                finish()
                            }.setCancelable(false).create().show()

                    }
                    else -> {
                        Toast.makeText(
                            this@SchemeActivity,
                            "에러가 발생했습니다. 나중에 다시시도해주세요",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                hideProgress()
                Toast.makeText(
                    this@SchemeActivity,
                    "에러가 발생했습니다. 나중에 다시시도해주세요",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        })

    }

    private fun downloadFile() {
        val res2: Call<ResponseBody> =
            retrofit.fileDownload(trackingId)

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
                filename+"_temp", if (shareNum == 2) ".enc" else ".json", outputDir
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
                    // Log.d(
                    //       this.javaClass.simpleName,
                    //         "file download: $fileSizeDownloaded of $fileSize"
                    //     )
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
        runOnUiThread {
            binding.msg.text = "여행기록을 정리하고 있습니다. 잠시만 기다려 주세요." }
        var id = db.getTrackingNum()
        id = if (id == null) {
            1
        } else {
            id + 1
        }
        val fis = FileInputStream(file)
        var imageCount = 0
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
                val list = arrayListOf<EventData>()
                val sdf = SimpleDateFormat("yyyy-MM-dd HH-mm-ss")
                var document = JsonData(0, null, null, null, null, "")

                while (jsonReader.hasNext()) { //next json array element
                    document = gson.fromJson(jsonReader, JsonData::class.java)
                    when (document.eventNum) {
                        0 -> {
                            list.add(EventData(id, 0, sdf.parse(document.time)))
                        }
                        3 -> {
                            list.add(
                                EventData(
                                    id,
                                    3,
                                    document.lat,
                                    document.lng,
                                    sdf.parse(document.time)
                                )
                            )
                        }
                        4 -> {
                            list.add(
                                EventData(
                                    id,
                                    4,
                                    document.trackingSpeed,
                                    sdf.parse(document.time)
                                )
                            )
                        }
                        5 -> {
                            val decodedString: ByteArray =
                                Base64.decode(document.data, Base64.DEFAULT)
                            val decodedByte =
                                BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            val filename = "${id}_${imageCount++}"
                            val pictureId = saveImage(decodedByte, filename)

                            list.add(
                                EventData(
                                    id,
                                    5,
                                    document.lat,
                                    document.lng,
                                    pictureId,
                                    filename,
                                    relativeLocation,
                                    false,
                                    sdf.parse(document.time)
                                )
                            )
                        }

                    }

                    //do something real
//                System.out.println(document);
                    numberOfRecords++
                    runOnUiThread {
                        binding.percent.text = numberOfRecords.toString()
                    }
                }
                list.add(EventData(id, 2, filename, sdf.parse(document.time)))
                val info = TrackingInfo(id, userId, trackingId, shareNum == 1)
                db.insertShareData(info, list)
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
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(
                this@SchemeActivity,
                "성공적으로 다운로드 되었습니다.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
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
            filename+"_temp", ".json", outputDir
        )
        val fos = FileOutputStream(outputFile)
        runOnUiThread {
            binding.msg.text = "여행기록을 복호화중입니다. 잠시만 기다려 주세요."
        }


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
        val totalSize = file.length().toFloat()
        var readSize = 0L
        while (fis.read(d).also { b = it } != -1) {
            cos.write(d, 0, b)
            //println(count)
            readSize += b

            if (++count % 10 == 0) {
                runOnUiThread {
                    val progress = readSize / totalSize
                    val param = binding.progress.layoutParams as LinearLayout.LayoutParams
                    param.weight = progress
                    binding.progress.layoutParams = param
                    binding.percent.text = String.format("%.1f%%", progress * 100)
                }
                println(readSize)
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

    @Throws(IOException::class)
    private fun saveImage(bitmap: Bitmap, name: String): Long {
        val fos: OutputStream
        val resolver = contentResolver
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation);
        } else {
            contentValues.put(
                MediaStore.MediaColumns.DATA,
                relativeLocation + File.separator + "$name.jpg"
            );
        }
        val imageUri: Uri? =
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        fos = resolver.openOutputStream(imageUri!!)!!
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)

        fos.close()

        return imageUri.path!!.split("/").last().toLong()
    }
}
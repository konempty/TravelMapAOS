package kim.hanbin.gpstracker

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.JsonObject
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.Constraint
import id.zelory.compressor.constraint.FormatConstraint
import id.zelory.compressor.constraint.QualityConstraint
import id.zelory.compressor.constraint.ResolutionConstraint
import kim.hanbin.gpstracker.RetrofitFactory.Companion.retrofit
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.joda.time.format.DateTimeFormat
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.nio.charset.Charset
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec


class ShareProgressDialog(
    context: Context,
    name: String,
    datas: List<EventData>,
    share: Int,
    compress: Int,
    pswd: String?
) {

    private val builder: AlertDialog.Builder by lazy {
        AlertDialog.Builder(context).setView(view)
    }

    private val view: View by lazy {
        View.inflate(context, R.layout.dialog_share_progress, null)
    }


    private var dialog: AlertDialog? = null

    private val progressView: LinearLayout by lazy {
        view.findViewById(R.id.progress)
    }

    val job: Job

    val count = AtomicInteger(0)

    init {
        val dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH-mm-ss")
        val filename =
            "compressedFile" + dtf.print(Date().time)
        val outputDir: File = context.cacheDir // context being the Activity pointer

        var outputFile: File = File.createTempFile(
            filename, ".json", outputDir
        )
        val out = BufferedWriter(
            OutputStreamWriter(
                FileOutputStream(outputFile),
                Charset.forName("UTF8")
            )
        )
        out.write("[")
        show()
        var total = datas.size.toFloat()
        TMCompress.compress = compress
        var isComma = false

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                for (data in datas) {
                    if (data.eventNum!! in 1..2) {
                        total--
                        continue
                    }
                    val obj = JSONObject()
                    obj.put("eventNum", data.eventNum)
                    obj.put("time", dtf.print(data.time!!.time))

                    when (data.eventNum) {
                        3,5 -> {
                            obj.put("lat", data.lat)
                            obj.put("lng", data.lng)
                            if(data.eventNum == 5){
                                val file = FileUtil.from(context, data.uri)
                                val fis = FileInputStream(file)
                                val bmp = BitmapFactory.decodeStream(fis)
                                fis.close()
                                try {

                                    val compressedImageFile =
                                        Compressor.compress(context, file) {
                                            constraint(TMCompress(bmp))
                                        }
                                    val fis2 = FileInputStream(compressedImageFile)


                                    val bytes = fis2.readBytes()
                                    fis2.close()
                                    val encodedStr = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                    obj.put("data", encodedStr)
                                } catch (ce: CancellationException) {
                                    // You can ignore or log this exception
                                    return@launch
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        4 -> {
                            obj.put("trackingSpeed", data.trackingSpeed)
                        }
                    }
                    val tmpCount = count.incrementAndGet()
                    launch(Dispatchers.Main) {
                        val param = progressView.layoutParams as LinearLayout.LayoutParams
                        param.weight = tmpCount / total
                        progressView.layoutParams = param
                    }
                    if (isComma)
                        out.write(",\n")
                    isComma = true

                    out.write(obj.toString())
                    if (count.get() % 100 == 0)
                        out.flush()
                    println("total : $total now : ${count.get()}")


                }
                out.write("]")
                out.flush()
                out.close()
                val salt = ByteArray(32)
                if (share == 2) {
                    val sr = SecureRandom()
                    sr.nextBytes(salt)
                    val iv = "9362469649674046"

                    val spec = PBEKeySpec(pswd!!.toCharArray(), salt, 1000, 32 * 8)
                    val key: SecretKey =
                        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
                    val aes: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    aes.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv.toByteArray()))

                    val fis = FileInputStream(outputFile)
                    outputFile = File.createTempFile(
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
                }
                //copyFileToDownloads(context, outputFile, filename, outputFile.length(), share == 2)

                //보내는 코드

                val mapRequestBody = LinkedHashMap<String, RequestBody>()
                val arrBody: ArrayList<MultipartBody.Part> = arrayListOf()
                val requestBody =
                    outputFile.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                mapRequestBody["share"] =
                    share.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                if (share == 2)
                    mapRequestBody["salt"] = Base64.encodeToString(salt, Base64.NO_WRAP)
                        .toRequestBody("text/plain".toMediaTypeOrNull())
                mapRequestBody["trackingName"] =
                    name.toRequestBody("text/plain".toMediaTypeOrNull())
                val countingRequestBody =
                    CountingRequestBody(requestBody, object : CountingRequestBody.Listener {
                        override fun onRequestProgress(bytesWitten: Long, contentLength: Long) {
                            Log.d(
                                this.javaClass.simpleName,
                                "bytesWitten : $bytesWitten contentLength : $contentLength"
                            )
                        }
                    })

                val body =
                    MultipartBody.Part.createFormData("file", outputFile.name, countingRequestBody);
                arrBody.add(body);
                val res = retrofit.upload(mapRequestBody, arrBody)
                res.enqueue(object : Callback<JsonObject> {
                    override fun onResponse(
                        call: Call<JsonObject>?,
                        response: Response<JsonObject>
                    ) {
                        val json = response.body()!!
                        if (json.get("success").asBoolean) {

                            println(json.get("result").asInt)
                        } else {
                            println(json.get("result").asString)
                            Toast.makeText(
                                context,
                                "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                        Toast.makeText(
                            context,
                            "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                            Toast.LENGTH_LONG
                        ).show()

                    }
                })

                dismiss()

            } catch (ce: CancellationException) {
                // You can ignore or log this exception
                return@launch
            } catch (e: Exception) {
                // Here it's better to at least log the exception
                e.printStackTrace()
            }

        }

    }

    fun copyFileToDownloads(
        context: Context,
        downloadedFile: File,
        fileName: String,
        fileSize: Long,
        encrypted: Boolean
    ): Uri? {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    if (encrypted) "application/octet-stream" else "application/json"
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
    }

    fun show() {
        dialog = builder.create()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.setCancelable(false)
        dialog?.show()
        view.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            dismiss()
        }
    }


    fun dismiss() {
        job.cancel()
        dialog?.dismiss()
    }

    class TMCompress(bmp: Bitmap) : Constraint {
        companion object {
            val sizes = arrayOf(0, 1440, 960)
            val qualities = arrayOf(100, 95, 90)
            private lateinit var quality: QualityConstraint
            var compress: Int = 0
                set(value) {
                    quality = QualityConstraint(qualities[compress])
                    field = value
                }
            val format = FormatConstraint(Bitmap.CompressFormat.JPEG)
        }

        val width = bmp.width
        val height = bmp.height
        private val resolution: ResolutionConstraint = if (compress != 0) {
            if (width < height) {
                if (height > sizes[compress]) {
                    ResolutionConstraint(
                        (width.toDouble() * sizes[compress] / height).toInt(),
                        sizes[compress]
                    )
                } else {
                    ResolutionConstraint(width, height)
                }
            } else {
                if (width > sizes[compress]) {
                    ResolutionConstraint(
                        sizes[compress],
                        (height.toDouble() * sizes[compress] / width).toInt()
                    )
                } else {
                    ResolutionConstraint(width, height)
                }
            }
        } else {
            ResolutionConstraint(width, height)
        }

        override fun isSatisfied(imageFile: File): Boolean {
            return (compress == 0 || (resolution.isSatisfied(imageFile) && quality.isSatisfied(
                imageFile
            ))) && format.isSatisfied(imageFile)
        }

        override fun satisfy(imageFile: File): File {
            var destination = imageFile
            if (compress != 0) {
                if (!resolution.isSatisfied(imageFile)) {
                    destination = resolution.satisfy(imageFile)
                }
                if (!quality.isSatisfied(imageFile)) {
                    destination = quality.satisfy(imageFile)
                }
            }
            if (format.isSatisfied(imageFile)) {
                destination = format.satisfy(imageFile)
            }

            return destination
        }
    }

}
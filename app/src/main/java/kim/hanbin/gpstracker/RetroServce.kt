package kim.hanbin.gpstracker

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface RetroServce {
    @GET("registerUser.do")
    fun register(
        @Query("token") token: String,
        @Query("nickname") nickname: String
    ): Call<JsonObject>

    @GET("checkNickname.do")
    fun checkNickname(@Query("nickname") nickname: String): Call<String>

    @POST("login.do")
    fun login(@Query("token") token: String): Call<JsonObject>

    @GET("delete_user.do")
    fun delete_user(
    ): Call<JsonObject>

    @Multipart
    @GET("upload.do")
    fun upload(
        @PartMap map: Map<String, RequestBody>,
        @Part files: ArrayList<MultipartBody.Part>
    ): Call<JsonObject>

    @GET("deleteFile.do")
    fun deleteFile(): Call<JsonObject>

    @GET("fileDownload.do")
    fun fileDownload(@Query("trackingNum") trackingNum: Int): Call<ResponseBody>
}
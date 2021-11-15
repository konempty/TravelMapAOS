package kim.hanbin.gpstracker

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface RetroService {
    @GET("registerUser.do")
    fun register(
        @Query("token") token: String,
        @Query("nickname") nickname: String
    ): Call<JsonObject>

    @POST("checkNickname.do")
    fun checkNickname(@Query("nickname") nickname: String): Call<String>

    @POST("loginProcess.do")
    fun login(@Query("token") token: String): Call<JsonObject>

    @POST("logout.do")
    fun logout(): Call<String>

    @GET("loginCheck.do")
    fun loginCheck(): Call<String>


    @GET("deleteUser.do")
    fun deleteUser(
    ): Call<JsonObject>

    @Multipart
    @POST("upload.do")
    fun upload(
        @PartMap map: HashMap<String, RequestBody>,
        @Part files: ArrayList<MultipartBody.Part>
    ): Call<JsonObject>

    @GET("deleteFile.do")
    fun deleteFile(@Query("trackingNum") trackingNum: Long): Call<JsonObject>

    @GET("fileDownload.do")
    @Streaming
    fun fileDownload(@Query("trackingNum") trackingNum: Long): Call<ResponseBody>

    @POST("getUserId.do")
    fun getUserId(@Query("userNickname") nickname: String): Call<String>

    @POST("addFriendRequest.do")
    fun addFriendRequest(@Query("id") id: Long): Call<String>

    @POST("deleteFriend.do")
    fun deleteFriend(@Query("id") id: Long): Call<String>

    @GET("getFriendRequestedList.do")
    fun getFriendRequestedList(): Call<JsonObject>

    @GET("getFriendList.do")
    fun getFriendList(): Call<JsonObject>

    @GET("checkPermission.do")
    fun checkPermission(@Query("id") id: Long): Call<String>
}
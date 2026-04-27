package com.example.guidebook.repository

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

object CloudinaryHelper {

    // ⚠️ Cloudinary Dashboard에서 복사한 값으로 교체하세요
    private const val CLOUD_NAME  = "dulqmyj05"
    private const val API_KEY     = "135822862878268"
    private const val API_SECRET  = "ycPjrxl78C6cM7IJL-bU2f82IXY"

    private val client = OkHttpClient()

    /**
     * Bitmap → Cloudinary 업로드 → URL 반환
     */
    suspend fun uploadBitmap(bitmap: Bitmap, publicId: String): String =
        withContext(Dispatchers.IO) {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
            val bytes = baos.toByteArray()

            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val signature = generateSignature(publicId, timestamp)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", "$publicId.png",
                    bytes.toRequestBody("image/png".toMediaTypeOrNull())
                )
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("public_id", publicId)
                .addFormDataPart("signature", signature)
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: error("빈 응답")
            if (!response.isSuccessful) error("업로드 실패: $body")

            JSONObject(body).getString("secure_url")
        }

    /**
     * 이미지 Uri (파일) → Cloudinary 업로드
     */
    suspend fun uploadBytes(bytes: ByteArray, publicId: String): String =
        withContext(Dispatchers.IO) {
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val signature = generateSignature(publicId, timestamp)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", "$publicId.jpg",
                    bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("public_id", publicId)
                .addFormDataPart("signature", signature)
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: error("빈 응답")
            if (!response.isSuccessful) error("업로드 실패: $body")

            JSONObject(body).getString("secure_url")
        }

    private fun generateSignature(publicId: String, timestamp: String): String {
        val toSign = "public_id=$publicId&timestamp=$timestamp$API_SECRET"
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(toSign.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

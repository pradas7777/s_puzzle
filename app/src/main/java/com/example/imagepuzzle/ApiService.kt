package com.example.imagepuzzle
//서버에서 사진 주고받을때 쓰는 통로

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    // 이미지를 서버로 보내고(POST), 결과로 이미지 파일(ResponseBody)을 받는 함수입니다.
    @Multipart
    @POST("/transform")
    fun transformImage(
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>
}
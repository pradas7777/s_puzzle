package com.example.imagepuzzle

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

// [중요] 최신 OkHttp4 방식의 import입니다.
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null

    // 코랩 서버 주소 (ngrok에서 뜬 최신 주소로 바꾸세요!)
    private val BASE_URL = "https://lita-shiest-inconveniently.ngrok-free.dev/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ivPreview = findViewById<ImageView>(R.id.ivPreview)
        val btnPick = findViewById<Button>(R.id.btnPickImage)
        val btnTransform = findViewById<Button>(R.id.btnTransform)

        val pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    selectedImageUri = it
                    ivPreview.setImageURI(it)
                    btnTransform.visibility = View.VISIBLE
                }
            }

        btnPick.setOnClickListener { pickImageLauncher.launch("image/*") }

        btnTransform.setOnClickListener {
            selectedImageUri?.let { makeFiveMasterpieces(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!Settings.canDrawOverlays(this)) {
            val intent =
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        } else {
            startPuzzleService()
        }
    }

    private fun startPuzzleService() {
        val serviceIntent = Intent(this, LockScreenService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
        else startService(serviceIntent)
    }

    private fun makeFiveMasterpieces(uri: Uri) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("AI 가 사진을 편집하는 중입니다(1/1)...")
            setCancelable(false)
            show()
        }

        // 1. [중요] 대기 시간을 100초로 넉넉하게 설정한 통신 일꾼(OkHttpClient)을 만듭니다.
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(100, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(100, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(100, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        // 2. 위에서 만든 일꾼을 Retrofit에 연결합니다.
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // 이 줄이 반드시 들어가야 합니다!
            .build()

        val service = retrofit.create(ApiService::class.java)
        var successCount = 0

        for (i in 1..1) {
            val file = File(cacheDir, "temp_image_$i.jpg")
            copyUriToFile(uri, file)

            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            service.transformImage(body).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        saveImageToInternalStorage(response.body()?.byteStream(), i)
                        successCount++

                        // 메인 스레드에서 UI를 업데이트합니다.
                        runOnUiThread {
                            progressDialog.setMessage("AI 화가가 명화를 그리는 중입니다 ($successCount/1)...")
                            if (successCount == 1) {
                                progressDialog.dismiss()
                                Toast.makeText(
                                    this@MainActivity,
                                    "1장의 퍼즐이 완성되었습니다!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent =
                                    Intent(this@MainActivity, LockScreenActivity::class.java)
                                startActivity(intent)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    // 여기서 왜 실패했는지 로그를 찍어봅니다. (아마 Timeout 에러가 찍혔을 거예요)
                    Log.e("AI_ERROR", "변환 실패 ($i): ${t.message}")
                }
            })
        }
    }

    private fun copyUriToFile(uri: Uri, file: File) {
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
    }

    private fun saveImageToInternalStorage(inputStream: InputStream?, index: Int): File? {
        return try {
            // 1. 저장할 위치와 파일명을 정합니다.
            val file = File(filesDir, "puzzle_image_$index.jpg")

            // 2. 서버에서 온 데이터를 한 땀 한 땀 파일로 옮겨 담습니다.
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(4 * 1024) // 4KB씩 나눠서 안전하게 복사
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush() // 찌꺼기 없이 꽉꽉 채워 저장
                }
            }

            Log.d("PUZZLE_SUCCESS", "${index}번 명화가 안전하게 저장되었습니다: ${file.length()} bytes")
            file
        } catch (e: Exception) {
            Log.e("PUZZLE_ERROR", "${index}번 저장 중 오류 발생: ${e.message}")
            null
        }
    }
}
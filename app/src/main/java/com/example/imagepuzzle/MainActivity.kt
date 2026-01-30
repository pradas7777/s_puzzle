package com.example.imagepuzzle

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) //
    }

    // 화면이 다시 보일 때마다 권한을 체크합니다.
    override fun onResume() {
        super.onResume()

        
        if (!Settings.canDrawOverlays(this)) {
            // 권한이 없으면 설정 화면으로 보냅니다.
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "퍼즐 작동을 위해 권한을 허용해주세요!", Toast.LENGTH_SHORT).show()
        } else {
            // 권한이 확인되면 서비스를 켜고, 바로 퍼즐 화면(LockScreenActivity)으로 점프!
            startPuzzleService()

            val intent = Intent(this, LockScreenActivity::class.java)
            startActivity(intent)
            finish() // 이제 메인 화면은 필요 없으니 끕니다.
        }
    }

    private fun startPuzzleService() {
        val serviceIntent = Intent(this, LockScreenService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}
package com.example.imagepuzzle

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class LockScreenService : Service() {
    private var screenReceiver: ScreenReceiver? = null

    // 1. 서비스가 시작될 때 실행됩니다.
    override fun onCreate() {
        super.onCreate()
        // 2. 여기서 '화면 꺼짐' 신호를 듣는 감시자를 등록합니다.
        // 이제 앱이 꺼져있어도 이 서비스가 살아있는 한 계속 감시합니다.
        screenReceiver = ScreenReceiver()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

        startMyForeground()
    }

    private fun startMyForeground() {
        val channelId = "LOCK_SERVICE"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, "퍼즐 서비스", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("치매 예방 퍼즐 작동 중")
            .setContentText("두뇌 건강을 위해 항상 대기 중입니다.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        // 4. 이 서비스를 '중요 일꾼(Foreground)'으로 격상시킵니다.
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 서비스가 종료될 때 감시자도 해제합니다.
        unregisterReceiver(screenReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "LOCK_CHANNEL", "Lock Screen Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
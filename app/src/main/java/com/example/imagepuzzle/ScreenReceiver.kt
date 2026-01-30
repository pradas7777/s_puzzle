package com.example.imagepuzzle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 화면이 꺼지는(SCREEN_OFF) 신호를 받으면 퍼즐 화면을 띄웁니다.
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            val lockIntent = Intent(context, LockScreenActivity::class.java)
            // 화면을 띄울 때 필요한 필수 설정(깃발)들입니다.
            lockIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
            context.startActivity(lockIntent)
        }
    }
}
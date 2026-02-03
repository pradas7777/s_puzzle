package com.example.imagepuzzle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import android.app.AlertDialog
import android.view.LayoutInflater

class LockScreenActivity : AppCompatActivity() {

    private var firstSelectedView: ImageView? = null // 첫 번째로 터치한 조각을 기억합니다.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_screen)
        setupLockScreenFlags()

        // 1. [다시 섞기] 버튼: 퍼즐을 새로 불러오고 섞습니다.
        findViewById<Button>(R.id.btnReset).setOnClickListener {
            loadPuzzle()
            Toast.makeText(this, "퍼즐을 다시 섞었습니다!", Toast.LENGTH_SHORT).show()
        }

        // 2. [잠금해제] 버튼: 앱을 종료하여 잠금을 풉니다.
        findViewById<Button>(R.id.btnUnlock).setOnClickListener {
            finish()
        }

        // 화면이 준비되면 퍼즐을 그립니다.
        loadPuzzle()
    }

    private fun loadPuzzle() {
        val gridLayout = findViewById<GridLayout>(R.id.puzzleGrid)
        val bitmap = loadAIGeneratedImage()

        if (bitmap != null) {
            setupPuzzle(bitmap, gridLayout)
        }
    }

    private fun loadAIGeneratedImage(): Bitmap? {
        val randomIndex = (1..1).random()
        val file = File(filesDir, "puzzle_image_$randomIndex.jpg")

        return if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            // 사진이 없을 때를 대비한 기본 이미지
            BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_background)
        }
    }

    private fun setupPuzzle(bitmap: Bitmap, gridLayout: GridLayout) {
        val gridSize = 3
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 900, 900, true)
        val pieceSize = 300 // 900 / 3

        // 조각 정보(비트맵 + 정답 인덱스)를 담을 리스트
        val puzzleData = mutableListOf<Pair<Bitmap, Int>>()

        // 1. 이미지를 9조각으로 자르며 정답 번호(0~8)를 매깁니다.
        for (i in 0 until gridSize * gridSize) {
            val x = (i % gridSize) * pieceSize
            val y = (i / gridSize) * pieceSize
            val piece = Bitmap.createBitmap(scaledBitmap, x, y, pieceSize, pieceSize)
            puzzleData.add(Pair(piece, i))
        }

        // 2. 조각 섞기
        puzzleData.shuffle()

        // 3. 그리드 레이아웃에 배치
        gridLayout.removeAllViews()
        for (data in puzzleData) {
            val imageView = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = pieceSize
                    height = pieceSize
                }
                setImageBitmap(data.first) // 조각 그림 설정
                tag = data.second // 정답 번호를 태그에 숨겨둡니다.
                setPadding(2, 2, 2, 2)
                scaleType = ImageView.ScaleType.FIT_XY

                // 조각 클릭 이벤트 (움직이기 로직)
                setOnClickListener { onPieceClick(this) }
            }
            gridLayout.addView(imageView)
        }
    }

    private fun onPieceClick(clickedView: ImageView) {
        if (firstSelectedView == null) {
            // 첫 번째 조각 선택: 약간 투명하게 해서 선택됨을 표시합니다.
            firstSelectedView = clickedView
            clickedView.alpha = 0.5f
        } else {
            // 두 번째 조각 선택: 두 조각의 그림과 태그(정답 번호)를 서로 바꿉니다.
            val firstView = firstSelectedView!!

            // 그림 바꾸기
            val tempBitmap = (firstView.drawable as android.graphics.drawable.BitmapDrawable).bitmap
            val clickedBitmap = (clickedView.drawable as android.graphics.drawable.BitmapDrawable).bitmap
            firstView.setImageBitmap(clickedBitmap)
            clickedView.setImageBitmap(tempBitmap)

            // 태그(번호) 바꾸기
            val tempTag = firstView.tag
            firstView.tag = clickedView.tag
            clickedView.tag = tempTag

            // 선택 해제 및 투명도 복구
            firstView.alpha = 1.0f
            firstSelectedView = null

            // 퍼즐을 다 맞췄는지 확인합니다.
            checkVictory()
        }
    }

    private fun checkVictory() {
        val gridLayout = findViewById<GridLayout>(R.id.puzzleGrid)
        var isAllCorrect = true

        for (i in 0 until gridLayout.childCount) {
            if (gridLayout.getChildAt(i).tag != i) {
                isAllCorrect = false
                break
            }
        }

        // 모든 조각이 정답 위치(0~8)에 있으면 성공 팝업을 띄웁니다.
        if (isAllCorrect) {
            showSuccessDialog()
        }
    }

    // [추가] 멋진 성공 축하 팝업창!
    private fun showSuccessDialog() {
        // 1. 우리가 만든 축하 레이아웃을 불러옵니다.
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_congratulations, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // 팝업 밖을 눌러도 안 꺼지게 합니다.
            .create()

        // 2. "다음 퍼즐 계속 풀기" 버튼 연결 (ID: btnNext)
        dialogView.findViewById<Button>(R.id.btnNext)?.setOnClickListener {
            loadPuzzle() // 새로운 명화를 무작위로 불러와서 다시 시작합니다.
            dialog.dismiss()
            Toast.makeText(this, "새로운 명화를 가져왔습니다!", Toast.LENGTH_SHORT).show()
        }

        // 3. "그만 풀기(잠금해제)" 버튼 연결 (ID: btnStop)
        dialogView.findViewById<Button>(R.id.btnExit)?.setOnClickListener {
            dialog.dismiss()
            finish() // 앱을 종료하고 잠금을 해제합니다.
        }

        dialog.show()
    }


    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
    }
}
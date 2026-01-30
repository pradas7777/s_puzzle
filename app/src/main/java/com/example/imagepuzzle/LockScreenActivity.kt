package com.example.imagepuzzle

import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.Context
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

class LockScreenActivity : AppCompatActivity() {

    private var selectedImageView: ImageView? = null
    private val puzzlePieces = mutableListOf<Bitmap>()
    private var originalBitmap: Bitmap? = null

    // 1. 여러 장의 사진을 준비합니다. (res/drawable 폴더에 사진들을 추가한 뒤 여기에 이름을 적어주세요)
    private val imageResList = listOf(
        R.drawable.puzzle_image,
        R.drawable.puzzle_image2
    )
    private var currentImageIndex = 0 // 2. 현재 몇 번째 사진인지 기억하는 변수입니다.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupLockScreenFlags()
        setContentView(R.layout.activity_lock_screen)

        // 3. 첫 번째 퍼즐을 시작합니다.
        loadPuzzle(currentImageIndex)

        findViewById<Button>(R.id.btnReset).setOnClickListener {
            originalBitmap?.let { setupPuzzleGrid(it) }
            Toast.makeText(this, "퍼즐을 다시 섞었습니다!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnUnlock).setOnClickListener { finish() }
    }

    // 4. 특정 순서의 사진을 불러와서 퍼즐을 세팅하는 함수입니다.
    private fun loadPuzzle(index: Int) {
        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
        originalBitmap = BitmapFactory.decodeResource(resources, imageResList[index], options)
        originalBitmap?.let { setupPuzzleGrid(it) }
    }

    private fun setupPuzzleGrid(bitmap: Bitmap) {
        val grid = findViewById<GridLayout>(R.id.puzzleGrid)
        grid.removeAllViews()
        puzzlePieces.clear()

        val pieceWidth = bitmap.width / 3
        val pieceHeight = bitmap.height / 3

        for (i in 0 until 9) {
            val row = i / 3
            val col = i % 3
            val piece = Bitmap.createBitmap(
                bitmap,
                col * pieceWidth,
                row * pieceHeight,
                pieceWidth,
                pieceHeight
            )
            puzzlePieces.add(piece)
        }

        val shuffledIndices = (0 until 9).shuffled()
        for (index in shuffledIndices) {
            val imageView = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                setPadding(1, 1, 1, 1)
                setImageBitmap(puzzlePieces[index])
                tag = index
                setOnClickListener { handlePieceClick(this) }
            }
            grid.addView(imageView)
        }
    }

    private fun handlePieceClick(clickedView: ImageView) {
        if (selectedImageView == null) {
            selectedImageView = clickedView
            clickedView.alpha = 0.5f
        } else {
            val firstBitmap =
                (selectedImageView?.drawable as android.graphics.drawable.BitmapDrawable).bitmap
            val secondBitmap =
                (clickedView.drawable as android.graphics.drawable.BitmapDrawable).bitmap

            val firstTag = selectedImageView?.tag as Int
            val secondTag = clickedView.tag as Int

            clickedView.setImageBitmap(firstBitmap)
            clickedView.tag = firstTag

            selectedImageView?.setImageBitmap(secondBitmap)
            selectedImageView?.tag = secondTag

            selectedImageView?.alpha = 1.0f
            selectedImageView = null

            checkSuccess()
        }
    }

    private fun checkSuccess() {
        val grid = findViewById<GridLayout>(R.id.puzzleGrid)
        for (i in 0 until grid.childCount) {
            val piece = grid.getChildAt(i) as ImageView
            if (piece.tag != i) return
        }

        // 1. 성공 시 조각 사이의 간격을 없애서 완성 사진 보여주기
        for (i in 0 until grid.childCount) {
            grid.getChildAt(i).setPadding(0, 0, 0, 0)
        }

        // 2. 커스텀 다이얼로그(팝업창) 생성
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_congratulations, null) // 우리가 만든 디자인 연결
        builder.setView(view)

        val ivSuccessPhoto = view.findViewById<ImageView>(R.id.ivSuccessPhoto)
        ivSuccessPhoto.setImageBitmap(originalBitmap)

        val dialog = builder.create()
        dialog.setCancelable(false) // 팝업 밖을 눌러도 안 꺼지게 설정

        // 3. 팝업창 안의 '다음 문제' 버튼 연결
        view.findViewById<Button>(R.id.btnNext).setOnClickListener {
            currentImageIndex = (currentImageIndex + 1) % imageResList.size
            loadPuzzle(currentImageIndex)
            dialog.dismiss() // 팝업 닫기
        }

        // 4. 팝업창 안의 '종료' 버튼 연결
        view.findViewById<Button>(R.id.btnExit).setOnClickListener {
            dialog.dismiss()
            finish() // 액티비티 종료
        }

        dialog.show()
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // 1. 잠금화면 위로 표시, 화면 켜기 설정
            setShowWhenLocked(true)

            // 2. 키가드(기본 잠금)를 무시하고 우리 앱을 먼저 보여줍니다.
            val keyguardManager =
                getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager

        } else {
            // 구버전 대응
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }
}
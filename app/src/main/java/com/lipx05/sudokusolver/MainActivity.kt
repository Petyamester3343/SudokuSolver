package com.lipx05.sudokusolver

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var gameBoard: SudokuBoard
    private lateinit var gameBoardSolver: Solver
    private lateinit var solveBtn: Button
    private lateinit var toggleCamBtn: Button
    private lateinit var captureBtn: Button
    private lateinit var camExecutor: ExecutorService
    private lateinit var imgCapture: ImageCapture
    private var isCamActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        gameBoard = findViewById(R.id.SudokuBoard)
        gameBoardSolver = gameBoard.getSolver()
        solveBtn = findViewById(R.id.solve_btn)
        toggleCamBtn = findViewById(R.id.toggle_cam)
        captureBtn = findViewById(R.id.captureBtn)

        camExecutor = Executors.newSingleThreadExecutor()

        toggleCamBtn.setOnClickListener {
            toggleCam()
        }

        captureBtn.setOnClickListener {
            captureImg()
            Toast.makeText(this, "Photo taken", Toast.LENGTH_SHORT).show()
        }

        startCam()
    }

    private fun toggleCam() {
        isCamActive = !isCamActive
        findViewById<PreviewView>(R.id.camPreview).visibility =
            if (isCamActive) View.VISIBLE else View.GONE

        solveBtn.visibility = if (isCamActive) View.GONE else View.VISIBLE

        captureBtn.visibility = if(isCamActive) View.VISIBLE else View.GONE

        toggleCamBtn.text = if (isCamActive) "Close Camera" else "Open Camera"
    }

    private fun startCam() {
        val camProvFut = ProcessCameraProvider.getInstance(this)
        camProvFut.addListener({
            val camProv = camProvFut.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = findViewById<PreviewView>(R.id.camPreview)
                    .surfaceProvider
            }
            imgCapture = ImageCapture.Builder().build()
            val camSelector = CameraSelector.DEFAULT_BACK_CAMERA
            camProv.bindToLifecycle(this, camSelector, preview, imgCapture)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImg() {
        val imageCapture = imgCapture
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processImgForOCR(image)
                    image.close()
                }

                override fun onError(e: ImageCaptureException) {
                    Log.e("CameraX", "Image capture failed: ${e.message}", e)
                }
            }
        )
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImgForOCR(img: ImageProxy) {
        val mediaImg = img.image ?: return
        val inImg = InputImage.fromMediaImage(mediaImg, img.imageInfo.rotationDegrees)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inImg)
            .addOnSuccessListener { visionText ->
                val parseGrid = parseSudokuGrid(visionText.text)
                updateSolverGrid(parseGrid)
                gameBoard.invalidate()
                toggleCam()
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Text recognition failed: ${e.message}", e)
            }
    }

    private fun parseSudokuGrid(recognizedTxt: String): Array<IntArray> {
        val grid = Array(9) {IntArray(9)}
        val lines = recognizedTxt.split("\n")

        for ((lineIndex, r) in (0..8).withIndex()) {
            val numbers = lines.getOrNull(lineIndex)?.split(" ")?.mapNotNull {
                it.toIntOrNull()
            }
            if(numbers != null && numbers.size == 9) {
                for(c in 0..8) {
                    grid[r][c] = numbers[c]
                }
            }
        }
        return grid
    }

    private fun updateSolverGrid(grid: Array<IntArray>) {
        for(r in 0..8) {
            for (c in 0..8) {
                gameBoardSolver.getBoard()[r][c] = grid[r][c]
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        camExecutor.shutdown()
    }

    fun btn1Press(v: View) {
        gameBoardSolver.setNumberPos(1)
        gameBoard.invalidate()
    }

    fun btn2Press(v: View) {
        gameBoardSolver.setNumberPos(2)
        gameBoard.invalidate()
    }

    fun btn3Press(v: View) {
        gameBoardSolver.setNumberPos(3)
        gameBoard.invalidate()
    }

    fun btn4Press(v: View) {
        gameBoardSolver.setNumberPos(4)
        gameBoard.invalidate()
    }

    fun btn5Press(v: View) {
        gameBoardSolver.setNumberPos(5)
        gameBoard.invalidate()
    }

    fun btn6Press(v: View) {
        gameBoardSolver.setNumberPos(6)
        gameBoard.invalidate()
    }

    fun btn7Press(v: View) {
        gameBoardSolver.setNumberPos(7)
        gameBoard.invalidate()
    }

    fun btn8Press(v: View) {
        gameBoardSolver.setNumberPos(8)
        gameBoard.invalidate()
    }

    fun btn9Press(v: View) {
        gameBoardSolver.setNumberPos(9)
        gameBoard.invalidate()
    }

    fun solve(v: View) {
        if (solveBtn.text.toString() == getString(R.string.solve_str)) {
            solveBtn.text = getString(R.string.solve_clear_str)
            gameBoardSolver.getEmptyBoxIndexes()
            val gameBoardThread = SolveBoardThread()
            Thread(gameBoardThread).start()
            gameBoard.invalidate()
        }
        else {
            solveBtn.text = getString(R.string.solve_str)
            gameBoardSolver.resetBoard()
            gameBoard.invalidate()
        }
    }

    inner class SolveBoardThread: Runnable {
        override fun run() {
            gameBoardSolver.solve(gameBoard)
        }
    }
}
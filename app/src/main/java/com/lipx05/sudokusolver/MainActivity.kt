package com.lipx05.sudokusolver

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var gameBoard: SudokuBoard
    private lateinit var gameBoardSolver: Solver
    private lateinit var solveBtn: Button
    private lateinit var toggleCamBtn: Button
    private lateinit var captureBtn: Button
    private lateinit var camManager: CamManager

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

        val previewView = findViewById<PreviewView>(R.id.camPreview)

        val ocrProcessor = OCRProcessor(
            onSuccess = { recognizedText ->
                Toast.makeText(this, recognizedText, Toast.LENGTH_SHORT).show()
                val parsedGrid = parseSudokuGrid(recognizedText)
                if(parsedGrid.isEmpty()) {
                    Log.e("OCR", "Parsed grid is empty or not valid!");
                } else {
                    Toast.makeText(
                        this, parsedGrid.joinToString(","), Toast.LENGTH_SHORT
                    ).show()
                    Log.d("OCR", "Parsed grid valid ($parsedGrid)")
                }
                updateSolverGrid(parsedGrid)
                gameBoard.invalidate()
                camManager.toggleCam(solveBtn, captureBtn, toggleCamBtn, this)
            },
            onFailure = { err ->
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
            }
        )

        camManager = CamManager(
            ctx = this,
            previewView = previewView,
            ocrProcessor = ocrProcessor
        )

        toggleCamBtn.setOnClickListener {
            camManager.toggleCam(solveBtn, captureBtn, toggleCamBtn, this)
        }

        captureBtn.setOnClickListener {
            camManager.captureImg(
                onSuccess = {
                    Toast.makeText(
                        this,
                        getString(R.string.photo_taken_str),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { err ->
                    Toast.makeText(
                        this,
                        err,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        requestCamPerm()
        camManager.startCam()
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
        for (row in grid) {
            Log.d("Grid", row.joinToString(", "))
        }

        for(r in 0..8) {
            for (c in 0..8) {
                gameBoardSolver.getBoard()[r][c] = grid[r][c]
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        camManager.shutdown()
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

    private fun requestCamPerm() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAM_PERM
            )
        }
    }

    inner class SolveBoardThread: Runnable {
        override fun run() {
            gameBoardSolver.solve(gameBoard)
        }
    }

    companion object {
        private const val REQUEST_CAM_PERM = 1001
    }
}
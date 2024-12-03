package com.lipx05.sudokusolver

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.ceil

class SudokuBoard(ctx: Context, attrs: AttributeSet? = null): View(ctx, attrs) {
    private val boardColor: Int
    private val cellFillColor: Int
    private val cellsHighlightColor: Int
    private val letterColor: Int
    private val letterSolveColor: Int

    private val boardColorPaint = Paint()
    private val cellFillColorPaint = Paint()
    private val cellsHighlightColorPaint = Paint()
    private val letterColorPaint = Paint()
    private val letterSolveColorPaint = Paint()

    private val letterColorPaintBounds = Rect()

    private var cellSize: Int = 0

    private val solver = Solver()

    init {
        val a: TypedArray = ctx.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SudokuBoard,
            0,
            0
        )

        try {
            boardColor = a.getInteger(
                R.styleable.SudokuBoard_boardColor,
                0
            )
            cellFillColor = a.getInteger(
                R.styleable.SudokuBoard_cellFillColor,
                0
            )
            cellsHighlightColor = a.getInteger(
                R.styleable.SudokuBoard_cellsHighlightColor,
                0
            )
            letterColor = a.getInteger(
                R.styleable.SudokuBoard_letterColor,
                0
            )
            letterSolveColor = a.getInteger(
                R.styleable.SudokuBoard_letterSolveColor,
                0
            )
        } finally {
            a.recycle()
        }
    }

    override fun onMeasure(w: Int, h: Int) {
        super.onMeasure(w, h)

        val dim = minOf(measuredWidth, measuredHeight)
        cellSize = dim / 9

        setMeasuredDimension(dim, dim)
    }

    override fun onDraw(cv: Canvas) {
        boardColorPaint.style = Paint.Style.STROKE
        boardColorPaint.strokeWidth = 16f
        boardColorPaint.color = boardColor
        boardColorPaint.isAntiAlias = true

        cellFillColorPaint.style = Paint.Style.FILL
        cellFillColorPaint.isAntiAlias = true
        cellFillColorPaint.color = cellFillColor

        cellsHighlightColorPaint.style = Paint.Style.FILL
        cellsHighlightColorPaint.isAntiAlias = true
        cellsHighlightColorPaint.color = cellsHighlightColor

        letterColorPaint.style = Paint.Style.FILL
        letterColorPaint.isAntiAlias = true
        letterColorPaint.color = letterColor

        letterSolveColorPaint.style = Paint.Style.FILL
        letterSolveColorPaint.isAntiAlias = true
        letterSolveColorPaint.color = letterSolveColor

        colorCell(cv, solver.getSelectedRow() - 1, solver.getSelectedCol() - 1)
        cv.drawRect(0f, 0f, width.toFloat(), height.toFloat(), boardColorPaint)
        drawBoard(cv)
        drawNumbers(cv)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val isValid: Boolean
        val x = event.x
        val y = event.y

        val action = event.action

        if (action == MotionEvent.ACTION_DOWN) {
            solver.setSelectedRow(ceil((y+1)/cellSize).toInt())
            solver.setSelectedCol(ceil((x+1)/cellSize).toInt())
            isValid = true
        } else {
            isValid = false
        }

        return isValid
    }

    private fun drawNumbers(cv: Canvas) {
        letterColorPaint.textSize = cellSize.toFloat()

        for (r in 0..<9) {
            for (c in 0..<9) {
                val board = solver.getBoard()
                if (board[r][c] != 0) {
                    val txt = solver.getBoard()[r][c].toString()

                    letterColorPaint.getTextBounds(
                        txt, 0, txt.length, letterColorPaintBounds
                    )
                    val w = letterColorPaint.measureText(txt)
                    val h = letterColorPaintBounds.height().toFloat()

                    cv.drawText(
                        txt,
                        (c * cellSize) + ((cellSize - w) / 2),
                        (r * cellSize + cellSize) - ((cellSize - h) / 2),
                        letterColorPaint
                    )
                }
            }
        }

        letterColorPaint.color = letterSolveColor

        solver.getEmptyBoxIndex().forEach { letter ->
            val r: Int = letter[0] as Int
            val c: Int = letter[1] as Int

            if (solver.getBoard()[r][c] != 0) {
                val txt = solver.getBoard()[r][c].toString()

                letterColorPaint.getTextBounds(
                    txt,
                    0,
                    txt.length,
                    letterColorPaintBounds
                )
                val w = letterColorPaint.measureText(txt)
                val h = letterColorPaintBounds.height().toFloat()

                cv.drawText(
                    txt,
                    (c * cellSize) + ((cellSize - w) / 2),
                    (r * cellSize + cellSize) - ((cellSize - h) / 2),
                    letterColorPaint
                )
            }
        }
    }

    private fun colorCell(cv: Canvas, r: Int, c: Int) {
        if (solver.getSelectedCol() != -1 && solver.getSelectedRow() != -1) {
            // paint the row and column
            cv.drawRect(
                (c * cellSize).toFloat(),
                0f,
                ((c + 1) * cellSize).toFloat(),
                cellSize * 9f,
                cellsHighlightColorPaint
            )
            cv.drawRect(
                0f,
                (r * cellSize).toFloat(),
                cellSize * 9f,
                ((r + 1) * cellSize).toFloat(),
                cellsHighlightColorPaint
            )

            if (r in 0..8 && c in 0..8) { // Validate the cell indices
                // Calculate the starting row and column of the 3x3 box
                val bSRow = (r / 3) * 3
                val bSCol = (c / 3) * 3

                // DEBUG: Log selected cell and box start for verification
                Log.d("Sudoku", "Selected Cell: ($r, $c)")
                Log.d("Sudoku", "Calculated Box Start: ($bSRow, $bSCol)")

                // Draw the 3x3 box
                val left = bSCol * cellSize.toFloat() - 1
                val top = bSRow * cellSize.toFloat() - 1
                val right = left + (3 * cellSize.toFloat()) - 1
                val bottom = top + (3 * cellSize.toFloat()) - 1

                cv.drawRect(left, top, right, bottom, cellFillColorPaint)

                // Highlight the selected cell
                val cellLeft = c * cellSize.toFloat()
                val cellTop = r * cellSize.toFloat()
                val cellRight = cellLeft + cellSize.toFloat()
                val cellBottom = cellTop + cellSize.toFloat()

                cv.drawRect(cellLeft, cellTop, cellRight, cellBottom, cellsHighlightColorPaint)
            } else {
                Log.w("Sudoku", "Invalid cell selection: ($r, $c)")
            }
        }

        //drawDebugGrid(cv)

        invalidate()
    }

    private fun drawThickLine() {
        boardColorPaint.style = Paint.Style.STROKE
        boardColorPaint.strokeWidth = 16f
        boardColorPaint.color = boardColor
    }

    private fun drawThinLine() {
        boardColorPaint.style = Paint.Style.STROKE
        boardColorPaint.strokeWidth = 4f
        boardColorPaint.color = boardColor
    }

    private fun drawBoard(cv: Canvas) {
        for (c: Int in 0..9) {
            if (c % 3 == 0) {
                drawThickLine()
            } else {
                drawThinLine()
            }
            cv.drawLine(
                (cellSize * c).toFloat(),
                0f,
                (cellSize * c).toFloat(),
                width.toFloat(),
                boardColorPaint
            )
        }

        for (r in 0..9) {
            if (r % 3 == 0) {
                drawThickLine()
            } else {
                drawThinLine()
            }
            cv.drawLine(
                0f,
                (cellSize * r).toFloat(),
                width.toFloat(),
                (cellSize * r).toFloat(),
                boardColorPaint
            )
        }
    }

    fun getSolver(): Solver {
        return solver
    }

    /*private fun drawDebugGrid(cv: Canvas) {
        val debugPaint = Paint().apply {
            color = Color.RED
            textSize = 36f
            isAntiAlias = true
        }
        for(r in 0..8) {
            for (c in 0..8) {
                val x = c * cellSize.toFloat()
                val y = r * cellSize.toFloat()
                cv.drawText("($r, $c)", x+20, y+65, debugPaint)
            }
        }
    }*/
}
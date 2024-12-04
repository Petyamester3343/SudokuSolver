package com.lipx05.sudokusolver

class Generator {
    private val size = 9
    private val boxSize = 3
    private val board = Array(size) {IntArray(size)}
    private val random = java.util.Random()

    fun generate(diff: Difficulty = Difficulty.MEDIUM): Array<IntArray> {
        clearBoard()
        fillBoard(0,0)
        removeCells(diff.emptyCells)
        return board.map { it.clone() }.toTypedArray()
    }

    private fun clearBoard() {
        for(i in 0 until size) {
            for (j in 0 until size) {
                board[i][j] = 0
            }
        }
    }

    private fun fillBoard(row: Int, col: Int): Boolean {
        if (col == size) {
            return fillBoard(row + 1, 0)
        }

        if (row == size) {
            return true
        }

        if(board[row][col] != 0) {
            return fillBoard(row, col + 1)
        }

        val numbers = (1..9).shuffled()

        for (number in numbers) {
            if (isValid(row, col, number)) {
                board[row][col] = number
                if(fillBoard(row, col+1)) {
                    return true
                }
                board[row][col] = 0
            }
        }

        return false
    }

    private fun isValid(row: Int, col: Int, num: Int): Boolean {
        for (y in 0 until size) {
            if(board[row][y] == num) return false
        }

        for (x in 0 until size) {
            if(board[x][col] == num) return false
        }

        val bRow = row - row % boxSize
        val bCol = col - col % boxSize

        for (i in 0 until boxSize) {
            for (j in 0 until boxSize) {
                if(board[bRow+i][bCol+j] == num) return false
            }
        }

        return true
    }

    private fun removeCells(toRmv: Int) {
        var c = toRmv
        while (c > 0) {
            val row = random.nextInt(size)
            val col = random.nextInt(size)

            if(board[row][col] != 0) {
                board[row][col] = 0
                c--
            }
        }
    }
}
package com.lipx05.sudokusolver

class Solver {
    private var board = Array(9) { _ -> IntArray(9) { _ -> 0 } }
    private var emptyBoxIndex: ArrayList<ArrayList<Any>> = ArrayList()

    private var selectedRow = -1
    private var selectedCol = -1

    fun getEmptyBoxIndex(): ArrayList<ArrayList<Any>> {
        return this.emptyBoxIndex
    }

    fun getEmptyBoxIndexes() {
        for(r in 0..8) {
            for (c in 0..8) {
                if (this.board[r][c] == 0) {
                    this.emptyBoxIndex.add(ArrayList())
                    this.emptyBoxIndex[this.emptyBoxIndex.size - 1].add(r)
                    this.emptyBoxIndex[this.emptyBoxIndex.size - 1].add(c)
                }
            }
        }
    }

    private fun check(r: Int, c: Int): Boolean {
        if (this.board[r][c] > 0) {
            for (i in 0..8) {
                if (this.board[i][c] == this.board[r][c] && r != i) return false
                if (this.board[r][i] == this.board[r][c] && c != i) return false
            }

            val boxRow: Int = r / 3
            val boxCol: Int = c / 3

            for (row in boxRow * 3..<boxRow * 3 + 3) {
                for (col in boxCol * 3..<boxCol * 3 + 3) {
                    if (this.board[r][c] == this.board[row][col] && row != r && col != c)
                        return false
                }
            }
        }

        return true
    }

    fun solve(display: SudokuBoard): Boolean {
        var row = -1
        var col = -1

        for (r in 0..8) {
            for (c in 0..8) {
                if (this.board[r][c] == 0) {
                    row = r
                    col = c
                    break
                }
            }
        }

        if (row == -1 || col == -1) {
            return true
        }

        for (i in 1..9) {
            this.board[row][col] = i
            display.invalidate()

            if (check(row, col)) {
                if (solve(display)) {
                    return true
                }
            }

            this.board[row][col] = 0
        }

        return false
    }

    fun resetBoard() {
        for(i in 0..8) {
            for (j in 0..8) {
                this.board[i][j] = 0
            }
        }

        this.emptyBoxIndex = ArrayList()
    }

    fun setNumberPos(num: Int) {
        if (this.selectedRow != -1 && this.selectedCol != -1) {
            if (this.board[this.selectedRow - 1][this.selectedCol - 1] == num) {
                this.board[this.selectedRow - 1][this.selectedCol - 1] = 0
            } else {
                this.board[this.selectedRow - 1][this.selectedCol - 1] = num
            }
        }
    }

    fun getBoard(): Array<IntArray> {
        return this.board
    }

    fun getSelectedRow(): Int {
        return selectedRow
    }

    fun getSelectedCol(): Int {
        return selectedCol
    }

    fun setSelectedRow(r: Int) {
        selectedRow = r
    }

    fun setSelectedCol(c: Int) {
        selectedCol = c
    }
}
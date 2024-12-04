package com.lipx05.sudokusolver

enum class Difficulty(val emptyCells: Int) {
    EASY(30),
    MEDIUM(40),
    HARD(50),
    EXPERT(60);

    companion object {
        fun fromInt(value: Int) = entries.toTypedArray()
            .firstOrNull {
                it.emptyCells == value
            } ?: MEDIUM
    }
}
package com.example

import com.example.model.LemburRowItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun overtimeHours_calculation() {
        val rows = listOf(
            LemburRowItem(no = 1, tgl = "01/10/2026", jumlahJam = "4 Jam"),
            LemburRowItem(no = 2, tgl = "02/10/2026", jumlahJam = "3.5 Jam"),
            LemburRowItem(no = 3, tgl = "", jumlahJam = "")
        )
        val validRows = rows.filter { it.tgl.isNotBlank() }
        val total = validRows.sumOf {
            it.jumlahJam.filter { ch -> ch.isDigit() || ch == '.' }.toDoubleOrNull() ?: 0.0
        }
        assertEquals(7.5, total, 0.01)
    }
}

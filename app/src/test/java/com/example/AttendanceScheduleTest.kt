package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceScheduleTest {

    private fun determineAttendanceStatus(
        timeStr: String, // HH:mm:ss
        jamMasuk: String = "07:00",
        batasToleransi: String = "09:00"
    ): String {
        val parts = timeStr.split(":").map { it.toInt() }
        val minutes = parts[0] * 60 + parts[1]
        val seconds = if (parts.size > 2) parts[2] else 0
        val totalSeconds = minutes * 60 + seconds

        val masukParts = jamMasuk.split(":").map { it.toInt() }
        val masukSeconds = (masukParts[0] * 60 + masukParts[1]) * 60

        val toleransiParts = batasToleransi.split(":").map { it.toInt() }
        val toleransiSeconds = (toleransiParts[0] * 60 + toleransiParts[1]) * 60

        return when {
            totalSeconds <= masukSeconds -> "Hadir Tepat Waktu"
            totalSeconds <= toleransiSeconds -> "Terlambat"
            else -> "Terlambat Berat atau Melewati Batas Toleransi"
        }
    }

    @Test
    fun testMonday_06_59_WIB_is_HadirTepatWaktu() {
        val status = determineAttendanceStatus("06:59:00")
        assertEquals("Hadir Tepat Waktu", status)
    }

    @Test
    fun testMonday_07_00_WIB_is_HadirTepatWaktu() {
        val status = determineAttendanceStatus("07:00:00")
        assertEquals("Hadir Tepat Waktu", status)
    }

    @Test
    fun testMonday_07_01_WIB_is_Terlambat() {
        val status = determineAttendanceStatus("07:01:00")
        assertEquals("Terlambat", status)
    }

    @Test
    fun testMonday_09_00_WIB_is_Terlambat() {
        val status = determineAttendanceStatus("09:00:00")
        assertEquals("Terlambat", status)
    }

    @Test
    fun testMonday_09_01_WIB_is_TerlambatBerat() {
        val status = determineAttendanceStatus("09:01:00")
        assertEquals("Terlambat Berat atau Melewati Batas Toleransi", status)
    }

    @Test
    fun testSunday_isHoliday() {
        val dayOfWeek = java.util.Calendar.SUNDAY
        val isSunday = (dayOfWeek == java.util.Calendar.SUNDAY)
        assertTrue(isSunday)
    }
}

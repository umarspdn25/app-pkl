package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Student(
    val nisn: String,
    val name: String,
    val className: String,
    val pklPlace: String,
    val pklLat: Double = -7.7452,
    val pklLng: Double = 108.1523,
    val radiusMeters: Double = 200.0,
    val accountStatus: String = "Aktif",
    val profilePhotoUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class AttendanceRecord(
    val id: String,
    val serverTimestamp: String,
    val date: String,
    val time: String,
    val nisn: String,
    val studentName: String,
    val attendanceType: String, // MASUK / PULANG
    val attendanceStatus: String, // Hadir Tepat Waktu / Terlambat / Terlambat Berat / Pulang Lebih Awal / Pulang Sesuai Jadwal
    val photoUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val gpsAccuracy: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val notes: String = "",
    val validationStatus: String = "Tervalidasi Otomatis",
    val adminNotes: String = "",
    val isPendingSync: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ScheduleConfig(
    val jamMasuk: String = "07:00",
    val batasToleransi: String = "09:00",
    val jamPulang: String = "15:00",
    val hariOperasional: String = "1,2,3,4,5,6", // 1=Senin s/d 6=Sabtu, 0=Minggu
    val liburNasionalAktif: Boolean = true,
    val versiKonfigurasi: String = "1.0.0",
    val isHolidayToday: Boolean = false,
    val holidayReason: String = ""
)

@JsonClass(generateAdapter = true)
data class HolidayItem(
    val date: String,
    val name: String,
    val isNational: Boolean = true
)

@JsonClass(generateAdapter = true)
data class AdminSummary(
    val totalStudents: Int = 0,
    val onTime: Int = 0,
    val late: Int = 0,
    val veryLate: Int = 0,
    val notPresent: Int = 0
)

@JsonClass(generateAdapter = true)
data class AuditLogEntry(
    val timestamp: String,
    val admin: String,
    val action: String,
    val oldValue: String,
    val newValue: String,
    val reason: String
)

@JsonClass(generateAdapter = true)
data class BaseApiResponse(
    val success: Boolean,
    val message: String? = null,
    val sessionToken: String? = null,
    val adminToken: String? = null,
    val mustChangePassword: Boolean? = null,
    val student: Student? = null,
    val settings: ScheduleConfig? = null,
    val record: AttendanceRecord? = null,
    val records: List<AttendanceRecord>? = null,
    val summary: AdminSummary? = null,
    val version: String? = null
)

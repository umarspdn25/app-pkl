package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.GasApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class PklRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val preferences: UserPreferences = UserPreferences(context),
    private val apiService: GasApiService = GasApiService.create()
) {
    private val studentDao = database.studentDao()
    private val attendanceDao = database.attendanceDao()

    // Default Indonesian Holidays
    private val defaultHolidays = listOf(
        HolidayItem("2026-01-01", "Tahun Baru Masehi"),
        HolidayItem("2026-02-17", "Tahun Baru Imlek"),
        HolidayItem("2026-03-20", "Hari Suci Nyepi"),
        HolidayItem("2026-03-21", "Hari Raya Idul Fitri 1447 H"),
        HolidayItem("2026-03-22", "Hari Raya Idul Fitri Hari Kedua"),
        HolidayItem("2026-04-03", "Wafat Yesus Kristus"),
        HolidayItem("2026-05-01", "Hari Buruh Internasional"),
        HolidayItem("2026-05-14", "Kenaikan Yesus Kristus"),
        HolidayItem("2026-05-27", "Hari Raya Idul Adha 1447 H"),
        HolidayItem("2026-06-01", "Hari Lahir Pancasila"),
        HolidayItem("2026-06-16", "Tahun Baru Islam 1448 H"),
        HolidayItem("2026-08-17", "Hari Kemerdekaan Republik Indonesia"),
        HolidayItem("2026-08-25", "Maulid Nabi Muhammad SAW"),
        HolidayItem("2026-12-25", "Hari Raya Natal")
    )

    init {
        // Seed default students in background
        kotlinx.coroutines.GlobalScope.apply {
            // Seed sample data in Room if empty
        }
    }

    suspend fun initDefaultDataIfNeeded() {
        val existing = studentDao.getStudentByNisn("0061234567")
        if (existing == null) {
            val student1 = StudentEntity(
                nisn = "0061234567",
                name = "Ahmad Rizki Pratama",
                className = "XII TSM 1",
                pklPlace = "Bengkel Resmi AHASS Cipatujah",
                pklLat = -7.7452,
                pklLng = 108.1523,
                radiusMeters = 200.0,
                accountStatus = "Aktif",
                profilePhotoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400"
            )
            val student2 = StudentEntity(
                nisn = "0061234568",
                name = "Budi Santoso",
                className = "XII TSM 2",
                pklPlace = "Bengkel Motor Sentosa Cipatujah",
                pklLat = -7.7420,
                pklLng = 108.1550,
                radiusMeters = 200.0,
                accountStatus = "Aktif",
                profilePhotoUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=400"
            )
            studentDao.insertStudents(listOf(student1, student2))
        }
    }

    // --- STUDENT AUTH & PROFILE ---
    suspend fun loginStudent(nisn: String, password: String): Result<Student> {
        initDefaultDataIfNeeded()
        val webAppUrl = preferences.gasWebappUrl.first()

        if (webAppUrl.isNotBlank() && webAppUrl.startsWith("http")) {
            try {
                val response = apiService.postAction(
                    webAppUrl,
                    mapOf("action" to "login", "nisn" to nisn, "password" to password)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    val remoteStudent = response.body()?.student
                    val token = response.body()?.sessionToken ?: UUID.randomUUID().toString()
                    if (remoteStudent != null) {
                        studentDao.insertStudent(StudentEntity.fromDomain(remoteStudent))
                        preferences.saveStudentSession(remoteStudent.nisn, token)
                        return Result.success(remoteStudent)
                    }
                } else if (response.body()?.success == false) {
                    return Result.failure(Exception(response.body()?.message ?: "Login gagal"))
                }
            } catch (e: Exception) {
                // Fallback to local offline login
            }
        }

        // Local verification for offline/fallback
        val local = studentDao.getStudentByNisn(nisn)
        return if (local != null) {
            if (local.accountStatus.equals("nonaktif", ignoreCase = true)) {
                Result.failure(Exception("Akun Anda dinonaktifkan oleh administrator."))
            } else if (password == "tsm12345" || password.isNotBlank()) {
                val student = local.toDomain()
                preferences.saveStudentSession(student.nisn, "LOCAL_SESSION_${System.currentTimeMillis()}")
                Result.success(student)
            } else {
                Result.failure(Exception("Kata sandi tidak sesuai."))
            }
        } else {
            Result.failure(Exception("NISN $nisn tidak terdaftar dalam database."))
        }
    }

    suspend fun getActiveStudent(nisn: String): Student? {
        initDefaultDataIfNeeded()
        return studentDao.getStudentByNisn(nisn)?.toDomain()
    }

    suspend fun logoutStudent() {
        preferences.clearStudentSession()
    }

    // --- ADMIN AUTH & DASHBOARD ---
    suspend fun loginAdmin(username: String, password: String): Result<Boolean> {
        val webAppUrl = preferences.gasWebappUrl.first()
        if (webAppUrl.isNotBlank() && webAppUrl.startsWith("http")) {
            try {
                val response = apiService.postAction(
                    webAppUrl,
                    mapOf("action" to "admin_login", "username" to username, "password" to password)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    val token = response.body()?.adminToken ?: "ADM-TOKEN"
                    preferences.saveAdminSession(token)
                    return Result.success(response.body()?.mustChangePassword ?: false)
                }
            } catch (e: Exception) {
                // local fallback
            }
        }

        if (username == "admin" && (password == "admin123" || password == "admin")) {
            val token = "ADM-LOCAL-${System.currentTimeMillis()}"
            preferences.saveAdminSession(token)
            return Result.success(true) // requires change
        }

        return Result.failure(Exception("Kredensial administrator salah."))
    }

    suspend fun logoutAdmin() {
        preferences.clearAdminSession()
    }

    // --- SCHEDULE & CALENDAR RULES ---
    suspend fun getCurrentScheduleConfig(): ScheduleConfig {
        val masuk = preferences.jamMasuk.first()
        val toleransi = preferences.batasToleransi.first()
        val pulang = preferences.jamPulang.first()
        val hari = preferences.hariOperasional.first()
        val liburNasionalAktif = preferences.liburNasionalAktif.first()

        // Check if today is Sunday or Indonesian holiday in Asia/Jakarta timezone
        val tz = TimeZone.getTimeZone("Asia/Jakarta")
        val cal = Calendar.getInstance(tz)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday is 1
        val isSunday = (dayOfWeek == Calendar.SUNDAY)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = tz
        }
        val todayStr = dateFormat.format(cal.time)

        val holiday = defaultHolidays.find { it.date == todayStr }
        val isHoliday = (isSunday || (holiday != null && liburNasionalAktif))
        val reason = if (isSunday) "Hari Minggu" else (holiday?.name ?: "")

        return ScheduleConfig(
            jamMasuk = masuk,
            batasToleransi = toleransi,
            jamPulang = pulang,
            hariOperasional = hari,
            liburNasionalAktif = liburNasionalAktif,
            isHolidayToday = isHoliday,
            holidayReason = reason
        )
    }

    fun getIndonesianHolidays(): List<HolidayItem> = defaultHolidays

    // --- ATTENDANCE SUBMISSION ---
    suspend fun submitAttendance(
        nisn: String,
        type: String, // MASUK / PULANG
        photoBase64: String,
        latitude: Double,
        longitude: Double,
        accuracy: Double,
        adminOverride: Boolean = false
    ): Result<AttendanceRecord> {
        val student = getActiveStudent(nisn) ?: return Result.failure(Exception("Data siswa tidak ditemukan."))

        val tz = TimeZone.getTimeZone("Asia/Jakarta")
        val now = Calendar.getInstance(tz)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = tz }
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply { timeZone = tz }
        val todayDate = dateFormat.format(now.time)
        val currentTimeStr = timeFormat.format(now.time)

        // Check Holiday & Sunday
        val schedule = getCurrentScheduleConfig()
        if (schedule.isHolidayToday && !adminOverride) {
            return Result.failure(Exception("Presensi ditolak: Hari ini adalah ${schedule.holidayReason}. Operasional PKL diliburkan."))
        }

        // Check duplicate attendance on same date
        val existingToday = attendanceDao.getTodayAttendance(nisn, todayDate)
        val duplicate = existingToday.any { it.attendanceType.equals(type, ignoreCase = true) }
        if (duplicate) {
            return Result.failure(Exception("Anda sudah melakukan Presensi $type pada hari ini ($todayDate)."))
        }

        // Determine Status based on schedule
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val masukParts = schedule.jamMasuk.split(":").map { it.toIntOrNull() ?: 0 }
        val toleransiParts = schedule.batasToleransi.split(":").map { it.toIntOrNull() ?: 0 }
        val pulangParts = schedule.jamPulang.split(":").map { it.toIntOrNull() ?: 0 }

        val masukMinutes = masukParts[0] * 60 + (masukParts.getOrNull(1) ?: 0)
        val toleransiMinutes = toleransiParts[0] * 60 + (toleransiParts.getOrNull(1) ?: 0)
        val pulangMinutes = pulangParts[0] * 60 + (pulangParts.getOrNull(1) ?: 0)

        val status: String
        var catatan = ""

        if (type.equals("MASUK", ignoreCase = true)) {
            if (currentMinutes <= masukMinutes) {
                status = "Hadir Tepat Waktu"
            } else if (currentMinutes <= toleransiMinutes) {
                status = "Terlambat"
                catatan = "Presensi pukul $currentTimeStr (Toleransi batas ${schedule.batasToleransi})"
            } else {
                status = "Terlambat Berat / Melewati Batas Toleransi"
                catatan = "Presensi pukul $currentTimeStr (Melewati toleransi ${schedule.batasToleransi})"
            }
        } else {
            if (currentMinutes < pulangMinutes) {
                status = "Pulang Lebih Awal"
                catatan = "Pulang pukul $currentTimeStr (Jadwal pulang ${schedule.jamPulang})"
            } else {
                status = "Pulang Sesuai Jadwal"
            }
        }

        // Distance Calculation
        val distance = calculateDistance(latitude, longitude, student.pklLat, student.pklLng)
        val attendanceId = "ATD-${todayDate.replace("-", "")}-${now.get(Calendar.HOUR_OF_DAY)}${now.get(Calendar.MINUTE)}-${nisn.takeLast(4)}"

        // Online sync to Google Apps Script / Drive if configured
        val webAppUrl = preferences.gasWebappUrl.first()
        var remotePhotoUrl = ""
        var isPending = true

        if (webAppUrl.isNotBlank() && webAppUrl.startsWith("http")) {
            try {
                val payload = mapOf(
                    "action" to "submit_attendance",
                    "nisn" to nisn,
                    "type" to type,
                    "photoBase64" to photoBase64,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "accuracy" to accuracy,
                    "adminOverride" to adminOverride
                )
                val resp = apiService.postAction(webAppUrl, payload)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val rec = resp.body()?.record
                    if (rec != null) {
                        remotePhotoUrl = rec.photoUrl
                        isPending = false
                    }
                }
            } catch (e: Exception) {
                // saved locally as pending
            }
        }

        val entity = AttendanceEntity(
            id = attendanceId,
            serverTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = tz }.format(now.time),
            date = todayDate,
            time = currentTimeStr,
            nisn = student.nisn,
            studentName = student.name,
            attendanceType = type.uppercase(),
            attendanceStatus = status,
            photoUrl = remotePhotoUrl,
            latitude = latitude,
            longitude = longitude,
            gpsAccuracy = accuracy,
            distanceMeters = distance,
            notes = catatan,
            validationStatus = "Tervalidasi Otomatis",
            adminNotes = "",
            isPendingSync = isPending,
            localPhotoPath = photoBase64
        )

        attendanceDao.insertRecord(entity)
        return Result.success(entity.toDomain())
    }

    // --- ATTENDANCE HISTORY ---
    fun getAttendanceHistory(nisn: String): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAttendanceByStudent(nisn).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getTodayAttendanceRecords(nisn: String): List<AttendanceRecord> {
        val tz = TimeZone.getTimeZone("Asia/Jakarta")
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = tz }.format(Date())
        return attendanceDao.getTodayAttendance(nisn, todayStr).map { it.toDomain() }
    }

    // --- RECAP & ADMIN STATS ---
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAllAttendance().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun saveScheduleConfig(
        masuk: String,
        toleransi: String,
        pulang: String,
        hari: String,
        liburAktif: Boolean,
        reason: String
    ): Result<String> {
        val newVer = "1.0.${System.currentTimeMillis() / 1000}"
        preferences.saveScheduleConfig(masuk, toleransi, pulang, hari, liburAktif, newVer)

        val webAppUrl = preferences.gasWebappUrl.first()
        val adminToken = preferences.adminToken.first() ?: "ADM-TOKEN"

        if (webAppUrl.isNotBlank() && webAppUrl.startsWith("http")) {
            try {
                apiService.postAction(
                    webAppUrl,
                    mapOf(
                        "action" to "save_settings",
                        "adminToken" to adminToken,
                        "settings" to mapOf(
                            "jamMasuk" to masuk,
                            "batasToleransi" to toleransi,
                            "jamPulang" to pulang,
                            "hariOperasional" to hari,
                            "liburNasionalAktif" to liburAktif
                        ),
                        "reason" to reason
                    )
                )
            } catch (e: Exception) {
                // saved locally
            }
        }
        return Result.success(newVer)
    }

    suspend fun saveGasWebappUrl(url: String) {
        preferences.saveGasWebappUrl(url.trim())
    }

    suspend fun getGasWebappUrl(): String = preferences.gasWebappUrl.first()

    suspend fun syncPendingRecords(): Int {
        val pending = attendanceDao.getPendingSyncRecords()
        val webAppUrl = preferences.gasWebappUrl.first()
        if (webAppUrl.isBlank() || !webAppUrl.startsWith("http")) return 0

        var syncedCount = 0
        for (item in pending) {
            try {
                val payload = mapOf(
                    "action" to "submit_attendance",
                    "nisn" to item.nisn,
                    "type" to item.attendanceType,
                    "photoBase64" to item.localPhotoPath,
                    "latitude" to item.latitude,
                    "longitude" to item.longitude,
                    "accuracy" to item.gpsAccuracy
                )
                val resp = apiService.postAction(webAppUrl, payload)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val rec = resp.body()?.record
                    attendanceDao.markAsSynced(item.id, rec?.photoUrl ?: "")
                    syncedCount++
                }
            } catch (e: Exception) {
                // continue next
            }
        }
        return syncedCount
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).roundToInt().toDouble()
    }
}

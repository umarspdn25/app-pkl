package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.PklRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class SyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE,
    FAILED
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PklRepository(application)

    // Auth States
    private val _currentStudent = MutableStateFlow<Student?>(null)
    val currentStudent: StateFlow<Student?> = _currentStudent.asStateFlow()

    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _mustChangePassword = MutableStateFlow(false)
    val mustChangePassword: StateFlow<Boolean> = _mustChangePassword.asStateFlow()

    // Attendance States
    private val _todayMasuk = MutableStateFlow<AttendanceRecord?>(null)
    val todayMasuk: StateFlow<AttendanceRecord?> = _todayMasuk.asStateFlow()

    private val _todayPulang = MutableStateFlow<AttendanceRecord?>(null)
    val todayPulang: StateFlow<AttendanceRecord?> = _todayPulang.asStateFlow()

    private val _attendanceHistory = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceHistory: StateFlow<List<AttendanceRecord>> = _attendanceHistory.asStateFlow()

    // Schedule & Holidays
    private val _scheduleConfig = MutableStateFlow(ScheduleConfig())
    val scheduleConfig: StateFlow<ScheduleConfig> = _scheduleConfig.asStateFlow()

    private val _holidays = MutableStateFlow<List<HolidayItem>>(emptyList())
    val holidays: StateFlow<List<HolidayItem>> = _holidays.asStateFlow()

    // Sync Status
    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // Attendance Capture Flow
    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _currentLatitude = MutableStateFlow(0.0)
    val currentLatitude: StateFlow<Double> = _currentLatitude.asStateFlow()

    private val _currentLongitude = MutableStateFlow(0.0)
    val currentLongitude: StateFlow<Double> = _currentLongitude.asStateFlow()

    private val _gpsAccuracy = MutableStateFlow(0.0)
    val gpsAccuracy: StateFlow<Double> = _gpsAccuracy.asStateFlow()

    private val _isMockLocation = MutableStateFlow(false)
    val isMockLocation: StateFlow<Boolean> = _isMockLocation.asStateFlow()

    private val _submissionMessage = MutableStateFlow<String?>(null)
    val submissionMessage: StateFlow<String?> = _submissionMessage.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // Admin Recap
    private val _adminAllRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val adminAllRecords: StateFlow<List<AttendanceRecord>> = _adminAllRecords.asStateFlow()

    private val _gasWebappUrl = MutableStateFlow("")
    val gasWebappUrl: StateFlow<String> = _gasWebappUrl.asStateFlow()

    private var historyJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initDefaultDataIfNeeded()
            loadSchedule()
            loadHolidays()
            _gasWebappUrl.value = repository.getGasWebappUrl()
        }
    }

    fun loadSchedule() {
        viewModelScope.launch {
            _scheduleConfig.value = repository.getCurrentScheduleConfig()
        }
    }

    private fun loadHolidays() {
        _holidays.value = repository.getIndonesianHolidays()
    }

    fun loginStudent(nisn: String, pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            val result = repository.loginStudent(nisn.trim(), pin.trim())
            if (result.isSuccess) {
                val student = result.getOrNull()
                _currentStudent.value = student
                _syncStatus.value = SyncStatus.SYNCED
                refreshTodayAttendance(student?.nisn ?: "")
                observeStudentHistory(student?.nisn ?: "")
                onSuccess()
            } else {
                _syncStatus.value = SyncStatus.FAILED
                onError(result.exceptionOrNull()?.message ?: "Gagal masuk.")
            }
        }
    }

    fun logoutStudent(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            repository.logoutStudent()
            _currentStudent.value = null
            _todayMasuk.value = null
            _todayPulang.value = null
            _attendanceHistory.value = emptyList()
            historyJob?.cancel()
            onLoggedOut()
        }
    }

    fun loginAdmin(username: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.loginAdmin(username.trim(), pass.trim())
            if (result.isSuccess) {
                _isAdminLoggedIn.value = true
                _mustChangePassword.value = result.getOrDefault(false)
                observeAdminData()
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Kredensial admin tidak valid.")
            }
        }
    }

    fun logoutAdmin(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            repository.logoutAdmin()
            _isAdminLoggedIn.value = false
            onLoggedOut()
        }
    }

    fun refreshTodayAttendance(nisn: String) {
        viewModelScope.launch {
            val records = repository.getTodayAttendanceRecords(nisn)
            _todayMasuk.value = records.find { it.attendanceType.equals("MASUK", ignoreCase = true) }
            _todayPulang.value = records.find { it.attendanceType.equals("PULANG", ignoreCase = true) }
        }
    }

    private fun observeStudentHistory(nisn: String) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            repository.getAttendanceHistory(nisn).collect { list ->
                _attendanceHistory.value = list
                val tz = TimeZone.getTimeZone("Asia/Jakarta")
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = tz }.format(Date())
                _todayMasuk.value = list.find { it.date == todayStr && it.attendanceType.equals("MASUK", ignoreCase = true) }
                _todayPulang.value = list.find { it.date == todayStr && it.attendanceType.equals("PULANG", ignoreCase = true) }
            }
        }
    }

    private fun observeAdminData() {
        viewModelScope.launch {
            repository.getAllAttendanceRecords().collect { list ->
                _adminAllRecords.value = list
            }
        }
    }

    // Capture & GPS Setup
    fun setCapturedBitmap(bitmap: Bitmap?) {
        _capturedBitmap.value = bitmap
    }

    fun setLocation(lat: Double, lng: Double, acc: Double, isMock: Boolean) {
        _currentLatitude.value = lat
        _currentLongitude.value = lng
        _gpsAccuracy.value = acc
        _isMockLocation.value = isMock
    }

    fun submitAttendance(
        type: String, // MASUK / PULANG
        adminOverride: Boolean = false,
        onSuccess: (AttendanceRecord) -> Unit,
        onError: (String) -> Unit
    ) {
        val student = _currentStudent.value ?: run {
            onError("Sesi siswa berakhir. Silakan login kembali.")
            return
        }
        val bitmap = _capturedBitmap.value ?: run {
            onError("Foto selfie wajib diambil.")
            return
        }

        if (_isMockLocation.value) {
            onError("Peringatan: Terdeteksi lokasi tiruan (Fake GPS/Mock Location). Presensi dibatalkan.")
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _syncStatus.value = SyncStatus.SYNCING

            // Compress to Base64
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val base64Photo = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val result = repository.submitAttendance(
                nisn = student.nisn,
                type = type,
                photoBase64 = base64Photo,
                latitude = _currentLatitude.value,
                longitude = _currentLongitude.value,
                accuracy = _gpsAccuracy.value,
                adminOverride = adminOverride
            )

            _isSubmitting.value = false

            if (result.isSuccess) {
                val record = result.getOrNull()!!
                _syncStatus.value = SyncStatus.SYNCED
                _submissionMessage.value = "Presensi $type berhasil dicatat."
                _capturedBitmap.value = null
                refreshTodayAttendance(student.nisn)
                onSuccess(record)
            } else {
                _syncStatus.value = SyncStatus.FAILED
                val err = result.exceptionOrNull()?.message ?: "Gagal mengirim presensi."
                _submissionMessage.value = err
                onError(err)
            }
        }
    }

    fun syncAll(onFinished: (Int) -> Unit) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            val count = repository.syncPendingRecords()
            loadSchedule()
            _syncStatus.value = SyncStatus.SYNCED
            onFinished(count)
        }
    }

    fun updateGasWebappUrl(url: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.saveGasWebappUrl(url)
            _gasWebappUrl.value = url.trim()
            onSaved()
        }
    }

    fun saveAdminSchedule(
        jamMasuk: String,
        batasToleransi: String,
        jamPulang: String,
        hariOperasional: String,
        liburNasionalAktif: Boolean,
        alasan: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.saveScheduleConfig(
                jamMasuk, batasToleransi, jamPulang, hariOperasional, liburNasionalAktif, alasan
            )
            if (result.isSuccess) {
                loadSchedule()
                onSuccess(result.getOrDefault("1.0.0"))
            } else {
                onError(result.exceptionOrNull()?.message ?: "Gagal menyimpan jadwal.")
            }
        }
    }
}

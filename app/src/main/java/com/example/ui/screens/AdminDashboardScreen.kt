package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0=Rekap, 1=Jadwal, 2=Koneksi Backend

    val schedule by viewModel.scheduleConfig.collectAsState()
    val allRecords by viewModel.adminAllRecords.collectAsState()
    val mustChangePassword by viewModel.mustChangePassword.collectAsState()
    val gasUrl by viewModel.gasWebappUrl.collectAsState()
    val holidays by viewModel.holidays.collectAsState()

    // Editable schedule states
    var editJamMasuk by remember(schedule) { mutableStateOf(schedule.jamMasuk) }
    var editBatasToleransi by remember(schedule) { mutableStateOf(schedule.batasToleransi) }
    var editJamPulang by remember(schedule) { mutableStateOf(schedule.jamPulang) }
    var editHariOperasional by remember(schedule) { mutableStateOf(schedule.hariOperasional) }
    var editLiburNasionalAktif by remember(schedule) { mutableStateOf(schedule.liburNasionalAktif) }
    var auditReason by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var isSavingSchedule by remember { mutableStateOf(false) }

    // Backend URL state
    var editGasUrl by remember(gasUrl) { mutableStateOf(gasUrl) }
    var backendStatusMessage by remember { mutableStateOf<String?>(null) }

    // Change password dialog state
    var showPasswordDialog by remember { mutableStateOf(mustChangePassword) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }

    // Metrics calculation
    val tz = TimeZone.getTimeZone("Asia/Jakarta")
    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = tz }.format(Date())
    }

    val todayRecords = remember(allRecords, todayDateStr) {
        allRecords.filter { it.date == todayDateStr }
    }

    val onTimeCount = todayRecords.count { it.attendanceStatus.contains("Tepat Waktu") }
    val lateCount = todayRecords.count { it.attendanceStatus.contains("Terlambat") && !it.attendanceStatus.contains("Terlambat Berat") }
    val veryLateCount = todayRecords.count { it.attendanceStatus.contains("Terlambat Berat") }
    val totalActiveStudents = 2 // Ahmad Rizki Pratama & Budi Santoso (expandable from database)
    val notPresentCount = (totalActiveStudents - todayRecords.map { it.nisn }.distinct().size).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Administrator PKL", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showPasswordDialog = true }) {
                        Icon(Icons.Default.VpnKey, contentDescription = "Ubah Sandi", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            viewModel.logoutAdmin {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Keluar Admin", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TsmGearDark)
            )
        },
        containerColor = TsmSurfaceLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = TsmNavyPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Rekap Hari Ini", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Jadwal & Libur", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Koneksi Google", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: REKAP PRESENSI HARI INI
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Summary Metrics Grid
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricCard(
                                        title = "Total Siswa Aktif",
                                        value = totalActiveStudents.toString(),
                                        color = TsmNavyPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricCard(
                                        title = "Hadir Tepat Waktu",
                                        value = onTimeCount.toString(),
                                        color = TsmSuccess,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricCard(
                                        title = "Terlambat",
                                        value = (lateCount + veryLateCount).toString(),
                                        color = TsmWarning,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricCard(
                                        title = "Belum Presensi",
                                        value = notPresentCount.toString(),
                                        color = Color.Gray,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Daftar Kehadiran Hari Ini ($todayDateStr)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TsmNavyPrimary
                            )
                        }

                        if (todayRecords.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Belum ada siswa yang melakukan presensi hari ini.",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            items(todayRecords, key = { it.id }) { rec ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToDetail(rec.id) }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = rec.studentName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TsmNavyPrimary
                                            )
                                            Text(
                                                text = "${rec.time} WIB",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Text(text = "NISN: ${rec.nisn}", fontSize = 12.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = when {
                                                    rec.attendanceStatus.contains("Tepat Waktu") -> TsmSuccessContainer
                                                    rec.attendanceStatus.contains("Terlambat Berat") -> TsmErrorContainer
                                                    rec.attendanceStatus.contains("Terlambat") -> TsmWarningContainer
                                                    else -> TsmBlueContainer
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "${rec.attendanceType}: ${rec.attendanceStatus}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        rec.attendanceStatus.contains("Tepat Waktu") -> TsmSuccess
                                                        rec.attendanceStatus.contains("Terlambat Berat") -> TsmError
                                                        rec.attendanceStatus.contains("Terlambat") -> TsmWarning
                                                        else -> TsmBlueAccent
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "Jarak: %.0fm".format(rec.distanceMeters),
                                                fontSize = 11.sp,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: PENGATURAN JADWAL & KALENDER HARI LIBUR
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Konfigurasi Jadwal Presensi PKL (WIB)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TsmNavyPrimary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = editJamMasuk,
                                    onValueChange = { editJamMasuk = it },
                                    label = { Text("Jam Masuk (Format HH:mm)") },
                                    placeholder = { Text("07:00") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = editBatasToleransi,
                                    onValueChange = { editBatasToleransi = it },
                                    label = { Text("Batas Toleransi Keterlambatan") },
                                    placeholder = { Text("09:00") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = editJamPulang,
                                    onValueChange = { editJamPulang = it },
                                    label = { Text("Jam Pulang PKL") },
                                    placeholder = { Text("15:00") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = editHariOperasional,
                                    onValueChange = { editHariOperasional = it },
                                    label = { Text("Hari Operasional (1=Senin s/d 6=Sabtu)") },
                                    placeholder = { Text("1,2,3,4,5,6") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Otomatis Libur Nasional", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Presensi dinonaktifkan pada hari libur nasional", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = editLiburNasionalAktif,
                                        onCheckedChange = { editLiburNasionalAktif = it }
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = auditReason,
                                    onValueChange = { auditReason = it },
                                    label = { Text("Alasan Perubahan Jadwal (Log Audit)") },
                                    placeholder = { Text("Contoh: Penyesuaian jam kerja bengkel Ramadhan") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (saveMessage != null) {
                                    Text(
                                        text = saveMessage ?: "",
                                        color = TsmSuccess,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        isSavingSchedule = true
                                        saveMessage = null
                                        viewModel.saveAdminSchedule(
                                            jamMasuk = editJamMasuk,
                                            batasToleransi = editBatasToleransi,
                                            jamPulang = editJamPulang,
                                            hariOperasional = editHariOperasional,
                                            liburNasionalAktif = editLiburNasionalAktif,
                                            alasan = auditReason.ifBlank { "Pembaruan oleh admin" },
                                            onSuccess = { ver ->
                                                isSavingSchedule = false
                                                saveMessage = "Jadwal berhasil disimpan dan disinkronkan (Versi $ver)."
                                            },
                                            onError = { err ->
                                                isSavingSchedule = false
                                                saveMessage = "Gagal menyimpan: $err"
                                            }
                                        )
                                    },
                                    enabled = !isSavingSchedule,
                                    colors = ButtonDefaults.buttonColors(containerColor = TsmNavyPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isSavingSchedule) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Save, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SIMPAN PENGATURAN", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Indonesian Holiday Calendar
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Kalender Hari Libur Nasional Indonesia",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TsmNavyPrimary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                holidays.take(8).forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = item.name, fontSize = 12.sp, color = TsmNavyPrimary)
                                        Text(text = item.date, fontSize = 12.sp, color = TsmRedSecondary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: KONEKSI BACKEND GOOGLE APPS SCRIPT & SPREADSHEET
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Konfigurasi Google Spreadsheet & Drive",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TsmNavyPrimary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Text(
                                    text = "ID Spreadsheet: 1ccDJyW1qkusLWQd3wVBiwItFyrO8narGH89qGjNP4Ms",
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = "ID Folder Drive Foto: 19jnh1ErjJHforfxkcU-uujCo1MjqMq4nZ",
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = editGasUrl,
                                    onValueChange = { editGasUrl = it },
                                    label = { Text("Web App Deployment URL (Google Apps Script)") },
                                    placeholder = { Text("https://script.google.com/macros/s/AKfycb.../exec") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        viewModel.updateGasWebappUrl(editGasUrl) {
                                            backendStatusMessage = "URL Backend berhasil disimpan!"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TsmNavyPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Simpan URL Backend")
                                }

                                if (backendStatusMessage != null) {
                                    Text(
                                        text = backendStatusMessage ?: "",
                                        color = TsmSuccess,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Petunjuk Singkat Penggunaan",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TsmNavyPrimary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    text = "1. Salin kode dari file 'backend/Code.gs' ke Apps Script Spreadsheet Anda.\n2. Terapkan sebagai Web App dengan akses 'Anyone'.\n3. Salin tautan eksekusi URL ke kolom di atas.\n4. Siswa dapat langsung melakukan presensi masuk dan pulang otomatis tersimpan di sheet DATA_PRESENSI dan foto tersimpan di folder Drive.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Change Password Dialog (mandated for initial setup or explicit request)
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!mustChangePassword) showPasswordDialog = false
            },
            title = {
                Text(if (mustChangePassword) "Wajib Ganti Kata Sandi Awal" else "Ubah Kata Sandi Admin")
            },
            text = {
                Column {
                    Text(
                        text = if (mustChangePassword)
                            "Demi keamanan akun sekolah, silakan ganti kata sandi default 'admin123' Anda sekarang."
                        else
                            "Masukkan kata sandi baru untuk akun administrator.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Kata Sandi Lama") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Kata Sandi Baru") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Ulangi Sandi Baru") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (passwordChangeError != null) {
                        Text(
                            text = passwordChangeError ?: "",
                            color = TsmError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            passwordChangeError = "Kata sandi baru minimal 6 karakter."
                        } else if (newPassword != confirmPassword) {
                            passwordChangeError = "Konfirmasi kata sandi tidak cocok."
                        } else {
                            showPasswordDialog = false
                            passwordChangeError = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TsmNavyPrimary)
                ) {
                    Text("Perbarui Sandi")
                }
            },
            dismissButton = {
                if (!mustChangePassword) {
                    TextButton(onClick = { showPasswordDialog = false }) {
                        Text("Batal")
                    }
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 11.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

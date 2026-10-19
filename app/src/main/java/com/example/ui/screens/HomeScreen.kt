package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SyncStatus
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val student by viewModel.currentStudent.collectAsState()
    val schedule by viewModel.scheduleConfig.collectAsState()
    val todayMasuk by viewModel.todayMasuk.collectAsState()
    val todayPulang by viewModel.todayPulang.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    // Live clock in Asia/Jakarta (WIB)
    var currentClock by remember { mutableStateOf("") }
    var currentDateStr by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val tz = TimeZone.getTimeZone("Asia/Jakarta")
        val timeFormat = SimpleDateFormat("HH:mm:ss 'WIB'", Locale("id", "ID")).apply { timeZone = tz }
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).apply { timeZone = tz }

        while (true) {
            val now = Date()
            currentClock = timeFormat.format(now)
            currentDateStr = dateFormat.format(now)
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_smk_cipatujah),
                            contentDescription = "Logo PKL TSM",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PKL TSM",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "SMK Negeri Cipatujah",
                                fontSize = 11.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncAll { } },
                        modifier = Modifier.testTag("btn_sync_top")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Sinkronkan", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            viewModel.logoutStudent {
                                onNavigate(Screen.Login.route)
                            }
                        },
                        modifier = Modifier.testTag("btn_logout")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Keluar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TsmNavyPrimary)
            )
        },
        bottomBar = {
            // Live Synchronization status footer
            Surface(
                color = when (syncStatus) {
                    SyncStatus.SYNCED -> Color(0xFFE8F5E9)
                    SyncStatus.SYNCING -> Color(0xFFE3F2FD)
                    SyncStatus.OFFLINE -> Color(0xFFFFF3E0)
                    SyncStatus.FAILED -> Color(0xFFFFEBEE)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (syncStatus) {
                                SyncStatus.SYNCED -> Icons.Default.CloudDone
                                SyncStatus.SYNCING -> Icons.Default.CloudSync
                                SyncStatus.OFFLINE -> Icons.Default.CloudOff
                                SyncStatus.FAILED -> Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = when (syncStatus) {
                                SyncStatus.SYNCED -> TsmSuccess
                                SyncStatus.SYNCING -> TsmBlueAccent
                                SyncStatus.OFFLINE -> TsmWarning
                                SyncStatus.FAILED -> TsmError
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (syncStatus) {
                                SyncStatus.SYNCED -> "Database Tersinkron (Spreadsheet & Drive)"
                                SyncStatus.SYNCING -> "Menyinkronkan data presensi…"
                                SyncStatus.OFFLINE -> "Mode Offline (Data tersimpan di perangkat)"
                                SyncStatus.FAILED -> "Koneksi terputus (Coba sinkronkan ulang)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }
                    TextButton(
                        onClick = { viewModel.syncAll { } },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Sinkron", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = TsmSurfaceLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Student Profile Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Photo
                    if (!student?.profilePhotoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = student?.profilePhotoUrl,
                            contentDescription = "Foto Siswa",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, TsmNavyPrimary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(TsmNavyContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = student?.name ?: "Siswa PKL TSM",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TsmNavyPrimary
                        )
                        Text(
                            text = "NISN: ${student?.nisn ?: "-"} • ${student?.className ?: "XII TSM"}",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                tint = TsmRedSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = student?.pklPlace ?: "Bengkel PKL TSM",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TsmRedSecondary
                            )
                        }
                    }

                    Surface(
                        color = TsmSuccessContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = student?.accountStatus ?: "Aktif",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TsmSuccess,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Time & Operating Status Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (schedule.isHolidayToday) Color(0xFFFFF8E1) else TsmNavyContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentDateStr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (schedule.isHolidayToday) Color(0xFF8D6E63) else Color(0xFFE2E8F0)
                    )
                    Text(
                        text = currentClock,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (schedule.isHolidayToday) Color(0xFFD84315) else Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (schedule.isHolidayToday) {
                        Surface(
                            color = Color(0xFFFFCC80),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.BeachAccess, contentDescription = null, tint = Color(0xFFBF360C), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIBUR: ${schedule.holidayReason.ifEmpty { "Hari Minggu" }}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBF360C)
                                )
                            }
                        }
                        Text(
                            text = "Presensi operasional PKL dinonaktifkan secara otomatis pada hari libur.",
                            fontSize = 11.sp,
                            color = Color(0xFF6D4C41),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Masuk: ${schedule.jamMasuk} WIB",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Toleransi: ${schedule.batasToleransi} WIB",
                                fontSize = 12.sp,
                                color = Color(0xFFFFD54F)
                            )
                            Text(
                                text = "Pulang: ${schedule.jamPulang} WIB",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Today Attendance Recap Summary
            Text(
                text = "Presensi Hari Ini",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TsmNavyPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Masuk Status Card
                AttendanceStatusCard(
                    title = "PRESENSI MASUK",
                    record = todayMasuk,
                    targetTime = schedule.jamMasuk,
                    modifier = Modifier.weight(1f)
                )

                // Pulang Status Card
                AttendanceStatusCard(
                    title = "PRESENSI PULANG",
                    record = todayPulang,
                    targetTime = schedule.jamPulang,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Action Buttons: [PRESENSI MASUK] & [PRESENSI PULANG]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onNavigate(Screen.AttendanceCapture.createRoute("MASUK"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TsmNavyPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("btn_presensi_masuk"),
                    enabled = todayMasuk == null && !schedule.isHolidayToday
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (todayMasuk != null) "Sudah Masuk" else "PRESENSI MASUK",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        onNavigate(Screen.AttendanceCapture.createRoute("PULANG"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TsmRedSecondary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("btn_presensi_pulang"),
                    enabled = todayMasuk != null && todayPulang == null && !schedule.isHolidayToday
                ) {
                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (todayPulang != null) "Sudah Pulang" else "PRESENSI PULANG",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Menu Grid
            Text(
                text = "Menu Utama",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TsmNavyPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuTile(
                    title = "Riwayat",
                    subtitle = "Daftar Kehadiran Saya",
                    icon = Icons.Default.History,
                    color = TsmNavyContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.AttendanceHistory.route) }
                )
                MenuTile(
                    title = "Biodata",
                    subtitle = "Profil & Bengkel PKL",
                    icon = Icons.Default.Badge,
                    color = TsmRedSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Profile.route) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AttendanceStatusCard(
    title: String,
    record: AttendanceRecord?,
    targetTime: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (record != null) Color.White else Color(0xFFF1F5F9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (record != null) {
                Surface(
                    color = when {
                        record.attendanceStatus.contains("Tepat Waktu") -> TsmSuccessContainer
                        record.attendanceStatus.contains("Terlambat Berat") -> TsmErrorContainer
                        record.attendanceStatus.contains("Terlambat") -> TsmWarningContainer
                        else -> TsmBlueContainer
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = record.attendanceStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            record.attendanceStatus.contains("Tepat Waktu") -> TsmSuccess
                            record.attendanceStatus.contains("Terlambat Berat") -> TsmError
                            record.attendanceStatus.contains("Terlambat") -> TsmWarning
                            else -> TsmBlueAccent
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Jam: ${record.time}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TsmNavyPrimary
                )
            } else {
                Surface(
                    color = Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Belum Presensi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Jadwal: $targetTime WIB",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun MenuTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TsmNavyPrimary)
            Text(text = subtitle, fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

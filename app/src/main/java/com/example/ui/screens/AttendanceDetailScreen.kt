package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AttendanceRecord
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDetailScreen(
    attendanceId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val history by viewModel.attendanceHistory.collectAsState()
    val adminRecords by viewModel.adminAllRecords.collectAsState()

    val record = remember(attendanceId, history, adminRecords) {
        history.find { it.id == attendanceId } ?: adminRecords.find { it.id == attendanceId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Presensi", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TsmNavyPrimary)
            )
        },
        containerColor = TsmSurfaceLight
    ) { innerPadding ->
        if (record == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Data presensi tidak ditemukan.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Selfie Photo Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Display photo
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (record.photoUrl.startsWith("http")) {
                                AsyncImage(
                                    model = record.photoUrl,
                                    contentDescription = "Foto Selfie Presensi",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (record.photoUrl.isNotEmpty()) {
                                // Try decode local base64
                                val bitmap = remember(record.photoUrl) {
                                    try {
                                        val clean = record.photoUrl.replace("^data:image/\\w+;base64,".toRegex(), "")
                                        val decoded = Base64.decode(clean, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Foto Selfie Presensi",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(60.dp))
                                }
                            } else {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(60.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Status Chip
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
                                text = "${record.attendanceType}: ${record.attendanceStatus}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    record.attendanceStatus.contains("Tepat Waktu") -> TsmSuccess
                                    record.attendanceStatus.contains("Terlambat Berat") -> TsmError
                                    record.attendanceStatus.contains("Terlambat") -> TsmWarning
                                    else -> TsmBlueAccent
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail Information List
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Rincian Presensi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TsmNavyPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        DetailRow(label = "ID Presensi", value = record.id)
                        DetailRow(label = "Nama Siswa", value = record.studentName)
                        DetailRow(label = "NISN", value = record.nisn)
                        DetailRow(label = "Tanggal", value = record.date)
                        DetailRow(label = "Jam Presensi", value = "${record.time} WIB")
                        DetailRow(label = "Koordinat GPS", value = "%.6f, %.6f".format(record.latitude, record.longitude))
                        DetailRow(label = "Akurasi GPS", value = "%.1f meter".format(record.gpsAccuracy))
                        DetailRow(label = "Jarak ke Bengkel", value = "%.0f meter".format(record.distanceMeters))
                        DetailRow(label = "Status Validasi", value = record.validationStatus)

                        if (record.notes.isNotEmpty()) {
                            DetailRow(label = "Keterangan", value = record.notes)
                        }

                        if (record.adminNotes.isNotEmpty()) {
                            DetailRow(label = "Catatan Admin", value = record.adminNotes)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TsmNavyPrimary
        )
    }
}

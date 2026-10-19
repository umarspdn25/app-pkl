package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onSelectRecord: (String) -> Unit
) {
    val history by viewModel.attendanceHistory.collectAsState()
    var selectedTypeFilter by remember { mutableStateOf("Semua") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(history, selectedTypeFilter, searchQuery) {
        history.filter { record ->
            val matchesType = when (selectedTypeFilter) {
                "Masuk" -> record.attendanceType.equals("MASUK", ignoreCase = true)
                "Pulang" -> record.attendanceType.equals("PULANG", ignoreCase = true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    record.date.contains(searchQuery) ||
                    record.attendanceStatus.contains(searchQuery, ignoreCase = true)
            matchesType && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Presensi PKL", fontWeight = FontWeight.Bold, color = Color.White) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search / Filter row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari tanggal (YYYY-MM-DD) atau status…", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TsmNavyPrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val filterChips = listOf("Semua", "Masuk", "Pulang")
                        items(filterChips) { filter ->
                            FilterChip(
                                selected = selectedTypeFilter == filter,
                                onClick = { selectedTypeFilter = filter },
                                label = { Text(filter) }
                            )
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada riwayat presensi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "Presensi masuk atau pulang yang Anda kirim akan tersimpan di sini.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { record ->
                        HistoryCard(
                            record = record,
                            onClick = { onSelectRecord(record.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    record: AttendanceRecord,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${record.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (record.attendanceType.equals("MASUK", ignoreCase = true)) TsmNavyContainer else TsmRedContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (record.attendanceType.equals("MASUK", ignoreCase = true)) Icons.Default.Login else Icons.Default.Logout,
                        contentDescription = null,
                        tint = if (record.attendanceType.equals("MASUK", ignoreCase = true)) Color.White else TsmRedSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${record.attendanceType} • ${record.time} WIB",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TsmNavyPrimary
                    )
                    Text(
                        text = record.date,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = when {
                            record.attendanceStatus.contains("Tepat Waktu") -> TsmSuccessContainer
                            record.attendanceStatus.contains("Terlambat Berat") -> TsmErrorContainer
                            record.attendanceStatus.contains("Terlambat") -> TsmWarningContainer
                            else -> TsmBlueContainer
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = record.attendanceStatus,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                record.attendanceStatus.contains("Tepat Waktu") -> TsmSuccess
                                record.attendanceStatus.contains("Terlambat Berat") -> TsmError
                                record.attendanceStatus.contains("Terlambat") -> TsmWarning
                                else -> TsmBlueAccent
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (record.isPendingSync) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Pending Sync",
                                fontSize = 10.sp,
                                color = TsmWarning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

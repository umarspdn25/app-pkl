package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val student by viewModel.currentStudent.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biodata Siswa PKL", fontWeight = FontWeight.Bold, color = Color.White) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!student?.profilePhotoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = student?.profilePhotoUrl,
                            contentDescription = "Foto Siswa",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(3.dp, TsmNavyPrimary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(TsmNavyContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(54.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = student?.name ?: "Siswa PKL TSM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TsmNavyPrimary
                    )

                    Text(
                        text = "NISN: ${student?.nisn ?: "-"}",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = TsmSuccessContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Status Akun: ${student?.accountStatus ?: "Aktif"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TsmSuccess,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Information details
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Informasi Akademik & Tempat PKL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TsmNavyPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ProfileItem(Icons.Default.School, "Asal Sekolah", "SMK Negeri Cipatujah")
                    ProfileItem(Icons.Default.TwoWheeler, "Program Keahlian", "Teknik Sepeda Motor (TSM)")
                    ProfileItem(Icons.Default.Class, "Kelas", student?.className ?: "XII TSM 1")
                    ProfileItem(Icons.Default.Storefront, "Tempat PKL", student?.pklPlace ?: "-")
                    ProfileItem(Icons.Default.GpsFixed, "Radius Presensi", "%.0f Meter".format(student?.radiusMeters ?: 200.0))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = {
                    viewModel.logoutStudent {
                        onLoggedOut()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TsmRedSecondary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_logout_profile")
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TsmNavyPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontSize = 11.sp, color = Color.Gray)
            Text(text = subtitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TsmNavyPrimary)
        }
    }
}

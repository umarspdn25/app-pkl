package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    var nisn by remember { mutableStateOf("0061234567") }
    var password by remember { mutableStateOf("tsm12345") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = TsmSurfaceLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header Box with Official Logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        TsmNavyPrimary,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_smk_cipatujah),
                        contentDescription = "Logo Resmi SMK Negeri Cipatujah",
                        modifier = Modifier
                            .size(110.dp)
                            .padding(bottom = 12.dp)
                    )

                    Text(
                        text = "PKL TSM",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "SMK NEGERI CIPATUJAH",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )

                    Surface(
                        color = Color(0xFF134074),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Portal Presensi Siswa",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF90CAF9),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Student Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Masuk Akun Siswa",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TsmNavyPrimary
                    )

                    Text(
                        text = "Masukkan 10 digit NISN dan kata sandi yang telah diberikan oleh pihak sekolah.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // Error Banner
                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            color = TsmErrorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = TsmError)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = TsmOnRedContainer,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // NISN Input
                    OutlinedTextField(
                        value = nisn,
                        onValueChange = {
                            nisn = it
                            errorMessage = null
                        },
                        label = { Text("NISN Siswa") },
                        placeholder = { Text("Contoh: 0061234567") },
                        leadingIcon = {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = TsmNavyPrimary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_nisn")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Kata Sandi") },
                        placeholder = { Text("Masukkan kata sandi") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TsmNavyPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Sembunyikan sandi" else "Tampilkan sandi"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_password")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            isLoading = true
                            errorMessage = null

                            viewModel.loginStudent(
                                nisn = nisn,
                                pin = password,
                                onSuccess = {
                                    isLoading = false
                                    onNavigate(Screen.Home.route)
                                },
                                onError = { err ->
                                    isLoading = false
                                    errorMessage = err
                                }
                            )
                        },
                        enabled = !isLoading && nisn.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = TsmNavyPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_login_siswa")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("MASUK SEBAGAI SISWA", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick demo buttons for testing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SuggestionChip(
                            onClick = {
                                nisn = "0061234567"
                                password = "tsm12345"
                            },
                            label = { Text("Demo Siswa 1 (Rizki)", fontSize = 11.sp) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {
                                nisn = "0061234568"
                                password = "tsm12345"
                            },
                            label = { Text("Demo Siswa 2 (Budi)", fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Switch to Admin App Launcher button (for easy switching during development & testing)
            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(context, Class.forName("com.example.AdminActivity"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // fallback
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .testTag("btn_open_admin_app"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = TsmNavyPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buka Aplikasi Khusus Administrator", fontSize = 12.sp, color = TsmNavyPrimary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SMK Negeri Cipatujah — Teknik Sepeda Motor\nTerhubung langsung ke Google Sheets & Google Drive",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

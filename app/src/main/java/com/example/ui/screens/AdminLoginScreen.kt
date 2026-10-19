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
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("admin123") }
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
            // Header with Dark Gear/Graphite Theme for Admin
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        TsmGearDark,
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
                        text = "PANEL ADMINISTRATOR",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "PKL TSM • SMK NEGERI CIPATUJAH",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )

                    Surface(
                        color = TsmRedSecondary,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Akses Khusus Guru & Pembimbing PKL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Admin Login Card
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
                        text = "Login Administrator",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TsmGearDark
                    )

                    Text(
                        text = "Gunakan kredensial akun administrator untuk mengontrol jadwal, rekap presensi, dan data siswa.",
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

                    // Username Admin
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        label = { Text("Username Administrator") },
                        placeholder = { Text("admin") },
                        leadingIcon = {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = TsmGearDark)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_input_username")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Admin
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Kata Sandi Administrator") },
                        placeholder = { Text("Masukkan kata sandi admin") },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = TsmGearDark)
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
                            .testTag("admin_input_password")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            isLoading = true
                            errorMessage = null

                            viewModel.loginAdmin(
                                username = username,
                                pass = password,
                                onSuccess = {
                                    isLoading = false
                                    onLoginSuccess()
                                },
                                onError = { err ->
                                    isLoading = false
                                    errorMessage = err
                                }
                            )
                        },
                        enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = TsmGearDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_login_admin")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("MASUK DASHBOARD ADMINISTRATOR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SuggestionChip(
                        onClick = {
                            username = "admin"
                            password = "admin123"
                        },
                        label = { Text("Kredensial Awal: admin / admin123", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Switch to Student App Launcher button
            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(context, Class.forName("com.example.MainActivity"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // fallback
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .testTag("btn_open_student_app"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = TsmNavyPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buka Aplikasi Siswa PKL", fontSize = 12.sp, color = TsmNavyPrimary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Pengelolaan Jadwal • Rekap Google Sheets • Foto Google Drive\nSMK Negeri Cipatujah",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

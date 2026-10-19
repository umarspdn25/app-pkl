package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.google.android.gms.location.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceCaptureScreen(
    type: String, // MASUK / PULANG
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val student by viewModel.currentStudent.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()
    val currentLat by viewModel.currentLatitude.collectAsState()
    val currentLng by viewModel.currentLongitude.collectAsState()
    val gpsAccuracy by viewModel.gpsAccuracy.collectAsState()
    val isMock by viewModel.isMockLocation.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Start GPS Location Acquisition
    @SuppressLint("MissingPermission")
    fun requestLocation() {
        if (!hasLocationPermission) return
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                val isMockLoc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    loc.isMock
                } else {
                    loc.isFromMockProvider
                }
                viewModel.setLocation(loc.latitude, loc.longitude, loc.accuracy.toDouble(), isMockLoc)
            } else {
                // Request single high-accuracy update
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()
                fusedClient.getCurrentLocation(request, null).addOnSuccessListener { newLoc ->
                    if (newLoc != null) {
                        val isMockLoc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) newLoc.isMock else newLoc.isFromMockProvider
                        viewModel.setLocation(newLoc.latitude, newLoc.longitude, newLoc.accuracy.toDouble(), isMockLoc)
                    }
                }
            }
        }.addOnFailureListener {
            locationError = "Gagal membaca GPS: ${it.message}"
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
        requestLocation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Foto Selfie $type",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TsmNavyPrimary)
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasCameraPermission) {
                // Camera permission notice
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Izin Kamera Diperlukan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Aplikasi presensi PKL TSM mewajibkan pengambilan foto selfie langsung untuk verifikasi kehadiran.",
                        fontSize = 13.sp,
                        color = Color(0xFFE2E8F0),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = TsmRedSecondary)
                    ) {
                        Text("Izinkan Kamera")
                    }
                }
            } else if (capturedBitmap != null) {
                // Photo Preview & Submission Confirmation
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Foto Selfie Presensi",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }

                    // Location telemetry card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TsmRedSecondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Telemetri Lokasi & GPS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Koordinat: %.6f, %.6f".format(currentLat, currentLng),
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Akurasi GPS: %.1f meter ${if (gpsAccuracy > 50) "(Kurang akurat)" else "(Sangat Baik)"}".format(gpsAccuracy),
                                color = if (gpsAccuracy > 50) Color(0xFFFFB74D) else Color(0xFF81C784),
                                fontSize = 12.sp
                            )
                            if (isMock) {
                                Text(
                                    text = "PERINGATAN: Terdeteksi Fake GPS / Lokasi Tiruan!",
                                    color = Color(0xFFFF5252),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (errorMessage != null) {
                        Surface(
                            color = TsmErrorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = TsmOnRedContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.setCapturedBitmap(null)
                                errorMessage = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_retake_photo"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ambil Ulang")
                        }

                        Button(
                            onClick = {
                                showConfirmDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_confirm_send"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "MASUK") TsmNavyPrimary else TsmRedSecondary
                            ),
                            enabled = !isSubmitting && !isMock
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kirim Presensi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Live CameraX Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            // Use Front Camera for direct selfie capture
                            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (exc: Exception) {
                                // Camera bind failed
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Face Oval Guide Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 240.dp, height = 310.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(120.dp))
                    )
                }

                // Header instructions
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 20.dp)
                ) {
                    Text(
                        text = "Posisikan wajah Anda di dalam lingkaran panduan",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Shutter Button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                ) {
                    IconButton(
                        onClick = {
                            val imgCap = imageCapture ?: return@IconButton
                            val executor = Executors.newSingleThreadExecutor()

                            imgCap.takePicture(
                                executor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        val bitmap = imageProxyToBitmap(imageProxy)
                                        imageProxy.close()
                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                            viewModel.setCapturedBitmap(bitmap)
                                            requestLocation()
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        errorMessage = "Gagal mengambil foto: ${exception.message}"
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(4.dp, TsmNavyPrimary, CircleShape)
                            .testTag("btn_shutter_capture")
                    ) {
                        Icon(
                            Icons.Default.Camera,
                            contentDescription = "Ambil Foto",
                            tint = TsmNavyPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text("Konfirmasi Presensi $type", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Pastikan foto selfie dan lokasi Anda sudah benar sebelum mengirimkan ke Google Sheets.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Siswa: ${student?.name ?: "-"}", fontSize = 13.sp)
                    Text("• Bengkel: ${student?.pklPlace ?: "-"}", fontSize = 13.sp)
                    Text("• Koordinat: %.5f, %.5f".format(currentLat, currentLng), fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.submitAttendance(
                            type = type,
                            onSuccess = {
                                onNavigateBack()
                            },
                            onError = { err ->
                                errorMessage = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TsmNavyPrimary)
                ) {
                    Text("Ya, Kirim Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// Convert ImageProxy to Bitmap with correct orientation
private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planeProxy = image.planes[0]
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val matrix = Matrix().apply {
        // Front camera rotation and mirroring
        postRotate(image.imageInfo.rotationDegrees.toFloat())
        postScale(-1f, 1f)
    }
    return Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
}

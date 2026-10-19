package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.navigation.Screen
import com.example.ui.theme.TsmNavyPrimary
import com.example.ui.theme.TsmRedSecondary
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val scale = remember { Animatable(0.7f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        delay(1400)
        if (viewModel.currentStudent.value != null) {
            onNavigate(Screen.Home.route)
        } else {
            onNavigate(Screen.Login.route)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TsmNavyPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .scale(scale.value)
        ) {
            // Official SMK Negeri Cipatujah TSM Emblem Logo
            Image(
                painter = painterResource(id = R.drawable.logo_smk_cipatujah),
                contentDescription = "Logo Resmi SMK Negeri Cipatujah",
                modifier = Modifier
                    .size(160.dp)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PKL TSM",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Text(
                text = "SMK NEGERI CIPATUJAH",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Teknik Sepeda Motor • Presensi Digital",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF90CAF9),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = "Versi 1.0.0 • Google Sheets & Drive Synchronized",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

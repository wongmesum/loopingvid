package com.example.feature.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onStartOnboarding: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showLicensesDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tentang Aplikasi & Pengembang",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("about_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .background(Color.Black)
                .padding(bottom = 24.dp)
        ) {
            // Hero Illustration Header
            AboutHeroHeader()

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Developer Profile Card ("About Me")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_me_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Developer",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Tentang Pengembang (About Me)",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Developer",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Asrocia Studio • ulas.tech",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Divider(color = Color(0xFF334155))

                        Text(
                            text = "Halo! Saya Asrocia, pengembang mandiri di balik LoopingVid & ulas.tech. Misi saya adalah menghadirkan alat kreasi video dan live streaming RTMP tingkat profesional yang ringan, aman, dan memproses data 100% di dalam HP pengguna tanpa iklan mengganggu.",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        // Contact & Social Badges Row
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DeveloperLinkChip(
                                icon = Icons.Default.Email,
                                label = "asrocia@gmail.com",
                                onClick = { sendEmail(context, "asrocia@gmail.com") },
                                testTag = "about_chip_email"
                            )
                            DeveloperLinkChip(
                                icon = Icons.Default.Language,
                                label = "ulas.tech",
                                onClick = { openWebsite(context, "https://ulas.tech") },
                                testTag = "about_chip_website"
                            )
                            DeveloperLinkChip(
                                icon = Icons.Default.Group,
                                label = "Komunitas FB",
                                onClick = { openWebsite(context, "https://www.facebook.com/share/1LwG2YHbik/") },
                                testTag = "about_chip_fb"
                            )
                        }
                    }
                }

                // Section 2: Core Capabilities & App Highlights Badges
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_app_features_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieFilter,
                                contentDescription = "Features",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Keunggulan & Standar Google Play",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FeatureBadgeRow(
                            icon = Icons.Default.Verified,
                            title = "100% Gratis & Tanpa Login",
                            description = "Langsung gunakan seluruh fitur editing, rendering, dan live stream tanpa perlu registrasi atau login akun."
                        )

                        FeatureBadgeRow(
                            icon = Icons.Default.Terminal,
                            title = "FFmpeg Multi-Thread Engine",
                            description = "Pemrosesan video loop, trimming, dan audio mastering berkinerja tinggi."
                        )

                        FeatureBadgeRow(
                            icon = Icons.Default.Radio,
                            title = "RTMP Live Stream 24/7",
                            description = "Penyiaran langsung ke YouTube Live, TikTok Live, dan Twitch dengan camera overlay."
                        )

                        FeatureBadgeRow(
                            icon = Icons.Default.Shield,
                            title = "Privasi Terjamin 100% On-Device",
                            description = "Tidak ada unggahan file tanpa izin, kredensial RTMP tersimpan terenkripsi secara lokal."
                        )
                    }
                }

                // Section 3: FAQ (Frequently Asked Questions)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_faq_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = "FAQ",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Pertanyaan Umum (FAQ)",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Jawaban seputar rendering video, paket lisensi, & live stream",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Divider(color = Color(0xFF334155))

                        FaqAccordionGroup(
                            categoryTitle = "Pemrosesan & Rendering Video",
                            categoryIcon = Icons.Default.Movie,
                            testTagPrefix = "faq_video",
                            items = listOf(
                                "Mengapa proses rendering video membutuhkan waktu?" to "Waktu rendering FFmpeg bergantung pada resolusi (1080p / 4K), bitrate, dan jumlah perulangan video. LoopingVid menggunakan engine multi-thread native yang memaksimalkan core CPU HP Anda secara efisien.",
                                "Format video & audio apa saja yang didukung?" to "LoopingVid mendukung penuh file video MP4 (H.264), MKV, MOV, WEBM, serta trek suara MP3, AAC, dan WAV untuk mastering audio studio."
                            )
                        )

                        Divider(color = Color(0xFF334155))

                        FaqAccordionGroup(
                            categoryTitle = "Paket Lisensi & Google Play Billing",
                            categoryIcon = Icons.Default.Star,
                            testTagPrefix = "faq_billing",
                            items = listOf(
                                "Apakah saya perlu membuat akun atau login?" to "Sama sekali tidak! LoopingVid dirancang 100% tanpa login. Anda bisa langsung menggunakan seluruh fitur tanpa perlu mengisi data registrasi atau mengingat kata sandi.",
                                "Apakah LoopingVid dapat digunakan secara gratis?" to "Ya! Semua fitur inti seperti pemotong video, looper perulangan, mastering audio dasar, dan live stream RTMP standar dapat Anda gunakan secara penuh tanpa watermark paksaan.",
                                "Apasaja keunggulan Lisensi Pro Studio?" to "Lisensi Pro Studio membuka ekspor 4K Ultra HD tanpa batas, mastering audio multi-preset canggih, fitur subtitle otomatis AI Gemini, dan prioritas antrean ekspor tanpa gangguan. Pembelian diverifikasi langsung via Google Play Store."
                            )
                        )

                        Divider(color = Color(0xFF334155))

                        FaqAccordionGroup(
                            categoryTitle = "Pemecahan Masalah Live Streaming (RTMP)",
                            categoryIcon = Icons.Default.Radio,
                            testTagPrefix = "faq_stream",
                            items = listOf(
                                "Mengapa siaran langsung terputus di latar belakang?" to "Pastikan izin Notifikasi & Foreground Service diaktifkan, serta matikan 'Optimasi Baterai' (Battery Optimization) untuk LoopingVid di Pengaturan HP agar OS Android tidak menghentikan siaran RTMP.",
                                "Bagaimana cara mendapatkan Kunci Streaming (Stream Key)?" to "Untuk YouTube Live, buka YouTube Studio di browser (mode Desktop) > Live Control Room > salin Stream URL & Key. Untuk TikTok/Twitch, salin RTMP key dari dashboard siaran langsung Anda."
                            )
                        )
                    }
                }

                // Section 4: Action Buttons Menu List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                ) {
                    AboutMenuItem(
                        icon = Icons.Default.HelpOutline,
                        title = "Panduan Interaktif Studio (Tour)",
                        testTag = "about_menu_onboarding_tour",
                        onClick = onStartOnboarding
                    )

                    HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                    AboutMenuItem(
                        icon = Icons.Default.PrivacyTip,
                        title = "Kebijakan Privasi & Kebijakan Data",
                        testTag = "about_menu_privacy",
                        onClick = onNavigateToPrivacy
                    )

                    HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                    AboutMenuItem(
                        icon = Icons.Default.Favorite,
                        title = "Dukung Proyek ini",
                        testTag = "about_menu_support",
                        onClick = onNavigateToSupport
                    )

                    HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                    AboutMenuItem(
                        icon = Icons.Default.Share,
                        title = "Bagikan Aplikasi",
                        testTag = "about_menu_share",
                        onClick = { shareApp(context) }
                    )

                    HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                    AboutMenuItem(
                        icon = Icons.Outlined.ThumbUp,
                        title = "Beri Peringkat di Play Store",
                        testTag = "about_menu_rate",
                        onClick = { rateApp(context) }
                    )

                    HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                    AboutMenuItem(
                        icon = Icons.Default.Code,
                        title = "Lisensi Kode Sumber Terbuka (Open Source)",
                        testTag = "about_menu_licenses",
                        onClick = { showLicensesDialog = true }
                    )
                }

                // Section 4: App Version & Copyright Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "LoopingVid v1.0.0 (Build 100)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "© 2026 Asrocia Studio • ulas.tech. All rights reserved.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Kompatibel dengan Pedoman Pengembang Google Play Store",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    // Open Source Licenses Dialog
    if (showLicensesDialog) {
        OpenSourceLicensesDialog(onDismiss = { showLicensesDialog = false })
    }
}

@Composable
private fun DeveloperLinkChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FeatureBadgeRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF10B981).copy(alpha = 0.15f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun AboutHeroHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(Color(0xFF0D1017))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Dark room background gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF090D16),
                        Color(0xFF05070B)
                    )
                )
            )

            // Monitor ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.25f),
                        Color(0xFF1E293B).copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.75f, height * 0.35f),
                    radius = width * 0.45f
                )
            )

            val monitorLeft = width * 0.55f
            val monitorTop = height * 0.12f
            val monitorWidth = width * 0.38f
            val monitorHeight = height * 0.55f

            // Monitor stand
            drawRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(monitorLeft + monitorWidth * 0.4f, monitorTop + monitorHeight),
                size = Size(monitorWidth * 0.2f, height * 0.18f)
            )

            // Monitor screen
            drawRoundRect(
                color = Color(0xFF020617),
                topLeft = Offset(monitorLeft, monitorTop),
                size = Size(monitorWidth, monitorHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )

            // Code lines
            for (i in 0..8) {
                val lineY = monitorTop + 20f + (i * 16f)
                val lineWidth = if (i % 3 == 0) monitorWidth * 0.5f else monitorWidth * 0.7f
                val lineColor = when (i % 3) {
                    0 -> Color(0xFF38BDF8)
                    1 -> Color(0xFF10B981)
                    else -> Color(0xFF94A3B8)
                }
                drawRect(
                    color = lineColor.copy(alpha = 0.85f),
                    topLeft = Offset(monitorLeft + 16f, lineY),
                    size = Size(lineWidth, 5f)
                )
            }
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black
                        )
                    )
                )
        )

        // Text Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "LoopingVid",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = Color(0xFF0284C7),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "PRO STUDIO",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Aplikasi Looper Video & Siaran Langsung RTMP 24/7 Terpercaya.",
                color = Color(0xFFD1D5DB),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun AboutMenuItem(
    icon: ImageVector,
    title: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun OpenSourceLicensesDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Lisensi Komponen Terbuka",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "LoopingVid dibangun dengan memanfaatkan pustaka sumber terbuka berkualitas tinggi berikut:",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LicenseItem("Jetpack Compose & Material 3", "Apache License 2.0 (Google LLC)")
                    LicenseItem("Kotlin Coroutines & Flow", "Apache License 2.0 (JetBrains s.r.o.)")
                    LicenseItem("FFmpeg Mobile Processing Kit", "LGPL v2.1 / GPL v3.0")
                    LicenseItem("SQLite Room Database Persistence", "Apache License 2.0 (Google LLC)")
                    LicenseItem("ExoPlayer / Media3 Engine", "Apache License 2.0 (Google LLC)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Tutup", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun LicenseItem(name: String, license: String) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = license, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
    }
}

private fun shareApp(context: Context) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "LoopingVid App")
            putExtra(Intent.EXTRA_TEXT, "Coba LoopingVid - Aplikasi Looper Video & RTMP Live Streamer 24/7 buatan ulas.tech!")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan LoopingVid"))
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak dapat membuka menu bagikan", Toast.LENGTH_SHORT).show()
    }
}

private fun rateApp(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
        context.startActivity(intent)
    } catch (e: Exception) {
        openWebsite(context, "https://play.google.com/store/apps/details?id=${context.packageName}")
    }
}

private fun sendEmail(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
            putExtra(Intent.EXTRA_SUBJECT, "Tanya Pengembang LoopingVid")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Email: $email", Toast.LENGTH_LONG).show()
    }
}

private fun openWebsite(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Website: $url", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun FaqAccordionGroup(
    categoryTitle: String,
    categoryIcon: ImageVector,
    testTagPrefix: String,
    items: List<Pair<String, String>>
) {
    var expandedIndex by remember { mutableStateOf<Int?>(0) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = categoryTitle,
                color = Color(0xFF10B981),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items.forEachIndexed { index, (question, answer) ->
            val isExpanded = expandedIndex == index

            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isExpanded) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color(0xFF334155)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_item_$index")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedIndex = if (isExpanded) null else index
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = question,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Tutup" else "Buka",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = answer,
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

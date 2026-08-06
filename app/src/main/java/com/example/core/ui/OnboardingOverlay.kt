package com.example.core.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class OnboardingStep(
    val stepIndex: Int,
    val totalSteps: Int,
    val route: String,
    val moduleTitle: String,
    val moduleSubtitle: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val highlights: List<String>,
    val targetTestTag: String
)

fun getOnboardingSteps(): List<OnboardingStep> = listOf(
    OnboardingStep(
        stepIndex = 1,
        totalSteps = 5,
        route = "loop",
        moduleTitle = "Modul Video Looper",
        moduleSubtitle = "Perulangan & Pengatur Durasi",
        title = "1. Penggandaan Video & Trimming",
        description = "Pilih video lokal Anda, tentukan kelipatan durasi (2x hingga 100x), dan potong segmen tertentu dengan pemotong presisi sebelum diproses oleh engine FFmpeg.",
        icon = Icons.Default.Loop,
        highlights = listOf(
            "Duplikasi durasi video tanpa batas hingga 24 jam",
            "Trimming segmen presisi per-milidetik",
            "Ekspor langsung ke antrean render latar belakang"
        ),
        targetTestTag = "tab_loop"
    ),
    OnboardingStep(
        stepIndex = 2,
        totalSteps = 5,
        route = "editor",
        moduleTitle = "Editor Studio Pro",
        moduleSubtitle = "Pengeditan Multi-Track & Pemotongan",
        title = "2. Lapisan Audio & Rasio Layar",
        description = "Sematkan musik latar, atur volume campuran, sesuaikan rasio aspek (16:9 Landscape, 9:16 Shorts/Reels, 1:1 Instagram), serta atur kecepatan putar dengan preview FFmpeg.",
        icon = Icons.Default.Edit,
        highlights = listOf(
            "Pencampuran trek audio ganda & kontrol pitch",
            "Pemotongan rasio aspek fleksibel (Shorts/YT)",
            "Pengatur kecepatan video 0.25x hingga 4.0x"
        ),
        targetTestTag = "tab_editor"
    ),
    OnboardingStep(
        stepIndex = 3,
        totalSteps = 5,
        route = "mastering",
        moduleTitle = "Mastering Audio Studio",
        moduleSubtitle = "Equalizer & Normalisasi Suara",
        title = "3. Pemrosesan Audio Profesional",
        description = "Tingkatkan kualitas audio dengan preset EQ studio (Bass Boost, Vocal Clarity, Podcast), tingkatkan volume desibel (LUFS), dan aktifkan peredam bising otomatis.",
        icon = Icons.Default.GraphicEq,
        highlights = listOf(
            "10 Band Equalizer Studio & Preset Siap Pakai",
            "Peredam Bising Audio (Noise Gate & Suppressor)",
            "Auto LUFS Loudness Normalization"
        ),
        targetTestTag = "tab_mastering"
    ),
    OnboardingStep(
        stepIndex = 4,
        totalSteps = 5,
        route = "live",
        moduleTitle = "Live Broadcast RTMP",
        moduleSubtitle = "Siaran Langsung Continuous 24/7",
        title = "4. Penyiaran Live Multi-Platform",
        description = "Hubungkan siaran langsung ke YouTube Live, TikTok Live, atau Twitch via RTMP. Dilengkapi kamera overlay langsung, kontrol bitrate, & pemantau koneksi.",
        icon = Icons.Default.Radio,
        highlights = listOf(
            "Penyiaran RTMP Ingestion langsung tanpa jeda",
            "Overlay kamera langsung dengan posisi fleksibel",
            "Pengawasan bitrate & status daya baterai"
        ),
        targetTestTag = "tab_live"
    ),
    OnboardingStep(
        stepIndex = 5,
        totalSteps = 5,
        route = "projects",
        moduleTitle = "Project Manager",
        moduleSubtitle = "Manajemen Proyek & Riwayat",
        title = "5. Kelola Proyek Kreatif",
        description = "Simpan proyek loop video, slideshow, dan visualizer dalam satu tempat. Riwayat render tetap tersedia dari tombol Riwayat.",
        icon = Icons.Default.History,
        highlights = listOf(
            "Foreground Service rendering tanpa gangguan",
            "Notifikasi progres ekspor real-time",
            "Ringkasan spesifikasi ekspor komplit"
        ),
        targetTestTag = "tab_proyek"
    )
)

object OnboardingPrefs {
    private const val PREF_NAME = "onboarding_prefs"
    private const val KEY_COMPLETED = "has_completed_onboarding"

    fun isCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun setCompleted(context: Context, completed: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_COMPLETED, completed).apply()
    }
}

@Composable
fun OnboardingOverlay(
    showOnboarding: Boolean,
    currentStepIndex: Int,
    onStepChange: (Int) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onFinishOnboarding: () -> Unit,
    onSkipOnboarding: () -> Unit
) {
    if (!showOnboarding) return

    val steps = remember { getOnboardingSteps() }
    val safeIndex = currentStepIndex.coerceIn(0, steps.size - 1)
    val currentStep = steps[safeIndex]

    // Pulsing animation for target CoachMark spotlight
    val infiniteTransition = rememberInfiniteTransition(label = "coachmark_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF0090D16))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { /* Block touches to underlying UI components */ }
            .testTag("onboarding_overlay_container")
    ) {
            // Scrim & Canvas Spotlight Highlight
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Ambient glow behind target bottom nav position
                val targetXFraction = when (currentStep.route) {
                    "loop" -> 0.10f
                    "mastering" -> 0.30f
                    "editor" -> 0.50f
                    "live" -> 0.70f
                    "projects" -> 0.90f
                    else -> 0.50f
                }

                val centerX = canvasWidth * targetXFraction
                val centerY = canvasHeight - 110f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.45f),
                            Color(0xFF0284C7).copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = 180f * pulseScale
                    )
                )

                drawCircle(
                    color = Color(0xFF38BDF8),
                    center = Offset(centerX, centerY),
                    radius = 45f * pulseScale,
                    style = Stroke(width = 4f)
                )
            }

            // Top Header Skip & Close Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "PANDUAN STUDIO INTERAKTIF",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onSkipOnboarding,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Panduan",
                        tint = Color.White
                    )
                }
            }

            // Central Floating CoachMark Tooltip Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .align(Alignment.Center)
                    .testTag("onboarding_coachmark_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Step Counter Pill & Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LANGKAH ${currentStep.stepIndex} DARI ${currentStep.totalSteps}",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(currentStep.stepIndex * 100) / currentStep.totalSteps}%",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        LinearProgressIndicator(
                            progress = currentStep.stepIndex / currentStep.totalSteps.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF334155)
                        )
                    }

                    // Module Title Header Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7).copy(alpha = 0.25f),
                            modifier = Modifier.size(48.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = currentStep.icon,
                                    contentDescription = currentStep.moduleTitle,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentStep.moduleTitle,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentStep.moduleSubtitle,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Divider(color = Color(0xFF334155))

                    // Step Detail Title & Description
                    Text(
                        text = currentStep.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = currentStep.description,
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )

                    // Highlights Bullet List
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        currentStep.highlights.forEach { feature ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = feature,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Page Indicator Dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        steps.forEachIndexed { idx, _ ->
                            val active = idx == safeIndex
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (active) 10.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) Color(0xFF38BDF8) else Color(0xFF475569)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom Navigation Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (safeIndex > 0) {
                            OutlinedButton(
                                onClick = {
                                    val prevIndex = safeIndex - 1
                                    onStepChange(prevIndex)
                                    onNavigateToRoute(steps[prevIndex].route)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("onboarding_prev_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kembali", fontSize = 13.sp)
                            }
                        } else {
                            TextButton(
                                onClick = onSkipOnboarding,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("onboarding_skip_text_button")
                            ) {
                                Text("Lompati", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (safeIndex < steps.size - 1) {
                                    val nextIndex = safeIndex + 1
                                    onStepChange(nextIndex)
                                    onNavigateToRoute(steps[nextIndex].route)
                                } else {
                                    onFinishOnboarding()
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("onboarding_next_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (safeIndex == steps.size - 1) "Mulai Jelajah" else "Lanjut",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

package com.example.feature.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kebijakan Privasi & Keamanan Data",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Standar Perlindungan Data Google Play",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("privacy_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Google Play Safety Standard Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .testTag("privacy_safety_badge_banner"),
                color = Color(0xFF064E3B).copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Security Shield",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "100% On-Device Processing Guaranteed",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aplikasi ini memproses semua video/audio secara lokal di HP Anda. Kami tidak menjual atau mengunggah file media Anda ke server luar.",
                            color = Color(0xFFD1D5DB),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Section 1: Ringkasan Pengumpulan & Penggunaan Data
            PolicySectionCard(
                icon = Icons.Default.PrivacyTip,
                title = "1. Pengumpulan & Penggunaan Data",
                subtitle = "Transparansi penuh sesuai pedoman Google Play User Data Policy",
                defaultExpanded = true,
                testTag = "policy_section_data_collection"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Aplikasi LoopingVid ('kami') berkomitmen penuh menjaga privasi pengguna. Berikut rincian penggunaan data:",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    BulletPoint(
                        title = "Tanpa Pendaftaran & Tanpa Login Akun",
                        description = "LoopingVid dapat digunakan secara penuh tanpa pendaftaran akun, login, atau input data pribadi. Seluruh fitur editing, live streaming, dan riwayat tersimpan 100% lokal."
                    )
                    BulletPoint(
                        title = "File Media Lokal (Video/Audio)",
                        description = "Semua proses looping video, mastering audio, dan rendering FFmpeg dilakukan 100% di dalam memori internal perangkat Anda."
                    )
                    BulletPoint(
                        title = "Data Kredensial RTMP & API Key",
                        description = "Kunci streaming RTMP (YouTube Live, TikTok Live) dan API Key Gemini disimpan di basis data lokal aplikasi dan tidak pernah dikirimkan ke pihak ketiga. Penyimpanan ini belum dienkripsi, namun pencadangan otomatis (adb backup) sudah dinonaktifkan sehingga isinya tidak dapat diekstrak dari perangkat."
                    )
                    BulletPoint(
                        title = "Log Performa & Analitik Anonim",
                        description = "Guna meningkatkan stabilitas aplikasi, kami dapat mengumpulkan metrik log kerusakan (crash logs) anonim tanpa mengidentifikasi identitas pribadi pengguna."
                    )
                }
            }

            // Section 2: Penjelasan Izin Perangkat (Permissions Disclosure)
            PolicySectionCard(
                icon = Icons.Default.PhonelinkSetup,
                title = "2. Pengungkapan Izin Perangkat (Runtime Permissions)",
                subtitle = "Alasan kebutuhan izin akses sesuai standar Play Store",
                defaultExpanded = true,
                testTag = "policy_section_permissions"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PermissionRow(
                        icon = Icons.Default.CameraAlt,
                        name = "Kamera (CAMERA)",
                        description = "Diperlukan hanya saat Anda memilih opsi live stream dengan feed kamera langsung atau webcam overlay."
                    )
                    PermissionRow(
                        icon = Icons.Default.Mic,
                        name = "Mikrofon (RECORD_AUDIO)",
                        description = "Diperlukan untuk merekam audio mikrofon secara real-time saat melakukan siaran langsung RTMP."
                    )
                    PermissionRow(
                        icon = Icons.Default.Folder,
                        name = "Penyimpanan (READ/WRITE_EXTERNAL_STORAGE)",
                        description = "Digunakan khusus untuk memilih video yang akan di-loop/diedit dan menyimpan hasil ekspor ke folder Movies/Downloads."
                    )
                    PermissionRow(
                        icon = Icons.Default.NotificationsActive,
                        name = "Foreground Service & Notifikasi",
                        description = "Digunakan untuk menampilkan notifikasi status aktif saat proses render FFmpeg berjalan di latar belakang agar OS Android tidak menghentikan ekspor secara tiba-tiba."
                    )
                }
            }

            // Section 3: Keamanan Layanan Pihak Ketiga & Sync Cloud
            PolicySectionCard(
                icon = Icons.Default.Security,
                title = "3. Layanan Pihak Ketiga & Cloud Sync",
                subtitle = "Firebase Firestore, YouTube Live RTMP & Gemini AI",
                defaultExpanded = false,
                testTag = "policy_section_third_party"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Aplikasi ini dapat berinteraksi dengan layanan eksternal pilihan Anda:",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp
                    )
                    BulletPoint(
                        title = "YouTube & TikTok RTMP Ingestion",
                        description = "Saat Anda melakukan siaran langsung, aliran data video dikirim langsung dari HP Anda ke server RTMP resmi target (misalnya rtmp://a.rtmp.youtube.com/live2) sesuai dengan ketentuan privasi masing-masing platform."
                    )
                    BulletPoint(
                        title = "Firebase Firestore (Opsional)",
                        description = "Jika Anda mengaktifkan opsi Cloud History Sync di Pengaturan, hanya riwayat nama job dan status ekspor yang disinkronkan secara aman ke Firebase Firestore."
                    )
                    BulletPoint(
                        title = "Google Gemini AI (Opsional)",
                        description = "Jika Anda memasukkan API Key Gemini untuk fitur transkripsi otomatis, permintaan dikirimkan langsung ke Google Gemini API secara aman via TLS/HTTPS."
                    )
                }
            }

            // Section 4: Hak Pengguna, Penghapusan Data & COPPA
            PolicySectionCard(
                icon = Icons.Default.ChildCare,
                title = "4. Hak Pengguna & Perlindungan Anak (COPPA)",
                subtitle = "Penghapusan data dan batasan usia pengguna",
                defaultExpanded = false,
                testTag = "policy_section_user_rights"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BulletPoint(
                        title = "Penghapusan Data Mandiri",
                        description = "Anda dapat menghapus seluruh kredensial, sejarah ekspor, dan pengaturan kapan saja cukup dengan memilih 'Hapus Data Aplikasi' di Pengaturan HP atau meng-uninstall aplikasi."
                    )
                    BulletPoint(
                        title = "Perlindungan Anak Di Bawah Umur",
                        description = "Aplikasi ini tidak ditujukan secara khusus untuk anak-anak di bawah usia 13 tahun (COPPA Compliance). Kami tidak secara sengaja mengumpulkan informasi pribadi dari anak-anak."
                    )
                }
            }

            // Section 5: Ketentuan Layanan & Hak Cipta
            PolicySectionCard(
                icon = Icons.Default.Gavel,
                title = "5. Ketentuan Penggunaan & Hak Cipta",
                subtitle = "Tanggung jawab konten dan kepatuhan hukum",
                defaultExpanded = false,
                testTag = "policy_section_terms"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Dengan menggunakan LoopingVid, Anda menyetujui ketentuan berikut:",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp
                    )
                    BulletPoint(
                        title = "Hak Cipta Konten",
                        description = "Pengguna bertanggung jawab penuh atas hak cipta musik, gambar, dan video yang diproses atau disiarkan secara langsung. Dilarang menyiarkan konten hak cipta tanpa izin."
                    )
                    BulletPoint(
                        title = "Kepatuhan Platform Streaming",
                        description = "Pengguna wajib mematuhi Community Guidelines dari platform target (YouTube, TikTok, Twitch, Facebook Live). Pengembang tidak bertanggung jawab atas sanksi atau penangguhan akun oleh pihak ketiga."
                    )
                }
            }

            // Section 6: Kontak Pengembang & Bantuan
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_contact_card"),
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
                            imageVector = Icons.Default.Info,
                            contentDescription = "Contact",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Kontak Pengembang & Pembaruan",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Jika Anda memiliki pertanyaan mengenai kebijakan privasi ini atau ingin mengajukan masukan:",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { sendEmail(context, "asrocia@gmail.com") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = "Email", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("asrocia@gmail.com", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { openWebsite(context, "https://ulas.tech") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = "Website", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ulas.tech", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Terakhir diperbarui: 1 Agustus 2026",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )

                        Text(
                            text = "Salin Tautan Privasi",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString("https://ulas.tech/privacy-policy"))
                                Toast.makeText(context, "Tautan kebijakan privasi disalin!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PolicySectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    defaultExpanded: Boolean = false,
    testTag: String,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(title: String, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, name: String, description: String) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

private fun sendEmail(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
            putExtra(Intent.EXTRA_SUBJECT, "Pertanyaan Privasi LoopingVid")
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

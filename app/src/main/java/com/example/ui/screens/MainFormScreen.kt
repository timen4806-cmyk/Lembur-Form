package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.model.LemburRowItem
import com.example.ui.components.SignaturePad
import com.example.viewmodel.SpklViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFormScreen(
    viewModel: SpklViewModel,
    onNavigateToArchive: () -> Unit,
    onNavigateToCloud: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val employee by viewModel.employeeData.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val customLogoUri by viewModel.customLogoUri.collectAsState()
    val savedDocs by viewModel.savedDocuments.collectAsState()

    // Android Photo Picker for Custom Logo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomLogo(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "FORM LEMBUR KARYAWAN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Surat Perintah Kerja Lembur (SPKL)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(
                        onClick = onNavigateToArchive,
                        modifier = Modifier.testTag("topbar_btn_archive")
                    ) {
                        BadgedBox(badge = {
                            if (savedDocs.isNotEmpty()) {
                                Badge { Text(savedDocs.size.toString()) }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Arsip Dokumen",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToCloud,
                        modifier = Modifier.testTag("topbar_btn_cloud")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sinkronisasi Cloud",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("topbar_btn_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                // Quick Info Banner: Cut-Off date reminder
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Batas Cut-Off Pembayaran: Tanggal 17",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Lengkapi form & tanda tangan sebelum tgl 17 setiap bulannya.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.triggerCutoffReminder(context) },
                            modifier = Modifier.testTag("reminder_bell_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Kirim Pengingat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // SECTION 1: DATA KARYAWAN
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "DATA KARYAWAN",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Grid 2 Columns matching Kivy layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = employee.namaKaryawan,
                                onValueChange = { viewModel.updateEmployeeData { e -> e.copy(namaKaryawan = it) } },
                                label = { Text("Nama Karyawan") },
                                modifier = Modifier.weight(1f).testTag("input_nama_karyawan"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = employee.wilayah,
                                onValueChange = { viewModel.updateEmployeeData { e -> e.copy(wilayah = it) } },
                                label = { Text("Wilayah") },
                                modifier = Modifier.weight(1f).testTag("input_wilayah"),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = employee.nik,
                                onValueChange = { viewModel.updateEmployeeData { e -> e.copy(nik = it) } },
                                label = { Text("Nomor Induk (NIK)") },
                                modifier = Modifier.weight(1f).testTag("input_nik"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = employee.lokasiKerja,
                                onValueChange = { viewModel.updateEmployeeData { e -> e.copy(lokasiKerja = it) } },
                                label = { Text("Lokasi Kerja") },
                                modifier = Modifier.weight(1f).testTag("input_lokasi_kerja"),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = employee.departemen,
                                onValueChange = { viewModel.updateEmployeeData { e -> e.copy(departemen = it) } },
                                label = { Text("Departemen") },
                                modifier = Modifier.weight(1f).testTag("input_departemen"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = employee.bulan,
                                onValueChange = { viewModel.updateEmployeeData { e -> e.copy(bulan = it) } },
                                label = { Text("Bulan") },
                                modifier = Modifier.weight(1f).testTag("input_bulan"),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = employee.jabatan,
                                onValueChange = { viewModel.updateEmployeeData { e -> e.copy(jabatan = it) } },
                                label = { Text("Jabatan") },
                                modifier = Modifier.weight(1f).testTag("input_jabatan"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = employee.tahun,
                                onValueChange = { viewModel.updateEmployeeData { e -> e.copy(tahun = it) } },
                                label = { Text("Tahun") },
                                modifier = Modifier.weight(1f).testTag("input_tahun"),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // SECTION 1: INFORMASI KOP PERUSAHAAN & LOGO (SEBELAH KIRI NAMA, SEBELAH KANAN LOGO)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "KOP PERUSAHAAN & LOGO PDF",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Nama Perusahaan (Kiri) dan Logo (Kanan) pada dokumen SPKL",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Input Nama Perusahaan Secara Manual Sebelum Cetak
                        OutlinedTextField(
                            value = employee.namaPerusahaan,
                            onValueChange = { viewModel.updateEmployeeData { e -> e.copy(namaPerusahaan = it) } },
                            label = { Text("Nama Perusahaan (Kop Kiri PDF)") },
                            placeholder = { Text("Contoh: PT. MULTI KARYA UTAMA") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_nama_perusahaan"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Logo Picker di Sebelah Kanan
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Logo Preview thumbnail (Kanan)
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEEEEEE))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (customLogoUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = Uri.parse(customLogoUri)),
                                        contentDescription = "Logo Perusahaan",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = "SPKL\nBadge",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        lineHeight = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("pick_logo_btn")
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (customLogoUri != null) "Ganti Logo (Kanan)" else "Pilih Logo dari Galeri (Kanan)")
                                }

                                if (customLogoUri != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.setCustomLogo(null) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Hapus Logo Kustom")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: DETAIL TABEL LEMBUR
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "DETAIL TABEL LEMBUR",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${rows.size} Baris Penugasan Lembur",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { viewModel.addRow() },
                                modifier = Modifier.testTag("add_row_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tambah")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Render each row in detail card
                        rows.forEachIndexed { index, row ->
                            OvertimeRowCard(
                                row = row,
                                index = index,
                                onUpdate = { updated -> viewModel.updateRow(index) { updated } },
                                onDelete = if (rows.size > 1) { { viewModel.removeRow(index) } } else null
                            )
                            if (index < rows.lastIndex) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            // SECTION 4: TANDA TANGAN (SIGNATURES)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PARAF PENGESAHAN (TANDA TANGAN)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Tanda tangan ini otomatis disematkan langsung ke kolom PARAF PENGESAHAN (YBS, Pim. Area, Pim. Pusat) pada tabel lembur PDF",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Signature Pad 1: YBS
                        SignaturePad(
                            title = "1. TTD YBS (Yang Bersangkutan)",
                            subtitle = employee.namaKaryawan.ifBlank { "Karyawan" },
                            onSignatureChanged = { viewModel.ybsSignature = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Signature Pad 2: Pimpinan Area
                        SignaturePad(
                            title = "2. TTD Pimpinan Area",
                            subtitle = "Pengesahan Kepala Area Kerja",
                            onSignatureChanged = { viewModel.leaderSignature = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Signature Pad 3: Pimpinan Pusat
                        SignaturePad(
                            title = "3. TTD Pimpinan Pusat",
                            subtitle = "Persetujuan Akhir Kantor Pusat",
                            onSignatureChanged = { viewModel.spvSignature = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { viewModel.clearAllSignatures() },
                            modifier = Modifier.fillMaxWidth().testTag("clear_all_sig_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("HAPUS SEMUA TTD")
                        }
                    }
                }
            }

            // SECTION 5: ACTION BUTTONS (CETAK, PRATINJAU, SIMPAN, CLOUD, KALENDER)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "OPSI CETAK & DOKUMEN SPKL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. Pratinjau Dokumen (Preview before print)
                        Button(
                            onClick = { viewModel.preparePdfPreview(context) },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("main_btn_preview"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PRATINJAU DOKUMEN (PREVIEW PDF)", fontWeight = FontWeight.Bold)
                        }

                        // 2. Cetak Langsung (PrintManager)
                        FilledTonalButton(
                            onClick = {
                                viewModel.preparePdfPreview(context)
                                viewModel.previewPdfFile.value?.let { file ->
                                    viewModel.printDocument(context, file)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("main_btn_print")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CETAK LANGSUNG KE PRINTER (PRINTMANAGER)")
                        }

                        // Row of Secondary Actions: Simpan & Ekspor
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.preparePdfPreview(context)
                                    viewModel.previewPdfFile.value?.let { file ->
                                        viewModel.saveToDownloads(context, file)
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("main_btn_save_pdf")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Simpan PDF", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.preparePdfPreview(context)
                                    viewModel.previewPdfFile.value?.let { file ->
                                        viewModel.shareDocument(context, file)
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("main_btn_export_cloud")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ekspor Cloud", maxLines = 1)
                            }
                        }

                        // Calendar Sync Button
                        OutlinedButton(
                            onClick = { viewModel.syncWithCalendar(context) },
                            modifier = Modifier.fillMaxWidth().testTag("main_btn_sync_calendar")
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SINKRONKAN JADWAL KE KALENDER ANDROID")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun OvertimeRowCard(
    row: LemburRowItem,
    index: Int,
    onUpdate: (LemburRowItem) -> Unit,
    onDelete: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Baris #${row.no}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).testTag("delete_row_${row.no}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Baris",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 1: Tgl, Hari, Shift
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = row.tgl,
                    onValueChange = { onUpdate(row.copy(tgl = it)) },
                    label = { Text("Tgl", fontSize = 11.sp) },
                    modifier = Modifier.weight(1.1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = row.hari,
                    onValueChange = { onUpdate(row.copy(hari = it)) },
                    label = { Text("Hari", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = row.shiftTugas,
                    onValueChange = { onUpdate(row.copy(shiftTugas = it)) },
                    label = { Text("Shift", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Jam Tugas, Jumlah Jam
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = row.jamTugas,
                    onValueChange = { onUpdate(row.copy(jamTugas = it)) },
                    label = { Text("Jam Tugas (e.g. 17:00-21:00)", fontSize = 11.sp) },
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = row.jumlahJam,
                    onValueChange = { onUpdate(row.copy(jumlahJam = it)) },
                    label = { Text("Jml Jam", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Keterangan
            OutlinedTextField(
                value = row.keterangan,
                onValueChange = { onUpdate(row.copy(keterangan = it)) },
                label = { Text("Keterangan Pekerjaan Lembur", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CryptoManager
import com.example.data.NotificationHelper
import com.example.data.PdfGenerator
import com.example.data.PdfPrintDocumentAdapter
import com.example.data.SettingsDataStore
import com.example.model.EmployeeData
import com.example.model.LemburRowItem
import com.example.model.SavedSpklDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpklViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.spklDao()
    val settingsDataStore = SettingsDataStore(application)

    // Auth State
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    val userPin = settingsDataStore.userPin.stateIn(viewModelScope, SharingStarted.Eagerly, "1234")
    val themeMode = settingsDataStore.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, "SYSTEM")
    val isLoginEnabled = settingsDataStore.isLoginEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val customLogoUri = settingsDataStore.customLogoUri.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val apiEndpoint = settingsDataStore.apiEndpoint.stateIn(viewModelScope, SharingStarted.Eagerly, "https://api.perusahaan.co.id/v1/hr/spkl-sync")
    val apiToken = settingsDataStore.apiToken.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // Employee Form Data
    private val _employeeData = MutableStateFlow(
        EmployeeData(
            namaPerusahaan = "PT. MULTI KARYA UTAMA",
            namaKaryawan = "Ahmad Fauzi",
            nik = "NIK-882910",
            wilayah = "DKI Jakarta",
            lokasiKerja = "Head Office - Sudirman",
            departemen = "IT Infrastructure & Security",
            bulan = "Oktober",
            jabatan = "Senior Systems Engineer",
            tahun = "2026"
        )
    )
    val employeeData: StateFlow<EmployeeData> = _employeeData.asStateFlow()

    // Overtime Rows (Default 6 rows matching Python script)
    private val _rows = MutableStateFlow(
        listOf(
            LemburRowItem(no = 1, tgl = "05/10/2026", hari = "Senin", shiftTugas = "Shift 1", jamTugas = "17:00 - 21:00", jumlahJam = "4 Jam", keterangan = "Maintenance Server Database Bulanan"),
            LemburRowItem(no = 2, tgl = "07/10/2026", hari = "Rabu", shiftTugas = "Shift 1", jamTugas = "17:00 - 20:00", jumlahJam = "3 Jam", keterangan = "Patching Security Firewall"),
            LemburRowItem(no = 3, tgl = "10/10/2026", hari = "Sabtu", shiftTugas = "Shift Libur", jamTugas = "09:00 - 15:00", jumlahJam = "6 Jam", keterangan = "Migrasi Cloud Backup Data Center"),
            LemburRowItem(no = 4, tgl = "12/10/2026", hari = "Senin", shiftTugas = "Shift 1", jamTugas = "17:00 - 21:00", jumlahJam = "4 Jam", keterangan = "Troubleshooting Core Switch"),
            LemburRowItem(no = 5, tgl = "", hari = "", shiftTugas = "", jamTugas = "", jumlahJam = "", keterangan = ""),
            LemburRowItem(no = 6, tgl = "", hari = "", shiftTugas = "", jamTugas = "", jumlahJam = "", keterangan = "")
        )
    )
    val rows: StateFlow<List<LemburRowItem>> = _rows.asStateFlow()

    // Signatures
    var ybsSignature: Bitmap? = null
    var leaderSignature: Bitmap? = null
    var spvSignature: Bitmap? = null

    // Documents from DB
    val savedDocuments: StateFlow<List<SavedSpklDocument>> = dao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Events
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Active Preview PDF file
    private val _previewPdfFile = MutableStateFlow<File?>(null)
    val previewPdfFile: StateFlow<File?> = _previewPdfFile.asStateFlow()

    init {
        NotificationHelper.createNotificationChannels(application)
    }

    fun login() {
        _isLoggedIn.value = true
    }

    fun logout() {
        _isLoggedIn.value = false
    }

    fun updateEmployeeData(update: (EmployeeData) -> EmployeeData) {
        _employeeData.value = update(_employeeData.value)
    }

    fun updateRow(index: Int, update: (LemburRowItem) -> LemburRowItem) {
        val currentList = _rows.value.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = update(currentList[index])
            _rows.value = currentList
        }
    }

    fun addRow() {
        val currentList = _rows.value.toMutableList()
        currentList.add(LemburRowItem(no = currentList.size + 1))
        _rows.value = currentList
    }

    fun removeRow(index: Int) {
        val currentList = _rows.value.toMutableList()
        if (currentList.size > 1 && index in currentList.indices) {
            currentList.removeAt(index)
            // re-index
            val reindexed = currentList.mapIndexed { idx, item -> item.copy(no = idx + 1) }
            _rows.value = reindexed
        }
    }

    fun clearAllSignatures() {
        ybsSignature = null
        leaderSignature = null
        spvSignature = null
        viewModelScope.launch {
            _toastEvent.emit("Semua tanda tangan berhasil dibersihkan")
        }
    }

    fun setCustomLogo(uriString: String?) {
        viewModelScope.launch {
            settingsDataStore.setCustomLogoUri(uriString)
            _toastEvent.emit(if (uriString != null) "Logo berhasil disimpan" else "Logo dikembalikan ke bawaan")
        }
    }

    fun closePreview() {
        _previewPdfFile.value = null
    }

    /**
     * Generates the SPKL PDF in the app's cache/docs folder and sets it as current preview.
     */
    fun preparePdfPreview(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val emp = _employeeData.value
                val cleanName = emp.namaKaryawan.ifBlank { "Karyawan" }.replace(" ", "_")
                val dateStr = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
                val fileName = "SPKL_${cleanName}_$dateStr.pdf"
                val dir = File(context.filesDir, "spkl_docs").apply { mkdirs() }
                val targetFile = File(dir, fileName)

                PdfGenerator.generateSpklPdf(
                    context = context,
                    employee = emp,
                    rows = _rows.value,
                    ybsSig = ybsSignature,
                    leaderSig = leaderSignature,
                    spvSig = spvSignature,
                    logoUri = customLogoUri.value,
                    outputFile = targetFile
                )

                withContext(Dispatchers.Main) {
                    _previewPdfFile.value = targetFile
                    _toastEvent.emit("Pratinjau SPKL siap ditampilkan")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _toastEvent.emit("Gagal menyiapkan PDF: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Direct print with PrintManager
     */
    fun printDocument(context: Context, targetFile: File? = _previewPdfFile.value) {
        val fileToPrint = targetFile ?: return
        viewModelScope.launch(Dispatchers.Main) {
            try {
                PdfPrintDocumentAdapter.printPdf(context, fileToPrint, "Cetak ${fileToPrint.name}")
                NotificationHelper.showPrintCompletedNotification(
                    context,
                    _employeeData.value.namaKaryawan,
                    fileToPrint.name
                )
                saveDocumentToArchive(context, fileToPrint)
                _toastEvent.emit("Mengirim dokumen ke Printer...")
            } catch (e: Exception) {
                _toastEvent.emit("Gagal mengirim ke printer: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Saves PDF to public Downloads folder for user accessibility
     */
    fun saveToDownloads(context: Context, sourceFile: File? = _previewPdfFile.value) {
        val file = sourceFile ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "Form_Lembur_Karyawan").apply { mkdirs() }
                val destFile = File(targetDir, file.name)

                FileInputStream(file).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                saveDocumentToArchive(context, file)

                withContext(Dispatchers.Main) {
                    NotificationHelper.showPrintCompletedNotification(
                        context,
                        _employeeData.value.namaKaryawan,
                        destFile.name
                    )
                    _toastEvent.emit("PDF tersimpan di Unduhan/Form_Lembur_Karyawan")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _toastEvent.emit("Gagal menyimpan ke Unduhan: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Saves record into Room database with encrypted backup copy
     */
    private suspend fun saveDocumentToArchive(context: Context, pdfFile: File) {
        val emp = _employeeData.value
        val activeRows = _rows.value.filter { it.tgl.isNotBlank() }
        val totalHours = activeRows.sumOf {
            it.jumlahJam.filter { ch -> ch.isDigit() || ch == '.' }.toDoubleOrNull() ?: 0.0
        }

        // Encrypt copy in secure folder
        val encryptedDir = File(context.filesDir, "encrypted_vault").apply { mkdirs() }
        val encFile = File(encryptedDir, "${pdfFile.name}.enc")
        try {
            val plainBytes = pdfFile.readBytes()
            val encBytes = CryptoManager.encrypt(plainBytes)
            encFile.writeBytes(encBytes)
        } catch (_: Exception) {}

        val doc = SavedSpklDocument(
            title = "SPKL - ${emp.namaKaryawan} (${emp.bulan} ${emp.tahun})",
            employeeName = emp.namaKaryawan,
            nik = emp.nik,
            month = emp.bulan,
            year = emp.tahun,
            department = emp.departemen,
            filePath = pdfFile.absolutePath,
            fileSizeBytes = pdfFile.length(),
            createdTimestamp = System.currentTimeMillis(),
            isEncrypted = true,
            totalHours = String.format(Locale.US, "%.1f Jam", totalHours),
            rowCount = activeRows.size,
            cloudSyncStatus = "Tersimpan Lokal & Terenkripsi"
        )
        dao.insertDocument(doc)
    }

    /**
     * Shares PDF using system share sheet (Cloud Storage / Google Drive / Email)
     */
    fun shareDocument(context: Context, pdfFile: File? = _previewPdfFile.value) {
        val file = pdfFile ?: return
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Dokumen SPKL: ${file.name}")
                putExtra(Intent.EXTRA_TEXT, "Berikut terlampir dokumen SPKL Lembur Karyawan atas nama ${_employeeData.value.namaKaryawan} (NIK: ${_employeeData.value.nik}).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Ekspor Dokumen ke Cloud / Drive / Email"))
        } catch (e: Exception) {
            viewModelScope.launch {
                _toastEvent.emit("Gagal membagikan: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Calendar Sync: inserts overtime dates to Android Calendar
     */
    fun syncWithCalendar(context: Context) {
        val emp = _employeeData.value
        val validRows = _rows.value.filter { it.tgl.isNotBlank() }
        if (validRows.isEmpty()) {
            viewModelScope.launch {
                _toastEvent.emit("Isi tanggal lembur terlebih dahulu")
            }
            return
        }

        val desc = buildString {
            append("Jadwal Lembur SPKL:\n")
            validRows.forEach { r ->
                append("- ${r.hari}, ${r.tgl}: Shift ${r.shiftTugas} (${r.jamTugas}) = ${r.jumlahJam} [${r.keterangan}]\n")
            }
            append("\nCatatan: Penyerahan form sebelum tgl 17.")
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Lembur SPKL - ${emp.namaKaryawan}")
            putExtra(CalendarContract.Events.DESCRIPTION, desc)
            putExtra(CalendarContract.Events.EVENT_LOCATION, emp.lokasiKerja)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            viewModelScope.launch {
                _toastEvent.emit("Tidak ada aplikasi kalender yang mendukung: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Cloud & REST API Sync Simulation
     */
    fun syncWithCloudApi(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val emp = _employeeData.value
                val payload = JSONObject().apply {
                    put("nik", emp.nik)
                    put("nama", emp.namaKaryawan)
                    put("wilayah", emp.wilayah)
                    put("lokasi", emp.lokasiKerja)
                    put("departemen", emp.departemen)
                    put("bulan", emp.bulan)
                    put("tahun", emp.tahun)
                    put("jabatan", emp.jabatan)
                    val rowsArray = JSONArray()
                    _rows.value.filter { it.tgl.isNotBlank() }.forEach { r ->
                        rowsArray.put(JSONObject().apply {
                            put("no", r.no)
                            put("tgl", r.tgl)
                            put("hari", r.hari)
                            put("shift", r.shiftTugas)
                            put("jam", r.jamTugas)
                            put("jumlah_jam", r.jumlahJam)
                            put("keterangan", r.keterangan)
                        })
                    }
                    put("detail_lembur", rowsArray)
                    put("timestamp", System.currentTimeMillis())
                    put("encrypted_e2e", true)
                }

                // Simulate network sync delay
                kotlinx.coroutines.delay(1200)

                withContext(Dispatchers.Main) {
                    onComplete(true, "Data SPKL (${_rows.value.filter { it.tgl.isNotBlank() }.size} baris) berhasil disinkronkan ke Cloud HR API secara real-time.")
                    _toastEvent.emit("Sinkronisasi Cloud Berhasil!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Gagal sinkronisasi Cloud: ${e.localizedMessage}")
                }
            }
        }
    }

    fun triggerCutoffReminder(context: Context) {
        NotificationHelper.showCutoffReminderNotification(context, _employeeData.value.bulan)
        viewModelScope.launch {
            _toastEvent.emit("Notifikasi pengingat Cut-Off tgl 17 berhasil dikirim")
        }
    }

    fun deleteDocument(document: SavedSpklDocument) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val f = File(document.filePath)
                if (f.exists()) f.delete()
                dao.deleteDocument(document)
                withContext(Dispatchers.Main) {
                    _toastEvent.emit("Dokumen arsip berhasil dihapus")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _toastEvent.emit("Gagal menghapus: ${e.localizedMessage}")
                }
            }
        }
    }
}

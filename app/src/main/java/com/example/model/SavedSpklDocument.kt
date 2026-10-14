package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_spkl_documents")
data class SavedSpklDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val employeeName: String,
    val nik: String,
    val month: String,
    val year: String,
    val department: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val createdTimestamp: Long,
    val isEncrypted: Boolean = true,
    val totalHours: String,
    val rowCount: Int,
    val cloudSyncStatus: String = "Tersimpan Lokal" // "Tersimpan Lokal", "Sinkron Cloud", "Pending"
)

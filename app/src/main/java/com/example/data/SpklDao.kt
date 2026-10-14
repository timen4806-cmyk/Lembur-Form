package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.SavedSpklDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface SpklDao {
    @Query("SELECT * FROM saved_spkl_documents ORDER BY createdTimestamp DESC")
    fun getAllDocuments(): Flow<List<SavedSpklDocument>>

    @Query("SELECT * FROM saved_spkl_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): SavedSpklDocument?

    @Query("SELECT * FROM saved_spkl_documents WHERE employeeName LIKE '%' || :query || '%' OR nik LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY createdTimestamp DESC")
    fun searchDocuments(query: String): Flow<List<SavedSpklDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: SavedSpklDocument): Long

    @Update
    suspend fun updateDocument(document: SavedSpklDocument)

    @Delete
    suspend fun deleteDocument(document: SavedSpklDocument)

    @Query("DELETE FROM saved_spkl_documents WHERE id = :id")
    suspend fun deleteById(id: Long)
}

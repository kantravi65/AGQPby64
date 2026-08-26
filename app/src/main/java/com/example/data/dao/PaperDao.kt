package com.example.data.dao

import androidx.room.*
import com.example.data.model.PaperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperDao {
    @Query("SELECT * FROM papers ORDER BY createdAt DESC")
    fun getAllPapers(): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers WHERE id = :id")
    suspend fun getPaperById(id: String): PaperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: PaperEntity)

    @Delete
    suspend fun deletePaper(paper: PaperEntity)

    @Query("DELETE FROM papers WHERE id = :id")
    suspend fun deleteById(id: String)
}

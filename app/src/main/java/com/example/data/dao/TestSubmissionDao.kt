package com.example.data.dao

import androidx.room.*
import com.example.data.model.TestSubmissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestSubmissionDao {
    @Query("SELECT * FROM test_submissions ORDER BY submitTime DESC")
    fun getAllSubmissions(): Flow<List<TestSubmissionEntity>>

    @Query("SELECT * FROM test_submissions WHERE id = :id LIMIT 1")
    suspend fun getSubmissionById(id: String): TestSubmissionEntity?

    @Query("SELECT * FROM test_submissions WHERE candidateRollNumber = :rollNumber LIMIT 1")
    suspend fun getSubmissionByRollNumber(rollNumber: String): TestSubmissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: TestSubmissionEntity)

    @Update
    suspend fun updateSubmission(submission: TestSubmissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmissions(submissions: List<TestSubmissionEntity>)

    @Query("DELETE FROM test_submissions WHERE id = :id")
    suspend fun deleteSubmission(id: String)
    
    @Query("DELETE FROM test_submissions")
    suspend fun clearAll()
}

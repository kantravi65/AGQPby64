package com.example.data.dao

import androidx.room.*
import com.example.data.model.TestAttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestAttemptDao {
    @Query("SELECT * FROM test_attempts ORDER BY timestamp DESC")
    fun getAllAttempts(): Flow<List<TestAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: TestAttemptEntity)
}

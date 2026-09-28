package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<AppConfig?>

    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): AppConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: AppConfig)
}

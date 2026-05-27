package com.airgf.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.airgf.app.data.local.db.entity.GfConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GfConfigDao {
    @Query("SELECT * FROM gf_config WHERE id = 1")
    suspend fun getConfig(): GfConfigEntity?

    @Query("SELECT * FROM gf_config WHERE id = 1")
    fun getConfigFlow(): Flow<GfConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: GfConfigEntity)

    @Query("DELETE FROM gf_config")
    suspend fun delete()
}

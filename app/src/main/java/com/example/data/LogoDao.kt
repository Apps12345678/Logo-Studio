package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LogoDao {
    @Query("SELECT * FROM logos ORDER BY updatedAt DESC")
    fun getAllLogos(): Flow<List<LogoEntity>>

    @Query("SELECT * FROM logos WHERE id = :id LIMIT 1")
    suspend fun getLogoById(id: Int): LogoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogo(logo: LogoEntity): Long

    @Update
    suspend fun updateLogo(logo: LogoEntity)

    @Delete
    suspend fun deleteLogo(logo: LogoEntity)

    @Query("DELETE FROM logos WHERE id = :id")
    suspend fun deleteLogoById(id: Int)
}

package com.example.data

import com.example.model.LogoProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LogoRepository(private val logoDao: LogoDao) {
    val allLogos: Flow<List<LogoProject>> = logoDao.getAllLogos()
        .map { list -> list.map { it.toDomain() } }

    suspend fun getLogoById(id: Int): LogoProject? {
        return logoDao.getLogoById(id)?.toDomain()
    }

    suspend fun saveLogo(project: LogoProject): Int {
        val entity = LogoEntity.fromDomain(project.copy(updatedAt = System.currentTimeMillis()))
        return if (entity.id == 0) {
            logoDao.insertLogo(entity).toInt()
        } else {
            logoDao.updateLogo(entity)
            entity.id
        }
    }

    suspend fun deleteLogoById(id: Int) {
        logoDao.deleteLogoById(id)
    }
}

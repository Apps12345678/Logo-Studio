package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.LogoElement
import com.example.model.LogoProject

@Entity(tableName = "logos")
data class LogoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val backgroundColor: Int,
    val isTransparentBg: Boolean,
    val elements: List<LogoElement>,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val updatedAt: Long
) {
    fun toDomain(): LogoProject = LogoProject(
        id = id,
        name = name,
        backgroundColor = backgroundColor,
        isTransparentBg = isTransparentBg,
        elements = elements,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(project: LogoProject): LogoEntity = LogoEntity(
            id = project.id,
            name = project.name,
            backgroundColor = project.backgroundColor,
            isTransparentBg = project.isTransparentBg,
            elements = project.elements,
            canvasWidth = project.canvasWidth,
            canvasHeight = project.canvasHeight,
            updatedAt = project.updatedAt
        )
    }
}

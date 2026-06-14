package com.example.model

import java.util.UUID

enum class ElementType {
    TEXT, SHAPE, BRUSH, IMAGE
}

enum class ShapeType {
    CIRCLE, RECTANGLE, ROUNDED_RECT, TRIANGLE, STAR, PENTAGON, HEXAGON, BADGE, EMBLEM_SHIELD, LINE
}

data class LogoElement(
    val id: String = UUID.randomUUID().toString(),
    val type: ElementType,
    
    // Text elements
    val text: String = "",
    val textColor: Int = 0xFF1A1A1A.toInt(),
    val textSize: Float = 36f,
    val fontFamily: String = "SansSerif", // SansSerif, Serif, Monospace, Cursive, Elegant, Brutalist, Rounded, Tech, Futuristic
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val letterSpacing: Float = 0f,
    
    // Shape elements
    val shapeType: ShapeType = ShapeType.CIRCLE,
    val shapeColor: Int = 0xFFFF5722.toInt(), // Deep Orange
    val isFilled: Boolean = true,
    val strokeWidth: Float = 6f,
    val width: Float = 120f,
    val height: Float = 120f,
    val opacity: Float = 1f,
    
    // Freebrush elements
    // Point coordinate serialization style: "x1,y1;x2,y2;x3,y3..."
    val pathPointsData: String = "",
    val brushColor: Int = 0xFF4CAF50.toInt(), // Green
    val brushThickness: Float = 10f,
    
    // Shared transformations
    val x: Float = 120f,
    val y: Float = 120f,
    val rotation: Float = 0f,
    val zIndex: Int = 0,
    
    // Image and Advanced Layer Properties
    val imagePath: String = "",
    val isVisible: Boolean = true,
    val groupId: String = "",
    val groupName: String = ""
) {
    // Utility to parse stroke path points
    fun getPoints(): List<Pair<Float, Float>> {
        if (pathPointsData.isEmpty()) return emptyList()
        return try {
            pathPointsData.split(";").mapNotNull {
                val parts = it.split(",")
                if (parts.size == 2) {
                    val px = parts[0].toFloatOrNull()
                    val py = parts[1].toFloatOrNull()
                    if (px != null && py != null) Pair(px, py) else null
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun fromPoints(points: List<Pair<Float, Float>>): String {
            return points.joinToString(";") { "${it.first},${it.second}" }
        }
    }
}

data class LogoProject(
    val id: Int = 0,
    val name: String,
    val backgroundColor: Int = 0xFFFFFFFF.toInt(), // ARGB Int
    val isTransparentBg: Boolean = false,
    val elements: List<LogoElement> = emptyList(),
    val canvasWidth: Float = 500f,
    val canvasHeight: Float = 500f,
    val updatedAt: Long = System.currentTimeMillis()
)

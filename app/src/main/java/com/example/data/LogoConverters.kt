package com.example.data

import androidx.room.TypeConverter
import com.example.model.LogoElement
import com.example.model.ElementType
import com.example.model.ShapeType
import org.json.JSONArray
import org.json.JSONObject

class LogoConverters {
    @TypeConverter
    fun fromElementList(elements: List<LogoElement>): String {
        val array = JSONArray()
        for (el in elements) {
            val obj = JSONObject().apply {
                put("id", el.id)
                put("type", el.type.name)
                put("text", el.text)
                put("textColor", el.textColor)
                put("textSize", el.textSize.toDouble())
                put("fontFamily", el.fontFamily)
                put("isBold", el.isBold)
                put("isItalic", el.isItalic)
                put("letterSpacing", el.letterSpacing.toDouble())
                put("shapeType", el.shapeType.name)
                put("shapeColor", el.shapeColor)
                put("isFilled", el.isFilled)
                put("strokeWidth", el.strokeWidth.toDouble())
                put("width", el.width.toDouble())
                put("height", el.height.toDouble())
                put("opacity", el.opacity.toDouble())
                put("pathPointsData", el.pathPointsData)
                put("brushColor", el.brushColor)
                put("brushThickness", el.brushThickness.toDouble())
                put("x", el.x.toDouble())
                put("y", el.y.toDouble())
                put("rotation", el.rotation.toDouble())
                put("zIndex", el.zIndex)
                put("imagePath", el.imagePath)
                put("isVisible", el.isVisible)
                put("groupId", el.groupId)
                put("groupName", el.groupName)
            }
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toElementList(data: String?): List<LogoElement> {
        if (data.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<LogoElement>()
        try {
            val array = JSONArray(data)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    LogoElement(
                        id = obj.optString("id"),
                        type = ElementType.valueOf(obj.optString("type", ElementType.SHAPE.name)),
                        text = obj.optString("text", ""),
                        textColor = obj.optInt("textColor", 0xFF1A1A1A.toInt()),
                        textSize = obj.optDouble("textSize", 36.0).toFloat(),
                        fontFamily = obj.optString("fontFamily", "SansSerif"),
                        isBold = obj.optBoolean("isBold", false),
                        isItalic = obj.optBoolean("isItalic", false),
                        letterSpacing = obj.optDouble("letterSpacing", 0.0).toFloat(),
                        shapeType = ShapeType.valueOf(obj.optString("shapeType", ShapeType.CIRCLE.name)),
                        shapeColor = obj.optInt("shapeColor", 0xFFFF5722.toInt()),
                        isFilled = obj.optBoolean("isFilled", true),
                        strokeWidth = obj.optDouble("strokeWidth", 6.0).toFloat(),
                        width = obj.optDouble("width", 120.0).toFloat(),
                        height = obj.optDouble("height", 120.0).toFloat(),
                        opacity = obj.optDouble("opacity", 1.0).toFloat(),
                        pathPointsData = obj.optString("pathPointsData", ""),
                        brushColor = obj.optInt("brushColor", 0xFF4CAF50.toInt()),
                        brushThickness = obj.optDouble("brushThickness", 10.0).toFloat(),
                        x = obj.optDouble("x", 120.0).toFloat(),
                        y = obj.optDouble("y", 120.0).toFloat(),
                        rotation = obj.optDouble("rotation", 0.0).toFloat(),
                        zIndex = obj.optInt("zIndex", 0),
                        imagePath = obj.optString("imagePath", ""),
                        isVisible = obj.optBoolean("isVisible", true),
                        groupId = obj.optString("groupId", ""),
                        groupName = obj.optString("groupName", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}

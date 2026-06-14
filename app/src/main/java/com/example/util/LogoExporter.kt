package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.model.LogoProject
import com.example.model.LogoElement
import com.example.model.ElementType
import com.example.model.ShapeType

object LogoExporter {
    fun renderToBitmap(project: LogoProject, size: Int = 1024): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (!project.isTransparentBg) {
            canvas.drawColor(project.backgroundColor)
        } else {
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        }

        // Scale factor: scale project space to export space
        val scale = size.toFloat() / project.canvasWidth

        // Sort elements by zIndex
        val sortedElements = project.elements.sortedBy { it.zIndex }

        for (el in sortedElements) {
            if (!el.isVisible) continue
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            canvas.save()
            // Translate to the element's actual position in export size
            canvas.translate(el.x * scale, el.y * scale)
            canvas.rotate(el.rotation)

            when (el.type) {
                ElementType.IMAGE -> {
                    if (!el.imagePath.isNullOrEmpty()) {
                        try {
                            val options = android.graphics.BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                            }
                            val imageBitmap = android.graphics.BitmapFactory.decodeFile(el.imagePath, options)
                            if (imageBitmap != null) {
                                paint.alpha = (el.opacity * 255).toInt().coerceIn(0, 255)
                                val w = el.width * scale
                                val h = el.height * scale
                                val destRect = RectF(-w / 2f, -h / 2f, w / 2f, h / 2f)
                                canvas.drawBitmap(imageBitmap, null, destRect, paint)
                                imageBitmap.recycle()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                ElementType.TEXT -> {
                    paint.color = el.textColor
                    paint.textSize = el.textSize * scale
                    paint.isFakeBoldText = el.isBold
                    if (el.isItalic) {
                        paint.textSkewX = -0.25f
                    }
                    
                    val tf = com.example.util.FontResolver.resolveTypeface(el.fontFamily)
                    paint.typeface = tf
                    paint.textAlign = Paint.Align.CENTER
                    
                    // Simple letter spacing
                    paint.letterSpacing = el.letterSpacing
                    
                    // Align text center baseline to (0,0)
                    val fontMetrics = paint.fontMetrics
                    val baselineY = -(fontMetrics.ascent + fontMetrics.descent) / 2f
                    canvas.drawText(el.text, 0f, baselineY, paint)
                }
                ElementType.SHAPE -> {
                    paint.color = el.shapeColor
                    paint.alpha = (el.opacity * 255).toInt().coerceIn(0, 255)
                    
                    if (el.isFilled) {
                        paint.style = Paint.Style.FILL
                    } else {
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = el.strokeWidth * scale
                    }

                    val w = el.width * scale
                    val h = el.height * scale

                    // Centered bounding coordinates
                    val left = -w / 2f
                    val top = -h / 2f
                    val right = w / 2f
                    val bottom = h / 2f

                    when (el.shapeType) {
                        ShapeType.CIRCLE -> {
                            val radius = (w.coerceAtLeast(h)) / 2f
                            canvas.drawCircle(0f, 0f, radius, paint)
                        }
                        ShapeType.RECTANGLE -> {
                            canvas.drawRect(left, top, right, bottom, paint)
                        }
                        ShapeType.ROUNDED_RECT -> {
                            val rx = 16f * scale
                            val ry = 16f * scale
                            canvas.drawRoundRect(RectF(left, top, right, bottom), rx, ry, paint)
                        }
                        ShapeType.TRIANGLE -> {
                            val path = Path().apply {
                                moveTo(0f, top)
                                lineTo(right, bottom)
                                lineTo(left, bottom)
                                close()
                            }
                            canvas.drawPath(path, paint)
                        }
                        ShapeType.STAR -> {
                            val path = Path()
                            val points = 5
                            val outerRadius = w / 2f
                            val innerRadius = outerRadius * 0.4f
                            var angle = -Math.PI / 2
                            val rotationAngle = Math.PI / points
                            
                            for (i in 0 until (points * 2)) {
                                val r = if (i % 2 == 0) outerRadius else innerRadius
                                val px = (r * Math.cos(angle)).toFloat()
                                val py = (r * Math.sin(angle)).toFloat()
                                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                angle += rotationAngle
                            }
                            path.close()
                            canvas.drawPath(path, paint)
                        }
                        ShapeType.PENTAGON -> {
                            drawRegPolygon(canvas, paint, 5, w / 2f)
                        }
                        ShapeType.HEXAGON -> {
                            drawRegPolygon(canvas, paint, 6, w / 2f)
                        }
                        ShapeType.BADGE -> {
                            val path = Path()
                            val points = 16
                            val outerRadius = w / 2f
                            val innerRadius = outerRadius * 0.85f
                            var angle = 0.0
                            val rot = Math.PI / points
                            for (i in 0 until (points * 2)) {
                                val r = if (i % 2 == 0) outerRadius else innerRadius
                                val px = (r * Math.cos(angle)).toFloat()
                                val py = (r * Math.sin(angle)).toFloat()
                                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                angle += rot
                            }
                            path.close()
                            canvas.drawPath(path, paint)
                        }
                        ShapeType.EMBLEM_SHIELD -> {
                            val path = Path().apply {
                                moveTo(0f, top)
                                lineTo(right, top)
                                cubicTo(right, top, right, bottom * 0.3f, right * 0.8f, bottom * 0.6f)
                                cubicTo(right * 0.8f, bottom * 0.6f, 0f, bottom * 0.9f, 0f, bottom)
                                cubicTo(0f, bottom, -right * 0.8f, bottom * 0.6f, -right * 0.8f, bottom * 0.6f)
                                cubicTo(-right * 0.8f, bottom * 0.6f, -right, bottom * 0.3f, -right, top)
                                close()
                            }
                            canvas.drawPath(path, paint)
                        }
                        ShapeType.LINE -> {
                            canvas.drawLine(left, 0f, right, 0f, paint)
                        }
                    }
                }
                ElementType.BRUSH -> {
                    val pts = el.getPoints()
                    if (pts.isNotEmpty()) {
                        paint.color = el.brushColor
                        paint.strokeWidth = el.brushThickness * scale
                        paint.style = Paint.Style.STROKE
                        paint.strokeCap = Paint.Cap.ROUND
                        paint.strokeJoin = Paint.Join.ROUND
                        paint.alpha = (el.opacity * 255).toInt().coerceIn(0, 255)

                        val path = Path()
                        // Points were stored in actual canvas coordinates.
                        // Since canvas translates to (el.x * scale, el.y * scale),
                        // we need to draw each point relative to el.x and el.y.
                        val first = pts[0]
                        path.moveTo((first.first - el.x) * scale, (first.second - el.y) * scale)
                        for (i in 1 until pts.size) {
                            path.lineTo((pts[i].first - el.x) * scale, (pts[i].second - el.y) * scale)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
            }
            canvas.restore()
        }
        return bitmap
    }

    private fun drawRegPolygon(canvas: Canvas, paint: Paint, sides: Int, radius: Float) {
        val path = Path()
        var angle = -Math.PI / 2
        val step = 2 * Math.PI / sides
        for (i in 0 until sides) {
            val px = (radius * Math.cos(angle)).toFloat()
            val py = (radius * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            angle += step
        }
        path.close()
        canvas.drawPath(path, paint)
    }
}

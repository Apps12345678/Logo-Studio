package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ElementType
import com.example.model.LogoElement
import com.example.model.ShapeType
import com.example.viewmodel.EditorTool
import com.example.viewmodel.LogoViewModel
import com.example.util.LogoExporter
import com.example.util.FileSaver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoEditorScreen(
    viewModel: LogoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentProject by viewModel.currentProject.collectAsState()
    val selectedId by viewModel.selectedElementId.collectAsState()
    val tool by viewModel.activeTool.collectAsState()
    val strokePoints by viewModel.currentStrokePoints.collectAsState()
    
    val brushColor by viewModel.brushColor.collectAsState()
    val brushThickness by viewModel.brushThickness.collectAsState()

    // Editor tab: 0 = Add, 1 = Style, 2 = Layers
    var activeTab by remember { mutableIntStateOf(0) }
    var renameQuery by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showGridGuidelines by remember { mutableStateOf(false) }

    val project = currentProject ?: return

    val selectedElement = remember(project.elements, selectedId) {
        project.elements.firstOrNull { it.id == selectedId }
    }

    // Auto update tab when selection changes to style
    LaunchedEffect(selectedId) {
        if (selectedId != null) {
            activeTab = 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val saveStatus by viewModel.saveStatus.collectAsState()
                    Column(
                        modifier = Modifier
                            .clickable { 
                                renameQuery = project.name
                                showRenameDialog = true
                            }
                            .testTag("editor_title_container")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.widthIn(max = 140.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Переименовать",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        val (statusText, statusColor) = when (saveStatus) {
                            LogoViewModel.SaveStatus.SAVED -> "Сохранено" to MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            LogoViewModel.SaveStatus.SAVING -> "Сохранение..." to MaterialTheme.colorScheme.secondary
                            LogoViewModel.SaveStatus.DIRTY -> "Изменено (автосохранение...)" to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                        
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onBack() },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = viewModel.canUndo,
                        modifier = Modifier.testTag("undo_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Отменить",
                            tint = if (viewModel.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }
                    // Redo
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = viewModel.canRedo,
                        modifier = Modifier.testTag("redo_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Повторить",
                            tint = if (viewModel.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }
                    // Save Status
                    IconButton(
                        onClick = {
                            viewModel.saveProjectDirectly()
                            Toast.makeText(context, "Проект сохранен!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить")
                    }
                    // Export high-res PNG image
                    Button(
                        onClick = {
                            val bitmap = LogoExporter.renderToBitmap(project, 1024)
                            val success = FileSaver.saveBitmapToGallery(context, bitmap, project.name)
                            if (success) {
                                Toast.makeText(context, "Изображение экспортировано в галерею в высоком качестве (1024px)!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Ошибка записи файла. Проверьте права доступа в настройках устройства.", Toast.LENGTH_LONG).show()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("export_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PNG", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Mode selectors and grid action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tool Toggle Segment
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.setTool(EditorTool.SELECT) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (tool == EditorTool.SELECT) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .size(36.dp)
                            .testTag("tool_select")
                    ) {
                        Icon(
                            Icons.Default.PanToolAlt,
                            contentDescription = "Курсор",
                            tint = if (tool == EditorTool.SELECT) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setTool(EditorTool.BRUSH) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (tool == EditorTool.BRUSH) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .size(36.dp)
                            .testTag("tool_brush")
                    ) {
                        Icon(
                            Icons.Default.Gesture,
                            contentDescription = "Кисть",
                            tint = if (tool == EditorTool.BRUSH) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Extra controls (Grid toggle, Bg selector)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showGridGuidelines = !showGridGuidelines },
                        modifier = Modifier
                            .size(36.dp)
                            .border(
                                width = 1.dp,
                                color = if (showGridGuidelines) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .testTag("grid_toggle")
                    ) {
                        Icon(
                            Icons.Default.GridOn,
                            contentDescription = "Сетка",
                            tint = if (showGridGuidelines) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleTransparentBackground(!project.isTransparentBg) },
                        modifier = Modifier
                            .size(36.dp)
                            .border(
                                width = 1.dp,
                                color = if (project.isTransparentBg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .testTag("transparency_toggle")
                    ) {
                        Icon(
                            Icons.Default.Texture,
                            contentDescription = "Прозрачность",
                            tint = if (project.isTransparentBg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 1. CORE GRAPHICS VIEWPORT CANVAS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(32.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .testTag("canvas_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val canvasWidthPx = constraints.maxWidth.toFloat()
                        val canvasHeightPx = constraints.maxHeight.toFloat()
                        
                        val modelWidth = project.canvasWidth
                        val modelHeight = project.canvasHeight
                        
                        val scaleX = canvasWidthPx / modelWidth
                        val scaleY = canvasHeightPx / modelHeight
                        val minScale = scaleX.coerceAtMost(scaleY)

                        // 1.1 Render Canvas Backgrounds
                        if (project.isTransparentBg) {
                            TransparentCheckers(modifier = Modifier.fillMaxSize())
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color(project.backgroundColor)))
                        }

                        // 1.2 Grid lines overlay
                        if (showGridGuidelines) {
                            GridGuidelines(modifier = Modifier.fillMaxSize())
                        }

                        // 1.3 Canvas Drawing + Gestures Capture Box
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(tool, project.elements, selectedId) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (tool == EditorTool.SELECT) {
                                                // Check for hit targets starting from uppermost layers (Z index sorted descending)
                                                var hit = project.elements
                                                    .sortedByDescending { it.zIndex }
                                                    .firstOrNull { el ->
                                                        val dx = offset.x - (el.x * scaleX)
                                                        val dy = offset.y - (el.y * scaleY)
                                                        // Accurate hit target for select - increased standard minimum to 80f for easier selection
                                                        val thresholdW = (el.width * scaleX).coerceAtLeast(80f)
                                                        val thresholdH = (el.height * scaleY).coerceAtLeast(80f)
                                                        
                                                        // General hitbox
                                                        dx in (-thresholdW / 2f)..(thresholdW / 2f) &&
                                                                dy in (-thresholdH / 2f)..(thresholdH / 2f)
                                                    }
                                                
                                                // Proximity helper: if no direct hit, but an element is already selected, let us check a generous proximity buffer
                                                if (hit == null && selectedId != null) {
                                                    val selElement = project.elements.firstOrNull { it.id == selectedId }
                                                    if (selElement != null) {
                                                        val dx = offset.x - (selElement.x * scaleX)
                                                        val dy = offset.y - (selElement.y * scaleY)
                                                        val proximityW = (selElement.width * scaleX * 2.5f).coerceAtLeast(200f)
                                                        val proximityH = (selElement.height * scaleY * 2.5f).coerceAtLeast(200f)
                                                        
                                                        if (dx in (-proximityW / 2f)..(proximityW / 2f) &&
                                                            dy in (-proximityH / 2f)..(proximityH / 2f)) {
                                                            hit = selElement
                                                        }
                                                    }
                                                }
                                                
                                                viewModel.selectElement(hit?.id)
                                            } else if (tool == EditorTool.BRUSH) {
                                                // Convert raw pixel point to localized database dimension
                                                viewModel.appendBrushPoint(
                                                    offset.x / scaleX,
                                                    offset.y / scaleY
                                                )
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (tool == EditorTool.SELECT) {
                                                val sel = selectedElement
                                                if (sel != null) {
                                                    val nextX = (sel.x + dragAmount.x / scaleX).coerceIn(-100f, modelWidth + 100f)
                                                    val nextY = (sel.y + dragAmount.y / scaleY).coerceIn(-100f, modelHeight + 100f)
                                                    viewModel.updateSelectedElement {
                                                        it.copy(x = nextX, y = nextY)
                                                    }
                                                }
                                            } else if (tool == EditorTool.BRUSH) {
                                                viewModel.appendBrushPoint(
                                                    change.position.x / scaleX,
                                                    change.position.y / scaleY
                                                )
                                            }
                                        },
                                        onDragEnd = {
                                            if (tool == EditorTool.BRUSH) {
                                                viewModel.finishBrushStroke()
                                            }
                                        }
                                    )
                                }
                        ) {
                            // Render all Vector elements on top of background
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                project.elements.sortedBy { it.zIndex }.forEach { el ->
                                    if (!el.isVisible) return@forEach
                                    drawContext.canvas.save()
                                    // Translate to correct local pixels based on coordinate scaling and center mapping
                                    drawContext.canvas.translate(el.x * scaleX, el.y * scaleY)
                                    drawContext.canvas.rotate(el.rotation)

                                    when (el.type) {
                                        ElementType.IMAGE -> {
                                            if (!el.imagePath.isNullOrEmpty()) {
                                                try {
                                                    val options = android.graphics.BitmapFactory.Options().apply {
                                                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                                                    }
                                                    val imageBitmap = android.graphics.BitmapFactory.decodeFile(el.imagePath, options)
                                                    if (imageBitmap != null) {
                                                        val paint = android.graphics.Paint().apply {
                                                            isAntiAlias = true
                                                            alpha = (el.opacity * 255).toInt().coerceIn(0, 255)
                                                        }
                                                        val w = el.width * minScale
                                                        val h = el.height * minScale
                                                        val left = -w / 2f
                                                        val top = -h / 2f
                                                        val right = w / 2f
                                                        val bottom = h / 2f
                                                        val destRect = android.graphics.RectF(left, top, right, bottom)
                                                        drawContext.canvas.nativeCanvas.drawBitmap(imageBitmap, null, destRect, paint)
                                                        imageBitmap.recycle()
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                        ElementType.TEXT -> {
                                            val tColor = el.textColor
                                            val paint = android.graphics.Paint().apply {
                                                isAntiAlias = true
                                                color = tColor
                                                textSize = el.textSize * minScale
                                                isFakeBoldText = el.isBold
                                                if (el.isItalic) {
                                                    textSkewX = -0.25f
                                                }
                                                
                                                val tf = com.example.util.FontResolver.resolveTypeface(el.fontFamily)
                                                typeface = tf
                                                textAlign = android.graphics.Paint.Align.CENTER
                                                letterSpacing = el.letterSpacing
                                            }
                                            
                                            val baselineY = -(paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
                                            drawContext.canvas.nativeCanvas.drawText(
                                                el.text,
                                                0f,
                                                baselineY,
                                                paint
                                            )
                                        }
                                        ElementType.SHAPE -> {
                                            val pColor = el.shapeColor
                                            val opacityAlpha = el.opacity
                                            val paint = android.graphics.Paint().apply {
                                                isAntiAlias = true
                                                color = pColor
                                                alpha = (opacityAlpha * 255).toInt().coerceIn(0, 255)
                                                if (el.isFilled) {
                                                    style = android.graphics.Paint.Style.FILL
                                                } else {
                                                    style = android.graphics.Paint.Style.STROKE
                                                    strokeWidth = el.strokeWidth * minScale
                                                }
                                            }

                                            val w = el.width * minScale
                                            val h = el.height * minScale
                                            val left = -w / 2f
                                            val top = -h / 2f
                                            val right = w / 2f
                                            val bottom = h / 2f

                                            when (el.shapeType) {
                                                ShapeType.CIRCLE -> {
                                                    drawContext.canvas.nativeCanvas.drawCircle(0f, 0f, (w.coerceAtLeast(h)) / 2f, paint)
                                                }
                                                ShapeType.RECTANGLE -> {
                                                    drawContext.canvas.nativeCanvas.drawRect(left, top, right, bottom, paint)
                                                }
                                                ShapeType.ROUNDED_RECT -> {
                                                    val rx = 16f * minScale
                                                    val ry = 16f * minScale
                                                    drawContext.canvas.nativeCanvas.drawRoundRect(
                                                        android.graphics.RectF(left, top, right, bottom),
                                                        rx, ry, paint
                                                    )
                                                }
                                                ShapeType.TRIANGLE -> {
                                                    val path = android.graphics.Path().apply {
                                                        moveTo(0f, top)
                                                        lineTo(right, bottom)
                                                        lineTo(left, bottom)
                                                        close()
                                                    }
                                                    drawContext.canvas.nativeCanvas.drawPath(path, paint)
                                                }
                                                ShapeType.STAR -> {
                                                    val path = android.graphics.Path()
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
                                                    drawContext.canvas.nativeCanvas.drawPath(path, paint)
                                                }
                                                ShapeType.PENTAGON -> {
                                                    val path = android.graphics.Path()
                                                    val sides = 5
                                                    var angle = -Math.PI / 2
                                                    val step = 2 * Math.PI / sides
                                                    for (i in 0 until sides) {
                                                        val px = (w / 2f * Math.cos(angle)).toFloat()
                                                        val py = (w / 2f * Math.sin(angle)).toFloat()
                                                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                                        angle += step
                                                    }
                                                    path.close()
                                                    drawContext.canvas.nativeCanvas.drawPath(path, paint)
                                                }
                                                ShapeType.HEXAGON -> {
                                                    val path = android.graphics.Path()
                                                    val sides = 6
                                                    var angle = -Math.PI / 2
                                                    val step = 2 * Math.PI / sides
                                                    for (i in 0 until sides) {
                                                        val px = (w / 2f * Math.cos(angle)).toFloat()
                                                        val py = (w / 2f * Math.sin(angle)).toFloat()
                                                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                                        angle += step
                                                    }
                                                    path.close()
                                                    drawContext.canvas.nativeCanvas.drawPath(path, paint)
                                                }
                                                ShapeType.BADGE -> {
                                                    val path = android.graphics.Path()
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
                                                    drawContext.canvas.nativeCanvas.drawPath(path, paint)
                                                }
                                                ShapeType.EMBLEM_SHIELD -> {
                                                    val path = android.graphics.Path().apply {
                                                        moveTo(0f, top)
                                                        lineTo(right, top)
                                                        cubicTo(right, top, right, bottom * 0.3f, right * 0.8f, bottom * 0.6f)
                                                        cubicTo(right * 0.8f, bottom * 0.6f, 0f, bottom * 0.9f, 0f, bottom)
                                                        cubicTo(0f, bottom, -right * 0.8f, bottom * 0.6f, -right * 0.8f, bottom * 0.6f)
                                                        cubicTo(-right * 0.8f, bottom * 0.6f, -right, bottom * 0.3f, -right, top)
                                                        close()
                                                    }
                                                    drawContext.canvas.nativeCanvas.drawPath(path, paint)
                                                }
                                                ShapeType.LINE -> {
                                                    drawContext.canvas.nativeCanvas.drawLine(left, 0f, right, 0f, paint)
                                                }
                                            }
                                        }
                                        ElementType.BRUSH -> {
                                            val pts = el.getPoints()
                                            if (pts.isNotEmpty()) {
                                                val paint = android.graphics.Paint().apply {
                                                    isAntiAlias = true
                                                    color = el.brushColor
                                                    strokeWidth = el.brushThickness * minScale
                                                    style = android.graphics.Paint.Style.STROKE
                                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                                    strokeJoin = android.graphics.Paint.Join.ROUND
                                                    alpha = (el.opacity * 255).toInt().coerceIn(0, 255)
                                                }
                                                val path = android.graphics.Path()
                                                path.moveTo((pts[0].first - el.x) * minScale, (pts[0].second - el.y) * minScale)
                                                for (i in 1 until pts.size) {
                                                    path.lineTo((pts[i].first - el.x) * minScale, (pts[i].second - el.y) * minScale)
                                                }
                                                drawContext.canvas.nativeCanvas.drawPath(path, paint)
                                            }
                                        }
                                    }
                                    drawContext.canvas.restore()

                                    // If elements are selected, draw active bounds selection dots and blue boxes/outlines
                                    if (selectedId == el.id && tool == EditorTool.SELECT) {
                                        val ex = el.x * scaleX
                                        val ey = el.y * scaleY
                                        val ew = el.width * minScale
                                        val eh = el.height * minScale
                                        
                                        drawContext.canvas.save()
                                        drawContext.canvas.translate(ex, ey)
                                        drawContext.canvas.rotate(el.rotation)
                                        
                                        // Draw thin bounding rectangle
                                        drawRect(
                                            color = Color(0xFF2196F3),
                                            topLeft = Offset(-ew/2f - 4f, -eh/2f - 4f),
                                            size = Size(ew + 8f, eh + 8f),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                        // Draw center focal point
                                        drawCircle(
                                            color = Color(0xFF2196F3),
                                            radius = 4.dp.toPx(),
                                            center = Offset(0f, 0f)
                                        )
                                        drawContext.canvas.restore()
                                    }
                                }

                                // 1.4 Draw temporary active brush drawing points in real-time
                                if (tool == EditorTool.BRUSH && strokePoints.isNotEmpty()) {
                                    val brushPath = Path().apply {
                                        moveTo(strokePoints[0].first * scaleX, strokePoints[0].second * scaleY)
                                        for (i in 1 until strokePoints.size) {
                                            lineTo(strokePoints[i].first * scaleX, strokePoints[i].second * scaleY)
                                        }
                                    }
                                    drawPath(
                                        path = brushPath,
                                        color = Color(brushColor),
                                        style = Stroke(
                                            width = brushThickness * minScale,
                                            cap = StrokeCap.Round,
                                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. BOTTOM PANEL CONFIGURATION (INSPECTORS & TAB PANELS)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .shadow(16.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Accent drag handle indicator in Elegant Dark
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(100))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        )
                    }
                    // Inspector Tab Bar Navigation
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("Добавить", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.testTag("tab_add")
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("Свойства", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.testTag("tab_style")
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = { Text("Слои (${project.elements.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.testTag("tab_layers")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    ) {
                        when (activeTab) {
                            0 -> TabAddContent(viewModel)
                            1 -> TabStyleContent(selectedElement, project, viewModel)
                            2 -> TabLayersContent(project, selectedId, viewModel)
                        }
                    }
                }
            }
        }
    }

    // Rename project dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Переименовать") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameQuery,
                        onValueChange = { renameQuery = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rename_input_field"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProjectName(renameQuery)
                        showRenameDialog = false
                    },
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

// ──────────────────────────────────────────────
// INSPECTOR TAB CONTENTS IMPLEMENTATIONS
// ──────────────────────────────────────────────

@Composable
fun TabAddContent(viewModel: LogoViewModel) {
    val context = LocalContext.current
    var textToAdd by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Text Adding Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textToAdd,
                onValueChange = { textToAdd = it },
                placeholder = { Text("Добавить текст...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("add_text_input"),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (textToAdd.isNotBlank()) {
                        viewModel.addTextElement(textToAdd)
                        textToAdd = ""
                        focusManager.clearFocus()
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Button(
                onClick = {
                    val finalInput = textToAdd.ifBlank { "TEXT" }
                    viewModel.addTextElement(finalInput)
                    textToAdd = ""
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_text_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Текст")
            }
        }

        // Shape title
        Text(
            text = "Выберите фигуру:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp)
        )

        // Custom list representing basic vectors
        val shapes = remember {
            listOf(
                Pair(ShapeType.CIRCLE, "Круг"),
                Pair(ShapeType.RECTANGLE, "Квадрат"),
                Pair(ShapeType.ROUNDED_RECT, "Скругл. Кв."),
                Pair(ShapeType.TRIANGLE, "Треугольник"),
                Pair(ShapeType.STAR, "Звезда"),
                Pair(ShapeType.PENTAGON, "Пятиугольник"),
                Pair(ShapeType.HEXAGON, "Шестиугольник"),
                Pair(ShapeType.BADGE, "Лента/Венец"),
                Pair(ShapeType.EMBLEM_SHIELD, "Герб/Щит"),
                Pair(ShapeType.LINE, "Линия")
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
        ) {
            items(shapes) { item ->
                InputChip(
                    selected = false,
                    onClick = { viewModel.addShapeElement(item.first) },
                    label = { Text(item.second, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("add_shape_${item.first.name.lowercase()}"),
                    leadingIcon = {
                        val icon = when (item.first) {
                            ShapeType.CIRCLE -> Icons.Default.RadioButtonUnchecked
                            ShapeType.RECTANGLE -> Icons.Default.Square
                            ShapeType.ROUNDED_RECT -> Icons.Default.CropPortrait
                            ShapeType.TRIANGLE -> Icons.Default.ChangeHistory
                            ShapeType.STAR -> Icons.Default.StarBorder
                            ShapeType.EMBLEM_SHIELD -> Icons.Default.Shield
                            ShapeType.LINE -> Icons.Default.HorizontalRule
                            else -> Icons.Default.Category
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        // Image picker card
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Импорт изображений:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                val path = copyUriToInternalStorage(context, uri)
                if (path != null) {
                    viewModel.addImageElement(path)
                }
            }
        }

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { imagePickerLauncher.launch("image/*") }
                .testTag("import_image_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Выбрать изображение JPG / PNG",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TabStyleContent(
    element: LogoElement?,
    project: com.example.model.LogoProject,
    viewModel: LogoViewModel
) {
    val focusManager = LocalFocusManager.current
    if (element == null) {
        // Editing general Canvas details
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Настройки Canvas (фон):",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            
            // Background Colors Row
            Text(
                text = "Выберите цвет фона:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            val bgColors = remember {
                listOf(
                    0xFFFFFFFF.toInt(), // White
                    0xFFF1F1F1.toInt(), // Offwhite
                    0xFF212121.toInt(), // Dark slate
                    0xFF121212.toInt(), // Pure dark
                    0xFFFFEB3B.toInt(), // Yellow
                    0xFFFFC107.toInt(), // Amber
                    0xFFFF5722.toInt(), // Deep Orange
                    0xFFE91E63.toInt(), // Pink
                    0xFF9C27B0.toInt(), // Purple
                    0xFF2196F3.toInt(), // Blue
                    0xFF4CAF50.toInt()  // Green
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bgColors) { colorValue ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(colorValue))
                            .border(
                                width = if (project.backgroundColor == colorValue && !project.isTransparentBg) 2.dp else 1.dp,
                                color = if (project.backgroundColor == colorValue && !project.isTransparentBg) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable { viewModel.setCanvasBackgroundColor(colorValue) }
                            .testTag("bg_color_$colorValue")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Выберите элемент на холсте или во вкладке 'Слои' для детальной настройки стиля.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // Editing selected element
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (element.type) {
                    ElementType.TEXT -> "Настройка Текста"
                    ElementType.SHAPE -> "Настройка Фигуры (${element.shapeType.name})"
                    ElementType.BRUSH -> "Настройка Шага Рисования"
                    ElementType.IMAGE -> "Настройка Изображения"
                },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Delete Layer in Properties
            IconButton(
                onClick = { viewModel.deleteSelectedElement() },
                modifier = Modifier.size(24.dp).testTag("delete_selected_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }

        // Shared transform fields: Rotation Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Поворот",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(64.dp)
            )
            Slider(
                value = element.rotation,
                onValueChange = { rot ->
                    viewModel.updateSelectedElement { it.copy(rotation = rot) }
                },
                valueRange = 0f..360f,
                modifier = Modifier.weight(1f).testTag("slider_rotation")
            )
            Text(
                text = "${element.rotation.toInt()}°",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }

        // Shared transform fields: Position X Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Позиция X",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(64.dp)
            )
            Slider(
                value = element.x,
                onValueChange = { xVal ->
                    viewModel.updateSelectedElement { it.copy(x = xVal) }
                },
                valueRange = -100f..(project.canvasWidth + 100f),
                modifier = Modifier.weight(1f).testTag("slider_pos_x")
            )
            Text(
                text = "${element.x.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }

        // Shared transform fields: Position Y Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Позиция Y",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(64.dp)
            )
            Slider(
                value = element.y,
                onValueChange = { yVal ->
                    viewModel.updateSelectedElement { it.copy(y = yVal) }
                },
                valueRange = -100f..(project.canvasHeight + 100f),
                modifier = Modifier.weight(1f).testTag("slider_pos_y")
            )
            Text(
                text = "${element.y.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }

        // Precise nudge layout (D-Pad)
        var stepSize by remember { mutableStateOf(5f) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Точный перенос",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Шаг перемещения:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Step size choice
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    listOf(1f, 5f, 10f, 50f).forEach { size ->
                        FilterChip(
                            selected = stepSize == size,
                            onClick = { stepSize = size },
                            label = { Text("${size.toInt()}px", fontSize = 10.sp) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
            
            // Simple robust D-Pad layout
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                // Center core
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                )

                // Up Button
                IconButton(
                    onClick = {
                        viewModel.updateSelectedElement { it.copy(y = (it.y - stepSize).coerceIn(-100f, project.canvasHeight + 100f)) }
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Вверх",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Down Button
                IconButton(
                    onClick = {
                        viewModel.updateSelectedElement { it.copy(y = (it.y + stepSize).coerceIn(-100f, project.canvasHeight + 100f)) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Вниз",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Left Button
                IconButton(
                    onClick = {
                        viewModel.updateSelectedElement { it.copy(x = (it.x - stepSize).coerceIn(-100f, project.canvasWidth + 100f)) }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Влево",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Right Button
                IconButton(
                    onClick = {
                        viewModel.updateSelectedElement { it.copy(x = (it.x + stepSize).coerceIn(-100f, project.canvasWidth + 100f)) }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Вправо",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (element.type) {
            ElementType.TEXT -> {
                // Live Input Field to edit label
                OutlinedTextField(
                    value = element.text,
                    onValueChange = { value ->
                        viewModel.updateSelectedElement { it.copy(text = value) }
                    },
                    label = { Text("Текст бренда") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("property_text_input"),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Font Family Dropdown / lazy row with custom files support
                val context = LocalContext.current
                var customFontFiles by remember { mutableStateOf(emptyList<java.io.File>()) }
                
                LaunchedEffect(Unit) {
                    val folder = java.io.File(context.filesDir, "custom_fonts")
                    if (folder.exists()) {
                        customFontFiles = folder.listFiles { file ->
                            val name = file.name.lowercase()
                            name.endsWith(".ttf") || name.endsWith(".otf")
                        }?.toList() ?: emptyList()
                    }
                }

                val fontPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        try {
                            var fileName = "custom_font_${System.currentTimeMillis()}.ttf"
                            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1 && cursor.moveToFirst()) {
                                    val dn = cursor.getString(nameIndex)
                                    if (!dn.isNullOrBlank()) {
                                        fileName = dn
                                    }
                                }
                            }
                            val inputStream = context.contentResolver.openInputStream(uri)
                            if (inputStream != null) {
                                val folder = java.io.File(context.filesDir, "custom_fonts")
                                if (!folder.exists()) {
                                    folder.mkdirs()
                                }
                                val destFile = java.io.File(folder, fileName)
                                java.io.FileOutputStream(destFile).use { output ->
                                    inputStream.use { input ->
                                        input.copyTo(output)
                                    }
                                }
                                // Set newly loaded font on selected element
                                viewModel.updateSelectedElement { it.copy(fontFamily = destFile.absolutePath) }
                                
                                // Refresh custom fonts file list
                                customFontFiles = folder.listFiles { f ->
                                    val name = f.name.lowercase()
                                    name.endsWith(".ttf") || name.endsWith(".otf")
                                }?.toList() ?: emptyList()
                                
                                Toast.makeText(context, "Шрифт загружен!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Не удалось прочитать файл шрифта", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Семейство шрифта:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    TextButton(
                        onClick = { fontPickerLauncher.launch("*/*") },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).testTag("button_import_custom_font")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Добавить",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Шрифт .ttf/.otf", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                val fonts = remember {
                    listOf(
                        "SansSerif", "Serif", "Monospace", "Cursive", "Elegant", "Brutalist", "Rounded", "Tech", "Futuristic"
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(fonts) { fontKey ->
                        FilterChip(
                            selected = element.fontFamily == fontKey,
                            onClick = {
                                viewModel.updateSelectedElement { it.copy(fontFamily = fontKey) }
                            },
                            label = { Text(fontKey, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("font_family_$fontKey")
                        )
                    }

                    items(customFontFiles) { file ->
                        val displayName = file.nameWithoutExtension.take(15)
                        FilterChip(
                            selected = element.fontFamily == file.absolutePath,
                            onClick = {
                                viewModel.updateSelectedElement { it.copy(fontFamily = file.absolutePath) }
                            },
                            label = { Text("📁 $displayName", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("font_family_custom_${file.nameWithoutExtension}")
                        )
                    }
                }

                // Size and Spacing Sliders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Размер",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(64.dp)
                    )
                    Slider(
                        value = element.textSize,
                        onValueChange = { sz ->
                            viewModel.updateSelectedElement { it.copy(textSize = sz) }
                        },
                        valueRange = 12f..110f,
                        modifier = Modifier.weight(1f).testTag("slider_text_size")
                    )
                    Text(
                        text = "${element.textSize.toInt()}sp",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Интервал",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(64.dp)
                    )
                    Slider(
                        value = element.letterSpacing,
                        onValueChange = { tr ->
                            viewModel.updateSelectedElement { it.copy(letterSpacing = tr) }
                        },
                        valueRange = -0.1f..0.8f,
                        modifier = Modifier.weight(1f).testTag("slider_tracking")
                    )
                    Text(
                        text = String.format("%.1f", element.letterSpacing),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Style switches (Bold and Italic)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = element.isBold,
                            onCheckedChange = { ch ->
                                viewModel.updateSelectedElement { it.copy(isBold = ch) }
                            },
                            modifier = Modifier.testTag("checkbox_bold")
                        )
                        Text("Жирный", style = MaterialTheme.typography.labelLarge)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = element.isItalic,
                            onCheckedChange = { ch ->
                                viewModel.updateSelectedElement { it.copy(isItalic = ch) }
                            },
                            modifier = Modifier.testTag("checkbox_italic")
                        )
                        Text("Курсив", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Color Selection
                Text(
                    text = "Цвет текста:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ColorSelector(
                    activeColor = element.textColor,
                    onColorPicked = { cl ->
                        viewModel.updateSelectedElement { it.copy(textColor = cl) }
                    }
                )
            }
            ElementType.SHAPE -> {
                // Width & Height control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ширина",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(64.dp)
                    )
                    Slider(
                        value = element.width,
                        onValueChange = { wd ->
                            viewModel.updateSelectedElement { it.copy(width = wd) }
                        },
                        valueRange = 10f..400f,
                        modifier = Modifier.weight(1f).testTag("slider_shape_width")
                    )
                    Text(
                        text = "${element.width.toInt()}px",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Высота",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(64.dp)
                    )
                    Slider(
                        value = element.height,
                        onValueChange = { ht ->
                            viewModel.updateSelectedElement { it.copy(height = ht) }
                        },
                        valueRange = 10f..400f,
                        modifier = Modifier.weight(1f).testTag("slider_shape_height")
                    )
                    Text(
                        text = "${element.height.toInt()}px",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Fill toggle (Outline vs Solid)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Заполнить контур фигурой", style = MaterialTheme.typography.labelLarge)
                    Switch(
                        checked = element.isFilled,
                        onCheckedChange = { fl ->
                            viewModel.updateSelectedElement { it.copy(isFilled = fl) }
                        },
                        modifier = Modifier.testTag("switch_filled")
                    )
                }

                // If stroke, show stroke thickness slider
                if (!element.isFilled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Толщина",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(64.dp)
                        )
                        Slider(
                            value = element.strokeWidth,
                            onValueChange = { sw ->
                                viewModel.updateSelectedElement { it.copy(strokeWidth = sw) }
                            },
                            valueRange = 1f..30f,
                            modifier = Modifier.weight(1f).testTag("slider_stroke_width")
                        )
                        Text(
                            text = "${element.strokeWidth.toInt()}px",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Opacity slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Прозрачн.",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(64.dp)
                    )
                    Slider(
                        value = element.opacity,
                        onValueChange = { op ->
                            viewModel.updateSelectedElement { it.copy(opacity = op) }
                        },
                        valueRange = 0.05f..1.0f,
                        modifier = Modifier.weight(1f).testTag("slider_opacity")
                    )
                    Text(
                        text = "${(element.opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Shape Colors
                Text(
                    text = "Цвет фигуры:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ColorSelector(
                    activeColor = element.shapeColor,
                    onColorPicked = { cl ->
                        viewModel.updateSelectedElement { it.copy(shapeColor = cl) }
                    }
                )
            }
            ElementType.IMAGE -> {
                // Width & Height control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ширина",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(64.dp)
                    )
                    Slider(
                        value = element.width,
                        onValueChange = { wd ->
                            viewModel.updateSelectedElement { it.copy(width = wd) }
                        },
                        valueRange = 20f..500f,
                        modifier = Modifier.weight(1f).testTag("slider_image_width")
                    )
                    Text(
                        text = "${element.width.toInt()}px",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Высота",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(64.dp)
                    )
                    Slider(
                        value = element.height,
                        onValueChange = { ht ->
                            viewModel.updateSelectedElement { it.copy(height = ht) }
                        },
                        valueRange = 20f..500f,
                        modifier = Modifier.weight(1f).testTag("slider_image_height")
                    )
                    Text(
                        text = "${element.height.toInt()}px",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Opacity slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Прозрачн.",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(64.dp)
                    )
                    Slider(
                        value = element.opacity,
                        onValueChange = { op ->
                            viewModel.updateSelectedElement { it.copy(opacity = op) }
                        },
                        valueRange = 0.05f..1.0f,
                        modifier = Modifier.weight(1f).testTag("slider_image_opacity")
                    )
                    Text(
                        text = "${(element.opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
            ElementType.BRUSH -> {
                // Brush color and opacity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Толщина Кисти",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(80.dp)
                    )
                    Slider(
                        value = element.brushThickness,
                        onValueChange = { sw ->
                            viewModel.updateSelectedElement { it.copy(brushThickness = sw) }
                        },
                        valueRange = 2f..40f,
                        modifier = Modifier.weight(1f).testTag("slider_brush_weight")
                    )
                    Text(
                        text = "${element.brushThickness.toInt()}px",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Color Selector
                Text(
                    text = "Цвет отрисовки:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ColorSelector(
                    activeColor = element.brushColor,
                    onColorPicked = { cl ->
                        viewModel.updateSelectedElement { it.copy(brushColor = cl) }
                    }
                )
            }
        }
    }
}

@Composable
fun TabLayersContent(
    project: com.example.model.LogoProject,
    selectedId: String?,
    viewModel: LogoViewModel
) {
    if (project.elements.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.LayersClear,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "На холсте нет ни одного слоя",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val checkedStateMap = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
    val checkedIds = checkedStateMap.filter { it.value }.keys.filter { id -> project.elements.any { it.id == id } }
    var groupNameInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
    ) {
        // Multi-select grouping action bar
        if (checkedIds.size >= 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    placeholder = { Text("Имя группы...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                Button(
                    onClick = {
                        val finalName = groupNameInput.ifBlank { "Группа" }
                        viewModel.groupElements(checkedIds, finalName)
                        checkedStateMap.clear()
                        groupNameInput = ""
                    },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Сгруппировать", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sortedList = project.elements.sortedByDescending { it.zIndex }
            items(sortedList) { el ->
                val isSelected = el.id == selectedId
                val isChecked = checkedStateMap[el.id] ?: false
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable { viewModel.selectElement(el.id) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Checkbox for grouping select
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                checkedStateMap[el.id] = checked
                            },
                            modifier = Modifier.size(24.dp).testTag("layer_check_${el.id}")
                        )

                        // Color Indicator or Layer Logo Icon
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Color(
                                        when (el.type) {
                                            ElementType.TEXT -> el.textColor
                                            ElementType.SHAPE -> el.shapeColor
                                            ElementType.BRUSH -> el.brushColor
                                            ElementType.IMAGE -> 0xFF2196F3.toInt() // Default blue indicator
                                        }
                                    )
                                )
                        )
                        
                        Column {
                            Text(
                                text = when (el.type) {
                                    ElementType.TEXT -> "Текст: \"${el.text}\""
                                    ElementType.SHAPE -> "Фигура: ${el.shapeType.name}"
                                    ElementType.BRUSH -> "Скетч-Отрисовка"
                                    ElementType.IMAGE -> "Изображение"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (el.groupId.isNotEmpty()) {
                                Text(
                                    text = "📁 ${el.groupName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "zIndex = ${el.zIndex}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Action controllers: Visibility, Depth Arrows, Ungroup, Duplicate, Delete
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Visibility Toggle Eye
                        IconButton(
                            onClick = { 
                                viewModel.toggleVisibility(el.id)
                            },
                            modifier = Modifier.size(28.dp).testTag("layer_visibility_${el.id}")
                        ) {
                            val eyeIcon = if (el.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            Icon(eyeIcon, contentDescription = "Видимость", modifier = Modifier.size(16.dp))
                        }

                        // Ungroup layer button if grouped
                        if (el.groupId.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.ungroupElements(el.groupId) },
                                modifier = Modifier.size(28.dp).testTag("layer_ungroup_${el.id}")
                            ) {
                                Icon(Icons.Default.LayersClear, contentDescription = "Разгруппировать", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        IconButton(
                            onClick = { 
                                viewModel.selectElement(el.id)
                                viewModel.moveSelectedElementLayer(up = false) 
                            },
                            modifier = Modifier.size(28.dp).testTag("layer_down_${el.id}")
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Вниз", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { 
                                viewModel.selectElement(el.id)
                                viewModel.moveSelectedElementLayer(up = true) 
                            },
                            modifier = Modifier.size(28.dp).testTag("layer_up_${el.id}")
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Вверх", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { 
                                viewModel.selectElement(el.id)
                                viewModel.duplicateSelectedElement() 
                            },
                            modifier = Modifier.size(28.dp).testTag("layer_duplicate_${el.id}")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Дублировать", modifier = Modifier.size(14.dp))
                        }
                        IconButton(
                            onClick = { 
                                viewModel.selectElement(el.id)
                                viewModel.deleteSelectedElement() 
                            },
                            modifier = Modifier.size(28.dp).testTag("layer_delete_${el.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// COLOR SELECTOR PRESET BAR
// ──────────────────────────────────────────────

@Composable
fun ColorSelector(
    activeColor: Int,
    onColorPicked: (Int) -> Unit
) {
    val presetColors = remember {
        listOf(
            0xFF1A1A1A.toInt(), // Black/Gray
            0xFFFFFFFF.toInt(), // White
            0xFFFF5722.toInt(), // Deep Orange
            0xFFFF9800.toInt(), // Orange
            0xFFFFC107.toInt(), // Amber
            0xFF8BC34A.toInt(), // Light Green
            0xFF4CAF50.toInt(), // Green
            0xFF00BCD4.toInt(), // Cyan
            0xFF03A9F4.toInt(), // Light Blue
            0xFF2196F3.toInt(), // Blue
            0xFF3F51B5.toInt(), // Indigo
            0xFF673AB7.toInt(), // Purple
            0xFFE91E63.toInt(), // Pink
            0xFF795548.toInt()  // Brown
        )
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presetColors) { colVal ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(colVal))
                    .border(
                        width = if (activeColor == colVal) 2.6.dp else 1.dp,
                        color = if (activeColor == colVal) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .clickable { onColorPicked(colVal) }
                    .testTag("style_color_$colVal")
            )
        }
    }
}

// Grid lines generator helper canvas
@Composable
fun GridGuidelines(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeW = 1f
        val lineSpacing = size.width / 10f
        
        for (i in 1 until 10) {
            val offsetVal = i * lineSpacing
            // Vertical grids
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(offsetVal, 0f),
                end = Offset(offsetVal, size.height),
                strokeWidth = strokeW
            )
            // Horizontal grids
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(0f, offsetVal),
                end = Offset(size.width, offsetVal),
                strokeWidth = strokeW
            )
        }
    }
}

fun copyUriToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val folder = java.io.File(context.filesDir, "imported_images")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = java.io.File(folder, "img_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}.png")
        val outputStream = java.io.FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

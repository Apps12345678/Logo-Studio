package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.example.model.LogoElement
import com.example.model.LogoProject
import com.example.model.ShapeType
import com.example.model.ElementType
import com.example.viewmodel.LogoViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: LogoViewModel,
    onNavigateToEditor: (Int) -> Unit
) {
    val logos by viewModel.allLogos.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newLogoName by remember { mutableStateOf("") }
    
    val filteredLogos = remember(logos, searchQuery) {
        if (searchQuery.isBlank()) {
            logos
        } else {
            logos.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Logo Studio",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "Создавайте уникальные логотипы",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Создать") },
                text = { Text("Создать") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("create_logo_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск логотипов...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("search_bar"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            // Templates header
            Text(
                text = "Шаблоны для быстрого старта",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            // Template Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    TemplatePreset(
                        title = "Минимал",
                        icon = Icons.Default.FilterFrames,
                        description = "Круг с акцентом",
                        action = { name ->
                            viewModel.startNewLogo(name)
                            viewModel.addShapeElement(ShapeType.CIRCLE)
                            viewModel.updateSelectedElement {
                                it.copy(
                                    width = 180f,
                                    height = 180f,
                                    shapeColor = 0xFF2196F3.toInt(), // Modern blue
                                    isFilled = false,
                                    strokeWidth = 8f
                                )
                            }
                            viewModel.addTextElement("STUDIO")
                            viewModel.updateSelectedElement {
                                it.copy(
                                    fontFamily = "Monospace",
                                    textSize = 28f,
                                    isBold = true,
                                    letterSpacing = 0.2f
                                )
                            }
                        }
                    ),
                    TemplatePreset(
                        title = "Крутой Щит",
                        icon = Icons.Default.Shield,
                        description = "Эмблема бренда",
                        action = { name ->
                            viewModel.startNewLogo(name)
                            viewModel.addShapeElement(ShapeType.EMBLEM_SHIELD)
                            viewModel.updateSelectedElement {
                                it.copy(
                                    width = 180f,
                                    height = 200f,
                                    shapeColor = 0xFFFF5722.toInt(), // Deep orange
                                    isFilled = true
                                )
                            }
                            viewModel.addShapeElement(ShapeType.STAR)
                            viewModel.updateSelectedElement {
                                it.copy(
                                    width = 60f,
                                    height = 60f,
                                    shapeColor = 0xFFFFFFFF.toInt(),
                                    isFilled = true
                                )
                            }
                            viewModel.addTextElement("REBEL")
                            viewModel.updateSelectedElement {
                                it.copy(
                                    y = 350f,
                                    fontFamily = "Brutalist",
                                    textSize = 32f,
                                    textColor = 0xFFFF5722.toInt(),
                                    isBold = true
                                )
                            }
                        }
                    ),
                    TemplatePreset(
                        title = "Брутализм",
                        icon = Icons.Default.Star,
                        description = "Поп-арт звезда",
                        action = { name ->
                            viewModel.startNewLogo(name)
                            viewModel.addShapeElement(ShapeType.STAR)
                            viewModel.updateSelectedElement {
                                it.copy(
                                    width = 210f,
                                    height = 210f,
                                    shapeColor = 0xFFE91E63.toInt(), // Vivid pink
                                    isFilled = true,
                                    rotation = 15f
                                )
                            }
                            viewModel.addTextElement("ROCK")
                            viewModel.updateSelectedElement {
                                it.copy(
                                    fontFamily = "Brutalist",
                                    textSize = 42f,
                                    textColor = 0xFFFFFFFF.toInt(),
                                    isBold = true,
                                    rotation = -10f
                                )
                            }
                        }
                    )
                ).forEach { preset ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { preset.action(preset.title) }
                            .testTag("template_${preset.title.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = preset.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = preset.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Saved logos list title
            Text(
                text = "Мои логотипы",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (filteredLogos.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "Нет сохраненных логотипов" else "Ничего не найдено",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Brush, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Создать первый логотип")
                        }
                    }
                }
            } else {
                // Main Grid of Logos
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredLogos, key = { it.id }) { logo ->
                        LogoGridItem(
                            logo = logo,
                            onEdit = { onNavigateToEditor(logo.id) },
                            onDelete = { viewModel.deleteLogo(logo.id) }
                        )
                    }
                }
            }
        }
    }

    // Create Logo dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newLogoName = ""
            },
            title = { Text("Новый логотип") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Введите имя для вашего нового логотипа:")
                    OutlinedTextField(
                        value = newLogoName,
                        onValueChange = { newLogoName = it },
                        placeholder = { Text("Мой бренд") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_logo_name_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startNewLogo(newLogoName)
                        showCreateDialog = false
                        newLogoName = ""
                    },
                    modifier = Modifier.testTag("confirm_create_button")
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newLogoName = ""
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun LogoGridItem(
    logo: LogoProject,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(logo.updatedAt) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(logo.updatedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .testTag("logo_item_${logo.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Preview box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFFE9E9E9))
            ) {
                // Drawing checkers if transparent, otherwise background color
                if (logo.isTransparentBg) {
                    TransparentCheckers(modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(logo.backgroundColor)))
                }

                // Vector Preview Canvas (Scales down vector data to fit preview viewport)
                val densityValue = 500f // Local database project resolution
                Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    val scaleX = size.width / densityValue
                    val scaleY = size.height / densityValue
                    val minScale = scaleX.coerceAtMost(scaleY)

                    // Draw all vectors
                    logo.elements.sortedBy { it.zIndex }.forEach { el ->
                        if (!el.isVisible) return@forEach
                        drawContext.canvas.save()
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
                                // Draw preview letter placeholder or stylized text mark
                                val cleanText = if (el.text.length > 5) el.text.take(4) + ".." else el.text
                                val previewPaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    color = el.textColor
                                    textSize = el.textSize * minScale * 1.5f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    typeface = com.example.util.FontResolver.resolveTypeface(el.fontFamily)
                                    isFakeBoldText = true
                                }
                                val baseline = -(previewPaint.fontMetrics.ascent + previewPaint.fontMetrics.descent) / 2f
                                drawContext.canvas.nativeCanvas.drawText(
                                    cleanText,
                                    0f,
                                    baseline,
                                    previewPaint
                                )
                            }
                            ElementType.SHAPE -> {
                                val sPaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    color = el.shapeColor
                                    alpha = (el.opacity * 255).toInt().coerceIn(0, 255)
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
                                        drawContext.canvas.nativeCanvas.drawCircle(0f, 0f, w / 2f, sPaint)
                                    }
                                    ShapeType.RECTANGLE -> {
                                        drawContext.canvas.nativeCanvas.drawRect(left, top, right, bottom, sPaint)
                                    }
                                    ShapeType.ROUNDED_RECT -> {
                                        drawContext.canvas.nativeCanvas.drawRoundRect(
                                            android.graphics.RectF(left, top, right, bottom),
                                            8f * minScale, 8f * minScale, sPaint
                                        )
                                    }
                                    ShapeType.TRIANGLE -> {
                                        val path = android.graphics.Path().apply {
                                            moveTo(0f, top)
                                            lineTo(right, bottom)
                                            lineTo(left, bottom)
                                            close()
                                        }
                                        drawContext.canvas.nativeCanvas.drawPath(path, sPaint)
                                    }
                                    ShapeType.STAR -> {
                                        val path = android.graphics.Path()
                                        val points = 5
                                        val outerRadius = w / 2f
                                        val innerRadius = outerRadius * 0.4f
                                        var angle = -Math.PI / 2
                                        val step = Math.PI / points
                                        for (i in 0 until (points * 2)) {
                                            val r = if (i % 2 == 0) outerRadius else innerRadius
                                            val px = (r * Math.cos(angle)).toFloat()
                                            val py = (r * Math.sin(angle)).toFloat()
                                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                            angle += step
                                        }
                                        path.close()
                                        drawContext.canvas.nativeCanvas.drawPath(path, sPaint)
                                    }
                                    ShapeType.BADGE, ShapeType.PENTAGON, ShapeType.HEXAGON, ShapeType.EMBLEM_SHIELD -> {
                                        // Draw simplified generic circle so preview loads fast
                                        drawContext.canvas.nativeCanvas.drawCircle(0f, 0f, (w/2f).coerceAtMost(h/2f), sPaint)
                                    }
                                    ShapeType.LINE -> {
                                        drawContext.canvas.nativeCanvas.drawLine(left, 0f, right, 0f, sPaint)
                                    }
                                }
                            }
                            ElementType.BRUSH -> {
                                val pts = el.getPoints()
                                if (pts.isNotEmpty()) {
                                    val bPaint = android.graphics.Paint().apply {
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
                                    drawContext.canvas.nativeCanvas.drawPath(path, bPaint)
                                }
                            }
                        }
                        drawContext.canvas.restore()
                    }
                }
            }

            // Info rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = logo.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = { onDelete() },
                        modifier = Modifier.size(24.dp).testTag("delete_logo_${logo.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Checkered container for transparent PNG layouts
@Composable
fun TransparentCheckers(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val boxSize = 24f
        val cols = (size.width / boxSize).toInt() + 1
        val rows = (size.height / boxSize).toInt() + 1
        
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                if ((c + r) % 2 == 0) {
                    drawRect(
                        color = Color(0xFFF1F1F1),
                        topLeft = Offset(c * boxSize, r * boxSize),
                        size = androidx.compose.ui.geometry.Size(boxSize, boxSize)
                    )
                } else {
                    drawRect(
                        color = Color(0xFFDEDEDE),
                        topLeft = Offset(c * boxSize, r * boxSize),
                        size = androidx.compose.ui.geometry.Size(boxSize, boxSize)
                    )
                }
            }
        }
    }
}

data class TemplatePreset(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
    val action: (String) -> Unit
)

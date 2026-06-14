package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ElementType
import com.example.model.LogoElement
import com.example.model.LogoProject
import com.example.model.ShapeType
import com.example.data.LogoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.util.UUID

sealed class AppScreen {
    object Dashboard : AppScreen()
    data class Editor(val logoId: Int) : AppScreen()
}

enum class EditorTool {
    SELECT, BRUSH
}

class LogoViewModel(private val repository: LogoRepository) : ViewModel() {

    // List of all logos from local DB
    val allLogos: StateFlow<List<LogoProject>> = repository.allLogos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Dashboard)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentProject = MutableStateFlow<LogoProject?>(null)
    val currentProject: StateFlow<LogoProject?> = _currentProject.asStateFlow()

    private val _selectedElementId = MutableStateFlow<String?>(null)
    val selectedElementId: StateFlow<String?> = _selectedElementId.asStateFlow()

    private val _activeTool = MutableStateFlow<EditorTool>(EditorTool.SELECT)
    val activeTool: StateFlow<EditorTool> = _activeTool.asStateFlow()

    // Undo/Redo stacks
    private val undoStack = mutableListOf<List<LogoElement>>()
    private val redoStack = mutableListOf<List<LogoElement>>()

    // Temporary brushing points
    private val _currentStrokePoints = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val currentStrokePoints: StateFlow<List<Pair<Float, Float>>> = _currentStrokePoints.asStateFlow()

    // Active brush settings
    private val _brushColor = MutableStateFlow(0xFF4CAF50.toInt()) // Green
    val brushColor: StateFlow<Int> = _brushColor.asStateFlow()

    private val _brushThickness = MutableStateFlow(10f)
    val brushThickness: StateFlow<Float> = _brushThickness.asStateFlow()

    enum class SaveStatus {
        SAVED, SAVING, DIRTY
    }

    private val _saveStatus = MutableStateFlow(SaveStatus.SAVED)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    init {
        var lastSelfSavedProject: LogoProject? = null

        viewModelScope.launch {
            _currentProject.collect { project ->
                if (project != null) {
                    val lastSaved = lastSelfSavedProject
                    if (lastSaved != null && 
                        lastSaved.id != project.id &&
                        lastSaved.name == project.name &&
                        lastSaved.elements == project.elements &&
                        lastSaved.backgroundColor == project.backgroundColor &&
                        lastSaved.isTransparentBg == project.isTransparentBg) {
                        // This was just an ID assignment/updatedAt change from auto-save.
                        lastSelfSavedProject = project
                    } else if (lastSaved != project) {
                        // New user edit or initial load
                        if (lastSaved == null) {
                            lastSelfSavedProject = project
                            _saveStatus.value = SaveStatus.SAVED
                        } else {
                            _saveStatus.value = SaveStatus.DIRTY
                        }
                    }
                } else {
                    lastSelfSavedProject = null
                }
            }
        }

        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            _currentProject
                .debounce(1000)
                .collect { project ->
                    if (project != null) {
                        val lastSavedState = lastSelfSavedProject
                        if (lastSavedState != null &&
                            lastSavedState.id == project.id &&
                            lastSavedState.name == project.name &&
                            lastSavedState.elements == project.elements &&
                            lastSavedState.backgroundColor == project.backgroundColor &&
                            lastSavedState.isTransparentBg == project.isTransparentBg) {
                            _saveStatus.value = SaveStatus.SAVED
                            return@collect
                        }
                        try {
                            _saveStatus.value = SaveStatus.SAVING
                            val currentId = project.id
                            val newId = repository.saveLogo(project)
                            var updatedProject = project
                            if (currentId == 0) {
                                val currentVal = _currentProject.value
                                if (currentVal != null && currentVal.id == 0) {
                                    updatedProject = currentVal.copy(id = newId)
                                    _currentProject.value = updatedProject
                                }
                            }
                            lastSelfSavedProject = updatedProject
                            _saveStatus.value = SaveStatus.SAVED
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            _saveStatus.value = SaveStatus.DIRTY
                        }
                    }
                }
        }
    }

    fun navigateToDashboard() {
        // Save current project before leaving
        viewModelScope.launch {
            _currentProject.value?.let { repoSave(it) }
            _currentProject.value = null
            _selectedElementId.value = null
            undoStack.clear()
            redoStack.clear()
            _currentScreen.value = AppScreen.Dashboard
        }
    }

    fun startNewLogo(name: String) {
        val finalName = name.ifBlank { "My Logo" }
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.SAVING
            val initialProject = LogoProject(
                id = 0,
                name = finalName,
                backgroundColor = 0xFFFFFFFF.toInt(),
                isTransparentBg = false,
                elements = emptyList()
            )
            val newId = repository.saveLogo(initialProject)
            val savedProject = initialProject.copy(id = newId)
            
            _currentProject.value = savedProject
            _saveStatus.value = SaveStatus.SAVED
            _selectedElementId.value = null
            _activeTool.value = EditorTool.SELECT
            undoStack.clear()
            redoStack.clear()
            _currentScreen.value = AppScreen.Editor(newId)
        }
    }

    fun loadLogo(id: Int) {
        viewModelScope.launch {
            val project = repository.getLogoById(id)
            if (project != null) {
                _currentProject.value = project
                _saveStatus.value = SaveStatus.SAVED
                _selectedElementId.value = null
                _activeTool.value = EditorTool.SELECT
                undoStack.clear()
                redoStack.clear()
                _currentScreen.value = AppScreen.Editor(id)
            }
        }
    }

    fun deleteLogo(id: Int) {
        viewModelScope.launch {
            repository.deleteLogoById(id)
        }
    }

    fun updateProjectName(newName: String) {
        val current = _currentProject.value ?: return
        _saveStatus.value = SaveStatus.DIRTY
        _currentProject.value = current.copy(name = newName)
    }

    fun setCanvasBackgroundColor(color: Int) {
        val current = _currentProject.value ?: return
        saveStateToUndo()
        _currentProject.value = current.copy(backgroundColor = color, isTransparentBg = false)
    }

    fun toggleTransparentBackground(isTransparent: Boolean) {
        val current = _currentProject.value ?: return
        saveStateToUndo()
        _currentProject.value = current.copy(isTransparentBg = isTransparent)
    }

    fun setTool(tool: EditorTool) {
        _activeTool.value = tool
        if (tool == EditorTool.BRUSH) {
            _selectedElementId.value = null
        }
    }

    fun selectElement(id: String?) {
        if (_activeTool.value == EditorTool.SELECT) {
            _selectedElementId.value = id
        }
    }

    // ──────────────────────────────────────────────
    // UNDO / REDO LOGIC
    // ──────────────────────────────────────────────

    private fun saveStateToUndo() {
        val current = _currentProject.value ?: return
        _saveStatus.value = SaveStatus.DIRTY
        undoStack.add(current.elements)
        if (undoStack.size > 50) { // Bound size to 50 edits
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun undo() {
        val current = _currentProject.value ?: return
        if (undoStack.isNotEmpty()) {
            _saveStatus.value = SaveStatus.DIRTY
            val previousElements = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(current.elements)
            _currentProject.value = current.copy(elements = previousElements)
            
            // Validate selected element remains valid
            if (_selectedElementId.value != null) {
                val exists = previousElements.any { it.id == _selectedElementId.value }
                if (!exists) {
                    _selectedElementId.value = null
                }
            }
        }
    }

    fun redo() {
        val current = _currentProject.value ?: return
        if (redoStack.isNotEmpty()) {
            _saveStatus.value = SaveStatus.DIRTY
            val nextElements = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(current.elements)
            _currentProject.value = current.copy(elements = nextElements)
        }
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    // ──────────────────────────────────────────────
    // VECTOR ELEMENT MANIPULATIONS
    // ──────────────────────────────────────────────

    fun addTextElement(text: String = "Brand") {
        val current = _currentProject.value ?: return
        saveStateToUndo()
        
        val nextZ = (current.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
        val newElement = LogoElement(
            type = ElementType.TEXT,
            text = text,
            x = current.canvasWidth / 2f,
            y = current.canvasHeight / 2f,
            zIndex = nextZ
        )
        
        _currentProject.value = current.copy(elements = current.elements + newElement)
        _selectedElementId.value = newElement.id
    }

    fun addShapeElement(shapeType: ShapeType) {
        val current = _currentProject.value ?: return
        saveStateToUndo()

        val nextZ = (current.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
        val newElement = LogoElement(
            type = ElementType.SHAPE,
            shapeType = shapeType,
            x = current.canvasWidth / 2f,
            y = current.canvasHeight / 2f,
            zIndex = nextZ
        )

        _currentProject.value = current.copy(elements = current.elements + newElement)
        _selectedElementId.value = newElement.id
    }

    fun updateSelectedElement(update: (LogoElement) -> LogoElement) {
        val current = _currentProject.value ?: return
        val selId = _selectedElementId.value ?: return
        val target = current.elements.firstOrNull { it.id == selId } ?: return
        
        val updatedTarget = update(target)
        val dx = updatedTarget.x - target.x
        val dy = updatedTarget.y - target.y

        saveStateToUndo()

        val belongsToGroup = target.groupId.isNotEmpty()
        val updatedElements = current.elements.map {
            if (it.id == selId) {
                updatedTarget
            } else if (belongsToGroup && it.groupId == target.groupId) {
                it.copy(
                    x = (it.x + dx).coerceIn(-100f, current.canvasWidth + 100f),
                    y = (it.y + dy).coerceIn(-100f, current.canvasHeight + 100f)
                )
            } else {
                it
            }
        }
        _currentProject.value = current.copy(elements = updatedElements)
    }

    fun addImageElement(filePath: String) {
        val current = _currentProject.value ?: return
        saveStateToUndo()

        val nextZ = (current.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
        val newElement = LogoElement(
            type = ElementType.IMAGE,
            imagePath = filePath,
            width = 150f,
            height = 150f,
            x = current.canvasWidth / 2f,
            y = current.canvasHeight / 2f,
            zIndex = nextZ
        )

        _currentProject.value = current.copy(elements = current.elements + newElement)
        _selectedElementId.value = newElement.id
    }

    fun toggleVisibility(elementId: String) {
        val current = _currentProject.value ?: return
        saveStateToUndo()
        val updated = current.elements.map {
            if (it.id == elementId) it.copy(isVisible = !it.isVisible) else it
        }
        _currentProject.value = current.copy(elements = updated)
    }

    fun groupElements(elementIds: List<String>, name: String = "Группа") {
        if (elementIds.size < 2) return
        val current = _currentProject.value ?: return
        saveStateToUndo()
        val newGroupId = UUID.randomUUID().toString()
        val updated = current.elements.map {
            if (elementIds.contains(it.id)) {
                it.copy(groupId = newGroupId, groupName = name)
            } else {
                it
            }
        }
        _currentProject.value = current.copy(elements = updated)
    }

    fun ungroupElements(groupId: String) {
        if (groupId.isEmpty()) return
        val current = _currentProject.value ?: return
        saveStateToUndo()
        val updated = current.elements.map {
            if (it.groupId == groupId) {
                it.copy(groupId = "", groupName = "")
            } else {
                it
            }
        }
        _currentProject.value = current.copy(elements = updated)
    }

    fun deleteSelectedElement() {
        val current = _currentProject.value ?: return
        val selId = _selectedElementId.value ?: return
        saveStateToUndo()

        val filtered = current.elements.filter { it.id != selId }
        _currentProject.value = current.copy(elements = filtered)
        _selectedElementId.value = null
    }

    fun duplicateSelectedElement() {
        val current = _currentProject.value ?: return
        val selId = _selectedElementId.value ?: return
        val target = current.elements.firstOrNull { it.id == selId } ?: return
        saveStateToUndo()

        val nextZ = (current.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
        val copy = target.copy(
            id = UUID.randomUUID().toString(),
            x = (target.x + 25f).coerceIn(20f, current.canvasWidth - 20f),
            y = (target.y + 25f).coerceIn(20f, current.canvasHeight - 20f),
            zIndex = nextZ
        )

        _currentProject.value = current.copy(elements = current.elements + copy)
        _selectedElementId.value = copy.id
    }

    fun moveSelectedElementLayer(up: Boolean) {
        val current = _currentProject.value ?: return
        val selId = _selectedElementId.value ?: return
        val target = current.elements.firstOrNull { it.id == selId } ?: return
        saveStateToUndo()

        // Gather all elements sorted by Z-index
        val sorted = current.elements.sortedBy { it.zIndex }.toMutableList()
        val index = sorted.indexOfFirst { it.id == selId }
        if (index == -1) return

        if (up && index < sorted.lastIndex) {
            // Swap with next layer
            val temp = sorted[index]
            sorted[index] = sorted[index + 1]
            sorted[index + 1] = temp
        } else if (!up && index > 0) {
            // Swap with previous layer
            val temp = sorted[index]
            sorted[index] = sorted[index - 1]
            sorted[index - 1] = temp
        }

        // Re-align serial index
        val updated = sorted.mapIndexed { i, element ->
            element.copy(zIndex = i)
        }
        _currentProject.value = current.copy(elements = updated)
    }

    // ──────────────────────────────────────────────
    // BRUSH DRAWING CONTROL
    // ──────────────────────────────────────────────

    fun setBrushColor(color: Int) {
        _brushColor.value = color
    }

    fun setBrushThickness(thickness: Float) {
        _brushThickness.value = thickness
    }

    fun appendBrushPoint(px: Float, py: Float) {
        _currentStrokePoints.value = _currentStrokePoints.value + Pair(px, py)
    }

    fun finishBrushStroke() {
        val points = _currentStrokePoints.value
        val current = _currentProject.value
        if (points.isNotEmpty() && current != null) {
            saveStateToUndo()

            // Calculate center of drawing bounding box
            val xs = points.map { it.first }
            val ys = points.map { it.second }
            val minX = xs.minOrNull() ?: 100f
            val maxX = xs.maxOrNull() ?: 150f
            val minY = ys.minOrNull() ?: 100f
            val maxY = ys.maxOrNull() ?: 150f
            
            val centerX = (minX + maxX) / 2f
            val centerY = (minY + maxY) / 2f

            // Serialize points
            val serializedPoints = LogoElement.fromPoints(points)

            val nextZ = (current.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
            val brushElement = LogoElement(
                type = ElementType.BRUSH,
                pathPointsData = serializedPoints,
                brushColor = _brushColor.value,
                brushThickness = _brushThickness.value,
                x = centerX,
                y = centerY,
                zIndex = nextZ
            )

            _currentProject.value = current.copy(elements = current.elements + brushElement)
        }
        _currentStrokePoints.value = emptyList()
    }

    // ──────────────────────────────────────────────
    // SAVE LOGO PERSISTENCE CONTROL
    // ──────────────────────────────────────────────

    fun saveProjectDirectly() {
        viewModelScope.launch {
            _currentProject.value?.let { 
                _saveStatus.value = SaveStatus.SAVING
                repoSave(it)
                _saveStatus.value = SaveStatus.SAVED
            }
        }
    }

    private suspend fun repoSave(project: LogoProject) {
        val newId = repository.saveLogo(project)
        if (project.id == 0) {
            _currentProject.value = project.copy(id = newId)
        }
    }
}

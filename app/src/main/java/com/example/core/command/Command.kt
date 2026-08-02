package com.example.core.command

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Command interface representing an undoable/redoable action in the video editing suite.
 */
interface Command {
    val actionName: String
    fun execute()
    fun undo()
}

/**
 * State representing current undo/redo stack status.
 */
data class UndoRedoState(
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val undoActionName: String? = null,
    val redoActionName: String? = null,
    val historySize: Int = 0
)

/**
 * Centralized Manager that tracks execution history of Commands.
 */
class UndoRedoManager(
    private val maxHistorySize: Int = 50
) {
    private val undoStack = java.util.ArrayDeque<Command>()
    private val redoStack = java.util.ArrayDeque<Command>()

    private val _state = MutableStateFlow(UndoRedoState())
    val state: StateFlow<UndoRedoState> = _state.asStateFlow()

    /**
     * Executes a new command and pushes it onto the undo stack, clearing the redo stack.
     */
    fun executeCommand(command: Command) {
        command.execute()
        undoStack.push(command)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeLast()
        }
        redoStack.clear()
        updateState()
    }

    /**
     * Performs undo on the top command of the stack.
     */
    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val command = undoStack.pop()
        command.undo()
        redoStack.push(command)
        updateState()
        return true
    }

    /**
     * Performs redo on the top command of the redo stack.
     */
    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val command = redoStack.pop()
        command.execute()
        undoStack.push(command)
        updateState()
        return true
    }

    /**
     * Clears undo and redo stacks.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateState()
    }

    private fun updateState() {
        _state.value = UndoRedoState(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            undoActionName = undoStack.peek()?.actionName,
            redoActionName = redoStack.peek()?.actionName,
            historySize = undoStack.size
        )
    }
}

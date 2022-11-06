import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.compose.ui.window.WindowPosition.PlatformDefault.y
import java.awt.Point

@Composable
fun DragAnimation() {
    DragState.display()
}

val LocalWindow = compositionLocalOf<ComposeWindow> { error("No Local Window found") }

@Composable
fun DragAndDropWindow(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    title: String = "",
    icon: Painter? = null,
    content: @Composable() (FrameWindowScope.() -> Unit)
) {

    Window(onCloseRequest = onCloseRequest, state = state, title = title, icon = icon) {
        CompositionLocalProvider(LocalWindow provides window) {
            content.invoke(this)
        }
    }
}

internal class DropTargetKey

internal data class DropTarget<T>(
    val bounds: Rect,
    val density: Density,
    val window: ComposeWindow,
    private val accepts: (T) -> Boolean,
    private val onDrop: (T) -> Boolean,
    private val onEnter: (T) -> Unit,
    private val onExit: (T) -> Unit,
) {

    private fun <Q> saveExec(it: Any?, default: Q, f: (T) -> Q): Q {
        return try {
            return f(it as T)
        } catch (e: Exception) {
            default
        }
    }

    fun canAccept(it: Any?): Boolean = saveExec(it, false, accepts)
    fun doAccept(it: Any?): Boolean = saveExec(it, false, onDrop)
    fun doEnter(it: Any?): Unit = saveExec(it, Unit, onEnter)
    fun doExit(it: Any?): Unit = saveExec(it, Unit, onExit)
}

internal data class DragTarget<T>(
    val item: T,
    val preview: @Composable (item: T) -> Unit
) {
    @Composable
    fun show() = preview.invoke(item)
}

internal object DragState {

    private var dragTarget: DragTarget<*>? by mutableStateOf(null)
    private var positionOnScreen by mutableStateOf<Offset?>(null)
    private val isDragging by derivedStateOf { dragTarget != null }

    private val dropTargets = HashMap<DropTargetKey, DropTarget<*>>()

    fun addDropTarget(key: DropTargetKey, dropTarget: DropTarget<*>) {
        dropTargets[key] = dropTarget
    }

    fun removeDropTarget(key: DropTargetKey) {
        dropTargets.remove(key)
    }

    fun startDrag(dragTarget: DragTarget<*>) {
        this.dragTarget = dragTarget
    }

    fun endDrag(): Boolean {
        if (!isDragging) return false
        val target = dropTargets.values
            .firstOrNull { getBounds(it).contains(positionOnScreen!!) } //&& it.canAccept(dragTarget?.item)

        val result = target?.doAccept(dragTarget?.item)
        cancelDrag()
        return result == true
    }

    fun cancelDrag() {
        this.dragTarget = null
        this.positionOnScreen = null
    }

    fun onDrag(positionOnScreen: Offset?) {
        val old = this.positionOnScreen
        val new = positionOnScreen
        this.positionOnScreen = positionOnScreen

        dropTargets.values.forEach {
            val bounds = getBounds(it)
            val inNew = new != null && bounds.contains(new)
            val inOld = old != null && bounds.contains(old)
            val accepts by lazy { it.canAccept(dragTarget?.item) }

            if (inNew && !inOld && accepts)
                it.doEnter(dragTarget?.item)
            if (!inNew && inOld) {
                it.doExit(dragTarget?.item)
            }
        }
    }

    private fun getBounds(dropTarget: DropTarget<*>): Rect {
        with(dropTarget.density) {
            val positionOnScreen = dropTarget.window.contentPane.locationOnScreen
            val top = positionOnScreen.y.dp + dropTarget.bounds.top.toDp() + 20.dp //WindowBar offset
            val left = positionOnScreen.x.dp + dropTarget.bounds.left.toDp()
            val right = left + dropTarget.bounds.width.toDp()
            val bottom = top + dropTarget.bounds.height.toDp()
            val bounds = Rect(left = left.value, top = top.value, right = right.value, bottom = bottom.value)
            return bounds
        }
    }

    //TODO: Make size configurable
    @Composable
    fun display() {
        with(LocalDensity.current) {
            if (isDragging && positionOnScreen != null) {
                Window(
                    onCloseRequest = {},
                    transparent = false,
                    resizable = false,
                    undecorated = true,
                    state = WindowState(
                        width = 200.dp,
                        height = 150.dp,
                        position = WindowPosition(positionOnScreen!!.x.toDp(), positionOnScreen!!.y.toDp())
                    )
                ) {
                    dragTarget?.show()
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun <T> Modifier.dragTarget(
    item: T,
    onDragSuccess: () -> Unit = {},
    preview: @Composable (item: T) -> Unit
) = composed {

    var pos by remember { mutableStateOf<Offset?>(null) }
    val mod = onPointerEvent(PointerEventType.Move) {
        if (it.buttons.isPrimaryPressed) {
            pos = it.awtEvent.locationOnScreen.toOffset()
        }
    }

    mod.pointerInput(preview, item) { //incremental key to ensure uniqueness
        detectDragGestures(
            onDragStart = {
                DragState.startDrag(DragTarget(item, preview))
            },
            onDragEnd = {
                if (DragState.endDrag()) onDragSuccess.invoke()
                pos = null
            },
            onDragCancel = {
                DragState.cancelDrag()
                pos = null
            },
            onDrag = { _, _ ->
                DragState.onDrag(pos)
            }
        )
    }

}

fun <T> Modifier.dropTarget(
    accepts: (T) -> Boolean = { true },
    onDrop: (T) -> Boolean,
    onEnter: (T) -> Unit,
    onExit: (T) -> Unit,
) = composed {

    var bounds by remember { mutableStateOf<Rect>(Rect.Zero) }
    val key by remember { mutableStateOf(DropTargetKey()) }
    val density = LocalDensity.current
    val window = LocalWindow.current

    val mod = onGloballyPositioned {
        bounds = it.boundsInWindow()
    }

    DisposableEffect(bounds, key) {
        DragState.addDropTarget(
            key,
            DropTarget(bounds, density, window, accepts, onDrop, onEnter, onExit)
        )

        onDispose {
            DragState.removeDropTarget(key)
        }
    }

    mod
}

internal fun Point.toOffset(): Offset = Offset(this.x.toFloat(), this.y.toFloat())
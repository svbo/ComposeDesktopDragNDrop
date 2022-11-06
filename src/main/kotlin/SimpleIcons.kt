import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

@Composable
@Preview
fun IconsApp(controller: SimpleIconsController) {
    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(5.dp)).fillMaxWidth()) {
                for (draggable in controller.state.draggables) {
                    Box(modifier = Modifier
                        .height(30.dp)
                        .dragTarget(
                            item = draggable,
                            onDragSuccess = { controller.triggerAction(SimpleIconsAction.Remove(draggable)) },
                            preview = { Icon(imageVector = draggable, contentDescription = draggable.name ) })
                    ) {
                        Icon(imageVector = draggable, contentDescription = draggable.name )
                    }
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                modifier = Modifier.height(50.dp)
                    .border(width = 1.dp, color = if(controller.state.isActive) Color.Red else Color.Black, shape = RoundedCornerShape(5.dp))
                    .fillMaxSize()
                    .dropTarget<ImageVector>(
                        onDrop = {
                            controller.triggerAction(SimpleIconsAction.Add(it))
                            controller.triggerAction(SimpleIconsAction.SetActive(false))
                            true
                        },
                        onExit = {controller.triggerAction(SimpleIconsAction.SetActive(false))},
                        onEnter = {controller.triggerAction(SimpleIconsAction.SetActive(true))},
                    )
            ) {
            }
        }
    }
}

fun main() = application {
    for (window in windows) {
        DragAndDropWindow(onCloseRequest = ::exitApplication, state = window.windowState) {
            IconsApp(SimpleIconsController(window))
        }
    }
    DragAnimation()
}

private val windows by mutableStateOf(
    listOf(
        SimpleIconsState(windowState = WindowState(position =  WindowPosition(100.dp, 100.dp), size = DpSize(300.dp, 400.dp)),
            draggables = listOf(Icons.Filled.Edit, Icons.Filled.Delete, Icons.Filled.Add),
            selected = listOf()),
        SimpleIconsState(windowState = WindowState(position =  WindowPosition(300.dp, 150.dp), size = DpSize(300.dp, 400.dp)),
            draggables = listOf(Icons.Filled.Refresh, Icons.Filled.Home, Icons.Filled.Done),
            selected = listOf()),
    )
)

class SimpleIconsController(window: SimpleIconsState) {
    var state by mutableStateOf(window)
        private set

    fun triggerAction(action: SimpleIconsAction) {
        state = when (action) {
            is SimpleIconsAction.Add -> state.copy(draggables = state.draggables.plus(action.draggable))
            is SimpleIconsAction.Remove -> state.copy(draggables = state.draggables.minus(action.draggable))
            is SimpleIconsAction.SetActive -> state.copy(isActive = action.isActive)
        }
    }
}

data class SimpleIconsState(
    val windowState: WindowState,
    val draggables: List<ImageVector>,
    val selected: List<ImageVector>,
    val isActive: Boolean = false
)

sealed class SimpleIconsAction() {
    class Add(val draggable: ImageVector) : SimpleIconsAction()
    class Remove(val draggable: ImageVector) : SimpleIconsAction()
    class SetActive(val isActive: Boolean): SimpleIconsAction()
}

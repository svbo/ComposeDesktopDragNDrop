import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

@Composable
@Preview
fun ToDoApp(controller: ToDoWindowController) {
    MaterialTheme {
        Row() {
            for (activity in ToDoActivity.values()) {
                Column(
                    modifier = Modifier.weight(weight = 1f)
                        .fillMaxHeight()
                        .border(
                            BorderStroke(
                                width = 2.dp, Color.DarkGray
                            )
                        )
                        .dropTarget<ToDoItem>(
                            accepts = { true },
                            onEnter = {},
                            onExit = {},
                            onDrop = {
                                controller.triggerAction(ToDoAction.Transition(item = it, newActivity = activity))
                                true
                            }
                        )
                ) {
                    Text(modifier = Modifier.padding(all = 5.dp), text = activity.name)
                    Spacer(modifier = Modifier.height(5.dp))
                    for (item in controller.state.forActivity(activity)) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(all = 5.dp)
                                .dragTarget(item = item, preview = { ToDoCardContent(it) }),
                            shape = RoundedCornerShape(5.dp),
                            border = BorderStroke(width = 2.dp, color = Color.DarkGray)
                        ) {
                            ToDoCardContent(item)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
                Spacer(modifier = Modifier.width(5.dp).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun ToDoCardContent(item: ToDoItem) {
    Column {
        Text(text = item.title)
        Text(text = item.description)
    }
}

fun main() = application {
    for (window in windows) {
        DragAndDropWindow(onCloseRequest = ::exitApplication, state = window.windowState) {
            ToDoApp(ToDoWindowController(window))
        }
    }
    DragAnimation()
}

private val windows by mutableStateOf(
    listOf(
        ToDoWindowState(
            windowState = WindowState(position = WindowPosition(100.dp, 100.dp)), assignee = "Peter Parker",
            draggables = listOf(
                ToDoItem(
                    activity = ToDoActivity.TODO,
                    title = "Become Spiderman",
                    description = "Get bitten by radioactive spider"
                ),
                ToDoItem(activity = ToDoActivity.TODO, title = "Save uncle Ben", description = "Don't be a dick")
            )
        )
    )
)

class ToDoWindowController(window: ToDoWindowState) {
    var state by mutableStateOf(window)
        private set

    fun triggerAction(action: ToDoAction) {
        state = when (action) {
            is ToDoAction.Transition -> {
                val newDraggables = state.draggables.toMutableList()
                val newItem = action.item.copy(activity = action.newActivity)
                newDraggables.remove(action.item)
                newDraggables.add(newItem)
                state.copy(draggables = newDraggables)
            }
        }
    }
}

data class ToDoWindowState(
    val assignee: String,
    val windowState: WindowState,
    val draggables: List<ToDoItem>,
) {
    fun forActivity(activity: ToDoActivity): List<ToDoItem> {
        return draggables.filter { it.activity == activity }
    }
}

data class ToDoItem(
    val activity: ToDoActivity,
    val title: String,
    val description: String,
)

sealed class ToDoAction() {
    class Transition(val item: ToDoItem, val newActivity: ToDoActivity) : ToDoAction()
}

enum class ToDoActivity {
    TODO, IN_PROGRESS, DONE, PLANNING, TESTING
}

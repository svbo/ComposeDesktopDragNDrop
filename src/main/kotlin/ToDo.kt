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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import java.awt.SystemColor.window

@Composable
@Preview
fun ToDoApp(triggerAction: (ToDoAction) -> Unit, state: ToDoState, assignee: String) {
    MaterialTheme {
        Column {
            Text(text = "ToDo Items for: $assignee")
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
                                    triggerAction(
                                        ToDoAction.Transition(
                                            item = it,
                                            newActivity = activity,
                                            newAssignee = assignee
                                        )
                                    )
                                    true
                                }
                            )
                    ) {
                        Text(modifier = Modifier.padding(all = 5.dp), text = activity.name)
                        Spacer(modifier = Modifier.height(5.dp))
                        for (item in state.find(activity, assignee)) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(all = 5.dp)
                                    .dragTarget(item = item, preview = {
                                        Card(
                                            modifier = Modifier.fillMaxSize(),
                                            shape = RoundedCornerShape(5.dp),
                                            border = BorderStroke(width = 2.dp, color = Color.DarkGray)
                                        ) {
                                            ToDoCardContent(it)
                                        }
                                    }),
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
}

@Composable
private fun ToDoCardContent(item: ToDoItem) {
    Column(modifier = Modifier.padding(all = 3.dp)) {
        Text(text = item.title)
        Text(text = item.description)
    }
}

fun main() = application {
    for (assignee in controller.state.assignees) {
        DragAndDropWindow(onCloseRequest = ::exitApplication) {
            ToDoApp(controller::triggerAction, controller.state, assignee)
        }
    }
    DragAnimation()
}

private val controller = ToDoWindowController(
    ToDoState(
        assignees = listOf("Peter Parker", "Homer Simpson"),
        draggables = listOf(
            ToDoItem(
                assignee = "Peter Parker",
                activity = ToDoActivity.TODO,
                title = "Become Spiderman",
                description = "Get bitten by radioactive spider"
            ),
            ToDoItem(
                assignee = "Peter Parker",
                activity = ToDoActivity.TODO, title = "Be a Hero", description = "Do Spidy stuff"
            ),
            ToDoItem(
                assignee = "Homer Simpson",
                activity = ToDoActivity.TODO, title = "Drink Duff", description = "At Moe's"
            ),
            ToDoItem(
                assignee = "Homer Simpson",
                activity = ToDoActivity.TODO, title = "Work", description = "No nuclear meltdown this time"
            )
        )
    )
)


class ToDoWindowController(state: ToDoState) {
    var state by mutableStateOf(state)
        private set

    fun triggerAction(action: ToDoAction) {
        state = when (action) {
            is ToDoAction.Transition -> {
                val newDraggables = state.draggables.toMutableList()
                val newItem = action.item.copy(activity = action.newActivity, assignee = action.newAssignee)
                newDraggables.remove(action.item)
                newDraggables.add(newItem)
                state.copy(draggables = newDraggables)
            }
        }
    }
}

data class ToDoState(
    val assignees: List<String>,
    val draggables: List<ToDoItem>,
) {
    fun find(activity: ToDoActivity, assignee: String): List<ToDoItem> {
        return draggables.filter { it.activity == activity && it.assignee == assignee }
    }
}

data class ToDoItem(
    val assignee: String,
    val activity: ToDoActivity,
    val title: String,
    val description: String,
)

sealed class ToDoAction() {
    class Transition(val item: ToDoItem, val newActivity: ToDoActivity, val newAssignee: String) : ToDoAction()
}

enum class ToDoActivity {
    TODO, PLANNING, IN_PROGRESS, TESTING, DONE
}

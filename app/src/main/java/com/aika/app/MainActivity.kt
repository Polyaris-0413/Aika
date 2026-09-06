package com.aika.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aika.app.ui.components.groupItemShape
import com.aika.app.ui.components.groupItemSpacing
import com.aika.app.ui.components.groupTitleSpacing
import com.aika.app.ui.components.listItemColors
import com.aika.app.ui.theme.AikaTheme
import com.aika.app.ui.theme.AnimationTokens

/**
 * 一条待办。列表物理顺序 = 显示顺序:未完成区在前,已完成区在后,
 * toggle 时把项从原位取出再插入目标区,配合稳定 id 让 LazyColumn 播放跨区滑动动画。
 */
data class Task(
    val id: Long,
    val title: String,
    val completed: Boolean = false,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AikaTheme {
                TodoScreen()
            }
        }
    }
}

@Composable
fun TodoScreen() {
    var nextTaskId by remember { mutableLongStateOf(0L) }
    val tasks = remember { mutableStateListOf<Task>() }
    var showAddSheet by remember { mutableStateOf(false) }

    fun toggleTask(task: Task) {
        val updated = task.copy(completed = !task.completed)
        tasks.removeAll { it.id == task.id }
        if (updated.completed) {
            // 完成时沉底,追加到已完成区末尾
            tasks.add(updated)
        } else {
            // 恢复时插回未完成区末尾(首个已完成项之前;整表无已完成项则到末尾)
            val insertAt = tasks.indexOfFirst { it.completed }
                .takeIf { it >= 0 } ?: tasks.size
            tasks.add(insertAt, updated)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.action_add_task)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(groupItemSpacing)
        ) {
            itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                val pendingCount = tasks.count { !it.completed }
                // 已完成标题挂在区内首个已完成项上,随其一起滑入
                val isFirstDone = task.completed && (index == 0 || !tasks[index - 1].completed)
                // 拼接形状按区内位置计算:未完成区在前可直接用全局 index
                val positionInGroup = if (task.completed) index - pendingCount else index
                val groupCount = if (task.completed) tasks.size - pendingCount else pendingCount

                Column(
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(AnimationTokens.Large),
                        placementSpec = tween(AnimationTokens.Medium)
                    )
                ) {
                    if (isFirstDone) {
                        Text(
                            text = stringResource(R.string.group_completed),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                top = 16.dp,
                                bottom = groupTitleSpacing
                            )
                        )
                    }
                    ListItem(
                        headlineContent = {
                            Text(
                                text = task.title,
                                color = if (task.completed) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        },
                        colors = listItemColors(),
                        modifier = Modifier
                            .clip(groupItemShape(positionInGroup, groupCount))
                            .clickable { toggleTask(task) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddTaskSheet(
            onConfirm = { title ->
                tasks.add(Task(id = nextTaskId++, title = title))
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskSheet(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.label_task_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.action_save_task))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TodoScreenPreview() {
    AikaTheme {
        TodoScreen()
    }
}

package com.aika.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aika.app.data.Task
import com.aika.app.data.TaskRepository
import com.aika.app.ui.components.animatedGroupItemShape
import com.aika.app.ui.components.groupItemSpacing
import com.aika.app.ui.components.groupTitleSpacing
import com.aika.app.ui.components.listItemColors
import com.aika.app.ui.theme.AikaTheme
import com.aika.app.ui.theme.AnimationTokens
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val repository = remember { TaskRepository(context) }
    val scope = rememberCoroutineScope()
    val tasks by repository.tasks.collectAsState(initial = emptyList())
    var showAddSheet by remember { mutableStateOf(false) }

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
                    // 标题随分区归属渐现/渐隐:完成项布局位置不变时(如唯一/末尾任务),
                    // 该过渡是唯一的可见反馈,故需独立于 placement 动画存在
                    AnimatedVisibility(
                        visible = isFirstDone,
                        enter = fadeIn(tween(AnimationTokens.Large)) +
                            expandVertically(tween(AnimationTokens.Medium)),
                        exit = fadeOut(tween(AnimationTokens.Medium)) +
                            shrinkVertically(tween(AnimationTokens.Medium))
                    ) {
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
                            .clip(
                                animatedGroupItemShape(
                                    positionInGroup,
                                    groupCount,
                                    tween(AnimationTokens.Medium)
                                )
                            )
                            .clickable {
                                scope.launch { repository.toggleTask(task) }
                            }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddTaskSheet(
            onConfirm = { title ->
                showAddSheet = false
                scope.launch { repository.addTask(title) }
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

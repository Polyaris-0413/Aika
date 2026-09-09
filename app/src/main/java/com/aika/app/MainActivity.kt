package com.aika.app

import android.os.Bundle
import android.text.format.DateFormat
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/** 已完成标题在 displayItems 中的占位 key(与任务 Long id 区分) */
private const val CompletedHeaderKey = "completed-header"

/** 今日日期,按系统语言本地化,日期与星期分段格式化后拼接("9月7日 星期一"样式,含分隔空格) */
private fun todayLabel(): String {
    val locale = Locale.getDefault()
    val datePattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMd")
    val weekdayPattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEE")
    val date = DateTimeFormatter.ofPattern(datePattern, locale).format(LocalDate.now())
    val weekday = DateTimeFormatter.ofPattern(weekdayPattern, locale).format(LocalDate.now())
    return "$date $weekday"
}

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
    // 旋转等配置变更会重建 Activity,remember 状态丢失;可恢复的 UI 状态一律 rememberSaveable
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    val pendingTasks = tasks.filter { !it.completed }
    val doneTasks = tasks.filter { it.completed }
    // 标题以 null 占位插入交界处,三个 items 块并成单一块:
    // 任务跨区移动才能被识别为同块内 key 移动(组合复用,涟漪与滑动动画连续),
    // 否则组合销毁重建,涟漪状态随旧组合销毁而中断
    val displayItems = if (doneTasks.isEmpty()) {
        pendingTasks
    } else {
        pendingTasks + listOf<Task?>(null) + doneTasks
    }
    // pinned 等价的固定顶栏:紧凑贴顶,标题与统计都在顶栏内
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = groupTitleSpacing)
            ) {
                val pendingLabel = stringResource(R.string.stat_pending)
                val completedLabel = stringResource(R.string.stat_completed)
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    Text(
                        text = "$pendingLabel ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedCounter(count = pendingTasks.size)
                    Text(
                        text = " · $completedLabel ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedCounter(count = doneTasks.size)
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.action_add_task)
                )
            }
        }
    ) { innerPadding ->
        // 面板始终显示(空态时承载居中占位),层级结构与有任务时保持一致
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            AnimatedContent(
                targetState = tasks.isEmpty(),
                transitionSpec = {
                    if (targetState) {
                        // 删空恢复:维持瞬时切换;占位出现淡入在深色下有黑闪感(同下方 animateItem 注释)
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        // 首个待办出现:仅占位淡出;列表直出不淡入(黑闪),占位居下层不拦截点击
                        EnterTransition.None togetherWith fadeOut(tween(AnimationTokens.Large))
                    }
                },
                label = "emptyState"
            ) { isEmpty ->
                if (isEmpty) {
                // 空状态:居中占位(图标圆底 + 标题 + 引导语)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_checklist),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = stringResource(R.string.empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            } else {
                LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(groupItemSpacing)
            ) {
                itemsIndexed(displayItems, key = { _, item -> item?.id ?: CompletedHeaderKey }) { index, item ->
                if (item == null) {
                    Text(
                        text = stringResource(R.string.group_completed),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 16.dp, top = 16.dp, bottom = groupTitleSpacing)
                            .animateItem(
                                // 同任务行:出现淡入在深色下有黑闪感,禁用;消失淡出保留(正常)
                                fadeInSpec = null,
                                placementSpec = tween(AnimationTokens.Medium),
                                fadeOutSpec = tween(AnimationTokens.Medium)
                            )
                    )
                } else {
                    TaskRow(
                        task = item,
                        positionInGroup = if (item.completed) index - pendingTasks.size - 1 else index,
                        groupCount = if (item.completed) doneTasks.size else pendingTasks.size,
                        modifier = Modifier.animateItem(
                            // 卡片底直出:整卡淡入在深色下表现为底色从纯黑渐显(黑闪)
                            fadeInSpec = null,
                            placementSpec = tween(AnimationTokens.Medium)
                        ),
                        onToggle = { scope.launch { repository.toggleTask(item) } }
                    )
                }
            }
            }
            }
        }
        }
    }

    if (showAddSheet) {
        AddTaskDialog(
            onConfirm = { title -> scope.launch { repository.addTask(title) } },
            onDismiss = { showAddSheet = false }
        )
    }
}

/** 顶栏统计数字:变化时新旧数字按增减方向垂直滚动交接(增加上滚、减少下滚) */
@Composable
private fun AnimatedCounter(count: Int) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically(tween(AnimationTokens.Medium)) { it } +
                    fadeIn(tween(AnimationTokens.Medium))) togetherWith
                    (slideOutVertically(tween(AnimationTokens.Medium)) { -it } +
                        fadeOut(tween(AnimationTokens.Medium)))
            } else {
                (slideInVertically(tween(AnimationTokens.Medium)) { -it } +
                    fadeIn(tween(AnimationTokens.Medium))) togetherWith
                    (slideOutVertically(tween(AnimationTokens.Medium)) { it } +
                        fadeOut(tween(AnimationTokens.Medium)))
            }.using(SizeTransform(clip = false))
        },
        label = "statCount"
    ) { value ->
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 拼接卡片样式的任务行:未完成/已完成两区共用,形状按区内位置计算;
 *  animateItem 属于 LazyItemScope,须由列表项 lambda 以 modifier 传入 */
@Composable
private fun TaskRow(
    task: Task,
    positionInGroup: Int,
    groupCount: Int,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    // 淡入只作用于前景文字:从已直出的卡片底泛出(不经过黑态)。
    // remember 状态在跨区移动的组合复用下保留,移动时文字不会重播淡入
    var contentVisible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(AnimationTokens.Large),
        label = "taskContentAlpha"
    )
    LaunchedEffect(Unit) { contentVisible = true }

    ListItem(
        headlineContent = {
            Text(
                text = task.title,
                color = if (task.completed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.alpha(contentAlpha)
            )
        },
        colors = listItemColors(),
        modifier = modifier
            .clip(
                animatedGroupItemShape(
                    positionInGroup,
                    groupCount,
                    tween(AnimationTokens.Medium)
                )
            )
            .clickable(onClick = onToggle)
    )
}

@Composable
private fun AddTaskDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    // dialog 与 bottom sheet 同为独立 popup 窗口:窗口销毁会连带强制隐藏 IME(无收起动画),
    // 故关闭前先向 IMM 发收起请求并清 view 焦点,让 IME 走系统正常收起流程
    val view = LocalView.current
    val imm = remember { view.context.getSystemService(InputMethodManager::class.java) }
    val focusRequester = remember { FocusRequester() }

    // dialog 窗口完成 attach 后自动聚焦输入框,焦点驱动输入法弹出
    LaunchedEffect(Unit) {
        view.post { focusRequester.requestFocus() }
    }

    fun dismiss() {
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = ::dismiss,
        title = { Text(stringResource(R.string.action_add_task)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.label_task_title)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(text.trim())
                    dismiss()
                },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save_task))
            }
        },
        dismissButton = {
            TextButton(onClick = ::dismiss) {
                Text(stringResource(R.string.action_cancel_task))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun TodoScreenPreview() {
    AikaTheme {
        TodoScreen()
    }
}

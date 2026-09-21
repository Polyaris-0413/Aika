package com.aika.app.ui

import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aika.app.R
import com.aika.app.data.Task
import com.aika.app.data.TaskRepository
import com.aika.app.ui.components.animatedGroupItemShape
import com.aika.app.ui.components.groupItemSpacing
import com.aika.app.ui.components.groupTitleSpacing
import com.aika.app.ui.components.listItemColors
import com.aika.app.ui.theme.AikaTheme
import com.aika.app.ui.theme.AnimationTokens
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 已完成标题在 displayItems 中的占位 key(与任务 Long id 区分) */
/** 已完成标题在 displayItems 中的占位 key(与任务 Long id 区分) */
private const val CompletedHeaderKey = "completed-header"

@Composable
fun TodoScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { TaskRepository(context) }
    val scope = rememberCoroutineScope()

    // 正在退场的任务 id:点击后先播缩小淡出动画,再提交数据(见 toggleTask)
    var exitingTaskIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // 点击切换:先让卡片播完退场动画再写库。LazyColumn 的滚动锚点是"视口第一项",
    // 该项直接移走会触发跟随跳到新位置;先"消失"再落位则不跟随(配合带状态前缀的 key),
    // 同时也保住了退场动画(用 requestScrollToItem 那类跳转请求会吃掉 item 动画)
    fun toggleTask(task: Task) {
        if (task.id in exitingTaskIds) return
        exitingTaskIds = exitingTaskIds + task.id
        scope.launch {
            // 等退场动画整段播完再提交:提前提交会把自管的缩小/淡出半途切断,
            // 该项在动画中途被移出组合,深色下表现为它的轮廓闪一下(实测反馈)
            delay(AnimationTokens.Medium.toLong())
            repository.toggleTask(task)
            exitingTaskIds = exitingTaskIds - task.id
        }
    }


    // 已登记过出现的任务 id:首屏连屏幕外的溢出项一起先全部登记,
    // 因此冷启动与"滚动露出溢出项"都不播放入场;只有真正新增(新 id)的项才播放。
    // 不能用"是否已过首屏"的全局开关:LazyColumn 只组合可视项,溢出项在数据变化后
    // 首次滚入视口也会重新组合,会被全局开关误判为新增而重播动画
    val seenTaskIds = remember { mutableSetOf<Long>() }
    // 各任务上次所在分区:跨区移动要在新位置播放入场,
    // 而 put 返回旧值,只有分区真的变了才是 true —— 溢出项滚入视口、滚动回收重建都不会重播
    val taskRegions = remember { mutableMapOf<Long, Boolean>() }
    // 「已完成」标题的入场:只在"已完成区从空变非空"的那次数据变化后播一次,
    // 标题被滚动回收重建时不重播(标志在下一次数据变化时被重新赋为 false)
    var headerJustAppeared by remember { mutableStateOf(false) }
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    LaunchedEffect(Unit) {
        var firstEmission = true
        var prevDoneEmpty = true
        repository.tasks.collect { list ->
            val doneEmpty = list.none { it.completed }
            if (firstEmission) {
                seenTaskIds.addAll(list.map { it.id })
                list.forEach { taskRegions[it.id] = it.completed }
                firstEmission = false
            } else {
                headerJustAppeared = !doneEmpty && prevDoneEmpty
            }
            prevDoneEmpty = doneEmpty
            tasks = list
        }
    }
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
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶栏:标题 + 统计 + 添加按钮。加了底部导航栏后 FAB 右下会与底栏打架,主操作移到这里
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                // end=4dp 取自 M3 Top App Bar 规范(容器 padding: 0 4px):
                // 该值与 Icon Button 的视觉变体无关(standard/filled/tonal/outlined 共用),
                // 所以换按钮样式时这个值不该跟着变
                .padding(start = 16.dp, end = 4.dp, bottom = groupTitleSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
            // 填色图标按钮:圆底包住图标(M3 原生组件,自带按下/聚焦状态色)
            FilledTonalIconButton(onClick = { showAddSheet = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.action_add_task)
                )
            }
        }
        // 面板始终显示(空态时承载居中占位),层级结构与有任务时保持一致
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            // 只有上边圆角:面板从底栏升起,底边与底栏同在屏幕下沿
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier
                .fillMaxSize()
                // 面板直接接底栏:两者底色不同(面板 surfaceContainer / 底栏 background),
                // 四角圆角靠这层色差衬托,不再需要额外的底色边
                .padding(bottom = contentPadding.calculateBottomPadding())
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
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(groupItemSpacing)
            ) {
                itemsIndexed(
                    displayItems,
                    // key 带状态前缀:跨区移动后 LazyColumn 的滚动锚点(原第一项 key)在新列表里找不到,
                    // 便不会跟随移走的项跳到新位置;跨区移动的视觉交给 TaskRow 的退场/入场动画
                    key = { _, item ->
                        item?.let { "${it.id}-${if (it.completed) "d" else "p"}" } ?: CompletedHeaderKey
                    },
                ) { index, item ->
                if (item == null) {
                    // 「已完成」标题:出现时自下方滑入并淡入,只在首次出现时播放。
                    // 不用缩放:缩放要占满整行才看得见,而整行大的项参与 LazyColumn 的图层动画
                    // 会把深色下的暗闪放大到整个宽度
                    val appear = remember { Animatable(if (headerJustAppeared) 0f else 1f) }
                    LaunchedEffect(Unit) {
                        if (headerJustAppeared) {
                            appear.animateTo(1f, tween(AnimationTokens.Large))
                        }
                    }
                    val titleRise = with(LocalDensity.current) { AnimationTokens.TitleRiseDp.dp.toPx() }
                    Text(
                        text = stringResource(R.string.group_completed),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = AnimationTokens.appearFade(appear.value)
                                translationY = (1f - appear.value) * titleRise
                            }
                            .padding(start = 16.dp, top = 16.dp, bottom = groupTitleSpacing)
                            .animateItem(
                                // 出现动画自管(滑入+淡入),此处 fadeIn 必须为 null;
                                // 消失淡出用 Large 与淡入一致,免得退得比进得还急
                                fadeInSpec = null,
                                placementSpec = tween(AnimationTokens.Medium),
                                fadeOutSpec = tween(AnimationTokens.Large)
                            )
                    )
                } else {
                    TaskRow(
                        task = item,
                        positionInGroup = if (item.completed) index - pendingTasks.size - 1 else index,
                        groupCount = if (item.completed) doneTasks.size else pendingTasks.size,
                        // 每个 id 只在首次组合时登记一次:已在集合内的(含滚动露出的溢出项)不播放入场
                        playAppear = seenTaskIds.add(item.id) ||
                            taskRegions.put(item.id, item.completed) != item.completed,
                        exiting = item.id in exitingTaskIds,
                        modifier = Modifier.animateItem(
                            // 出现动画由 TaskRow 自管(滑入+淡入),此处 fadeIn 必须为 null,否则双重 alpha
                            fadeInSpec = null,
                            placementSpec = tween(AnimationTokens.Medium)
                        ),
                        onToggle = { toggleTask(item) }
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
    playAppear: Boolean,
    exiting: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    // 入场:新增项自 0.9 放大到 1 并淡入。
    // remember 在跨区移动的组合复用下保留,移动时不重播
    val appear = remember { Animatable(if (playAppear) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (playAppear) appear.animateTo(1f, tween(AnimationTokens.Large))
    }

    // 退场:点击后原地缩小并淡出,播完由调用方提交数据(TaskRow 会被 LazyColumn 回收,不能靠它提交)
    val exit = remember { Animatable(0f) }
    LaunchedEffect(exiting) {
        if (exiting) exit.animateTo(1f, tween(AnimationTokens.Medium))
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
        modifier = modifier
            .graphicsLayer {
                // 进场分两段:先淡入到位、再放大(同步时看不出放大,见 AppearFadeFraction 注释);
                // 退场则与原尺寸一起缩小并淡出
                val appearGrow = AnimationTokens.appearGrow(appear.value)
                val scale = (AnimationTokens.ScaleEndpoint +
                    (1f - AnimationTokens.ScaleEndpoint) * appearGrow) *
                    (1f - (1f - AnimationTokens.ScaleEndpoint) * exit.value)
                scaleX = scale
                scaleY = scale
                alpha = AnimationTokens.appearFade(appear.value) * (1f - exit.value)
            }
            .clip(
                animatedGroupItemShape(
                    positionInGroup,
                    groupCount,
                    tween(AnimationTokens.Medium)
                )
            )
            .clickable(enabled = !exiting, onClick = onToggle)
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
        TodoScreen(contentPadding = PaddingValues())
    }
}

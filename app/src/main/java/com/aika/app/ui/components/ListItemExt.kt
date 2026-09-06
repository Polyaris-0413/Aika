package com.aika.app.ui.components

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * 圆角分组卡片辅助函数,移植自 Folio 的 ListItemExt.kt。
 * 拼接原理:组内多个 ListItem 以 2dp 间距竖排,各 item 用 clip 切分段圆角
 * (首项上大下小、中间全小、末项上小下大),视觉上连成一张圆角卡片。
 *
 * Folio 版移植自 Finito 的 ListItemExt.kt(源自 Grit)。
 * Copyright (C) 2026  Shubham Gorai
 * SPDX-License-Identifier: GPL-3.0-only
 * 来源:https://github.com/shub39/Grit(shared/ui/.../components/ListItemExt.kt)
 * GPL-3.0 全文:https://www.gnu.org/licenses/gpl-3.0.html
 */

private const val CONNECTED_CORNER_RADIUS = 4
// M3 形状 token:卡片档 = medium 12dp(见 material-3 规范)
private const val END_CORNER_RADIUS = 12

/** 拼接组内 item 的间距(圆角贴合处的缝隙) */
val groupItemSpacing: Dp = 2.dp

/** 分组标题与卡片组的间距(M3 间距基线 8dp) */
val groupTitleSpacing: Dp = 8.dp

/** 分组卡片底色:surfaceContainerHigh,比默认 ListItem 的 surfaceContainerLow 更突出 */
@Composable
fun listItemColors(): ListItemColors {
    return ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
}

/** 四角半径,顺序与 RoundedCornerShape 一致:topStart / topEnd / bottomEnd / bottomStart */
private data class CornerRadii(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomEnd: Dp,
    val bottomStart: Dp,
)

/** 组内位置对应的四角半径:首项上大下小、中间全小、末项上小下大、单项全大——形状规则单一来源 */
private fun groupItemCornerRadii(index: Int, count: Int): CornerRadii = when {
    count <= 1 -> CornerRadii(END, END, END, END)
    index == 0 -> CornerRadii(END, END, CONNECTED, CONNECTED)
    index == count - 1 -> CornerRadii(CONNECTED, CONNECTED, END, END)
    else -> CornerRadii(CONNECTED, CONNECTED, CONNECTED, CONNECTED)
}

private val END = END_CORNER_RADIUS.dp
private val CONNECTED = CONNECTED_CORNER_RADIUS.dp

/** 按组内位置选拼接形状(无过渡,适合形状不变的静态组) */
fun groupItemShape(index: Int, count: Int): Shape {
    val radii = groupItemCornerRadii(index, count)
    return RoundedCornerShape(radii.topStart, radii.topEnd, radii.bottomEnd, radii.bottomStart)
}

/** 按组内位置选拼接形状,四角半径随目标位置平滑过渡(适合会跨组移动的 item) */
@Composable
fun animatedGroupItemShape(index: Int, count: Int, spec: FiniteAnimationSpec<Dp>): Shape {
    val target = groupItemCornerRadii(index, count)
    val topStart by animateDpAsState(target.topStart, spec, label = "groupTopStart")
    val topEnd by animateDpAsState(target.topEnd, spec, label = "groupTopEnd")
    val bottomEnd by animateDpAsState(target.bottomEnd, spec, label = "groupBottomEnd")
    val bottomStart by animateDpAsState(target.bottomStart, spec, label = "groupBottomStart")
    return RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
}

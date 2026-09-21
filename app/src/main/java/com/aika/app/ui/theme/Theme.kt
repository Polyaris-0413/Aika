package com.aika.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 暂用 Material 3 的默认配色(darkColorScheme/lightColorScheme 的默认角色值)。
 *
 * 原先那套"种子色 + HCT 动态取色"已撤掉:配色方案留到后面单独设计,
 * 现在只保证明暗两套能跑通、且不引入额外依赖。
 */
@Composable
fun AikaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        typography = Typography,
        content = content,
    )
}

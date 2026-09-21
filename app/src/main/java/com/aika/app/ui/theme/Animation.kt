package com.aika.app.ui.theme

/**
 * 全局动画数值(时长 + 缓动),分档管理,与 Folio 的 AnimationTokens 同源:
 * 同类动作用同一档;新增动画先查档位复用,没有合适档位才新增,避免数值散落。
 */
object AnimationTokens {
    /** 一般动效(ms):列表项位移 */
    const val Medium = 250

    /** 内容出现/切换(ms):列表项淡入 */
    const val Large = 300

    /**
     * 新增项入场位移(dp):自下而上滑入。
     * 深色下卡片色(surfaceContainerHigh)与面板色(surfaceContainer)亮度差仅约 4%,
     * 面板色在 OLED 上视觉等同黑;单纯 alpha 淡入会被感知为"黑闪",
     * 位移提供进入方向感,把淡入前段读作"正在浮入"。
     */
    const val AppearRiseDp = 12
}

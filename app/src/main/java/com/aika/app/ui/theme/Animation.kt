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
}

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
     * 进出场缩放端点(比例):新增项自该比例放大到 1,被点击项缩小到该比例后消失。
     * 缩放是眼睛最敏感的进出信号(深色下卡片色与面板色亮度差仅约 4%,纯淡入几乎不可感知),
     * 且缩放是原地动画、不依赖目标位置,因此可以先播完退场动画再提交数据,
     * 让 LazyColumn 的滚动锚点不跟随移走的项跳动。
     */
    const val ScaleEndpoint = 0.9f

    /**
     * 入场阶段划分:前该比例只做淡入,剩余比例只做缩放。
     * 两段必须分开:与 alpha 同步时,"从无到有"会盖过约 10% 的尺寸变化,肉眼看不出放大
     * (退场能看到缩小,是因为卡片起初就是完全可见的大卡片)。
     */
    const val AppearFadeFraction = 0.4f

    /** 入场进度 → 淡入进度:前 AppearFadeFraction 段先淡入到位 */
    fun appearFade(progress: Float): Float =
        (progress / AppearFadeFraction).coerceAtMost(1f)

    /** 入场进度 → 缩放进度:与 [appearFade] 互补的后半段,此时元素已完全可见 */
    fun appearGrow(progress: Float): Float =
        ((progress - AppearFadeFraction) / (1f - AppearFadeFraction)).coerceIn(0f, 1f)

    /**
     * 退场进行到该比例即提交数据:余下的尾程与入场动画重叠。
     * 若等退场全部播完再提交(比例为 1),观感会变成"旧项彻底消失后新内容才开始",两项动画脱节。
     */
    const val ExitCommitFraction = 0.6f
}

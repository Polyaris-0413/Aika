package com.aika.app.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * 全局动画数值(时长 + 缓动),分档管理,与 Folio 的 AnimationTokens 同源:
 * 同类动作用同一档;新增动画先查档位复用,没有合适档位才新增,避免数值散落。
 */
object AnimationTokens {
    /** 一般动效(ms):列表项位移与退场。取 M3 duration token 的 medium1 */
    const val Medium = 250

    /** 内容出现(ms):「已完成」标题的淡入淡出等。取 M3 duration token 的 medium2。
     *  列表项入场已改用 [appearSpring],不再受此值约束 */
    const val Large = 300

    /**
     * 进出场缩放端点(比例):新增项自该比例放大到 1,被点击项缩小到该比例后消失。
     * 越接近 1 变化幅度越小。
     *
     * 缩放是眼睛最敏感的进出信号(深色下卡片色与面板色亮度差仅约 4%,纯淡入几乎不可感知),
     * 且缩放是原地动画、不依赖目标位置,因此可以先播完退场动画再提交数据,
     * 让 LazyColumn 的滚动锚点不跟随移走的项跳动。
     */
    const val ScaleEndpoint = 0.95f

    /**
     * 入场阶段划分:前该比例只做淡入,剩余比例只做缩放。
     * 两段必须分开:与 alpha 同步时,"从无到有"会盖过约 10% 的尺寸变化,肉眼看不出放大
     * (退场能看到缩小,是因为卡片起初就是完全可见的大卡片)。
     */
    const val AppearFadeFraction = 0.4f

    /** 入场进度 → 淡入进度:前 AppearFadeFraction 段先淡入到位 */
    fun appearFade(progress: Float): Float =
        (progress / AppearFadeFraction).coerceAtMost(1f)

    /**
     * 入场缩放允许透出的过冲上限。expressive 的 spring 到位前会冲过目标值,
     * 若这里夹到 1,回弹会被抹掉、缩放准确地停在 1.0(等于白用 spring)。
     */
    const val AppearOvershoot = 0.2f

    /**
     * 入场进度 → 缩放进度:与 [appearFade] 互补的后半段,此时元素已完全可见。
     * 上限为 1 + [AppearOvershoot],让 spring 的过冲能反映到缩放上。
     */
    fun appearGrow(progress: Float): Float =
        ((progress - AppearFadeFraction) / (1f - AppearFadeFraction))
            .coerceIn(0f, 1f + AppearOvershoot)

    /**
     * 列表项入场的弹性规格。
     *
     * M3 Expressive 把动画按域拆开:位置/尺寸/缩放走 spring(可以过冲、再收住),
     * 透明度/颜色走 tween(可预测、不过冲)。这里只给缩放用,淡入仍由 [appearFade] 按 tween 语义派生。
     *
     * 不用 MaterialTheme.motionScheme 的 defaultSpatialSpec():当前 material3 版本里
     * MotionScheme 还是 internal,拿不到。改为直接给参数 ——
     * 阻尼比决定过冲量:理论过冲 ≈ exp(-πζ/√(1-ζ²))。
     * Compose 只提供 0.2 / 0.5 / 0.75 / 1.0 四个命名档,这里取中间的 0.65(过冲约 7%),
     * 对应"有回弹但不抢戏"的位置。
     */
    fun appearSpring(): FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.65f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /**
     * 「已完成」标题入场的上滑距离(dp):标题是纯文字,不适合用缩放做进入
     * (缩放要项占满整行才看得见,而整行大的项参与 LazyColumn 的图层动画
     * 会放大深色下的暗闪),故改用位移给进入感。
     */
    const val TitleRiseDp = 8

    /**
     * 分页切换(ms):与 book-story 的 FadeTransition 同源。
     * 新页淡入 + 自 TabSwitchScaleFrom 放大,旧页单纯淡出。
     */
    const val TabSwitch = 250

    /** 分页淡入的初始缩放(97.5% → 100%):纯淡入显得平,加一点"浮入"感 */
    const val TabSwitchScaleFrom = 0.975f
}

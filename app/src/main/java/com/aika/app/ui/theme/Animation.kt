package com.aika.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

/*
 * M3 的 easing 曲线,取自官方 motion token(与 material-3 技能文档里的
 * --md-sys-motion-easing-* 同值)。MDC 对 Emphasized 另给出一段等价的 path 写法,
 * 这里统一用三次贝塞尔:Compose 原生支持,且六条形式一致。
 *
 * Emphasized 与 Standard 的"首尾"曲线数值相同,仍分开命名:语义不同,日后可独立调整。
 */
val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val StandardDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
val StandardAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

/**
 * 全局动画数值(时长 + 缓动),分档管理,与 Folio 的 AnimationTokens 同源:
 * 同类动作用同一档;新增动画先查档位复用,没有合适档位才新增,避免数值散落。
 *
 * 时长与 easing 的配对采用 M3 官方的 Suggested Pairings
 * (见 material-3 技能的 typography-and-shape.md):
 *   进入屏幕       Emphasized Decelerate  400ms
 *   永久退场       Emphasized Accelerate  200ms
 *   暂时退场       Emphasized             300ms
 *   小工具类过渡   Standard               300ms
 * 官方未给配对的动作(如统计数字滚动)沿用 M3 duration token 的档位。
 */
object AnimationTokens {
    /** 一般动效(ms):统计数字滚动等,官方无专门配对。取 M3 duration token 的 medium1 */
    const val Medium = 250

    /** 进入屏幕(ms):「已完成」标题入场。官方 Element enters screen */
    const val EnterScreen = 400

    /** 暂时退场(ms):标题消失、空状态占位淡出。官方 Element exits temporarily */
    const val ExitTemporary = 300

    /** 永久退场(ms):卡片被点击后缩小消失。官方 Element exits permanently */
    const val ExitPermanent = 200

    /** 分页切换(ms):官方 Small utility transition */
    const val TabSwitch = 300

    /** 分页淡入的初始缩放(97.5% → 100%):纯淡入显得平,加一点"浮入"感 */
    const val TabSwitchScaleFrom = 0.975f

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
     * 入场缩放允许透出的过冲上限(项目自定值,M3 无对应 token)。
     * spring 到位前会冲过目标值,若这里夹到 1,回弹会被抹掉、缩放准确地停在 1.0。
     * 当前 spatial spring 的阻尼比为官方值 0.9(过冲仅约 0.15%),实际用不到这个上限。
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
     * spatial spring:用于位置、尺寸、形状等"在屏幕上动"的属性。
     * 数值取官方 motionSpringDefaultSpatial(damping 0.9 / stiffness 700)——
     * MDC 原文:"小件用 fast spring,全屏用 slow spring,介于两者之间的用 default",
     * 列表项正属此类。
     *
     * 官方的 effects spring(damping 1)不在此实现:透明度与颜色在本项目走 tween,
     * 官方也正是这么配的(Effects springs 用于"不应过冲"的属性,如 alpha)。
     */
    fun <T> spatialSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.9f,
        stiffness = 700f,
    )

    /**
     * 「已完成」标题入场的上滑距离(dp):标题是纯文字,不适合用缩放做进入
     * (缩放要项占满整行才看得见,而整行大的项参与 LazyColumn 的图层动画
     * 会放大深色下的暗闪),故改用位移给进入感。
     */
    const val TitleRiseDp = 8
}

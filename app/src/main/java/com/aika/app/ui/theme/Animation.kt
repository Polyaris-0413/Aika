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
     * 「已完成」标题入场的上滑距离(dp):标题是纯文字,不适合用缩放做进入
     * (缩放要项占满整行才看得见,而整行大的项参与 LazyColumn 的图层动画
     * 会放大深色下的暗闪),故改用位移给进入感。
     */
    const val TitleRiseDp = 8
}

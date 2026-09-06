package com.aika.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一条待办。列表顺序由 DAO 查询决定:未完成区在前按 pendingAt,已完成区在后按 completedAt,
 * 配合稳定 id 让 LazyColumn 在完成/恢复时播放跨区滑动动画。
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val completed: Boolean = false,
    /** 最近进入未完成区的时间戳:创建时写入,恢复完成项时刷新,未完成区按此排序 */
    val pendingAt: Long,
    /** 完成时间戳,未完成为 null */
    val completedAt: Long? = null,
)

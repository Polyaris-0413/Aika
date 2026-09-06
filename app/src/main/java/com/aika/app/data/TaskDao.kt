package com.aika.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    /** 未完成区在前按 pendingAt(恢复的项沉到区尾),已完成区在后按 completedAt,与分区显示一致 */
    @Query(
        "SELECT * FROM tasks ORDER BY completed ASC, " +
            "CASE WHEN completed = 1 THEN completedAt ELSE pendingAt END ASC",
    )
    fun observeAll(): Flow<List<Task>>

    @Insert
    suspend fun insert(task: Task)

    /** 完成时写 completedAt,恢复时刷新 pendingAt:分区排序键在状态切换处维护 */
    @Query(
        "UPDATE tasks SET completed = :completed, pendingAt = :pendingAt, " +
            "completedAt = :completedAt WHERE id = :id",
    )
    suspend fun setCompleted(id: Long, completed: Boolean, pendingAt: Long, completedAt: Long?)
}

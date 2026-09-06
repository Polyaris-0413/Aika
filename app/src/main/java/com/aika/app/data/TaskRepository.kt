package com.aika.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/** 待办仓库:任务数据入口,基于 Room */
class TaskRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).taskDao()

    val tasks: Flow<List<Task>> = dao.observeAll()

    suspend fun addTask(title: String) =
        dao.insert(Task(title = title, pendingAt = System.currentTimeMillis()))

    /** 完成沉底(completedAt 记录完成时间);恢复回未完成区末尾(刷新 pendingAt) */
    suspend fun toggleTask(task: Task) {
        if (task.completed) {
            dao.setCompleted(task.id, false, System.currentTimeMillis(), null)
        } else {
            dao.setCompleted(task.id, true, task.pendingAt, System.currentTimeMillis())
        }
    }
}

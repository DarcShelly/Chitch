package com.chitch.app.domain

import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.entity.TaskStatus
import java.time.LocalTime

/**
 * Shared ordering for every task list view: active/open tasks first (soonest start or due time
 * first), then active tasks already done for their current period, then archived tasks last.
 */
object TaskSorting {
    fun listComparator(): Comparator<TaskEntity> = compareBy(
        { it.status != TaskStatus.ACTIVE },
        { it.isCompletedNow() },
        { it.startTime ?: it.dueTime ?: LocalTime.MAX }
    )
}

fun List<TaskEntity>.sortedForDisplay(): List<TaskEntity> = sortedWith(TaskSorting.listComparator())

/** Same ordering as [sortedForDisplay], for lists of items that wrap a [TaskEntity] rather than being one. */
fun <T> List<T>.sortedForDisplayBy(selector: (T) -> TaskEntity): List<T> {
    val comparator = TaskSorting.listComparator()
    return sortedWith(Comparator { a, b -> comparator.compare(selector(a), selector(b)) })
}

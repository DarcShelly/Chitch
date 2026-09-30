package com.chitch.app.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitch.app.data.entity.RoutineEntity
import com.chitch.app.data.entity.RoutineStatus
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.entity.TaskStatus
import com.chitch.app.data.repository.RoutineRepository
import com.chitch.app.data.repository.TaskRepository
import com.chitch.app.domain.isCompletedNow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime

data class RoutineListItem(
    val routine: RoutineEntity,
    val taskCount: Int,
    val nextTask: TaskEntity?
)

class RoutinesListViewModel(
    routineRepository: RoutineRepository,
    taskRepository: TaskRepository
) : ViewModel() {

    val items: StateFlow<List<RoutineListItem>> = combine(
        routineRepository.observeAll(),
        taskRepository.observeAllTasksRaw()
    ) { routines, allTasks ->
        routines.map { routine ->
            val routineTasks = allTasks.filter { it.routineId == routine.id && it.status == TaskStatus.ACTIVE }
            val nextTask = routineTasks
                .filterNot { it.isCompletedNow() }
                .sortedBy { it.startTime ?: it.dueTime ?: LocalTime.MAX }
                .firstOrNull()
            RoutineListItem(routine, routineTasks.size, nextTask)
        }.sortedWith(
            compareBy(
                { it.routine.status != RoutineStatus.ACTIVE },
                { it.nextTask?.let { t -> t.startTime ?: t.dueTime } ?: LocalTime.MAX }
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

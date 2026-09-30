package com.chitch.app.data.repository

import com.chitch.app.data.AppDatabase
import com.chitch.app.data.entity.DailyHabitCheckEntity
import com.chitch.app.data.entity.DailyReviewEntity
import com.chitch.app.data.entity.HabitEntity
import com.chitch.app.data.entity.MoodRating
import com.chitch.app.domain.DailySuggestionEngine
import com.chitch.app.domain.DailySuggestionInput
import com.chitch.app.domain.DailySummaryAggregator
import com.chitch.app.domain.HabitCompletionEvaluator
import com.chitch.app.domain.PeriodSummary
import com.chitch.app.domain.isCompletedNow
import com.chitch.app.widget.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

enum class SummaryRange { YESTERDAY, WEEK, MONTH }

data class SubmitReviewResult(
    val completedHabits: List<HabitEntity>,
    val suggestions: List<String>
)

private const val RETENTION_DAYS = 90L

/**
 * Orchestrates the daily review: snapshots today's task/habit counts, evaluates every active
 * habit's rolling streak (see [HabitCompletionEvaluator]) and completes any that just qualified,
 * generates suggestions, and prunes data past the 3-month retention window.
 */
class DailyReviewRepository(
    private val db: AppDatabase,
    private val habitRepository: HabitRepository,
    private val userRepository: UserRepository,
    private val widgetRefresher: WidgetRefresher
) {
    private val reviewDao = db.dailyReviewDao()
    private val habitCheckDao = db.dailyHabitCheckDao()
    private val taskDao = db.taskDao()
    private val habitDao = db.habitDao()

    fun observeReview(date: LocalDate): Flow<DailyReviewEntity?> = reviewDao.observeByDate(date)

    suspend fun getReview(date: LocalDate): DailyReviewEntity? = reviewDao.getByDate(date)

    suspend fun getCheckedHabitIds(date: LocalDate): Set<Long> = habitCheckDao.getCheckedHabitIds(date).toSet()

    suspend fun submitReview(
        date: LocalDate,
        mood: MoodRating,
        notesForTomorrow: String?,
        thingsLiked: String?,
        thingsMissed: String?,
        checkedHabitIds: Set<Long>
    ): SubmitReviewResult {
        val activeHabits = habitDao.getActive()
        val activeTasks = taskDao.getAllActiveTasks()
        val tasksDone = activeTasks.count { it.isCompletedNow() }
        val tasksMissed = activeTasks.size - tasksDone
        val habitsChecked = checkedHabitIds.size
        val habitsMissed = (activeHabits.size - habitsChecked).coerceAtLeast(0)

        val now = Instant.now()
        reviewDao.upsert(
            DailyReviewEntity(
                date = date,
                mood = mood,
                notesForTomorrow = notesForTomorrow?.ifBlank { null },
                thingsLiked = thingsLiked?.ifBlank { null },
                thingsMissed = thingsMissed?.ifBlank { null },
                tasksDoneCount = tasksDone,
                tasksMissedCount = tasksMissed,
                habitsCheckedCount = habitsChecked,
                habitsMissedCount = habitsMissed,
                createdAt = now,
                updatedAt = now
            )
        )

        habitCheckDao.deleteForDate(date)
        if (checkedHabitIds.isNotEmpty()) {
            habitCheckDao.insertAll(checkedHabitIds.map { DailyHabitCheckEntity(date, it) })
        }

        val maxMissedDays = userRepository.get().maxMissedDaysForHabitCompletion
        val completedHabits = mutableListOf<HabitEntity>()
        activeHabits.forEach { habit ->
            val doneDates = habitCheckDao.getDoneDates(habit.id, habit.streakWindowStartDate, date).toSet()
            val result = HabitCompletionEvaluator.evaluate(
                windowStartDate = habit.streakWindowStartDate,
                today = date,
                doneDates = doneDates,
                maxMissedDays = maxMissedDays
            )
            if (result.eligible) {
                habitRepository.completeHabit(habit.id)
                completedHabits += habit
            }
        }

        purgeOldData(date)
        widgetRefresher.refresh()

        val suggestions = DailySuggestionEngine.suggest(
            DailySuggestionInput(
                date = date,
                mood = mood,
                tasksMissedCount = tasksMissed,
                habitsMissedCount = habitsMissed,
                hasThingsMissedNote = !thingsMissed.isNullOrBlank()
            )
        )

        return SubmitReviewResult(completedHabits, suggestions)
    }

    suspend fun getSummary(range: SummaryRange, today: LocalDate = LocalDate.now()): PeriodSummary {
        val (start, end) = when (range) {
            SummaryRange.YESTERDAY -> today.minusDays(1) to today.minusDays(1)
            SummaryRange.WEEK -> today.minusDays(6) to today
            SummaryRange.MONTH -> today.minusDays(29) to today
        }
        return DailySummaryAggregator.aggregate(reviewDao.getBetween(start, end))
    }

    suspend fun purgeOldData(today: LocalDate = LocalDate.now()) {
        val cutoff = today.minusDays(RETENTION_DAYS)
        reviewDao.deleteOlderThan(cutoff)
        habitCheckDao.deleteOlderThan(cutoff)
    }
}

package com.chitch.app.data

import androidx.room.TypeConverter
import com.chitch.app.data.entity.HabitStatus
import com.chitch.app.data.entity.MoodRating
import com.chitch.app.data.entity.RepeatType
import com.chitch.app.data.entity.RoutineStatus
import com.chitch.app.data.entity.TaskStatus
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localTimeToString(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun repeatTypeToString(value: RepeatType): String = value.name

    @TypeConverter
    fun stringToRepeatType(value: String): RepeatType = RepeatType.valueOf(value)

    @TypeConverter
    fun taskStatusToString(value: TaskStatus): String = value.name

    @TypeConverter
    fun stringToTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun routineStatusToString(value: RoutineStatus): String = value.name

    @TypeConverter
    fun stringToRoutineStatus(value: String): RoutineStatus = RoutineStatus.valueOf(value)

    @TypeConverter
    fun habitStatusToString(value: HabitStatus): String = value.name

    @TypeConverter
    fun stringToHabitStatus(value: String): HabitStatus = HabitStatus.valueOf(value)

    @TypeConverter
    fun moodRatingToString(value: MoodRating): String = value.name

    @TypeConverter
    fun stringToMoodRating(value: String): MoodRating = MoodRating.valueOf(value)

    /** Unit separator (0x1F) joins file URIs — never appears in a content:// URI string. */
    @TypeConverter
    fun stringListToString(value: List<String>): String = value.joinToString(FILE_URI_DELIMITER)

    @TypeConverter
    fun stringToStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(FILE_URI_DELIMITER)

    @TypeConverter
    fun daysOfWeekToString(value: Set<DayOfWeek>): String = value.joinToString(FILE_URI_DELIMITER) { it.name }

    @TypeConverter
    fun stringToDaysOfWeek(value: String): Set<DayOfWeek> =
        if (value.isEmpty()) emptySet() else value.split(FILE_URI_DELIMITER).map(DayOfWeek::valueOf).toSet()

    companion object {
        private const val FILE_URI_DELIMITER = ""
    }
}

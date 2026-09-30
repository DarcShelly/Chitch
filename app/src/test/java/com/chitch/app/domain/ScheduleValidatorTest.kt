package com.chitch.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ScheduleValidatorTest {

    private fun window(startHour: Int, startMin: Int, endHour: Int, endMin: Int) =
        TimeWindow(LocalTime.of(startHour, startMin), LocalTime.of(endHour, endMin))

    @Test
    fun `overlapping windows collide`() {
        val a = window(9, 0, 10, 0)
        val b = window(9, 30, 10, 30)
        assertTrue(a.overlaps(b))
        assertTrue(b.overlaps(a))
    }

    @Test
    fun `back-to-back windows do not collide`() {
        val a = window(9, 0, 10, 0)
        val b = window(10, 0, 11, 0)
        assertTrue(!a.overlaps(b))
    }

    @Test
    fun `separate windows do not collide`() {
        val a = window(9, 0, 10, 0)
        val b = window(11, 0, 12, 0)
        assertTrue(!a.overlaps(b))
    }

    @Test
    fun `findCollision returns the colliding window`() {
        val candidate = window(9, 30, 10, 30)
        val existing = listOf(window(6, 0, 7, 0), window(9, 0, 10, 0))
        val result = ScheduleValidator.findCollision(candidate, existing)
        assertEquals(window(9, 0, 10, 0), result)
    }

    @Test
    fun `findCollision returns null when nothing overlaps`() {
        val candidate = window(9, 30, 10, 30)
        val existing = listOf(window(6, 0, 7, 0), window(11, 0, 12, 0))
        assertNull(ScheduleValidator.findCollision(candidate, existing))
    }

    @Test
    fun `findInternalCollision detects overlap within a routine`() {
        val windows = listOf(window(7, 0, 8, 0), window(7, 30, 9, 0), window(9, 0, 10, 0))
        val collision = ScheduleValidator.findInternalCollision(windows)
        assertNotNull(collision)
        assertEquals(window(7, 0, 8, 0), collision!!.first)
        assertEquals(window(7, 30, 9, 0), collision.second)
    }

    @Test
    fun `findInternalCollision is null for a non-overlapping routine`() {
        val windows = listOf(window(7, 0, 8, 0), window(8, 0, 9, 0), window(9, 0, 10, 0))
        assertNull(ScheduleValidator.findInternalCollision(windows))
    }

    @Test
    fun `routine within free hours passes`() {
        // 8 sleep + 8 work = 8 free hours. A 4-hour routine span fits.
        val windows = listOf(window(19, 0, 21, 0), window(21, 0, 23, 0))
        val result = ScheduleValidator.validateRoutineHours(
            windows,
            sleepHours = 8,
            workStart = LocalTime.of(9, 0),
            workEnd = LocalTime.of(17, 0)
        )
        assertEquals(HoursCheckResult.Ok, result)
    }

    @Test
    fun `routine exceeding free hours is rejected`() {
        // 8 sleep + 8 work = 8 free hours. This routine spans 10 hours (8am-6pm).
        val windows = listOf(window(8, 0, 12, 0), window(12, 0, 18, 0))
        val result = ScheduleValidator.validateRoutineHours(
            windows,
            sleepHours = 8,
            workStart = LocalTime.of(9, 0),
            workEnd = LocalTime.of(17, 0)
        )
        assertTrue(result is HoursCheckResult.Exceeded)
        val exceeded = result as HoursCheckResult.Exceeded
        assertEquals(10.0, exceeded.spanHours, 0.001)
        assertEquals(8.0, exceeded.maxAllowedHours, 0.001)
    }

    @Test
    fun `routine hours with no working hours set only subtracts sleep`() {
        val windows = listOf(window(0, 0, 15, 0)) // 15 hour span
        val result = ScheduleValidator.validateRoutineHours(
            windows,
            sleepHours = 8,
            workStart = null,
            workEnd = null
        )
        // 24 - 8 sleep = 16 free hours, 15 hour span fits
        assertEquals(HoursCheckResult.Ok, result)
    }

    @Test
    fun `empty windows always pass hours check`() {
        assertEquals(
            HoursCheckResult.Ok,
            ScheduleValidator.validateRoutineHours(emptyList(), 8, null, null)
        )
    }
}

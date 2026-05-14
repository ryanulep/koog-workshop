package com.jetbrains.koog.workshop.agents.homeservices

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeServicesFindSlotToolsTest {

    private val schedule = HomeServicesSchedule()
    private val findTools = HomeServicesFindSlotTools(schedule)

    // Grab future dates that definitely have free slots (later days are emptier)
    private val futureDates: List<LocalDate> =
        schedule.slots.map { it.date }.distinct().sorted().takeLast(3)

    @Test
    fun `returns only slots matching the requested service type`() {
        val result = findTools.getAvailableSlots(ServiceType.PLUMBING, limit = 50)
        // PLUMBING is handled by SHK only
        assertFalse(result.contains("electrician"), "Plumbing results should not contain electrician slots")
        assertFalse(result.contains("handyman"), "Plumbing results should not contain handyman slots")
    }

    @Test
    fun `HANDYMAN returns slots from both handyman specialists`() {
        val result = findTools.getAvailableSlots(ServiceType.HANDYMAN, limit = 50)
        assertContains(result, "handyman_1")
        assertContains(result, "handyman_2")
    }

    @Test
    fun `HVAC returns SHK specialist slots`() {
        val result = findTools.getAvailableSlots(ServiceType.HVAC, limit = 50)
        assertContains(result, "shk")
    }

    @Test
    fun `respects limit parameter`() {
        val result = findTools.getAvailableSlots(ServiceType.PLUMBING, limit = 2)
        val slotLines = result.lines().filter { it.trimStart().startsWith("- svc_") }
        assertTrue(slotLines.size <= 2, "Expected at most 2 slot lines but got ${slotLines.size}")
    }

    @Test
    fun `default limit is 5`() {
        val result = findTools.getAvailableSlots(ServiceType.HANDYMAN)
        val slotLines = result.lines().filter { it.trimStart().startsWith("- svc_") }
        assertTrue(slotLines.size <= 5, "Default limit should cap at 5, got ${slotLines.size}")
    }

    @Test
    fun `shows overflow message when more slots exist beyond limit`() {
        val result = findTools.getAvailableSlots(ServiceType.HANDYMAN, limit = 1)
        if (result.contains("Available slots")) {
            assertContains(result, "more slots available")
        }
    }

    @Test
    fun `respects startDate parameter`() {
        val laterDate = futureDates.last()
        val result = findTools.getAvailableSlots(
            ServiceType.HANDYMAN,
            limit = 50,
            startDate = laterDate.toString(),
        )
        if (result.contains("Available slots")) {
            val slotLines = result.lines().filter { it.trimStart().startsWith("- svc_") }
            for (line in slotLines) {
                assertContains(line, laterDate.toString(), message = "All slots should be on or after $laterDate")
            }
        }
    }

    @Test
    fun `returns error for invalid startDate format`() {
        val result = findTools.getAvailableSlots(ServiceType.PLUMBING, startDate = "April 28")
        assertContains(result, "Error")
        assertContains(result, "yyyy-MM-dd")
    }

    @Test
    fun `returns no-slots message when service has no availability`() {
        // Book every free SHK slot to force the no-slots path for HVAC
        val schedule = HomeServicesSchedule()
        val shkSlots = schedule.slots.filter { it.specialistType == SpecialistType.SHK }
        for (slot in shkSlots) {
            if (schedule.isFree(slot.id)) {
                schedule.bookAnAppointment(slot.id, Booking(customerName = "Test", address = "Test address"))
            }
        }
        val findTools = HomeServicesFindSlotTools(schedule)
        val result = findTools.getAvailableSlots(ServiceType.HVAC)
        assertContains(result, "No available slots found for HVAC")
    }

    @Test
    fun `output does not contain specialist type names`() {
        val result = findTools.getAvailableSlots(ServiceType.PLUMBING, limit = 50)
        if (result.contains("Available slots")) {
            val slotLines = result.lines().filter { it.trimStart().startsWith("- svc_") }
            for (line in slotLines) {
                // The slot ID contains "shk" which is fine — but the line should not
                // separately list the specialist type like "SHK" or "ELECTRICIAN"
                val afterId = line.substringAfter(",")
                assertFalse(afterId.contains("SHK"), "Output should not expose specialist type names")
                assertFalse(afterId.contains("ELECTRICIAN"), "Output should not expose specialist type names")
            }
        }
    }

    @Test
    fun `each returned slot line contains date and time window`() {
        val result = findTools.getAvailableSlots(ServiceType.ELECTRICAL, limit = 3)
        if (result.contains("Available slots")) {
            val slotLines = result.lines().filter { it.trimStart().startsWith("- svc_") }
            for (line in slotLines) {
                assertTrue(
                    line.contains("Morning") || line.contains("Early afternoon") || line.contains("Late afternoon"),
                    "Slot line should contain a time window label: $line"
                )
                assertTrue(line.contains(Regex("""\d{4}-\d{2}-\d{2}""")), "Slot line should contain a date: $line")
            }
        }
    }
}

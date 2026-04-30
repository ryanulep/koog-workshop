package com.jetbrains.example.koog.compose.agents.homeservices

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeServicesBookToolsTest {

    private fun freshScheduleAndTools(): Triple<HomeServicesSchedule, HomeServicesFindTools, HomeServicesBookTools> {
        val schedule = HomeServicesSchedule()
        return Triple(schedule, HomeServicesFindTools(schedule), HomeServicesBookTools(schedule))
    }

    private fun firstFreeSlotId(schedule: HomeServicesSchedule, specialist: SpecialistType): String =
        schedule.slots.first { schedule.isFree(it.id) && it.specialistType == specialist }.id

    @Test
    fun `successfully books a free SHK slot for plumbing`() {
        val (schedule, _, bookTools) = freshScheduleAndTools()
        val slotId = firstFreeSlotId(schedule, SpecialistType.SHK)

        val result = bookTools.scheduleAppointment(
            customerName = "Jane Doe",
            serviceType = ServiceType.PLUMBING,
            slotId = slotId,
            address = "123 Main St",
            issueDescription = "Broken pipe",
        )

        assertContains(result, "Appointment confirmed!")
        assertContains(result, "Jane Doe")
        assertContains(result, "123 Main St")
        assertContains(result, "PLUMBING")
        assertContains(result, slotId)
    }

    @Test
    fun `booked slot is recorded in bookings`() {
        val (schedule, _, bookTools) = freshScheduleAndTools()
        val slotId = firstFreeSlotId(schedule, SpecialistType.SHK)

        bookTools.scheduleAppointment(
            customerName = "Jane Doe",
            serviceType = ServiceType.PLUMBING,
            slotId = slotId,
            address = "123 Main St",
            issueDescription = "Broken pipe",
        )

        assertFalse(schedule.isFree(slotId))
        val booking = schedule.getBooking(slotId)!!
        assertEquals("Jane Doe", booking.customerName)
        assertEquals("123 Main St", booking.address)
    }

    @Test
    fun `returns error for unknown slot ID`() {
        val (_, _, bookTools) = freshScheduleAndTools()
        val result = bookTools.scheduleAppointment(
            customerName = "Jane Doe",
            serviceType = ServiceType.PLUMBING,
            slotId = "svc_fake_9999_1",
            address = "123 Main St",
            issueDescription = "Broken pipe",
        )
        assertContains(result, "Error")
        assertContains(result, "Unknown slot ID")
    }

    @Test
    fun `returns error when slot is already booked`() {
        val (schedule, _, bookTools) = freshScheduleAndTools()
        val bookedSlotId = schedule.slots.first { !schedule.isFree(it.id) }.id

        val result = bookTools.scheduleAppointment(
            customerName = "Jane Doe",
            serviceType = ServiceType.PLUMBING,
            slotId = bookedSlotId,
            address = "123 Main St",
            issueDescription = "Broken pipe",
        )
        assertContains(result, "Error")
        assertContains(result, "already booked")
    }

    @Test
    fun `returns error for incompatible service type`() {
        val (schedule, _, bookTools) = freshScheduleAndTools()
        // Try to book ELECTRICAL on a SHK slot (SHK supports PLUMBING and HVAC only)
        val shkSlotId = firstFreeSlotId(schedule, SpecialistType.SHK)

        val result = bookTools.scheduleAppointment(
            customerName = "Jane Doe",
            serviceType = ServiceType.ELECTRICAL,
            slotId = shkSlotId,
            address = "123 Main St",
            issueDescription = "Electrical wiring broke",
        )
        assertContains(result, "Error")
        assertContains(result, "cannot handle")
    }

    @Test
    fun `SHK slot accepts HVAC service`() {
        val (schedule, _, bookTools) = freshScheduleAndTools()
        val slotId = firstFreeSlotId(schedule, SpecialistType.SHK)

        val result = bookTools.scheduleAppointment(
            customerName = "Jane Doe",
            serviceType = ServiceType.HVAC,
            slotId = slotId,
            address = "456 Oak Ave",
            issueDescription = "Air conditioner malfunction",
        )
        assertContains(result, "Appointment confirmed!")
        assertContains(result, "HVAC")
    }

    @Test
    fun `notes are stored when provided`() {
        val (schedule, _, bookTools) = freshScheduleAndTools()
        val slotId = firstFreeSlotId(schedule, SpecialistType.ELECTRICIAN)

        val result = bookTools.scheduleAppointment(
            customerName = "Jane Doe",
            serviceType = ServiceType.ELECTRICAL,
            slotId = slotId,
            address = "789 Elm St",
            notes = "Gate code: 1234",
            issueDescription = "Electrical wiring broke",
        )

        assertContains(result, "Gate code: 1234")
        assertEquals("Gate code: 1234", schedule.getBooking(slotId)!!.notes)
    }

    @Test
    fun `blank notes are stored as null`() {
        val (schedule, _, bookTools) = freshScheduleAndTools()
        val slotId = firstFreeSlotId(schedule, SpecialistType.ELECTRICIAN)

        bookTools.scheduleAppointment(
            customerName = "Jane Doe",
            serviceType = ServiceType.ELECTRICAL,
            slotId = slotId,
            address = "789 Elm St",
            notes = "  ",
            issueDescription = "Electrical wiring broke",
        )

        assertNull(schedule.getBooking(slotId)!!.notes)
    }

    @Test
    fun `notes default to empty when omitted`() {
        val (schedule, _, bookTools) = freshScheduleAndTools()
        val slotId = firstFreeSlotId(schedule, SpecialistType.ELECTRICIAN)

        bookTools.scheduleAppointment(
            customerName = "Jane Doe",
            serviceType = ServiceType.ELECTRICAL,
            slotId = slotId,
            address = "789 Elm St",
            issueDescription = "Electrical wiring broke",
        )

        assertNull(schedule.getBooking(slotId)!!.notes)
    }

    @Test
    fun `booking same slot twice returns already-booked error`() {
        val (schedule, _, bookTools) = freshScheduleAndTools()
        val slotId = firstFreeSlotId(schedule, SpecialistType.HANDYMAN_1)

        bookTools.scheduleAppointment(
            customerName = "First",
            serviceType = ServiceType.HANDYMAN,
            slotId = slotId,
            address = "111 Pine St",
            issueDescription = "Broken table",
        )

        val result = bookTools.scheduleAppointment(
            customerName = "Second",
            serviceType = ServiceType.HANDYMAN,
            slotId = slotId,
            address = "222 Birch St",
            issueDescription = "Broken chair",
        )
        assertContains(result, "Error")
        assertContains(result, "already booked")
    }

    @Test
    fun `confirmation includes date and time window`() {
        val (schedule, _, bookTools) = freshScheduleAndTools()
        val slotId = firstFreeSlotId(schedule, SpecialistType.SHK)

        val result = bookTools.scheduleAppointment(
            customerName = "Jane Doe",
            serviceType = ServiceType.PLUMBING,
            slotId = slotId,
            address = "123 Main St",
            issueDescription = "Broken pipe",
        )

        assertTrue(
            result.contains("Morning") || result.contains("Early afternoon") || result.contains("Late afternoon"),
            "Confirmation should include time window"
        )
        assertTrue(result.contains(Regex("""\d{4}-\d{2}-\d{2}""")), "Confirmation should include date")
    }

    @Test
    fun `booked slot no longer appears in getAvailableSlots`() {
        val (schedule, findTools, bookTools) = freshScheduleAndTools()
        val slotId = firstFreeSlotId(schedule, SpecialistType.SHK)

        bookTools.scheduleAppointment(
            customerName = "Jane Doe",
            serviceType = ServiceType.PLUMBING,
            slotId = slotId,
            address = "123 Main St",
            issueDescription = "Broken pipe",
        )

        val result = findTools.getAvailableSlots(ServiceType.PLUMBING, limit = 100)
        val slotLines = result.lines().filter { it.contains(slotId) }
        assertTrue(slotLines.isEmpty(), "Booked slot $slotId should not appear in available slots")
    }
}

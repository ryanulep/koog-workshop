package com.jetbrains.example.koog.compose.agents.homeservices

import java.time.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeServicesScheduleTest {

    private val schedule = HomeServicesSchedule()

    @Test
    fun `generates exactly 10 business days`() {
        val distinctDates = schedule.slots.map { it.date }.distinct()
        assertEquals(10, distinctDates.size)
    }

    @Test
    fun `no slots on weekends`() {
        val weekendSlots = schedule.slots.filter {
            it.date.dayOfWeek == DayOfWeek.SATURDAY || it.date.dayOfWeek == DayOfWeek.SUNDAY
        }
        assertTrue(weekendSlots.isEmpty(), "Expected no weekend slots but found ${weekendSlots.size}")
    }

    @Test
    fun `every specialist has slots on every business day`() {
        val dates = schedule.slots.map { it.date }.distinct()
        for (specialist in SpecialistType.entries) {
            for (date in dates) {
                val count = schedule.slots.count { it.specialistType == specialist && it.date == date }
                assertEquals(6, count, "$specialist should have 6 slots on $date")
            }
        }
    }

    @Test
    fun `each business day has two slots per time window per specialist`() {
        val dates = schedule.slots.map { it.date }.distinct()
        for (specialist in SpecialistType.entries) {
            for (date in dates) {
                for (window in TimeWindow.entries) {
                    val count = schedule.slots.count {
                        it.specialistType == specialist && it.date == date && it.timeWindow == window
                    }
                    assertEquals(2, count, "$specialist on $date should have 2 $window slots")
                }
            }
        }
    }

    @Test
    fun `later days have more free slots than earlier days`() {
        val dates = schedule.slots.map { it.date }.distinct().sorted()
        val firstDayFree = schedule.slots.count { it.date == dates.first() && schedule.isFree(it.id) }
        val lastDayFree = schedule.slots.count { it.date == dates.last() && schedule.isFree(it.id) }
        assertTrue(lastDayFree > firstDayFree, "Last day ($lastDayFree free) should have more free slots than first day ($firstDayFree free)")
    }

    @Test
    fun `slot IDs follow expected format`() {
        val idPattern = Regex("""svc_(shk|electrician|handyman_1|handyman_2)_\d{4}_[1-6]""")
        for (slot in schedule.slots) {
            assertTrue(idPattern.matches(slot.id), "Slot ID '${slot.id}' does not match expected format")
        }
    }

    @Test
    fun `all slot IDs are unique`() {
        val ids = schedule.slots.map { it.id }
        assertEquals(ids.size, ids.distinct().size, "Duplicate slot IDs found")
    }

    @Test
    fun `startDate is today`() {
        assertEquals(schedule.today, schedule.startDate)
    }

    @Test
    fun `endDate is after startDate`() {
        assertTrue(schedule.endDate > schedule.startDate)
    }

    @Test
    fun `booked slots have customer names`() {
        for (slot in schedule.slots) {
            val booking = schedule.getBooking(slot.id) ?: continue
            assertTrue(booking.customerName.isNotBlank(), "All bookings should have a customer name")
        }
    }

    @Test
    fun `all booked slot IDs reference existing slots`() {
        val slotIds = schedule.slots.map { it.id }.toSet()
        for (slot in schedule.slots) {
            if (!schedule.isFree(slot.id)) {
                assertTrue(slot.id in slotIds, "Booking ID '${slot.id}' does not reference an existing slot")
            }
        }
    }

    @Test
    fun `isFree returns true for unbooked slots`() {
        val freeSlot = schedule.slots.first { schedule.isFree(it.id) }
        assertTrue(schedule.isFree(freeSlot.id))
    }

    @Test
    fun `isFree returns false for booked slots`() {
        val bookedSlotId = schedule.slots.first { !schedule.isFree(it.id) }.id
        assertFalse(schedule.isFree(bookedSlotId))
    }

    @Test
    fun `bookAnAppointment makes slot non-free`() {
        val freeSlotId = schedule.slots.first { schedule.isFree(it.id) }.id
        schedule.bookAnAppointment(freeSlotId, Booking(customerName = "Test", address = "Test St"))
        assertFalse(schedule.isFree(freeSlotId))
    }

    @Test
    fun `getBooking returns booking after bookAnAppointment`() {
        val freeSlotId = schedule.slots.first { schedule.isFree(it.id) }.id
        schedule.bookAnAppointment(freeSlotId, Booking(customerName = "Test", address = "Test St", notes = "Ring bell"))
        val booking = schedule.getBooking(freeSlotId)
        assertNotNull(booking)
        assertEquals("Test", booking.customerName)
        assertEquals("Test St", booking.address)
        assertEquals("Ring bell", booking.notes)
    }

    @Test
    fun `getBooking returns null for free slot`() {
        val freeSlotId = schedule.slots.first { schedule.isFree(it.id) }.id
        assertNull(schedule.getBooking(freeSlotId))
    }

    @Test
    fun `bookAnAppointment returns false for unknown slot ID`() {
        val result = schedule.bookAnAppointment("svc_fake_9999_1", Booking(customerName = "Test", address = "Test St"))
        assertFalse(result)
    }

    @Test
    fun `bookAnAppointment returns false for already booked slot`() {
        val bookedSlotId = schedule.slots.first { !schedule.isFree(it.id) }.id
        val result = schedule.bookAnAppointment(bookedSlotId, Booking(customerName = "Test", address = "Test St"))
        assertFalse(result)
    }
}

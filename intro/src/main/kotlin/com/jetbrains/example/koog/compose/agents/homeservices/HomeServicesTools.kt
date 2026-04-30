package com.jetbrains.example.koog.compose.agents.homeservices

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.jetbrains.example.koog.compose.agents.homeservices.TimeWindow.EARLY_AFTERNOON
import com.jetbrains.example.koog.compose.agents.homeservices.TimeWindow.LATE_AFTERNOON
import com.jetbrains.example.koog.compose.agents.homeservices.TimeWindow.MORNING
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class ServiceType {
    PLUMBING, ELECTRICAL, HVAC, HANDYMAN
}

enum class SpecialistType(val idPart: String, val supportedServices: Set<ServiceType>) {
    SHK("shk", setOf(ServiceType.PLUMBING, ServiceType.HVAC)),
    ELECTRICIAN("electrician", setOf(ServiceType.ELECTRICAL)),
    HANDYMAN_1("handyman_1", setOf(ServiceType.HANDYMAN)),
    HANDYMAN_2("handyman_2", setOf(ServiceType.HANDYMAN)),
}

enum class TimeWindow(val label: String, val hours: String, val startHour: Int) {
    MORNING("Morning", "9:00-12:00", 9),
    EARLY_AFTERNOON("Early afternoon", "12:00-15:00", 12),
    LATE_AFTERNOON("Late afternoon", "15:00-18:00", 15),
}

private val DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

data class Slot(
    val id: String,
    val specialistType: SpecialistType,
    val date: LocalDate,
    val timeWindow: TimeWindow,
)

data class Booking(
    val customerName: String,
    val address: String,
    val notes: String? = null,
)

/**
 * Shared schedule data used by both [HomeServicesFindTools] and [HomeServicesBookTools].
 *
 * [slots] is immutable — it describes every time slot in the schedule.
 * [bookings] tracks which slots have been booked and their customer details.
 */
class HomeServicesSchedule {
    val today: LocalDate = LocalDate.now()
    val currentTime: LocalTime = LocalTime.now().withSecond(0).withNano(0)
    val slots: List<Slot> = generateSlots()
    private val bookings: MutableMap<String, Booking> = generateSampleBookings().toMutableMap()

    val startDate: LocalDate = slots.minOf { it.date }
    val endDate: LocalDate = slots.maxOf { it.date }

    fun isFree(slotId: String): Boolean = slotId !in bookings

    fun getBooking(slotId: String): Booking? = bookings[slotId]

    /**
     * Books a slot. Returns `true` on success, `false` if the slot ID
     * is unknown or already booked.
     */
    fun bookAnAppointment(slotId: String, booking: Booking): Boolean {
        if (slots.none { it.id == slotId }) return false
        if (!isFree(slotId)) return false
        bookings[slotId] = booking
        return true
    }

    private fun generateSlots(): List<Slot> =
        businessDates().flatMapIndexed { _, date ->
            SpecialistType.entries.flatMap { specialist ->
                (1..6).map { slotIndex ->
                    Slot(
                        id = "svc_${specialist.idPart}_${date.format(ID_DATE_FORMATTER)}_$slotIndex",
                        specialistType = specialist,
                        date = date,
                        timeWindow = slotTimeWindow(slotIndex),
                    )
                }
            }
        }

    private fun generateSampleBookings(): Map<String, Booking> {
        val sampleNames = listOf(
            "J. Smith", "M. Garcia", "A. Johnson", "R. Patel",
            "K. Williams", "T. Brown", "L. Davis", "S. Lee",
        )
        val bookedWindowsBySpecialist = bookedWindowsBySpecialist()
        val result = mutableMapOf<String, Booking>()

        businessDates().forEachIndexed { dayIndex, date ->
            SpecialistType.entries.forEach { specialist ->
                val bookedWindows = bookedWindowsBySpecialist
                    .getValue(specialist)
                    .getOrElse(dayIndex) { emptyList() }
                    .toMutableList()

                (1..6).forEach { slotIndex ->
                    val timeWindow = slotTimeWindow(slotIndex)
                    if (bookedWindows.remove(timeWindow)) {
                        val slotId = "svc_${specialist.idPart}_${date.format(ID_DATE_FORMATTER)}_$slotIndex"
                        val name = sampleNames[(dayIndex * 6 + slotIndex + specialist.ordinal) % sampleNames.size]
                        result[slotId] = Booking(customerName = name, address = "Sample address")
                    }
                }
            }
        }
        return result
    }

    private fun bookedWindowsBySpecialist(): Map<SpecialistType, List<List<TimeWindow>>> = mapOf(
        SpecialistType.SHK to listOf(
            listOf(MORNING, MORNING, EARLY_AFTERNOON, LATE_AFTERNOON),
            listOf(MORNING, EARLY_AFTERNOON, LATE_AFTERNOON, LATE_AFTERNOON),
            listOf(MORNING, MORNING, EARLY_AFTERNOON),
            listOf(MORNING, EARLY_AFTERNOON, EARLY_AFTERNOON),
            listOf(MORNING, LATE_AFTERNOON),
            listOf(EARLY_AFTERNOON, LATE_AFTERNOON),
            listOf(MORNING, EARLY_AFTERNOON),
            emptyList(),
            emptyList(),
            emptyList(),
        ),
        SpecialistType.ELECTRICIAN to listOf(
            listOf(MORNING, MORNING, EARLY_AFTERNOON, EARLY_AFTERNOON, LATE_AFTERNOON),
            listOf(MORNING, MORNING, EARLY_AFTERNOON, LATE_AFTERNOON),
            listOf(MORNING, EARLY_AFTERNOON, LATE_AFTERNOON),
            listOf(MORNING, EARLY_AFTERNOON, EARLY_AFTERNOON),
            listOf(MORNING, LATE_AFTERNOON),
            listOf(EARLY_AFTERNOON, LATE_AFTERNOON),
            listOf(LATE_AFTERNOON),
            emptyList(),
            emptyList(),
            emptyList(),
        ),
        SpecialistType.HANDYMAN_1 to listOf(
            listOf(MORNING, MORNING, EARLY_AFTERNOON, EARLY_AFTERNOON, LATE_AFTERNOON),
            listOf(MORNING, EARLY_AFTERNOON, EARLY_AFTERNOON, LATE_AFTERNOON),
            listOf(MORNING, MORNING, LATE_AFTERNOON),
            listOf(MORNING, EARLY_AFTERNOON, LATE_AFTERNOON),
            listOf(EARLY_AFTERNOON, EARLY_AFTERNOON),
            listOf(MORNING, LATE_AFTERNOON),
            listOf(LATE_AFTERNOON),
            emptyList(),
            emptyList(),
            emptyList(),
        ),
        SpecialistType.HANDYMAN_2 to listOf(
            listOf(MORNING, EARLY_AFTERNOON, EARLY_AFTERNOON, LATE_AFTERNOON, LATE_AFTERNOON),
            listOf(MORNING, MORNING, EARLY_AFTERNOON, LATE_AFTERNOON),
            listOf(MORNING, EARLY_AFTERNOON, LATE_AFTERNOON),
            listOf(MORNING, MORNING, EARLY_AFTERNOON),
            listOf(EARLY_AFTERNOON, LATE_AFTERNOON),
            listOf(MORNING, EARLY_AFTERNOON),
            listOf(MORNING),
            emptyList(),
            emptyList(),
            emptyList(),
        ),
    )

    private fun businessDates(): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var cursor = today
        while (dates.size < 10) {
            if (cursor.dayOfWeek != DayOfWeek.SATURDAY && cursor.dayOfWeek != DayOfWeek.SUNDAY) {
                dates += cursor
            }
            cursor = cursor.plusDays(1)
        }
        return dates
    }

    private fun slotTimeWindow(slotIndex: Int): TimeWindow = when (slotIndex) {
        1, 2 -> MORNING
        3, 4 -> EARLY_AFTERNOON
        5, 6 -> LATE_AFTERNOON
        else -> error("Unsupported slot index: $slotIndex")
    }

    companion object {
        private val ID_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMdd")
    }
}

/**
 * Tool set for searching available slots. Does NOT include booking.
 */
class HomeServicesFindTools(private val schedule: HomeServicesSchedule) : ToolSet {

    @Tool
    @LLMDescription(
        "Find available appointment slots for a home service. " +
            "Returns up to `limit` free slots for the requested service type, sorted by earliest date first."
    )
    fun getAvailableSlots(
        @LLMDescription("Service type: PLUMBING, ELECTRICAL, HVAC, or HANDYMAN")
        serviceType: ServiceType,
        @LLMDescription("Maximum number of slots to return (default 5)")
        limit: Int = 5,
        @LLMDescription("Earliest date to consider in yyyy-MM-dd format (default: today)")
        startDate: String = "",
    ): String {
        val compatibleSpecialists = SpecialistType.entries.filter { serviceType in it.supportedServices }.toSet()
        val now = schedule.currentTime
        val fromDate = if (startDate.isNotBlank()) {
            try {
                LocalDate.parse(startDate, DISPLAY_DATE_FORMATTER)
            } catch (_: Exception) {
                return "Error: Invalid startDate '$startDate'. Use yyyy-MM-dd format, e.g. 2026-05-01."
            }
        } else {
            schedule.today
        }

        val allFree = schedule.slots.filter { slot ->
            schedule.isFree(slot.id) &&
                slot.specialistType in compatibleSpecialists &&
                slot.date >= fromDate &&
                !(slot.date == schedule.today && now.hour >= slot.timeWindow.startHour)
        }

        val matches = allFree.take(limit)

        if (matches.isEmpty()) {
            // Relax the time-of-day filter to find the earliest future slot
            val nextAvailable = schedule.slots.firstOrNull { slot ->
                schedule.isFree(slot.id) &&
                    slot.specialistType in compatibleSpecialists &&
                    slot.date >= fromDate
            }
            return if (nextAvailable != null) {
                val day = nextAvailable.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
                "No available slots found for $serviceType starting from ${fromDate.format(DISPLAY_DATE_FORMATTER)}. " +
                    "The earliest available slot is ${nextAvailable.id} on " +
                    "$day ${nextAvailable.date.format(DISPLAY_DATE_FORMATTER)}, " +
                    "${nextAvailable.timeWindow.label} (${nextAvailable.timeWindow.hours})."
            } else {
                "No available slots found for $serviceType from ${schedule.startDate.format(DISPLAY_DATE_FORMATTER)} " +
                    "through ${schedule.endDate.format(DISPLAY_DATE_FORMATTER)}."
            }
        }

        return buildString {
            appendLine("Available slots (showing ${matches.size} of ${allFree.size} total):")
            for (slot in matches) {
                val day = slot.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
                appendLine(
                    "  - ${slot.id}, $day ${slot.date.format(DISPLAY_DATE_FORMATTER)}, " +
                        "${slot.timeWindow.label} (${slot.timeWindow.hours})"
                )
            }
            if (allFree.size > matches.size) {
                appendLine("  (${allFree.size - matches.size} more slots available — increase limit or adjust startDate to see them)")
            }
        }
    }
}

/**
 * Tool set for booking an appointment. Separated from [HomeServicesFindTools]
 * so the strategy can gate access: the LLM can only book after confirmation.
 */
class HomeServicesBookTools(private val schedule: HomeServicesSchedule) : ToolSet {

    @Tool
    @LLMDescription("Book a home service appointment into a specific slot. Fails if the slot is already BOOKED or if the specialist cannot handle the requested service.")
    fun scheduleAppointment(
        @LLMDescription("Customer's full name") customerName: String,
        @LLMDescription("Service type: PLUMBING, ELECTRICAL, HVAC, or HANDYMAN") serviceType: ServiceType,
        @LLMDescription("Slot ID from getAvailableSlots, e.g. svc_shk_0428_2") slotId: String,
        @LLMDescription("Service address") address: String,
        @LLMDescription("Brief description of the issue to be resolved") issueDescription: String,
        @LLMDescription("Access notes such as gate code, pet, parking, or buzzer instructions") notes: String = "",
    ): String {
        val slot = schedule.slots.find { it.id == slotId }
            ?: return "Error: Unknown slot ID '$slotId'."

        if (!schedule.isFree(slotId)) return "Error: Slot $slotId is already booked."

        if (serviceType !in slot.specialistType.supportedServices) {
            return "Error: Slot $slotId belongs to ${slot.specialistType}, which cannot handle $serviceType."
        }

        val booked = schedule.bookAnAppointment(slotId, Booking(
            customerName = customerName,
            address = address,
            notes = notes.ifBlank { null },
        ))
        if (!booked) return "Error: Slot $slotId could not be booked — it may have just been taken. Please pick another slot."

        val dayName = slot.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
        return buildString {
            appendLine("Appointment confirmed!")
            appendLine("  Service: $serviceType")
            appendLine("  Issue: $issueDescription")
            appendLine("  Specialist: ${slot.specialistType}")
            appendLine("  Customer: $customerName")
            appendLine("  Date: $dayName, ${slot.date.format(DISPLAY_DATE_FORMATTER)}")
            appendLine("  Window: ${slot.timeWindow.label} (${slot.timeWindow.hours})")
            appendLine("  Address: $address")
            if (notes.isNotBlank()) appendLine("  Notes: $notes")
            appendLine("  Booking ID: $slotId")
        }
    }
}

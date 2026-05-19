@file:Suppress("EnumEntryName")

package com.jetbrains.koog.workshop.agents.weather

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Granularity options for weather forecasts
 */
@Serializable
enum class Granularity {
    daily,
    hourly
}

/**
 * Tools for the weather agent
 */
@OptIn(ExperimentalTime::class)
class WeatherTools(
    private val openMeteoClient: OpenMeteoClient = OpenMeteoClient(),
    private val defaultTimeZone: TimeZone = TimeZone.UTC,
    private val clock: Clock = Clock.System,
) : ToolSet {

    suspend fun weatherForecast(
        location: String,
        // The date to get the weather forecast for in ISO format (e.g., '2023-05-20'). If empty, the forecast starts from today.
        date: String = "",
        days: Int = 1,
        granularity: Granularity = Granularity.daily
    ): String {
        val locations = openMeteoClient.searchLocation(location)
        if (locations.isEmpty()) {
            return "Location not found"
        }

        val loc = locations.first()
        val forecastDays = days.coerceIn(1, 7)

        val startDate = if (date.isNotBlank()) {
            try {
                LocalDate.parse(date)
            } catch (_: Exception) {
                null
            }
        } else null

        val endDate = startDate?.plus(forecastDays - 1, kotlinx.datetime.DateTimeUnit.DAY)

        val forecast = openMeteoClient.getWeatherForecast(
            latitude = loc.latitude,
            longitude = loc.longitude,
            forecastDays = forecastDays,
            startDate = startDate?.toString(),
            endDate = endDate?.toString()
        )

        val formattedForecast = when (granularity) {
            Granularity.hourly -> formatHourlyForecast(forecast, date)
            Granularity.daily -> formatDailyForecast(forecast, date)
        }

        val granularityText = when (granularity) {
            Granularity.daily -> "daily"
            Granularity.hourly -> "hourly"
        }
        val dateInfo = if (date.isBlank()) "starting from today" else "for $date"
        val formattedLocation = if (loc.country.isNullOrBlank()) loc.name else "${loc.name}, ${loc.country}"

        return "Weather forecast for $formattedLocation ($granularityText, $dateInfo):\n$formattedForecast"
    }

    @Tool("current_datetime")
    @LLMDescription("Get the current date and time in the specified timezone")
    suspend fun currentDatetime(
        @LLMDescription("The timezone to get the current date and time in (e.g., 'UTC', 'America/New_York', 'Europe/London'). Defaults to UTC.")
        timezone: String = "UTC"
    ): String {
        val zoneId = try {
            TimeZone.of(timezone)
        } catch (_: Exception) {
            defaultTimeZone
        }

        val now = clock.now()
        val localDateTime = now.toLocalDateTime(zoneId)
        val offset = zoneId.offsetAt(now)

        val time = localDateTime.time
        val timeStr = "${time.hour.toString().padStart(2, '0')}:${
            time.minute.toString().padStart(2, '0')
        }:${time.second.toString().padStart(2, '0')}"

        val datetime = "${localDateTime.date}T$timeStr$offset"
        return "Current datetime: $datetime, Date: ${localDateTime.date}, Time: $timeStr, Timezone: ${zoneId.id}"
    }

    @Tool("add_datetime")
    @LLMDescription("Add a duration to a date. Use this tool when you need to calculate offsets, such as tomorrow, in two days, etc.")
    suspend fun addDatetime(
        @LLMDescription("The date to add to in ISO format (e.g., '2023-05-20')")
        date: String,
        @LLMDescription("The number of days to add")
        days: Int,
        @LLMDescription("The number of hours to add")
        hours: Int,
        @LLMDescription("The number of minutes to add")
        minutes: Int
    ): String {
        val baseDate = if (date.isNotBlank()) {
            try {
                LocalDate.parse(date)
            } catch (_: Exception) {
                clock.now().toLocalDateTime(defaultTimeZone).date
            }
        } else {
            clock.now().toLocalDateTime(defaultTimeZone).date
        }

        val baseDateTime = LocalDateTime(baseDate.year, baseDate.month, baseDate.day, 0, 0)
        val baseInstant = baseDateTime.toInstant(defaultTimeZone)

        val period = DateTimePeriod(days = days, hours = hours, minutes = minutes)
        val resultDate = baseInstant.plus(period, defaultTimeZone).toLocalDateTime(defaultTimeZone).date.toString()

        return buildString {
            append("Date: $resultDate")
            if (date.isBlank()) {
                append(" (starting from today)")
            } else {
                append(" (starting from $date)")
            }

            if (days != 0 || hours != 0 || minutes != 0) {
                append(" after adding")
                if (days != 0) append(" $days days")
                if (hours != 0) {
                    if (days != 0) append(",")
                    append(" $hours hours")
                }
                if (minutes != 0) {
                    if (days != 0 || hours != 0) append(",")
                    append(" $minutes minutes")
                }
            }
        }
    }

    private fun formatDailyForecast(forecast: WeatherForecast, date: String): String {
        val daily = forecast.daily ?: return "No daily forecast data available"

        val startDate = date.ifBlank {
            Clock.System.now().toLocalDateTime(defaultTimeZone).date.toString()
        }

        val startIndex = daily.time.indexOfFirst { it >= startDate }.coerceAtLeast(0)

        return buildString {
            for (i in startIndex until daily.time.size) {
                val dateStr = daily.time[i]
                val maxTemp = daily.temperature2mMax?.getOrNull(i)?.toString() ?: "N/A"
                val minTemp = daily.temperature2mMin?.getOrNull(i)?.toString() ?: "N/A"
                val weatherCode = daily.weatherCode?.getOrNull(i)
                val weatherDesc = getWeatherDescription(weatherCode)
                val precipSum = daily.precipitationSum?.getOrNull(i)?.toString() ?: "0"

                append("$dateStr: $weatherDesc, ")
                append("Temperature: $minTemp°C to $maxTemp°C, ")
                append("Precipitation: $precipSum mm")

                if (i < daily.time.size - 1) append("\n")
            }
        }
    }

    private fun formatHourlyForecast(forecast: WeatherForecast, date: String): String {
        val hourly = forecast.hourly ?: return "No hourly forecast data available"

        val startDate = date.ifBlank {
            Clock.System.now().toLocalDateTime(defaultTimeZone).date.toString()
        }

        val startIndex = hourly.time.indexOfFirst {
            it.startsWith(startDate) || it > startDate
        }.coerceAtLeast(0)

        return buildString {
            val groupedByDate = hourly.time.subList(startIndex, hourly.time.size).mapIndexed { index, time ->
                val actualIndex = startIndex + index
                val dateTime = time.split("T")
                val d = dateTime[0]
                val hour = if (dateTime.size > 1) dateTime[1].substringBefore(":") else "00"

                val temp = hourly.temperature2m?.getOrNull(actualIndex)?.toString() ?: "N/A"
                val precipProb = hourly.precipitationProbability?.getOrNull(actualIndex)?.toString() ?: "N/A"
                val weatherCode = hourly.weatherCode?.getOrNull(actualIndex)
                val weatherDesc = getWeatherDescription(weatherCode)

                Triple(d, "$hour:00: $weatherDesc, Temperature: $temp°C, Precipitation probability: $precipProb%", actualIndex)
            }.groupBy { it.first }

            groupedByDate.forEach { (d, forecasts) ->
                append("$d:\n")
                forecasts.forEach { (_, f, _) -> append("  $f\n") }
            }
        }
    }

    private fun getWeatherDescription(code: Int?): String {
        return when (code) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75 -> "Snow fall"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown"
        }
    }
}

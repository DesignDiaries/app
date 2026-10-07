package com.example.util

import com.example.data.model.Priority
import com.example.data.model.Recurrence
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class ParsedTaskInput(
    val cleanTitle: String,
    val priority: Priority,
    val dueDate: Long?,
    val dueTimeMinutes: Int?,
    val tags: List<String>,
    val recurrence: Recurrence,
    val dateLabel: String?,
    val timeLabel: String?
)

object NaturalLanguageParser {

    private val TIME_REGEX = Pattern.compile(
        "(?:^|\\s)(?:at\\s+)?(1[0-2]|0?[1-9])(?::([0-5][0-9]))?\\s*(am|pm)(?=\\s|$)|(?:^|\\s)([01]?[0-9]|2[0-3]):([0-5][0-9])(?=\\s|$)",
        Pattern.CASE_INSENSITIVE
    )

    private val PRIORITY_REGEX = Pattern.compile(
        "(?:^|\\s)(!urgent|!high|!med|!medium|!low|urgent|p0|p1|p2|p3)(?=\\s|$)",
        Pattern.CASE_INSENSITIVE
    )

    private val TAG_REGEX = Pattern.compile("(?:^|\\s)#([a-zA-Z0-9_-]+)(?=\\s|$)")

    private val RECURRENCE_REGEX = Pattern.compile(
        "(?:^|\\s)(every\\s+day|daily|every\\s+week|weekly|every\\s+month|monthly|weekdays)(?=\\s|$)",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(input: String): ParsedTaskInput {
        var text = input.trim()
        if (text.isEmpty()) {
            return ParsedTaskInput(
                cleanTitle = "",
                priority = Priority.MEDIUM,
                dueDate = null,
                dueTimeMinutes = null,
                tags = emptyList(),
                recurrence = Recurrence.NONE,
                dateLabel = null,
                timeLabel = null
            )
        }

        // 1. Extract Tags
        val tags = mutableListOf<String>()
        val tagMatcher = TAG_REGEX.matcher(text)
        while (tagMatcher.find()) {
            val tag = tagMatcher.group(1)
            if (tag != null) tags.add(tag.lowercase())
        }
        text = text.replace(TAG_REGEX.toRegex(), " ")

        // 2. Extract Priority
        var detectedPriority = Priority.MEDIUM
        val priorityMatcher = PRIORITY_REGEX.matcher(text)
        if (priorityMatcher.find()) {
            val pStr = priorityMatcher.group(1)
            detectedPriority = Priority.fromString(pStr)
            val matched = priorityMatcher.group(0)!!
            text = text.replaceFirst(matched, " ")
        }

        // 3. Extract Recurrence
        var detectedRecurrence = Recurrence.NONE
        val recMatcher = RECURRENCE_REGEX.matcher(text)
        if (recMatcher.find()) {
            val recStr = recMatcher.group(1)?.lowercase() ?: ""
            detectedRecurrence = when {
                recStr.contains("day") || recStr == "daily" -> Recurrence.DAILY
                recStr.contains("weekdays") -> Recurrence.WEEKDAYS
                recStr.contains("week") || recStr == "weekly" -> Recurrence.WEEKLY
                recStr.contains("month") || recStr == "monthly" -> Recurrence.MONTHLY
                else -> Recurrence.NONE
            }
            val matched = recMatcher.group(0)!!
            text = text.replaceFirst(matched, " ")
        }

        // 4. Extract Time
        var detectedMinutes: Int? = null
        var timeLabel: String? = null
        val timeMatcher = TIME_REGEX.matcher(text)
        if (timeMatcher.find()) {
            val ampm = timeMatcher.group(3)
            if (ampm != null) {
                var hour = timeMatcher.group(1)?.toIntOrNull() ?: 12
                val min = timeMatcher.group(2)?.toIntOrNull() ?: 0
                val isPm = ampm.equals("pm", ignoreCase = true)
                if (isPm && hour < 12) hour += 12
                if (!isPm && hour == 12) hour = 0
                detectedMinutes = hour * 60 + min
                timeLabel = String.format(Locale.getDefault(), "%d:%02d %s", if (hour % 12 == 0) 12 else hour % 12, min, if (isPm) "PM" else "AM")
            } else {
                // 24 hour notation
                val hour = timeMatcher.group(4)?.toIntOrNull() ?: 0
                val min = timeMatcher.group(5)?.toIntOrNull() ?: 0
                detectedMinutes = hour * 60 + min
                timeLabel = String.format(Locale.getDefault(), "%02d:%02d", hour, min)
            }
            val matched = timeMatcher.group(0)!!
            text = text.replaceFirst(matched, " ")
        }

        // 5. Extract Date
        var detectedDate: Long? = null
        var dateLabel: String? = null
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val words = text.split("\\s+".toRegex()).toMutableList()
        val cleanedWords = mutableListOf<String>()

        var i = 0
        while (i < words.size) {
            val word = words[i].lowercase().replace("[^a-z0-9]".toRegex(), "")
            when {
                word == "today" -> {
                    detectedDate = cal.timeInMillis
                    dateLabel = "Today"
                }
                word == "tomorrow" || word == "tmrw" -> {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    detectedDate = cal.timeInMillis
                    dateLabel = "Tomorrow"
                }
                word == "yesterday" -> {
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    detectedDate = cal.timeInMillis
                    dateLabel = "Yesterday"
                }
                word in listOf("mon", "monday") -> {
                    moveToDayOfWeek(cal, Calendar.MONDAY)
                    detectedDate = cal.timeInMillis
                    dateLabel = "Monday"
                }
                word in listOf("tue", "tuesday") -> {
                    moveToDayOfWeek(cal, Calendar.TUESDAY)
                    detectedDate = cal.timeInMillis
                    dateLabel = "Tuesday"
                }
                word in listOf("wed", "wednesday") -> {
                    moveToDayOfWeek(cal, Calendar.WEDNESDAY)
                    detectedDate = cal.timeInMillis
                    dateLabel = "Wednesday"
                }
                word in listOf("thu", "thursday") -> {
                    moveToDayOfWeek(cal, Calendar.THURSDAY)
                    detectedDate = cal.timeInMillis
                    dateLabel = "Thursday"
                }
                word in listOf("fri", "friday") -> {
                    moveToDayOfWeek(cal, Calendar.FRIDAY)
                    detectedDate = cal.timeInMillis
                    dateLabel = "Friday"
                }
                word in listOf("sat", "saturday") -> {
                    moveToDayOfWeek(cal, Calendar.SATURDAY)
                    detectedDate = cal.timeInMillis
                    dateLabel = "Saturday"
                }
                word in listOf("sun", "sunday") -> {
                    moveToDayOfWeek(cal, Calendar.SUNDAY)
                    detectedDate = cal.timeInMillis
                    dateLabel = "Sunday"
                }
                word == "next" && i + 1 < words.size -> {
                    val nextWord = words[i + 1].lowercase().replace("[^a-z0-9]".toRegex(), "")
                    if (nextWord == "week") {
                        cal.add(Calendar.WEEK_OF_YEAR, 1)
                        detectedDate = cal.timeInMillis
                        dateLabel = "Next week"
                        i++ // Skip next word
                    } else {
                        cleanedWords.add(words[i])
                    }
                }
                else -> {
                    cleanedWords.add(words[i])
                }
            }
            i++
        }

        val cleanTitle = cleanedWords.joinToString(" ").trim()
            .replace("\\s+".toRegex(), " ")
            .replace("^[!#]\\w+\\s*".toRegex(), "")
            .trim()

        return ParsedTaskInput(
            cleanTitle = if (cleanTitle.isNotBlank()) cleanTitle else input.trim(),
            priority = detectedPriority,
            dueDate = detectedDate,
            dueTimeMinutes = detectedMinutes,
            tags = tags,
            recurrence = detectedRecurrence,
            dateLabel = dateLabel,
            timeLabel = timeLabel
        )
    }

    private fun moveToDayOfWeek(cal: Calendar, targetDay: Int) {
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = (targetDay - currentDay + 7) % 7
        if (daysToAdd == 0) daysToAdd = 7 // Next week's day
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
    }
}

package com.example

import com.example.data.model.Priority
import com.example.data.model.Recurrence
import com.example.util.NaturalLanguageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSimpleTask() {
    val parsed = NaturalLanguageParser.parse("Buy milk")
    assertEquals("Buy milk", parsed.cleanTitle)
    assertEquals(Priority.MEDIUM, parsed.priority)
    assertEquals(Recurrence.NONE, parsed.recurrence)
  }

  @Test
  fun testTaskWithTimeAndPriority() {
    val parsed = NaturalLanguageParser.parse("Doctor appointment tomorrow 4:30pm !urgent #health")
    assertEquals("Doctor appointment", parsed.cleanTitle)
    assertEquals(Priority.URGENT, parsed.priority)
    assertEquals(16 * 60 + 30, parsed.dueTimeMinutes)
    assertTrue(parsed.tags.contains("health"))
    assertEquals("Tomorrow", parsed.dateLabel)
  }

  @Test
  fun testRecurringTask() {
    val parsed = NaturalLanguageParser.parse("Team standup every day 9am #work")
    assertEquals("Team standup", parsed.cleanTitle)
    assertEquals(Recurrence.DAILY, parsed.recurrence)
    assertEquals(9 * 60, parsed.dueTimeMinutes)
    assertTrue(parsed.tags.contains("work"))
  }
}


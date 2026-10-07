package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Priority
import com.example.data.model.Recurrence
import com.example.util.NaturalLanguageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TaskLaunch", appName)
  }

  @Test
  fun `test natural language parsing for quick add`() {
    val input = "Call mom tomorrow 6pm !high #family"
    val parsed = NaturalLanguageParser.parse(input)

    assertEquals("Call mom", parsed.cleanTitle)
    assertEquals(Priority.HIGH, parsed.priority)
    assertNotNull(parsed.dueDate)
    assertEquals(18 * 60, parsed.dueTimeMinutes)
    assertTrue(parsed.tags.contains("family"))
    assertEquals("Tomorrow", parsed.dateLabel)
  }

  @Test
  fun `test natural language parsing for urgent tag and recurrence`() {
    val input = "Team standup every day 9am !urgent #work"
    val parsed = NaturalLanguageParser.parse(input)

    assertEquals("Team standup", parsed.cleanTitle)
    assertEquals(Priority.URGENT, parsed.priority)
    assertEquals(Recurrence.DAILY, parsed.recurrence)
    assertEquals(9 * 60, parsed.dueTimeMinutes)
    assertTrue(parsed.tags.contains("work"))
  }

  @Test
  fun `test LauncherViewModel instantiation`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.LauncherViewModel(application)
    assertNotNull(vm)
  }

  @Test
  fun `test MainActivity lifecycle setup`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    assertNotNull(controller.get())
  }

  @Test
  fun `test Pomodoro and Habit functionality`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.LauncherViewModel(application)

    // Pomodoro modes
    vm.setPomodoroMode(com.example.ui.viewmodel.PomodoroMode.DEEP_SPRINT)
    assertEquals(50 * 60, vm.pomodoroState.value.secondsRemaining)
    assertEquals(com.example.ui.viewmodel.PomodoroMode.DEEP_SPRINT, vm.pomodoroState.value.mode)

    // Habits
    val initialHabits = vm.habits.value
    assertTrue(initialHabits.isNotEmpty())
    val firstHabitId = initialHabits.first().id
    val initialStatus = initialHabits.first().isCompleted
    vm.toggleHabit(firstHabitId)
    val updatedHabit = vm.habits.value.first { it.id == firstHabitId }
    assertEquals(!initialStatus, updatedHabit.isCompleted)
  }
}


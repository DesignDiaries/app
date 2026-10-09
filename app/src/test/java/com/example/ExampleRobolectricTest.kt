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

  @Test
  fun `test dock customization and reset`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.LauncherViewModel(application)

    val customList = listOf("com.example.app1", "com.example.app2")
    vm.setDockApps(customList)
    assertEquals(customList, vm.customDockApps.value)

    vm.addAppToDock("com.example.app3")
    assertTrue(vm.customDockApps.value.contains("com.example.app3"))

    vm.removeAppFromDock("com.example.app1")
    assertTrue(!vm.customDockApps.value.contains("com.example.app1"))
  }

  @Test
  fun `test empty and blank inputs do not crash and handle gracefully`() = kotlinx.coroutines.test.runTest {
    // Empty NLP input
    val emptyResult = NaturalLanguageParser.parse("")
    assertEquals("", emptyResult.cleanTitle)
    assertEquals(Priority.MEDIUM, emptyResult.priority)

    val blankResult = NaturalLanguageParser.parse("    ")
    assertEquals("", blankResult.cleanTitle)

    // Only tags/symbols input
    val onlyTagResult = NaturalLanguageParser.parse("#work !high")
    assertEquals("", onlyTagResult.cleanTitle)
    assertEquals(Priority.HIGH, onlyTagResult.priority)
    assertTrue(onlyTagResult.tags.contains("work"))

    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.LauncherViewModel(application)

    // Empty quick add submission
    vm.onQuickAddTextChanged("")
    vm.submitQuickAdd()
    assertEquals("", vm.quickAddText.value)

    vm.onQuickAddTextChanged("   ")
    vm.submitQuickAdd()
    assertEquals("", vm.quickAddText.value)

    // Invalid / empty JSON import
    val emptyImportResult = vm.importBackupJson("")
    assertEquals(false, emptyImportResult)

    val invalidJsonResult = vm.importBackupJson("{ malformed json }")
    assertEquals(false, invalidJsonResult)

    // Scratchpad with empty content
    vm.updateScratchpadNotes("")
    val created = vm.convertScratchpadToTasks()
    assertEquals(0, created)
  }

  @Test
  fun `test rapid taps on Pomodoro timer and quick add`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.LauncherViewModel(application)

    // Rapid alternating toggles
    vm.togglePomodoro("Test Task")
    vm.togglePomodoro("Test Task")
    vm.togglePomodoro("Test Task")
    // Should be in a stable state
    assertTrue(vm.pomodoroState.value.secondsRemaining > 0)

    // Reset Pomodoro
    vm.resetPomodoro()
    assertEquals(false, vm.pomodoroState.value.isRunning)

    // Rapid submitQuickAdd calls
    vm.onQuickAddTextChanged("Rapid Task Test")
    vm.submitQuickAdd()
    vm.submitQuickAdd()
    vm.submitQuickAdd()
    // Text cleared immediately on first tap
    assertEquals("", vm.quickAddText.value)
  }

  @Test
  fun `test alarm scheduler does not crash under permission restrictions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testTask = com.example.data.model.TaskEntity(
      id = 12345L,
      title = "Test Alarm Task",
      reminderTime = System.currentTimeMillis() + 60000L
    )

    // Scheduling and cancelling should handle all API levels without throwing
    com.example.service.AlarmScheduler.scheduleReminder(context, testTask)
    com.example.service.AlarmScheduler.cancelReminder(context, testTask.id)
  }
}


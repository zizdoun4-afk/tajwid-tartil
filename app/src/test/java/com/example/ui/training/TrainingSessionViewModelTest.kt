package com.example.ui.training

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.MemorizationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TrainingSessionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var app: Application
    private lateinit var viewModel: TrainingSessionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        viewModel = TrainingSessionViewModel(app, surahNumber = 1, ayahNumber = 1)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `step 1 gating - cannot proceed until 3x playback action performed`() {
        assertEquals(TrainingStep.LISTEN_3X, viewModel.uiState.value.currentStep)
        assertFalse("Step 1 must be gated until 3x playback action is performed", viewModel.canProceedToNextStep())

        // Attempting to advance must fail
        viewModel.goToNextStep()
        assertEquals(TrainingStep.LISTEN_3X, viewModel.uiState.value.currentStep)

        // Complete step 1 action
        viewModel.markStep1Completed()
        assertTrue("Step 1 should be complete after 3x playback", viewModel.canProceedToNextStep())

        viewModel.goToNextStep()
        assertEquals(TrainingStep.ACCOMPANIED_READING, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `step 2 gating - cannot proceed until accompanied reading playback action performed`() {
        viewModel.markStep1Completed()
        viewModel.goToNextStep()
        assertEquals(TrainingStep.ACCOMPANIED_READING, viewModel.uiState.value.currentStep)

        // Cannot proceed initially
        assertFalse("Step 2 must be gated until accompanied reading is performed", viewModel.canProceedToNextStep())
        viewModel.goToNextStep()
        assertEquals(TrainingStep.ACCOMPANIED_READING, viewModel.uiState.value.currentStep)

        // Complete step 2 action
        viewModel.markStep2Completed()
        assertTrue("Step 2 should be complete after accompanied reading", viewModel.canProceedToNextStep())

        viewModel.goToNextStep()
        assertEquals(TrainingStep.SOLO_RECORDING, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `step 3 gating - requires valid user recording file`() {
        viewModel.markStep1Completed()
        viewModel.goToNextStep()
        viewModel.markStep2Completed()
        viewModel.goToNextStep()
        assertEquals(TrainingStep.SOLO_RECORDING, viewModel.uiState.value.currentStep)

        // Without recording file
        assertFalse("Step 3 cannot proceed without recording", viewModel.canProceedToNextStep())
        viewModel.goToNextStep()
        assertEquals(TrainingStep.SOLO_RECORDING, viewModel.uiState.value.currentStep)

        // Create a temporary recording file
        val tempFile = File(app.cacheDir, "test_take.m4a").apply {
            writeText("dummy audio data")
        }

        // Simulate recording stopped with file
        val currentUi = viewModel.uiState.value
        val field = TrainingSessionViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<TrainingSessionUiState>
        stateFlow.value = currentUi.copy(userRecordingFile = tempFile)

        assertTrue("Step 3 should proceed when valid recording exists", viewModel.canProceedToNextStep())
        viewModel.goToNextStep()
        assertEquals(TrainingStep.PLAYBACK_REVIEW, viewModel.uiState.value.currentStep)

        // Clean up
        tempFile.delete()
    }

    @Test
    fun `step 4 gating - requires recording file to remain available`() {
        viewModel.markStep1Completed()
        viewModel.goToNextStep()
        viewModel.markStep2Completed()
        viewModel.goToNextStep()

        val tempFile = File(app.cacheDir, "test_take_review.m4a").apply {
            writeText("dummy audio data")
        }
        val currentUi = viewModel.uiState.value
        val field = TrainingSessionViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<TrainingSessionUiState>
        stateFlow.value = currentUi.copy(userRecordingFile = tempFile)

        viewModel.goToNextStep()
        assertEquals(TrainingStep.PLAYBACK_REVIEW, viewModel.uiState.value.currentStep)
        assertTrue(viewModel.canProceedToNextStep())

        // If file deleted, should not be able to proceed
        tempFile.delete()
        assertFalse(viewModel.canProceedToNextStep())
    }

    @Test
    fun `step 5 blind test - requires explicit validation`() = runTest {
        viewModel.markStep1Completed()
        viewModel.goToNextStep()
        viewModel.markStep2Completed()
        viewModel.goToNextStep()

        val tempFile = File(app.cacheDir, "test_take_blind.m4a").apply {
            writeText("dummy audio data")
        }
        val currentUi = viewModel.uiState.value
        val field = TrainingSessionViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<TrainingSessionUiState>
        stateFlow.value = currentUi.copy(userRecordingFile = tempFile)

        viewModel.goToNextStep() // To PLAYBACK_REVIEW
        viewModel.goToNextStep() // To BLIND_TEST
        assertEquals(TrainingStep.BLIND_TEST, viewModel.uiState.value.currentStep)

        // Step 5 cannot proceed via Next
        assertFalse(viewModel.canProceedToNextStep())

        // Retry: marks reviewed, not completed
        viewModel.validateBlindTest(succeeded = false)
        var count = 0
        while (viewModel.uiState.value.status != MemorizationStatus.REVIEW && count < 50) {
            Thread.sleep(40)
            count++
        }
        assertFalse(viewModel.uiState.value.isSessionCompleted)
        assertEquals(MemorizationStatus.REVIEW, viewModel.uiState.value.status)

        // Success: marks memorized, session completed
        viewModel.validateBlindTest(succeeded = true)
        count = 0
        while (viewModel.uiState.value.status != MemorizationStatus.MEMORIZED && count < 50) {
            Thread.sleep(40)
            count++
        }
        assertTrue(viewModel.uiState.value.isSessionCompleted)
        assertEquals(MemorizationStatus.MEMORIZED, viewModel.uiState.value.status)

        tempFile.delete()
    }

    @Test
    fun `abandon session removes temporary recording file`() {
        val tempFile = File(app.cacheDir, "test_take_abandon.m4a").apply {
            writeText("dummy audio data")
        }
        assertTrue(tempFile.exists())

        val currentUi = viewModel.uiState.value
        val field = TrainingSessionViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<TrainingSessionUiState>
        stateFlow.value = currentUi.copy(userRecordingFile = tempFile)

        viewModel.abandonSession()

        // Wait brief delay for background cleanupScope
        Thread.sleep(100)
        assertFalse("Temporary file should be removed on session abandon", tempFile.exists())
    }
}

package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.domain.model.QuranReciter
import com.example.ui.components.ChooseReciterButton
import com.example.ui.theme.NoorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Jetpack Compose UI verification for NOOR's simplified single "Choose Reciter" control.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoorRegressionUiVerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chooseReciterButton_rendersSingleButtonOpensAllRecitersUpdatesSelectionAndCloses() {
        var latestReciter = QuranReciter.YASSER_AL_DOSARI

        composeRule.setContent {
            NoorTheme {
                var selected by remember { mutableStateOf(QuranReciter.YASSER_AL_DOSARI) }
                ChooseReciterButton(
                    selectedReciter = selected,
                    onSelectReciter = { next ->
                        selected = next
                        latestReciter = next
                    }
                )
            }
        }

        // 1. Exactly ONE "Choose Reciter" button is visible; NO reciter list dialog is visible initially
        composeRule.onAllNodesWithTag("choose_reciter_button").assertCountEquals(1)
        composeRule.onNodeWithTag("choose_reciter_button").assertIsDisplayed()
        composeRule.onAllNodesWithTag("reciter_picker_dialog").assertCountEquals(0)

        // 2. Tap the single Choose Reciter button -> opens clean selection menu with active Sheikh indicated
        composeRule.onNodeWithTag("choose_reciter_button").performClick()
        composeRule.onNodeWithTag("reciter_picker_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag(
            "reciter_selected_indicator_${QuranReciter.YASSER_AL_DOSARI.id}",
            useUnmergedTree = true
        ).assertIsDisplayed()

        // 3. Select a different reciter (Alafasy) -> updates selection and immediately closes menu
        composeRule.onNodeWithTag("reciter_option_${QuranReciter.ALAFASY.id}").performClick()
        composeRule.waitForIdle()

        assertEquals(QuranReciter.ALAFASY, latestReciter)
        composeRule.onAllNodesWithTag("reciter_picker_dialog").assertCountEquals(0)

        // 4. Re-opening shows Alafasy as the newly selected reciter
        composeRule.onNodeWithTag("choose_reciter_button").performClick()
        composeRule.onNodeWithTag(
            "reciter_selected_indicator_${QuranReciter.ALAFASY.id}",
            useUnmergedTree = true
        ).assertIsDisplayed()
    }
}

package dev.hossain.remotenotify.ui.about

import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.BuildConfig
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.platform.AppVersionProvider
import dev.hossain.remotenotify.platform.BuildInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.ExperimentalTime

/**
 * Tests for [AboutAppPresenter].
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class AboutAppPresenterTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val navigator: Navigator = mockk(relaxed = true)
    private val analytics: Analytics = mockk(relaxed = true)
    private val appVersionProvider: AppVersionProvider = mockk()

    @Before
    fun setUp() {
        // Mock the app version provider to return a specific version for testing
        coEvery { appVersionProvider.getAppVersion() } returns "v${BuildConfig.VERSION_NAME} (${BuildInfo.GIT_SHA})"
    }

    @After
    fun tearDown() {
        unmockkStatic(LocalUriHandler::class)
    }

    @Test
    fun `initial state contains app version and showEducationSheet is false`() = runTest {
        val expectedAppVersion = "v${BuildConfig.VERSION_NAME} (${BuildInfo.GIT_SHA})"

        moleculeFlow(RecompositionClock.Immediate) {
            AboutAppPresenter(navigator = navigator, analytics = analytics, appVersionProvider = appVersionProvider)
        }.test {
            val state = awaitItem()
            assertThat(state.appVersion).isEqualTo(expectedAppVersion)
            assertThat(state.showEducationSheet).isFalse()
            // Verify that getAppVersion was called
            coVerify { appVersionProvider.getAppVersion() }
        }
    }

    @Test
    fun `event GoBack invokes navigator pop`() = runTest {
        moleculeFlow(RecompositionClock.Immediate) {
            AboutAppPresenter(navigator = navigator, analytics = analytics, appVersionProvider = appVersionProvider)
        }.test {
            val state = awaitItem()
            state.eventSink(AboutAppScreenEvent.GoBack)
            coVerify { navigator.pop() }
        }
    }

    @Test
    fun `event OpenGitHubProject opens project url via UriHandler`() = runTest {
        val uriHandler = mockk<androidx.compose.ui.platform.UriHandler>(relaxed = true)
        mockkStatic(LocalUriHandler::class)
        every { LocalUriHandler.current } returns uriHandler

        val expectedUrl = "https://github.com/hossain-khan/android-remote-notify"

        moleculeFlow(RecompositionClock.Immediate) {
            AboutAppPresenter(navigator = navigator, analytics = analytics, appVersionProvider = appVersionProvider)
        }.test {
            val state = awaitItem()
            state.eventSink(AboutAppScreenEvent.OpenGitHubProject)
            coVerify { uriHandler.openUri(expectedUrl) }
        }
    }

    @Test
    fun `event OpenLearnMoreSheet sets showEducationSheet to true and logs analytics`() = runTest {
        moleculeFlow(RecompositionClock.Immediate) {
            AboutAppPresenter(navigator = navigator, analytics = analytics, appVersionProvider = appVersionProvider)
        }.test {
            val initialState = awaitItem()
            assertThat(initialState.showEducationSheet).isFalse()

            initialState.eventSink(AboutAppScreenEvent.OpenLearnMoreSheet)

            val updatedState = awaitItem()
            assertThat(updatedState.showEducationSheet).isTrue()
            coVerify { analytics.logViewTutorial(isComplete = false) }
        }
    }

    @Test
    fun `event DismissLearnMoreSheet sets showEducationSheet to false and logs analytics`() = runTest {
        moleculeFlow(RecompositionClock.Immediate) {
            AboutAppPresenter(navigator = navigator, analytics = analytics, appVersionProvider = appVersionProvider)
        }.test {
            val initialState = awaitItem()
            // First, open the sheet to ensure it's in the correct state for dismissal
            initialState.eventSink(AboutAppScreenEvent.OpenLearnMoreSheet)
            val stateAfterOpen = awaitItem()
            assertThat(stateAfterOpen.showEducationSheet).isTrue() // Sanity check

            // Now, dismiss the sheet
            stateAfterOpen.eventSink(AboutAppScreenEvent.DismissLearnMoreSheet)

            val finalState = awaitItem()
            assertThat(finalState.showEducationSheet).isFalse()
            coVerify { analytics.logViewTutorial(isComplete = true) }
        }
    }

    @Test
    fun `impression effect logs screen view`() = runTest {
        moleculeFlow(RecompositionClock.Immediate) {
            AboutAppPresenter(navigator = navigator, analytics = analytics, appVersionProvider = appVersionProvider)
        }.test {
            awaitItem() // Trigger LaunchedImpressionEffect by collecting the first item
            coVerify { analytics.logScreenView(AboutAppScreen::class) }
        }
    }
}

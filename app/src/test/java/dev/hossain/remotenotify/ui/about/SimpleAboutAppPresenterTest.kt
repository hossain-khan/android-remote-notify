package dev.hossain.remotenotify.ui.about

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.runtime.Navigator
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.platform.AppVersionProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.time.ExperimentalTime

/**
 * Simplified tests for [AboutAppPresenter].
 * 
 * Note: This is a simplified version of the test that doesn't use Molecule or Compose UI testing.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class SimpleAboutAppPresenterTest {
    private val navigator: Navigator = mockk(relaxed = true)
    private val analytics: Analytics = mockk(relaxed = true)
    private val appVersionProvider: AppVersionProvider = mockk()

    @Before
    fun setUp() {
        // Mock the app version provider to return a specific version for testing
        coEvery { appVersionProvider.getAppVersion() } returns "v1.0.0 (test-sha)"
    }

    @Test
    fun `verify app version provider is called`() = runTest {
        // Create the presenter
        AboutAppPresenter(navigator = navigator, analytics = analytics, appVersionProvider = appVersionProvider)
        
        // Verify that getAppVersion was called
        coVerify { appVersionProvider.getAppVersion() }
    }

    @Test
    fun `verify analytics screen view is logged`() = runTest {
        // Create the presenter
        AboutAppPresenter(navigator = navigator, analytics = analytics, appVersionProvider = appVersionProvider)
        
        // Verify that screen view was logged
        coVerify { analytics.logScreenView(AboutAppScreen::class) }
    }
}
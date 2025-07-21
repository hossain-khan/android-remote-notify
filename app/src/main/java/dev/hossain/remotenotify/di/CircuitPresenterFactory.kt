package dev.hossain.remotenotify.di

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.hossain.remotenotify.ui.about.AboutAppPresenter
import dev.hossain.remotenotify.ui.about.AboutAppScreen
import dev.hossain.remotenotify.ui.addalert.AddNewRemoteAlertPresenter
import dev.hossain.remotenotify.ui.addalert.AddNewRemoteAlertScreen
import dev.hossain.remotenotify.ui.alertchecklog.AlertCheckLogViewerPresenter
import dev.hossain.remotenotify.ui.alertchecklog.AlertCheckLogViewerScreen
import dev.hossain.remotenotify.ui.alertlist.AlertsListPresenter
import dev.hossain.remotenotify.ui.alertlist.AlertsListScreen
import dev.hossain.remotenotify.ui.alertmediumconfig.ConfigureNotificationMediumPresenter
import dev.hossain.remotenotify.ui.alertmediumconfig.ConfigureNotificationMediumScreen
import dev.hossain.remotenotify.ui.alertmediumlist.NotificationMediumListPresenter
import dev.hossain.remotenotify.ui.alertmediumlist.NotificationMediumListScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * A unified [Presenter.Factory] that consolidates all presenter creation logic in one place.
 * This replaces the need for individual `*PresenterFactory` classes by directly using
 * the presenter's factory interfaces.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class CircuitPresenterFactory(
    private val aboutAppPresenterFactory: AboutAppPresenter.Factory,
    private val addNewRemoteAlertPresenterFactory: AddNewRemoteAlertPresenter.Factory,
    private val alertCheckLogViewerPresenterFactory: AlertCheckLogViewerPresenter.Factory,
    private val alertsListPresenterFactory: AlertsListPresenter.Factory,
    private val configureNotificationMediumPresenterFactory: ConfigureNotificationMediumPresenter.Factory,
    private val notificationMediumListPresenterFactory: NotificationMediumListPresenter.Factory,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? =
        when (screen) {
            AboutAppScreen -> aboutAppPresenterFactory.create(navigator)
            AddNewRemoteAlertScreen -> addNewRemoteAlertPresenterFactory.create(navigator)
            AlertCheckLogViewerScreen -> alertCheckLogViewerPresenterFactory.create(navigator)
            AlertsListScreen -> alertsListPresenterFactory.create(navigator)
            is ConfigureNotificationMediumScreen -> configureNotificationMediumPresenterFactory.create(screen, navigator)
            NotificationMediumListScreen -> notificationMediumListPresenterFactory.create(navigator)
            else -> null
        }
}

package dev.hossain.remotenotify.ui.addalert

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoSet(AppScope::class)
class AddNewRemoteAlertPresenterFactory(
    private val factory: AddNewRemoteAlertPresenter.Factory,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? =
        when (screen) {
            AddNewRemoteAlertScreen -> factory.create(navigator)
            else -> null
        }
}

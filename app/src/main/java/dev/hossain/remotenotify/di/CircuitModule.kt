package dev.hossain.remotenotify.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Metro module that provides dependencies for the Circuit framework.
 */
@BindingContainer
interface CircuitModule { // TODO rename this to CircuitMultibinding

    /**
     * Metro multi-binding method that provides a set of Presenter.Factory instances.
     */
    @Multibinds(allowEmpty = true)
    val presenterFactories: Set<Presenter.Factory>

    /**
     * Metro multi-binding method that provides a set of Ui.Factory instances.
     */
    @Multibinds(allowEmpty = true)
    val viewFactories: Set<Ui.Factory>

    companion object {
        /**
         * Provides a singleton instance of Circuit with presenter and ui configured.
         */
        @SingleIn(AppScope::class)
        @Provides
        fun provideCircuit(
            presenterFactories: @JvmSuppressWildcards Set<Presenter.Factory>,
            uiFactories: @JvmSuppressWildcards Set<Ui.Factory>,
        ): Circuit =
            Circuit
                .Builder()
                .addPresenterFactories(presenterFactories)
                .addUiFactories(uiFactories)
                .build()
    }
}

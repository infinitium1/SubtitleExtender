package com.subtitleextender.ui.main

import androidx.annotation.StringRes

/**
 * One-shot events emitted by [MainViewModel] for the UI layer to react to,
 * such as launching a system dialog or showing a transient message.
 *
 * These are modeled separately from [MainUiState] because, unlike state,
 * they should be consumed exactly once (e.g. a snackbar should not reappear
 * after a configuration change) - the ViewModel exposes them through a
 * [kotlinx.coroutines.channels.Channel] rather than a
 * [kotlinx.coroutines.flow.StateFlow].
 */
sealed interface MainUiEvent {

    /** Ask the UI to launch the system "create document" dialog. */
    data class LaunchSaveDocument(val suggestedFileName: String) : MainUiEvent

    /** Ask the UI to show a short message, e.g. via a Snackbar. */
    data class ShowMessage(@StringRes val messageRes: Int, val isError: Boolean) : MainUiEvent
}

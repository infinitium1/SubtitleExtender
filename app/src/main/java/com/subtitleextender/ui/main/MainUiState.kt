package com.subtitleextender.ui.main

import android.net.Uri
import com.subtitleextender.data.model.ExtensionDuration

/**
 * Immutable snapshot of everything [MainScreen] needs to render at any given
 * moment. Produced exclusively by [MainViewModel]; the UI never mutates it
 * directly.
 *
 * @property selectedFileUri the SAF [Uri] of the subtitle file the user
 *   picked, or `null` if none has been selected yet.
 * @property selectedFileName the display name of the selected file, used for
 *   both showing feedback and deriving the suggested output filename.
 * @property selectedExtension which of the four allowed durations is
 *   currently chosen.
 * @property isProcessing whether a parse/extend/write operation is currently
 *   running, used to disable the process button and show progress.
 * @property pendingSaveContent the fully-processed SRT text waiting to be
 *   written once the user picks a destination, or `null` if there is nothing
 *   pending.
 * @property suggestedFileName the filename offered as the default in the
 *   "save as" dialog.
 */
data class MainUiState(
    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    val selectedExtension: ExtensionDuration = ExtensionDuration.DEFAULT,
    val isProcessing: Boolean = false,
    val pendingSaveContent: String? = null,
    val suggestedFileName: String = "subtitle_extended.srt"
)

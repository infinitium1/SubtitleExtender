package com.subtitleextender.ui.main

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subtitleextender.R
import com.subtitleextender.data.model.ExtensionDuration
import com.subtitleextender.data.parser.SrtParseException
import com.subtitleextender.data.parser.SrtParser
import com.subtitleextender.data.parser.SrtWriter
import com.subtitleextender.data.processor.SubtitleExtensionProcessor
import com.subtitleextender.data.repository.SubtitleRepository
import com.subtitleextender.util.LineEndingDetector
import com.subtitleextender.util.SubtitleFileNaming
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Handles subtitle file selection, extension processing, and save-file
 * coordination for [MainScreen].
 *
 * Following MVVM, this class never touches Android UI or launches intents
 * directly. It only exposes state ([uiState]) and one-shot events ([events])
 * for the Composable layer to act on (e.g. launching a system file picker),
 * which keeps [MainViewModel] fully unit-testable in isolation from any
 * Activity or Compose code.
 *
 * All parsing and processing work runs off the main thread (see
 * [onProcessClicked]), so files with thousands of subtitle blocks never
 * cause a dropped frame or an ANR.
 */
class MainViewModel(
    private val repository: SubtitleRepository
) : ViewModel() {

    private val parser = SrtParser()
    private val writer = SrtWriter()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = Channel<MainUiEvent>(Channel.BUFFERED)
    val events: Flow<MainUiEvent> = _events.receiveAsFlow()

    /** Called when the user picks a source file via the system document picker. */
    fun onFileSelected(uri: Uri) {
        val displayName = repository.queryDisplayName(uri) ?: uri.lastPathSegment ?: DEFAULT_FILE_NAME
        _uiState.value = _uiState.value.copy(
            selectedFileUri = uri,
            selectedFileName = displayName,
            pendingSaveContent = null
        )
    }

    /** Called when the user changes the selected extension duration. */
    fun onExtensionSelected(duration: ExtensionDuration) {
        _uiState.value = _uiState.value.copy(selectedExtension = duration)
    }

    /** Called when the user taps "Extend Subtitle Duration". */
    fun onProcessClicked() {
        val state = _uiState.value
        val uri = state.selectedFileUri
        if (uri == null) {
            emitEvent(MainUiEvent.ShowMessage(R.string.error_no_file_selected, isError = true))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                // Parsing and extending run on Dispatchers.Default (CPU-bound
                // work) rather than the ViewModel's default Main dispatcher,
                // so a 10,000+ block file never blocks the UI thread.
                val (outputContent, suggestedName) = withContext(Dispatchers.Default) {
                    val rawContent = repository.readTextFile(uri)
                    val entries = parser.parse(rawContent)
                    val extended = SubtitleExtensionProcessor.extend(
                        entries = entries,
                        extensionMillis = state.selectedExtension.milliseconds
                    )
                    val lineEnding = LineEndingDetector.detect(rawContent)
                    val content = writer.write(extended, lineEnding)
                    val name = SubtitleFileNaming.buildExtendedFileName(
                        state.selectedFileName ?: DEFAULT_FILE_NAME
                    )
                    content to name
                }

                _uiState.value = _uiState.value.copy(
                    pendingSaveContent = outputContent,
                    suggestedFileName = suggestedName
                )
                emitEvent(MainUiEvent.LaunchSaveDocument(suggestedName))
            } catch (e: CancellationException) {
                throw e
            } catch (e: SrtParseException) {
                emitEvent(MainUiEvent.ShowMessage(R.string.error_invalid_srt, isError = true))
            } catch (e: IOException) {
                emitEvent(MainUiEvent.ShowMessage(R.string.error_generic, isError = true))
            } catch (e: Exception) {
                emitEvent(MainUiEvent.ShowMessage(R.string.error_generic, isError = true))
            } finally {
                _uiState.value = _uiState.value.copy(isProcessing = false)
            }
        }
    }

    /** Called once the user picks (or cancels picking) a destination for the extended file. */
    fun onSaveLocationSelected(uri: Uri?) {
        if (uri == null) return
        val content = _uiState.value.pendingSaveContent ?: return

        viewModelScope.launch {
            try {
                repository.writeTextFile(uri, content)
                _uiState.value = _uiState.value.copy(pendingSaveContent = null)
                emitEvent(MainUiEvent.ShowMessage(R.string.success_message, isError = false))
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                emitEvent(MainUiEvent.ShowMessage(R.string.error_generic, isError = true))
            } catch (e: Exception) {
                emitEvent(MainUiEvent.ShowMessage(R.string.error_generic, isError = true))
            }
        }
    }

    private fun emitEvent(event: MainUiEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    companion object {
        private const val DEFAULT_FILE_NAME = "subtitle.srt"

        /**
         * Manual [ViewModelProvider.Factory] used instead of a DI framework.
         * This keeps the project's dependency footprint small while still
         * allowing [SubtitleRepository] to be constructor-injected (and
         * therefore swapped for a fake) in tests.
         */
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repository = SubtitleRepository(context.applicationContext)
                    return MainViewModel(repository) as T
                }
            }
    }
}

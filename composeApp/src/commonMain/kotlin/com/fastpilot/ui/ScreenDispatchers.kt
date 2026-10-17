package com.fastpilot.ui

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Where the screens load and compute their data: off the main thread, so a screen parsing a
 * race's laps or fitting tyre degradation does not freeze the UI or hold back the screen the
 * user opens next. Tests swap it for their test dispatcher.
 */
object ScreenDispatchers {
    var background: CoroutineDispatcher = Dispatchers.Default
}

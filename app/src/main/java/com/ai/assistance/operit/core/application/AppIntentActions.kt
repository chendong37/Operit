package com.ai.assistance.operit.core.application

import com.ai.assistance.operit.BuildConfig

/**
 * Runtime intent names are scoped to the installed application id, not the Kotlin namespace.
 * This keeps release, debug, clone, and independently branded installs from receiving each
 * other's implicit broadcasts.
 */
object AppIntentActions {
    private fun scoped(suffix: String): String = BuildConfig.APPLICATION_ID + suffix

    val cancelCurrentOperation = scoped(".action.CANCEL_CURRENT_OPERATION")
    val ensureMicrophoneForeground = scoped(".action.ENSURE_MICROPHONE_FOREGROUND")
    val exitApp = scoped(".action.EXIT_APP")
    val floatingChatServiceStarted = scoped(".action.FLOATING_CHAT_SERVICE_STARTED")
    val floatingChatServiceStopped = scoped(".action.FLOATING_CHAT_SERVICE_STOPPED")
    val floatingChatWindowShown = scoped(".action.FLOATING_CHAT_WINDOW_SHOWN")
    val floatingChatWindowShowFailed = scoped(".action.FLOATING_CHAT_WINDOW_SHOW_FAILED")
    val openDataRecovery = scoped(".action.OPEN_DATA_RECOVERY")
    val openSettingsShortcut = scoped(".action.OPEN_SETTINGS_SHORTCUT")
    val openVoiceFloatingWindow = scoped(".action.OPEN_VOICE_FLOATING_WINDOW")
    val prepareWakeHandoff = scoped(".action.PREPARE_WAKE_HANDOFF")
    val screenCaptureForegroundStart = scoped(".action.SCREEN_CAPTURE_FGS_START")
    val setWakeListeningSuspendedForFloatingFullscreen =
        scoped(".action.SET_WAKE_LISTENING_SUSPENDED_FOR_FLOATING_FULLSCREEN")
    val setWakeListeningSuspendedForIme =
        scoped(".action.SET_WAKE_LISTENING_SUSPENDED_FOR_IME")
    val showerBinderReady = scoped(".action.SHOWER_BINDER_READY")
    val startOrRefreshExternalHttp = scoped(".action.START_OR_REFRESH_EXTERNAL_HTTP")
    val stopExternalHttp = scoped(".action.STOP_EXTERNAL_HTTP")
    val toggleWakeListening = scoped(".action.TOGGLE_WAKE_LISTENING")

    val debugInstallToolPkg = scoped(".DEBUG_INSTALL_TOOLPKG")
    val debugRefreshPackages = scoped(".DEBUG_REFRESH_PACKAGES")
    val dumpComposeDslUi = scoped(".DUMP_COMPOSE_DSL_UI")
    val executeJs = scoped(".EXECUTE_JS")
    val externalChat = scoped(".EXTERNAL_CHAT")
    val externalChatResult = scoped(".EXTERNAL_CHAT_RESULT")
    val triggerWorkflow = scoped(".TRIGGER_WORKFLOW")
    val workflowResult = scoped(".WORKFLOW_RESULT")

    val openRouteArgsJsonExtra = scoped(".extra.OPEN_ROUTE_ARGS_JSON")
    val openRouteIdExtra = scoped(".extra.OPEN_ROUTE_ID")
}

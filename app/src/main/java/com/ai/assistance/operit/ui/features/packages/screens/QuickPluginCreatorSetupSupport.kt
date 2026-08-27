package com.ai.assistance.operit.ui.features.packages.screens

import android.content.Context
import com.ai.assistance.operit.R
import com.ai.assistance.operit.core.tools.AIToolHandler
import com.ai.assistance.operit.core.tools.StringResultData
import com.ai.assistance.operit.core.tools.packTool.PackageManager
import com.ai.assistance.operit.data.model.AITool
import com.ai.assistance.operit.data.model.ToolParameter
import com.ai.assistance.operit.data.model.ToolResult
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.OperitPaths
import java.io.File

private const val OPERIT_EDITOR_PACKAGE_NAME = "operit_editor"
private const val SANDBOX_PACKAGE_DEV_INSTALL_SCRIPT_ASSET =
    "tools/sandboxpackage_dev_install_or_update.js"

internal fun runQuickPluginCreatorSetup(
    context: Context,
    packageManager: PackageManager,
    toolHandler: AIToolHandler
): ToolResult {
    return try {
        val scriptFile = copyBundledSandboxPackageDevInstallScript(context)
        val enableMessage = packageManager.enablePackage(OPERIT_EDITOR_PACKAGE_NAME)
        if (enableMessage.startsWith("Package not found", ignoreCase = true)) {
            return ToolResult(
                toolName = "$OPERIT_EDITOR_PACKAGE_NAME:debug_run_sandbox_script",
                success = false,
                result = StringResultData(""),
                error = enableMessage
            )
        }

        val result =
            toolHandler.executeTool(
                AITool(
                    name = "$OPERIT_EDITOR_PACKAGE_NAME:debug_run_sandbox_script",
                    parameters =
                        listOf(
                            ToolParameter(
                                name = "source_path",
                                value = scriptFile.absolutePath
                            )
                        )
                )
            )

        if (!result.success) {
            ToolResult(
                toolName = result.toolName,
                success = false,
                result = StringResultData(""),
                error = result.error ?: context.getString(R.string.quick_plugin_creator_setup_failed)
            )
        } else {
            ToolResult(
                toolName = result.toolName,
                success = true,
                result = StringResultData(context.getString(R.string.quick_plugin_creator_setup_success))
            )
        }
    } catch (e: Exception) {
        AppLogger.e("QuickPluginCreatorSetup", "Failed to run quick plugin creator setup", e)
        ToolResult(
            toolName = "$OPERIT_EDITOR_PACKAGE_NAME:debug_run_sandbox_script",
            success = false,
            result = StringResultData(""),
            error = e.message ?: e.javaClass.simpleName
        )
    }
}

private fun copyBundledSandboxPackageDevInstallScript(context: Context): File {
    val scriptFile = File(
        OperitPaths.skillsDir(),
        "SandboxPackage_DEV/scripts/install_or_update.js",
    )
    scriptFile.parentFile?.mkdirs()

    context.assets.open(SANDBOX_PACKAGE_DEV_INSTALL_SCRIPT_ASSET).use { input ->
        scriptFile.outputStream().use { output -> input.copyTo(output) }
    }

    return scriptFile
}

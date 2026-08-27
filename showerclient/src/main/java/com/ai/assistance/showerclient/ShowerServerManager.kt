package com.ai.assistance.showerclient

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Helper to manage the lifecycle of the Shower server (shower-server.jar) on the device.
 *
 * This implementation is host-agnostic: it relies only on [ShellRunner] and Binder
 * registration via [ShowerBinderRegistry]. The host app is responsible for:
 * - Providing a [ShellRunner] via [ShowerEnvironment.shellRunner]
 * - Packaging `shower-server.jar` into its assets
 */
object ShowerServerManager {

    private const val TAG = "ShowerServerManager"
    private const val ASSET_JAR_NAME = "shower-server.jar"
    private const val LOCAL_JAR_NAME = "shower-server.jar"

    @Volatile
    var additionalTargetPackages: Set<String> = emptySet()

    private data class ServerInstance(
        val packageName: String,
        val token: String,
    ) {
        val remoteJarPath = "/data/local/tmp/shower-server-$token.jar"
        val remoteLogPath = "/data/local/tmp/shower-$token.log"
        val remotePidPath = "/data/local/tmp/shower-$token.pid"
    }

    private fun serverInstance(context: Context): ServerInstance {
        val packageName = context.applicationContext.packageName
        require(packageName.matches(Regex("[A-Za-z0-9_.]+"))) {
            "Unsupported application id for Shower server: $packageName"
        }
        return ServerInstance(
            packageName = packageName,
            token = packageName.replace('.', '-'),
        )
    }

    private fun stopCommand(instance: ServerInstance): String {
        val pid = "${'$'}pid"
        val cmdline = "${'$'}cmdline"
        return "pid=${'$'}(cat ${instance.remotePidPath} 2>/dev/null || true); " +
            "case \"$pid\" in ''|*[!0-9]*) ;; *) " +
            "if [ -r /proc/$pid/cmdline ]; then " +
            "cmdline=${'$'}(tr '\\000' ' ' < /proc/$pid/cmdline); " +
            "case \"$cmdline\" in " +
            "*\"com.ai.assistance.shower.Main ${instance.packageName} ${instance.remoteLogPath}\"*) " +
            "kill \"$pid\" ;; esac; fi ;; esac; rm -f ${instance.remotePidPath}"
    }

    /**
     * Ensure the Shower server is started in the background.
     * Returns true if the start command was issued successfully and a Binder
     * was received within the timeout window.
     */
    suspend fun ensureServerStarted(context: Context): Boolean {
        // 0) If we already have an alive Binder from the handoff broadcast, just reuse it.
        if (ShowerBinderRegistry.hasAliveService()) {
            ShowerLog.d(TAG, "Shower Binder already cached and alive, skipping start")
            return true
        }

        val runner = ShowerEnvironment.shellRunner
        if (runner == null) {
            ShowerLog.e(TAG, "No ShellRunner configured in ShowerEnvironment; cannot start server")
            return false
        }

        val appContext = context.applicationContext
        val instance = serverInstance(appContext)
        val jarFile = try {
            copyJarToExternalDir(appContext)
        } catch (e: Exception) {
            ShowerLog.e(TAG, "Failed to copy shower-server.jar from assets", e)
            return false
        }

        // 1) Kill existing server (ignore errors about missing process).
        val killCmd = stopCommand(instance)
        ShowerLog.d(TAG, "Stopping existing Shower server (if any) with command: $killCmd")
        runner.run(killCmd, ShellIdentity.DEFAULT)

        // 2) With highest available identity, remove any stale jar and log under /data/local/tmp.
        val remoteJarPath = instance.remoteJarPath
        val cleanupCmd = "rm -f $remoteJarPath ${instance.remoteLogPath} || true"
        ShowerLog.d(TAG, "Cleaning up previous Shower jar and log with command: $cleanupCmd")
        val cleanupResult = runner.run(cleanupCmd, ShellIdentity.DEFAULT)
        if (!cleanupResult.success) {
            ShowerLog.w(
                TAG,
                "Cleanup of Shower jar/log may have failed (exitCode=${cleanupResult.exitCode}). stdout='${cleanupResult.stdout}', stderr='${cleanupResult.stderr}'"
            )
        }

        // 3) Copy the jar from this host package's isolated Downloads staging directory to
        // /data/local/tmp using shell identity,
        // so that the resulting file is owned by the shell user.
        val copyCmd = "cp ${jarFile.absolutePath} $remoteJarPath"
        ShowerLog.d(TAG, "Copying Shower jar with shell identity using command: $copyCmd")
        val copyResult = runner.run(copyCmd, ShellIdentity.SHELL)
        if (!copyResult.success) {
            ShowerLog.e(
                TAG,
                "Failed to copy Shower jar to $remoteJarPath (exitCode=${copyResult.exitCode}). stdout='${copyResult.stdout}', stderr='${copyResult.stderr}'"
            )
            return false
        }

        // 4) Start app_process with CLASSPATH pointing to /data/local/tmp/shower-server.jar, in background.
        val targetPackagesArg = appContext.packageName
        val startCmd =
            "CLASSPATH=$remoteJarPath app_process / com.ai.assistance.shower.Main " +
                "$targetPackagesArg ${instance.remoteLogPath} >/dev/null 2>&1 & " +
                "echo ${'$'}! > ${instance.remotePidPath}"
        ShowerLog.d(TAG, "Starting Shower server with command: $startCmd")
        val startResult = runner.run(startCmd, ShellIdentity.SHELL)
        if (!startResult.success) {
            ShowerLog.e(
                TAG,
                "Failed to start Shower server (exitCode=${startResult.exitCode}). stdout='${startResult.stdout}', stderr='${startResult.stderr}'"
            )
            return false
        }

        // 5) Poll for up to 10 seconds for the Binder handoff broadcast to be received and cached.
        for (attempt in 0 until 50) { // 50 * 200ms = 10s
            delay(200)
            if (ShowerBinderRegistry.hasAliveService()) {
                ShowerLog.d(
                    TAG,
                    "Shower Binder cached and alive after ~${(attempt + 1) * 200}ms"
                )
                return true
            }
        }

        ShowerLog.e(TAG, "Shower Binder was not received within the expected time")
        return false
    }

    /**
     * Stop the Shower server process if running.
     */
    suspend fun stopServer(context: Context): Boolean {
        val runner = ShowerEnvironment.shellRunner
        if (runner == null) {
            ShowerLog.e(TAG, "No ShellRunner configured in ShowerEnvironment; cannot stop server")
            return false
        }
        val cmd = stopCommand(serverInstance(context))
        val result = runner.run(cmd, ShellIdentity.DEFAULT)
        if (!result.success) {
            ShowerLog.e(TAG, "Failed to stop Shower server: ${result.stderr}")
        }
        return result.success
    }

    /**
     * Copy shower-server.jar from assets to an external directory.
     * Host apps can override this behaviour by providing a different wrapper
     * around [ShellRunner] if needed.
     */
    private suspend fun copyJarToExternalDir(context: Context): File = withContext(Dispatchers.IO) {
        val baseDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            context.packageName,
        )
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        val outFile = File(baseDir, LOCAL_JAR_NAME)
        context.assets.open(ASSET_JAR_NAME).use { input ->
            FileOutputStream(outFile).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        }
        ShowerLog.d(TAG, "Copied $ASSET_JAR_NAME to ${outFile.absolutePath}")
        outFile
    }
}

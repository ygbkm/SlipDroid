package com.github.ygbkm.slipdroid

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val sdk33_Tiramisu = 33
const val sdk34_UpsideDownCake = 34

const val bgServiceNotifChanID = "background_service"
const val bgServiceNotifChanName = "Background service"
const val bgServiceNotifTitle = "SlipDroid"
const val bgServiceNotifText = "Proxy is running."

class MainApp : Application() {
	override fun onCreate() {
		super.onCreate()
		val notifChan = NotificationChannel(
			bgServiceNotifChanID,
			bgServiceNotifChanName,
			NotificationManager.IMPORTANCE_MIN,
		)
		val notifMgr = getSystemService(NotificationManager::class.java)
		notifMgr.createNotificationChannel(notifChan)
	}
}

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		// if (Build.VERSION.SDK_INT >= sdk34_UpsideDownCake) {
		// 	ActivityCompat.requestPermissions(
		// 		this,
		// 		arrayOf(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE),
		// 		0,
		// 	)
		// }
		if (Build.VERSION.SDK_INT >= sdk33_Tiramisu) {
			ActivityCompat.requestPermissions(
				this,
				arrayOf(Manifest.permission.POST_NOTIFICATIONS),
				0,
			)
		}
		setContent {
			MaterialTheme {
				MainScreen()
			}
		}
	}
}

class RunnerService : Service() {

	enum class Intents {
		Start,
		StartArgs,
		Log,
		LogData,
		Stop,
		Stopped,
	}

	private var job: Job? = null
	private val scope = CoroutineScope(Dispatchers.IO)

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		when (intent?.action) {
			Intents.Start.toString() -> start(intent)
			Intents.Stop.toString() -> stop()
		}
		return super.onStartCommand(intent, flags, startId)
	}

	override fun onDestroy() {
		stop()
		super.onDestroy()
	}

	private fun stop() {
		try {
			broadcastLog("stopping process...")
			NativeLib.stopClient()
		} catch (e: Exception) {
			broadcastLog("error: ${e.message}")
			e.printStackTrace()
		}
		job?.cancel()
		job = null
		broadcastLog("stopped process.")
		broadcastStopped()
		stopSelf()
	}

	private fun start(intent: Intent?) {

		if (Build.VERSION.SDK_INT >= sdk34_UpsideDownCake) {
			val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
			startForeground(1, notif(), type)
		} else {
			startForeground(1, notif())
		}

		try {
			NativeLib.initLogger()
		} catch (_: Exception) {
			// Already initialized.
		}

		val args = intent?.getStringExtra(Intents.StartArgs.toString()) ?: ""

		job = scope.launch {
			try {
				broadcastLog("starting process...")
				NativeLib.startClient(args)
			} catch (e: Exception) {
				broadcastLog("error: ${e.message}")
				e.printStackTrace()
			}
			broadcastLog("started process.")
		}
	}

	private fun broadcastStopped() {
		val intent = Intent(Intents.Stopped.toString())
		intent.setPackage(packageName)
		sendBroadcast(intent)
	}

	private fun broadcastLog(log: String) {
		val intent = Intent(Intents.Log.toString())
		intent.putExtra(Intents.LogData.toString(), log)
		intent.setPackage(packageName)
		sendBroadcast(intent)
	}

	private fun notif(): Notification {
		return NotificationCompat.Builder(this, bgServiceNotifChanID)
			.setContentTitle(bgServiceNotifTitle)
			.setContentText(bgServiceNotifText)
			.setSmallIcon(R.drawable.ic_launcher_foreground)
			.build()
	}
}

// if (Build.VERSION.SDK_INT >= sdk34_UpsideDownCake) {
// 	//val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_
// 	//startForeground(svcId, notif, type)
// 	startForeground(1, notif())
// } else {
// 	startForeground(1, notif())
// }


// 	if (intent?.action == ACTION_STOP_SERVICE) {
// 		stopProcess()
// 		stopForeground(STOP_FOREGROUND_REMOVE)
// 		stopSelf()
// 		return START_NOT_STICKY
// 	}
//
// 	val args = intent?.getStringExtra("args") ?: ""
// 	startForeground(1, createNotification("Running binary..."))
//
// 	// Run the binary in a background thread
// 	job = scope.launch {
// 		try {
// 			val binaryFile = setupBinary()
// 			broadcastLog("Binary path: ${binaryFile.absolutePath}")
//
// 			// Construct command
// 			// NOTE: We split arguments by space strictly here.
// 			// A real app might need a regex for quoted args.
// 			val command = mutableListOf(binaryFile.absolutePath)
// 			if (args.isNotBlank()) {
// 				command.addAll(args.split("\\s+".toRegex()))
// 			}
//
// 			broadcastLog("Executing: $command")
//
// 			val pb = ProcessBuilder(command)
// 			pb.directory(filesDir)
// 			// Merge stderr into stdout so we only need one reader
// 			pb.redirectErrorStream(true)
//
// 			process = pb.start()
//
// 			// Read output
// 			val reader = BufferedReader(InputStreamReader(process!!.inputStream))
// 			var line: String?
// 			while (reader.readLine().also { line = it } != null) {
// 				broadcastLog(line ?: "")
// 			}
//
// 			val exitCode = process?.waitFor()
// 			broadcastLog("Process finished with exit code: $exitCode")
//
// 		} catch (e: Exception) {
// 			broadcastLog("Error: ${e.message}")
// 			e.printStackTrace()
// 		} finally {
// 			// Notify UI that we are done
// 			val finishIntent = Intent(ACTION_LOG).apply {
// 				putExtra("finished", true)
// 			}
// 			sendBroadcast(finishIntent)
// 			stopForeground(STOP_FOREGROUND_REMOVE)
// 			stopSelf()
// 		}
// 	}
//
// 	return START_STICKY
// }

// private fun stopProcess() {
// 	if (process != null && process!!.isAlive) {
// 		broadcastLog("Stopping process...")
// 		process?.destroy()
// 	}
// 	job?.cancel()
// }

// // Copy binary from assets to internal storage and make executable
// private fun setupBinary(): File {
// 	val file = File(filesDir, BINARY_NAME)
// 	// Optimization: Only copy if doesn't exist or you want to force update
// 	// For development, we overwrite every time to ensure latest binary
// 	if (file.exists()) file.delete()
//
// 	assets.open(BINARY_NAME).use { input ->
// 		file.outputStream().use { output ->
// 			input.copyTo(output)
// 		}
// 	}
//
// 	// Make executable (chmod 755)
// 	file.setExecutable(true, true)
// 	return file
// }
//
// private fun broadcastLog(message: String) {
// 	val intent = Intent(ACTION_LOG)
// 	intent.putExtra(EXTRA_LOG_DATA, message)
// 	intent.setPackage(packageName) // Security: restrict broadcast to own app
// 	sendBroadcast(intent)
// }
//
// private fun createNotificationChannel() {
// 	val channel = NotificationChannel(
// 		CHANNEL_ID,
// 		"Binary Runner Service",
// 		NotificationManager.IMPORTANCE_LOW
// 	)
// 	val manager = getSystemService(NotificationManager::class.java)
// 	manager.createNotificationChannel(channel)
// }
//
// private fun createNotification(contentText: String): Notification {
// 	val notificationIntent = Intent(this, MainActivity::class.java)
// 	val pendingIntent = PendingIntent.getActivity(
// 		this, 0, notificationIntent,
// 		PendingIntent.FLAG_IMMUTABLE
// 	)
//
// 	return NotificationCompat.Builder(this, CHANNEL_ID)
// 		.setContentTitle("Binary Runner")
// 		.setContentText(contentText)
// 		.setSmallIcon(android.R.drawable.ic_media_play) // Use standard icon
// 		.setContentIntent(pendingIntent)
// 		.setOngoing(true)
// 		.build()
// }
//

// private fun broadcastMsg(msg: String) {
// 	val intent = Intent(Intents.Msg.toString())
// 	intent.putExtra(Intents.MsgData.toString(), msg)
// 	intent.setPackage(packageName)
// 	sendBroadcast(intent)
// }

//const val binaryName = "slipstream-client"
// private fun binaryFile(): File {
// 	val file = File(filesDir, binaryName)
// 	if (file.exists()) {
// 		return file
// 	}
// 	println("begin-asset-list")
// 	println(assets.list("."))
// 	println("end-asset-list")
// 	val input = assets.open(binaryName)
// 	val output = file.outputStream()
// 	input.copyTo(output)
// 	input.close()
// 	file.setExecutable(true)
// 	return file
// }

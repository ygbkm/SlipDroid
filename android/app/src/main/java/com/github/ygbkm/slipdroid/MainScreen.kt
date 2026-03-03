package com.github.ygbkm.slipdroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun MainScreen() {
	val context = LocalContext.current
	var inputArgs by remember { mutableStateOf("--help") }
	var isRunning by remember { mutableStateOf(false) }
	var logs by remember { mutableStateOf(listOf<String>()) }
	val listState = rememberLazyListState()

	DisposableEffect(Unit) {
		val receiver = object : BroadcastReceiver() {
			override fun onReceive(ctx: Context?, intent: Intent?) {
				if (intent?.action == RunnerService.Intents.Stopped.toString()) {
					isRunning = false
				}
				if (intent?.action == RunnerService.Intents.Log.toString()) {
					val s = intent.getStringExtra(RunnerService.Intents.LogData.toString())!!
					logs += s
				}
			}
		}
		val filter = IntentFilter().apply {
			addAction(RunnerService.Intents.Stopped.toString())
			addAction(RunnerService.Intents.Log.toString())
		}
		ContextCompat.registerReceiver(
			context,
			receiver,
			filter,
			ContextCompat.RECEIVER_NOT_EXPORTED
		)
		onDispose {
			context.unregisterReceiver(receiver)
		}
	}

	// Scroll to bottom when logs update
	LaunchedEffect(logs.size) {
		if (logs.isNotEmpty()) {
			listState.animateScrollToItem(logs.size - 1)
		}
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp)
	) {
		Text("Binary Runner", style = MaterialTheme.typography.headlineMedium)

		Spacer(modifier = Modifier.height(16.dp))

		OutlinedTextField(
			value = inputArgs,
			onValueChange = { inputArgs = it },
			label = { Text("Arguments") },
			modifier = Modifier.fillMaxWidth(),
			enabled = !isRunning
		)

		Spacer(modifier = Modifier.height(16.dp))

		Button(
			onClick = {
				if (isRunning) {
					val intent = Intent(context, RunnerService::class.java)
					intent.action = RunnerService.Intents.Stop.toString()
					context.startService(intent)
				} else {
					val intent = Intent(context, RunnerService::class.java)
					intent.action = RunnerService.Intents.Start.toString()
					intent.putExtra(RunnerService.Intents.StartArgs.toString(), inputArgs)
					context.startService(intent)
					isRunning = true
				}
			},
			modifier = Modifier.fillMaxWidth(),
			colors = ButtonDefaults.buttonColors(
				containerColor = if (isRunning)
					MaterialTheme.colorScheme.error else
					MaterialTheme.colorScheme.primary
			)
		) {
			Text(
				if (isRunning)
					"Stop Process" else
					"Start Process"
			)
		}

		Spacer(modifier = Modifier.height(16.dp))
		Text("Logs:", style = MaterialTheme.typography.labelLarge)

		Box(
			modifier = Modifier
				.weight(1f)
				.fillMaxWidth()
				.background(Color(0xFFEEEEEE))
				.padding(8.dp)
		) {
			LazyColumn(state = listState) {
				items(logs) { log ->
					Text(
						text = log,
						fontFamily = FontFamily.Monospace,
						fontSize = 12.sp
					)
				}
			}
		}
	}
}

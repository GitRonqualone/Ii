package com.example.aiassistant.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiassistant.data.ChatMessage
import com.example.aiassistant.ui.theme.Accent
import com.example.aiassistant.ui.theme.Bg
import com.example.aiassistant.ui.theme.BubbleUser
import com.example.aiassistant.ui.theme.Panel
import com.example.aiassistant.ui.theme.TextPrimary
import com.example.aiassistant.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app", Context.MODE_PRIVATE) }

    var showSettings by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var attachment by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf("Обычный") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        vm.apiKey.value = prefs.getString("api_key", "") ?: ""
        prefs.getString("model", null)?.let { vm.model.value = it }
    }

    LaunchedEffect(vm.messages.size, vm.messages.lastOrNull()?.content?.length) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.size - 1)
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { attachment = displayName(context, it) }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrEmpty()) {
            input = TextFieldValue(input.text + spoken)
        }
    }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите…")
        }
        runCatching { voiceLauncher.launch(intent) }
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text("AI Ассистент") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.messages, key = { it.id }) { msg ->
                    MessageBubble(msg)
                }
            }

            vm.error.value?.let {
                Text(
                    text = it,
                    color = Color(0xFFFF7A7A),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Surface(
                color = Panel,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {

                    attachment?.let { name ->
                        Surface(color = BubbleUser, shape = RoundedCornerShape(10.dp)) {
                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.InsertDriveFile, contentDescription = null,
                                    modifier = Modifier.size(16.dp), tint = TextSecondary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    modifier = Modifier.widthIn(max = 200.dp)
                                )
                                IconButton(onClick = { attachment = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        Icons.Outlined.Close, contentDescription = "Убрать",
                                        modifier = Modifier.size(14.dp), tint = TextSecondary
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { pickFile.launch("*/*") },
                            enabled = !vm.busy.value
                        ) {
                            Icon(
                                Icons.Outlined.AttachFile,
                                contentDescription = "Прикрепить файл",
                                tint = TextSecondary
                            )
                        }

                        TextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = {
                                Text("Спросите что угодно…", color = TextSecondary)
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        if (input.text.isBlank() && attachment == null) {
                            IconButton(onClick = { startVoiceInput() }) {
                                Icon(
                                    Icons.Outlined.Mic,
                                    contentDescription = "Голосовой ввод",
                                    tint = TextSecondary
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    vm.send(input.text.trim(), attachment)
                                    input = TextFieldValue("")
                                    attachment = null
                                },
                                enabled = !vm.busy.value
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Send,
                                    contentDescription = "Отправить",
                                    tint = Accent
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var modeMenu by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { modeMenu = true }) {
                                Text(
                                    "Режим: $mode",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Icon(
                                    Icons.Outlined.KeyboardArrowDown, contentDescription = null,
                                    tint = TextSecondary, modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(expanded = modeMenu, onDismissRequest = { modeMenu = false }) {
                                listOf("Обычный", "Выполнение задачи").forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = { mode = m; modeMenu = false }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        var modelMenu by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { modelMenu = true }) {
                                Text(
                                    shortName(vm.model.value),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                                Icon(
                                    Icons.Outlined.KeyboardArrowDown, contentDescription = null,
                                    tint = TextSecondary, modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                                ChatViewModel.MODELS.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = {
                                            vm.model.value = m
                                            prefs.edit().putString("model", m).apply()
                                            modelMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        var key by remember { mutableStateOf(vm.apiKey.value) }
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Настройки") },
            text = {
                Column {
                    Text(
                        "API-ключ Moonshot AI (platform.moonshot.ai → API Keys):",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.apiKey.value = key.trim()
                    prefs.edit().putString("api_key", key.trim()).apply()
                    showSettings = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Box(Modifier.fillMaxWidth()) {
        Surface(
            color = if (isUser) BubbleUser else Color.Transparent,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(if (isUser) Alignment.CenterEnd else Alignment.CenterStart)
                .widthIn(max = 340.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                msg.attachmentName?.let { name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.InsertDriveFile, contentDescription = null,
                            modifier = Modifier.size(14.dp), tint = Accent
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            name,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = msg.content + if (msg.streaming) " ▍" else "",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun shortName(model: String) = model.substringAfterLast("/").ifBlank { model }

private fun displayName(context: Context, uri: Uri): String {
    var name = "файл"
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx) ?: name
            }
        }
    }
    return name
}

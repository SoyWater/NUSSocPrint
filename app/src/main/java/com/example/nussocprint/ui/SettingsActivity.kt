// SettingsActivity.kt
package com.example.nussocprint.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.nussocprint.ui.theme.NUSSocPrintTheme
import com.example.nussocprint.util.EncryptedDataStore
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NUSSocPrintTheme() {
                SettingsScreen(
                    onSave = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSave: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Load existing credentials when the screen appears
    LaunchedEffect(Unit) {
        try {
            val creds = EncryptedDataStore.getCredentials(context)
            creds?.let {
                username = it.username
                password = it.password
            }
        } catch (_: Exception) {
            // ignore; if not initialized or decrypt failed we just leave fields empty
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SMB Credentials") },
                navigationIcon = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Configure your NUS account used by the SoC print service.",
                style = MaterialTheme.typography.bodyMedium
            )

            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e1234543") }
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "Hide" else "Show")
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter your NUS password") }
                    )

                    Text(
                        "Stored credentials are encrypted using Android Keystore and kept only on-device.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (username.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Username is required") }
                            return@Button
                        }

                        isSaving = true
                        scope.launch {
                            try {
                                EncryptedDataStore.saveCredentials(context, username.trim(), password)
                                snackbarHostState.showSnackbar("Credentials saved securely")
                                onSave()
                            } catch (_: Exception) {
                                snackbarHostState.showSnackbar("Failed to save credentials")
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Save")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            EncryptedDataStore.clearCredentials(context)
                            username = ""
                            password = ""
                            snackbarHostState.showSnackbar("Credentials cleared")
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    Spacer(Modifier.width(8.dp))
                    Text("Clear")
                }
            }

            // Extra spacing at the bottom
            Spacer(Modifier.weight(1f))
        }
    }
}
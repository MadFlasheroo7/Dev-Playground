package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CryptoType { SYMMETRIC, ASYMMETRIC }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoVisualizerScreen(onBack: () -> Unit) {
    var selectedType by remember { mutableStateOf(CryptoType.SYMMETRIC) }
    val scope = rememberCoroutineScope()

    // Simulation states
    var plainText by remember { mutableStateOf("Secret Message") }
    var cipherText by remember { mutableStateOf("Secret Message") }
    var isLocked by remember { mutableStateOf(false) }
    var logMessage by remember { mutableStateOf("System ready.") }
    var lastUsedKeyName by remember { mutableStateOf("(none)") }

    var logs = remember { mutableStateListOf<String>() }

    fun resetSim() {
        isLocked = false
        cipherText = plainText
        logMessage = "Simulation reset."
        lastUsedKeyName = "(none)"
        logs.clear()
        logs.add("System initialized.")
    }

    LaunchedEffect(selectedType) {
        resetSim()
    }

    // Encrypt Helper
    fun encryptData() {
        scope.launch {
            if (isLocked) return@launch
            logMessage = "Encrypting..."
            delay(600)
            isLocked = true
            cipherText = "U2VjcmV0TWVzc2FnZQ==" // base64 representation of Secret Message
            lastUsedKeyName = if (selectedType == CryptoType.SYMMETRIC) "Shared Secret Key" else "Alice's Public Key"
            logs.add("Encryption complete. Key used: $lastUsedKeyName")
            logs.add("Payload is now encrypted cipher: $cipherText")
            logMessage = "Encrypted successfully."
        }
    }

    // Decrypt Helper
    fun decryptData(useCorrectKey: Boolean) {
        scope.launch {
            if (!isLocked) return@launch
            logMessage = "Attempting Decryption..."
            delay(600)

            if (selectedType == CryptoType.SYMMETRIC) {
                if (useCorrectKey) {
                    isLocked = false
                    cipherText = plainText
                    lastUsedKeyName = "Shared Secret Key"
                    logs.add("Decrypted successfully using Shared Key!")
                    logMessage = "Decrypted successfully."
                } else {
                    logs.add("Decryption failed! Wrong key.")
                    logMessage = "Error: Invalid Key!"
                }
            } else {
                // Asymmetric
                if (useCorrectKey) {
                    isLocked = false
                    cipherText = plainText
                    lastUsedKeyName = "Alice's Private Key"
                    logs.add("Decrypted successfully using Alice's Private Key!")
                    logMessage = "Decrypted successfully."
                } else {
                    logs.add("Decryption failed! You cannot decrypt Asymmetric chests using Alice's Public Key (Eve failed).")
                    logMessage = "Eve intercepted but failed to unlock!"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Symmetric vs Asymmetric Cryptography", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Crypto Selector Tab
            TabRow(selectedTabIndex = selectedType.ordinal) {
                CryptoType.values().forEach { type ->
                    Tab(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        text = {
                            Text(
                                text = type.name,
                                fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Encryption Chest Card Visualizer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${selectedType.name} Chest Visualizer",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(Modifier.height(16.dp))

                    // Simulated Chest Box
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(
                                if (isLocked) Color(0xFFC62828).copy(alpha = 0.8f) else Color(0xFF2E7D32).copy(alpha = 0.8f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(3.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isLocked) "LOCKED 🔒" else "OPEN 🔓",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Data: $cipherText",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Last Action status: $logMessage",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cryptography Commands", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    if (selectedType == CryptoType.SYMMETRIC) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { encryptData() },
                                modifier = Modifier.weight(1.2f),
                                enabled = !isLocked
                            ) {
                                Text("Encrypt Shared Key")
                            }

                            Button(
                                onClick = { decryptData(true) },
                                modifier = Modifier.weight(1.2f),
                                enabled = isLocked
                            ) {
                                Text("Decrypt Shared Key")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { decryptData(false) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            enabled = isLocked
                        ) {
                            Text("Decrypt (Attack with Wrong Key)")
                        }
                    } else {
                        // Asymmetric Key Pair
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { encryptData() },
                                modifier = Modifier.weight(1f),
                                enabled = !isLocked
                            ) {
                                Text("Bob: Lock (Public Key)")
                            }

                            Button(
                                onClick = { decryptData(true) },
                                modifier = Modifier.weight(1.1f),
                                enabled = isLocked
                            ) {
                                Text("Alice: Unlock (Private Key)")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { decryptData(false) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            enabled = isLocked
                        ) {
                            Text("Eve: Intercept (Attack with Public Key)")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Explanation & Snippets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tutorial card
                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Concept Lesson",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = getCryptoLesson(selectedType),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Code snippet card
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Cryptographic Code Example",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF80CBC4),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = getCryptoCode(selectedType),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFECEFF1)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Logs
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cryptography Log Stream", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clip(RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        val logScrollState = rememberScrollState()
                        LaunchedEffect(logs.size) {
                            logScrollState.animateScrollTo(logScrollState.maxValue)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(logScrollState)
                        ) {
                            logs.forEach { log ->
                                Text(
                                    text = log,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

fun getCryptoLesson(type: CryptoType): String {
    return when (type) {
        CryptoType.SYMMETRIC -> {
            "Symmetric Cryptography uses a single shared secret key to both encrypt and decrypt data.\n\n" +
            "Key Observations:\n" +
            "• Simple and fast.\n" +
            "• Vulnerable: Both parties must share the exact same key. Intercepting the key exposes all future communications."
        }
        CryptoType.ASYMMETRIC -> {
            "Asymmetric Cryptography uses a mathematically linked Key Pair (Public Key + Private Key).\n\n" +
            "Key Observations:\n" +
            "• Public Key: Openly shared. Used ONLY to lock (encrypt) data.\n" +
            "• Private Key: Kept secret. Used ONLY to unlock (decrypt) data.\n" +
            "• Attackers (Eve) who intercept the public key cannot unlock the chest, defeating passive interception."
        }
    }
}

fun getCryptoCode(type: CryptoType): String {
    return when (type) {
        CryptoType.SYMMETRIC -> {
            "// Symmetric AES Key\n" +
            "val key = generateAESKey()\n\n" +
            "// Alice Encrypts\n" +
            "val cipher = encrypt(data, key)\n\n" +
            "// Bob Decrypts using SAME key\n" +
            "val decrypted = decrypt(cipher, key)"
        }
        CryptoType.ASYMMETRIC -> {
            "// Alice's Key Pair\n" +
            "val keyPair = generateRSAKeyPair()\n" +
            "val pubKey = keyPair.publicKey\n" +
            "val privKey = keyPair.privateKey\n\n" +
            "// Bob locks using Alice's Public Key\n" +
            "val cipher = encrypt(data, pubKey)\n\n" +
            "// Alice unlocks using her Private Key\n" +
            "val result = decrypt(cipher, privKey)"
        }
    }
}

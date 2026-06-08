package pro.jayeshseth.devcontrols.ui.privacy

import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/** Biometric lock — platform BiometricPrompt gating a secret. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricLockScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var unlocked by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    fun authenticate() {
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt.Builder(context)
            .setTitle("Unlock secret")
            .setSubtitle("Authenticate with your biometric")
            .setNegativeButton("Cancel", executor) { _, _ -> status = "Cancelled" }
            .build()
        prompt.authenticate(CancellationSignal(), executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { unlocked = true; status = "Unlocked ✓" }
            override fun onAuthenticationError(code: Int, msg: CharSequence) { status = "Error: $msg" }
            override fun onAuthenticationFailed() { status = "Not recognized" }
        })
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Biometric Lock") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (unlocked) {
                Text("🔓", fontSize = 64.sp)
                Text("Top-secret value: 42-1337-CAFE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { unlocked = false; status = "" }) { Text("Lock again") }
            } else {
                Text("🔒", fontSize = 64.sp)
                Text("Authenticate to reveal the secret.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { authenticate() }) { Text("Unlock with biometrics") }
            }
            Spacer(Modifier.height(12.dp))
            Text(status, color = MaterialTheme.colorScheme.primary)
        }
    }
}

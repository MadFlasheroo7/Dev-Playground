package pro.jayeshseth.devcontrols.ui.privacy

import android.util.Base64
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Crypto Playground — AES-256-GCM encrypt/decrypt with a PBKDF2-derived key from a passphrase. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoPlaygroundScreen(onBack: () -> Unit) {
    var pass by remember { mutableStateOf("correct horse battery") }
    var plain by remember { mutableStateOf("secret message") }
    var cipher by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    fun key(salt: ByteArray) = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        .generateSecret(PBEKeySpec(pass.toCharArray(), salt, 120_000, 256)).encoded
        .let { SecretKeySpec(it, "AES") }

    fun encrypt() = runCatching {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key(salt), GCMParameterSpec(128, iv)) }
        val ct = c.doFinal(plain.toByteArray())
        cipher = Base64.encodeToString(salt + iv + ct, Base64.NO_WRAP); status = "Encrypted ✓"
    }.onFailure { status = "✗ ${it.message}" }

    fun decrypt() = runCatching {
        val all = Base64.decode(cipher, Base64.NO_WRAP)
        val salt = all.copyOfRange(0, 16); val iv = all.copyOfRange(16, 28); val ct = all.copyOfRange(28, all.size)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(salt), GCMParameterSpec(128, iv)) }
        plain = String(c.doFinal(ct)); status = "Decrypted ✓"
    }.onFailure { status = "✗ wrong passphrase or corrupt data" }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Crypto Playground") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(pass, { pass = it }, label = { Text("Passphrase") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(plain, { plain = it }, label = { Text("Plaintext") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row { Button(onClick = { encrypt() }) { Text("Encrypt ↓") }; Spacer(Modifier.width(8.dp)); OutlinedButton(onClick = { decrypt() }) { Text("Decrypt ↑") } }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(cipher, { cipher = it }, label = { Text("Ciphertext (base64)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(status, color = MaterialTheme.colorScheme.primary)
            Text("AES-256-GCM · PBKDF2-HMAC-SHA256 (120k iters). Salt+IV prepended.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
        }
    }
}

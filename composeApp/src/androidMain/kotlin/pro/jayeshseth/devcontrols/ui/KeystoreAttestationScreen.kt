package pro.jayeshseth.devcontrols.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher

enum class KeyAlgorithm(val label: String, val spec: String) {
    EC_P256("EC (P-256 / secp256r1)", "EC"),
    RSA_2048("RSA 2048-bit", "RSA"),
    RSA_4096("RSA 4096-bit", "RSA")
}

data class ParsedRootOfTrust(
    val deviceLocked: Boolean,
    val verifiedBootState: String,
    val bootStateColor: Color,
    val bootKeyHash: String,
    val verifiedBootHash: String?
)

data class ParsedAttestationRecord(
    val attestationVersion: Int,
    val attestationSecurityLevel: String,
    val attestationSecurityColor: Color,
    val keymasterVersion: Int,
    val keymasterSecurityLevel: String,
    val challengeHex: String,
    val challengeString: String,
    val osVersion: String?,
    val osPatchLevel: String?,
    val origin: String?,
    val rootOfTrust: ParsedRootOfTrust?
)

data class CertDetails(
    val index: Int,
    val type: String,
    val subject: String,
    val issuer: String,
    val serialNumber: String,
    val notBefore: String,
    val notAfter: String,
    val sigAlg: String,
    val sha256Fingerprint: String,
    val pem: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KeystoreAttestationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var keyAlias by remember { mutableStateOf("devcontrols_attest_key") }
    var selectedAlgorithm by remember { mutableStateOf(KeyAlgorithm.EC_P256) }
    var useStrongBox by remember { mutableStateOf(false) }
    var requireBiometrics by remember { mutableStateOf(true) }
    var challengeText by remember { mutableStateOf("DevControlsHardwareProbe") }

    var isGenerating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var isErrorStatus by remember { mutableStateOf(false) }

    var attestationRecord by remember { mutableStateOf<ParsedAttestationRecord?>(null) }
    var certChainList by remember { mutableStateOf<List<CertDetails>>(emptyList()) }
    var expandedCertIndex by remember { mutableStateOf<Int?>(0) }

    // Biometric demo states
    var selectedCryptoTab by remember { mutableStateOf(0) } // 0: Sign/Verify, 1: Encrypt/Decrypt
    var signInputText by remember { mutableStateOf("Proof of hardware presence") }
    var signatureResult by remember { mutableStateOf<String?>(null) }
    var signatureVerified by remember { mutableStateOf<Boolean?>(null) }
    var encryptInputText by remember { mutableStateOf("Sensitive hardware payload") }
    var encryptedCiphertext by remember { mutableStateOf<String?>(null) }
    var decryptedPlaintext by remember { mutableStateOf<String?>(null) }
    var isOperatingBiometric by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, content: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, content))
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    fun parseAttestationFromKey(alias: String) {
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val chain = ks.getCertificateChain(alias)
            if (chain.isNullOrEmpty()) {
                statusMessage = "Key '$alias' found, but no certificate chain is present."
                return
            }
            val certs = chain.mapIndexed { idx, c ->
                val x509 = c as X509Certificate
                val sha256 = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(x509.encoded)
                    .joinToString(":") { "%02X".format(it) }
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val pem = buildString {
                    appendLine("-----BEGIN CERTIFICATE-----")
                    appendLine(android.util.Base64.encodeToString(x509.encoded, android.util.Base64.NO_WRAP).chunked(64).joinToString("\n"))
                    appendLine("-----END CERTIFICATE-----")
                }
                CertDetails(
                    index = idx,
                    type = when (idx) {
                        0 -> "Leaf (Attested Key)"
                        chain.size - 1 -> "Root (Google Attestation CA)"
                        else -> "Intermediate CA #$idx"
                    },
                    subject = x509.subjectX500Principal.name,
                    issuer = x509.issuerX500Principal.name,
                    serialNumber = x509.serialNumber.toString(16).uppercase(),
                    notBefore = sdf.format(x509.notBefore),
                    notAfter = sdf.format(x509.notAfter),
                    sigAlg = x509.sigAlgName,
                    sha256Fingerprint = sha256,
                    pem = pem
                )
            }
            certChainList = certs

            val leaf = chain[0] as X509Certificate
            val extValue = leaf.getExtensionValue("1.3.6.1.4.1.11129.2.1.17")
            if (extValue != null) {
                attestationRecord = AttestationDecoder.decode(extValue)
                statusMessage = "Hardware attestation chain retrieved successfully (${certs.size} certificates)."
                isErrorStatus = false
            } else {
                attestationRecord = null
                statusMessage = "Key certificate generated, but Hardware Key Attestation extension (1.3.6.1.4.1.11129.2.1.17) is not present. (Device or key may not support hardware attestation)."
                isErrorStatus = false
            }
        } catch (e: Throwable) {
            statusMessage = "Failed to inspect attestation: ${e.message}"
            isErrorStatus = true
        }
    }

    fun generateHardwareKey() {
        scope.launch {
            isGenerating = true
            statusMessage = "Generating hardware-backed key with attestation..."
            isErrorStatus = false
            signatureResult = null
            signatureVerified = null

            withContext(Dispatchers.IO) {
                try {
                    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                    val challengeBytes = challengeText.toByteArray(Charsets.UTF_8)

                    fun generateKeyInternal(enableStrongBox: Boolean) {
                        if (keyStore.containsAlias(keyAlias)) {
                            keyStore.deleteEntry(keyAlias)
                        }
                        when (selectedAlgorithm) {
                            KeyAlgorithm.EC_P256 -> {
                                val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
                                val builder = KeyGenParameterSpec.Builder(
                                    keyAlias,
                                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                                )
                                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                                    .setAttestationChallenge(challengeBytes)

                                if (enableStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    builder.setIsStrongBoxBacked(true)
                                }
                                if (requireBiometrics) {
                                    builder.setUserAuthenticationRequired(true)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        builder.setUserAuthenticationValidityDurationSeconds(-1)
                                    }
                                }
                                kpg.initialize(builder.build())
                                kpg.generateKeyPair()
                            }
                            KeyAlgorithm.RSA_2048, KeyAlgorithm.RSA_4096 -> {
                                val keySize = if (selectedAlgorithm == KeyAlgorithm.RSA_2048) 2048 else 4096
                                val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
                                val builder = KeyGenParameterSpec.Builder(
                                    keyAlias,
                                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or
                                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                                )
                                    .setKeySize(keySize)
                                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                                    .setEncryptionPaddings(
                                        KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1,
                                        KeyProperties.ENCRYPTION_PADDING_RSA_OAEP
                                    )
                                    .setAttestationChallenge(challengeBytes)

                                if (enableStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    builder.setIsStrongBoxBacked(true)
                                }
                                if (requireBiometrics) {
                                    builder.setUserAuthenticationRequired(true)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        builder.setUserAuthenticationValidityDurationSeconds(-1)
                                    }
                                }
                                kpg.initialize(builder.build())
                                kpg.generateKeyPair()
                            }
                        }
                    }

                    var fellBackToTee = false
                    try {
                        generateKeyInternal(useStrongBox)
                    } catch (t: Throwable) {
                        val isStrongBoxUnavailable = t.javaClass.simpleName.contains("StrongBox", ignoreCase = true) ||
                                t.message?.contains("StrongBox", ignoreCase = true) == true
                        if (useStrongBox && isStrongBoxUnavailable) {
                            fellBackToTee = true
                            generateKeyInternal(false)
                        } else {
                            throw t
                        }
                    }
                    parseAttestationFromKey(keyAlias)
                    if (fellBackToTee) {
                        statusMessage = "StrongBox is unavailable on this device; generated hardware key using TEE fallback."
                        isErrorStatus = false
                    }
                } catch (t: Throwable) {
                    statusMessage = "Key generation failed: ${t.message ?: t.javaClass.simpleName}"
                    isErrorStatus = true
                } finally {
                    isGenerating = false
                }
            }
        }
    }

    fun executeBiometricSign() {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val privateKey = ks.getKey(keyAlias, null) as? PrivateKey
        val cert = ks.getCertificate(keyAlias)
        if (privateKey == null || cert == null) {
            Toast.makeText(context, "Generate or load key '$keyAlias' first!", Toast.LENGTH_SHORT).show()
            return
        }

        val sigAlg = if (selectedAlgorithm == KeyAlgorithm.EC_P256) "SHA256withECDSA" else "SHA256withRSA"
        val signature = Signature.getInstance(sigAlg)
        signature.initSign(privateKey)

        if (requireBiometrics) {
            isOperatingBiometric = true
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt.Builder(context)
                .setTitle("Hardware Auth per Use")
                .setSubtitle("Biometric challenge for key '$keyAlias'")
                .setDescription("Authorizing cryptographic signature operation inside TEE/StrongBox")
                .setNegativeButton("Cancel", executor) { _, _ ->
                    isOperatingBiometric = false
                    statusMessage = "Biometric prompt cancelled"
                }
                .build()

            val cryptoObject = BiometricPrompt.CryptoObject(signature)
            prompt.authenticate(
                cryptoObject,
                CancellationSignal(),
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        isOperatingBiometric = false
                        try {
                            val authenticatedSig = result.cryptoObject?.signature ?: signature
                            authenticatedSig.update(signInputText.toByteArray(Charsets.UTF_8))
                            val sigBytes = authenticatedSig.sign()
                            val sigHex = sigBytes.joinToString("") { "%02x".format(it) }
                            signatureResult = sigHex

                            // Verify signature
                            val verifier = Signature.getInstance(sigAlg)
                            verifier.initVerify(cert.publicKey)
                            verifier.update(signInputText.toByteArray(Charsets.UTF_8))
                            val valid = verifier.verify(sigBytes)
                            signatureVerified = valid
                            statusMessage = if (valid) "Signed and hardware-verified successfully!" else "Signature verification failed!"
                            isErrorStatus = !valid
                        } catch (e: Throwable) {
                            statusMessage = "Signature error: ${e.message}"
                            isErrorStatus = true
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        isOperatingBiometric = false
                        statusMessage = "Auth Error: $errString"
                        isErrorStatus = true
                    }

                    override fun onAuthenticationFailed() {
                        statusMessage = "Biometric not recognized. Try again."
                    }
                }
            )
        } else {
            // Direct sign without prompt
            try {
                signature.update(signInputText.toByteArray(Charsets.UTF_8))
                val sigBytes = signature.sign()
                val sigHex = sigBytes.joinToString("") { "%02x".format(it) }
                signatureResult = sigHex

                val verifier = Signature.getInstance(sigAlg)
                verifier.initVerify(cert.publicKey)
                verifier.update(signInputText.toByteArray(Charsets.UTF_8))
                val valid = verifier.verify(sigBytes)
                signatureVerified = valid
                statusMessage = if (valid) "Signed and verified successfully!" else "Signature verification failed!"
                isErrorStatus = !valid
            } catch (e: Throwable) {
                statusMessage = "Signing failed: ${e.message}"
                isErrorStatus = true
            }
        }
    }

    fun executeEncrypt() {
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val cert = ks.getCertificate(keyAlias)
            if (cert == null) {
                Toast.makeText(context, "Generate or load key '$keyAlias' first!", Toast.LENGTH_SHORT).show()
                return
            }
            if (selectedAlgorithm == KeyAlgorithm.EC_P256) {
                Toast.makeText(context, "EC keys do not support Cipher encryption in AndroidKeyStore. Use RSA 2048/4096.", Toast.LENGTH_LONG).show()
                return
            }
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, cert.publicKey)
            val encryptedBytes = cipher.doFinal(encryptInputText.toByteArray(Charsets.UTF_8))
            encryptedCiphertext = android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP)
            decryptedPlaintext = null
            statusMessage = "Encrypted payload with RSA public key successfully!"
            isErrorStatus = false
        } catch (e: Throwable) {
            statusMessage = "Encryption failed: ${e.message}"
            isErrorStatus = true
        }
    }

    fun executeBiometricDecrypt() {
        val cipherTextBase64 = encryptedCiphertext
        if (cipherTextBase64.isNullOrBlank()) {
            Toast.makeText(context, "Encrypt a payload first!", Toast.LENGTH_SHORT).show()
            return
        }
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val privateKey = ks.getKey(keyAlias, null) as? PrivateKey
        if (privateKey == null) {
            Toast.makeText(context, "Key '$keyAlias' private key not found!", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedAlgorithm == KeyAlgorithm.EC_P256) {
            Toast.makeText(context, "EC keys do not support Cipher decryption in AndroidKeyStore. Use RSA 2048/4096.", Toast.LENGTH_LONG).show()
            return
        }

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)

        if (requireBiometrics) {
            isOperatingBiometric = true
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt.Builder(context)
                .setTitle("Hardware Auth per Use")
                .setSubtitle("Biometric challenge for decryption '$keyAlias'")
                .setDescription("Authorizing private key decrypt operation inside TEE/StrongBox")
                .setNegativeButton("Cancel", executor) { _, _ ->
                    isOperatingBiometric = false
                    statusMessage = "Biometric prompt cancelled"
                }
                .build()

            val cryptoObject = BiometricPrompt.CryptoObject(cipher)
            prompt.authenticate(
                cryptoObject,
                CancellationSignal(),
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        isOperatingBiometric = false
                        try {
                            val authCipher = result.cryptoObject?.cipher ?: cipher
                            val cipherBytes = android.util.Base64.decode(cipherTextBase64, android.util.Base64.NO_WRAP)
                            val decrypted = authCipher.doFinal(cipherBytes)
                            decryptedPlaintext = String(decrypted, Charsets.UTF_8)
                            statusMessage = "Decrypted successfully with Biometric Authentication!"
                            isErrorStatus = false
                        } catch (e: Throwable) {
                            statusMessage = "Decryption error: ${e.message}"
                            isErrorStatus = true
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        isOperatingBiometric = false
                        statusMessage = "Auth Error: $errString"
                        isErrorStatus = true
                    }

                    override fun onAuthenticationFailed() {
                        statusMessage = "Biometric not recognized. Try again."
                    }
                }
            )
        } else {
            try {
                val cipherBytes = android.util.Base64.decode(cipherTextBase64, android.util.Base64.NO_WRAP)
                val decrypted = cipher.doFinal(cipherBytes)
                decryptedPlaintext = String(decrypted, Charsets.UTF_8)
                statusMessage = "Decrypted successfully without biometric gating."
                isErrorStatus = false
            } catch (e: Throwable) {
                statusMessage = "Decryption failed: ${e.message}"
                isErrorStatus = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Keystore & Attestation", fontWeight = FontWeight.Bold)
                        Text(
                            "Hardware Key Generation, TEE/StrongBox & Biometrics",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card: Hardware Key Configuration
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("1. Key Configuration & Generation", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

                    OutlinedTextField(
                        value = keyAlias,
                        onValueChange = { keyAlias = it },
                        label = { Text("KeyStore Alias") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Cryptographic Algorithm:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyAlgorithm.entries.forEach { algo ->
                            FilterChip(
                                selected = selectedAlgorithm == algo,
                                onClick = { selectedAlgorithm = algo },
                                label = { Text(algo.label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("StrongBox Keymaster", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(
                                "Enforce dedicated secure element chip (e.g. Titan M). Falls back to TEE if unsupported.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useStrongBox,
                            onCheckedChange = { useStrongBox = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric Auth-per-Use", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(
                                "Key requires biometric authentication before every signature / decrypt operation.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = requireBiometrics,
                            onCheckedChange = { requireBiometrics = it }
                        )
                    }

                    OutlinedTextField(
                        value = challengeText,
                        onValueChange = { challengeText = it },
                        label = { Text("Attestation Challenge") },
                        supportingText = { Text("Nonce/challenge injected into hardware certificate chain") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { generateHardwareKey() },
                            enabled = !isGenerating && keyAlias.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Attesting...")
                            } else {
                                Text("Generate Key & Attest")
                            }
                        }

                        OutlinedButton(
                            onClick = { parseAttestationFromKey(keyAlias) },
                            enabled = !isGenerating
                        ) {
                            Text("Inspect Existing")
                        }
                    }
                }
            }

            // Status message banner
            if (statusMessage.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isErrorStatus) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp)
                ) {
                    Text(
                        statusMessage,
                        color = if (isErrorStatus) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Card: Hardware Attestation Report
            if (attestationRecord != null) {
                val record = attestationRecord!!
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("2. Hardware Attestation Inspector", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(record.attestationSecurityColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    record.attestationSecurityLevel,
                                    color = record.attestationSecurityColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Attestation Version", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("v${record.attestationVersion}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Keymaster / KeyMint", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("v${record.keymasterVersion} (${record.keymasterSecurityLevel})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Key Origin", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(record.origin ?: "Generated", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        if (record.osVersion != null || record.osPatchLevel != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Device OS Version", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(record.osVersion ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Column {
                                    Text("Security Patch Level", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(record.osPatchLevel ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        // Root of Trust breakdown
                        if (record.rootOfTrust != null) {
                            val rot = record.rootOfTrust
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Root of Trust", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(rot.bootStateColor.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "Boot: ${rot.verifiedBootState}",
                                                color = rot.bootStateColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Bootloader Lock Status:", fontSize = 12.sp)
                                        Text(
                                            if (rot.deviceLocked) "LOCKED (Secure)" else "UNLOCKED (Tampered/Dev)",
                                            color = if (rot.deviceLocked) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Text("Verified Boot Key SHA-256:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        rot.bootKeyHash,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 14.sp
                                    )

                                    if (rot.verifiedBootHash != null) {
                                        Text("Verified Boot Hash:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            rot.verifiedBootHash,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Challenge validation
                        Column {
                            Text("Challenge in Certificate:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${record.challengeString} (Hex: ${record.challengeHex.take(24)}...)",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Card: Biometric Auth-per-use Sign / Verify
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("3. Biometric CryptoObject Operations", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "Test cryptographic hardware isolation by gating live Sign/Verify and Encrypt/Decrypt operations through BiometricPrompt.CryptoObject.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCryptoTab == 0,
                            onClick = { selectedCryptoTab = 0 },
                            label = { Text("Sign / Verify (ECDSA/RSA)") }
                        )
                        FilterChip(
                            selected = selectedCryptoTab == 1,
                            onClick = { selectedCryptoTab = 1 },
                            label = { Text("Encrypt / Decrypt (RSA)") }
                        )
                    }

                    if (selectedCryptoTab == 0) {
                        OutlinedTextField(
                            value = signInputText,
                            onValueChange = { signInputText = it },
                            label = { Text("Payload to Sign") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = { executeBiometricSign() },
                            enabled = !isOperatingBiometric,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (requireBiometrics) "Authenticate with Biometrics & Sign" else "Sign with Hardware Key")
                        }

                        if (signatureResult != null) {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (signatureVerified == true) "Signature Verified ✓" else "Verification Failed ✗",
                                    color = if (signatureVerified == true) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                IconButton(onClick = { copyToClipboard("Signature", signatureResult!!) }) {
                                    Text("📋", fontSize = 16.sp)
                                }
                            }
                            Text(
                                signatureResult!!,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 4,
                                lineHeight = 14.sp
                            )
                        }
                    } else {
                        if (selectedAlgorithm == KeyAlgorithm.EC_P256) {
                            Text(
                                "Note: Encrypt/Decrypt via Biometric CryptoObject requires an RSA keypair. EC keys only support Sign/Verify. Please select RSA 2048 or 4096 in Key Configuration above.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        OutlinedTextField(
                            value = encryptInputText,
                            onValueChange = { encryptInputText = it },
                            label = { Text("Plaintext to Encrypt") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { executeEncrypt() },
                                modifier = Modifier.weight(1f),
                                enabled = selectedAlgorithm != KeyAlgorithm.EC_P256
                            ) {
                                Text("1. Encrypt with Public Key", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { executeBiometricDecrypt() },
                                modifier = Modifier.weight(1f),
                                enabled = !isOperatingBiometric && encryptedCiphertext != null && selectedAlgorithm != KeyAlgorithm.EC_P256
                            ) {
                                Text(if (requireBiometrics) "2. Biometric Decrypt" else "2. Decrypt", fontSize = 12.sp)
                            }
                        }

                        if (encryptedCiphertext != null) {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ciphertext (Base64):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = { copyToClipboard("Ciphertext", encryptedCiphertext!!) }) {
                                    Text("📋", fontSize = 14.sp)
                                }
                            }
                            Text(
                                encryptedCiphertext!!,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 3,
                                lineHeight = 13.sp
                            )
                        }

                        if (decryptedPlaintext != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Decrypted Plaintext:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                                IconButton(onClick = { copyToClipboard("Decrypted", decryptedPlaintext!!) }) {
                                    Text("📋", fontSize = 14.sp)
                                }
                            }
                            Text(
                                decryptedPlaintext!!,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Card: Certificate Chain Explorer
            if (certChainList.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("4. Certificate Chain (${certChainList.size})", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            OutlinedButton(
                                onClick = {
                                    val fullPem = certChainList.joinToString("\n\n") { it.pem }
                                    copyToClipboard("CertChainPEM", fullPem)
                                }
                            ) {
                                Text("Copy Chain PEM", fontSize = 12.sp)
                            }
                        }

                        certChainList.forEach { cert ->
                            val isExpanded = expandedCertIndex == cert.index
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedCertIndex = if (isExpanded) null else cert.index
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(cert.type, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(if (isExpanded) "▲" else "▼", fontSize = 11.sp)
                                    }
                                    Text("Subject: ${cert.subject}", fontSize = 11.sp, maxLines = 1)
                                    Text("Issuer: ${cert.issuer}", fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    if (isExpanded) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        Text("Serial: ${cert.serialNumber}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text("Validity: ${cert.notBefore} → ${cert.notAfter}", fontSize = 11.sp)
                                        Text("Algorithm: ${cert.sigAlg}", fontSize = 11.sp)
                                        Text("SHA-256 Fingerprint:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(cert.sha256Fingerprint, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        Spacer(Modifier.height(4.dp))
                                        OutlinedButton(
                                            onClick = { copyToClipboard("Certificate-${cert.index}", cert.pem) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Copy Certificate PEM", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Robust, zero-dependency ASN.1 DER parser specifically tuned for
 * Android Key Attestation Extension (OID 1.3.6.1.4.1.11129.2.1.17).
 */
object AttestationDecoder {

    fun decode(extensionBytes: ByteArray): ParsedAttestationRecord {
        // Step 1: Unwrap outer OCTET STRING if present
        val payload = unwrapOctetString(extensionBytes)
        val stream = DerInputStream(payload)

        // KeyDescription is a SEQUENCE
        val seq = stream.readSequence()
        val seqStream = DerInputStream(seq)

        val attestationVersion = seqStream.readInteger().toInt()
        val attestationSecurityLevelInt = seqStream.readEnumerated()
        val keymasterVersion = seqStream.readInteger().toInt()
        val keymasterSecurityLevelInt = seqStream.readEnumerated()
        val challengeBytes = seqStream.readOctetString()
        val uniqueIdBytes = seqStream.readOctetString()

        val softwareEnforcedBytes = seqStream.readSequence()
        val teeEnforcedBytes = if (seqStream.hasMore()) seqStream.readSequence() else ByteArray(0)

        // Parse authorization lists (teeEnforced takes precedence for hardware security)
        val swAuth = parseAuthList(softwareEnforcedBytes)
        val teeAuth = parseAuthList(teeEnforcedBytes)

        val osVersionInt = teeAuth.osVersion ?: swAuth.osVersion
        val osPatchLevelInt = teeAuth.osPatchLevel ?: swAuth.osPatchLevel
        val originInt = teeAuth.origin ?: swAuth.origin
        val rootOfTrust = teeAuth.rootOfTrust ?: swAuth.rootOfTrust

        val challengeHex = challengeBytes.joinToString("") { "%02x".format(it) }
        val challengeString = runCatching { String(challengeBytes, Charsets.UTF_8) }.getOrDefault(challengeHex)

        val secLevelPair = securityLevelToInfo(attestationSecurityLevelInt)
        val kmSecPair = securityLevelToInfo(keymasterSecurityLevelInt)

        return ParsedAttestationRecord(
            attestationVersion = attestationVersion,
            attestationSecurityLevel = secLevelPair.first,
            attestationSecurityColor = secLevelPair.second,
            keymasterVersion = keymasterVersion,
            keymasterSecurityLevel = kmSecPair.first,
            challengeHex = challengeHex,
            challengeString = challengeString,
            osVersion = osVersionInt?.let { formatOsVersion(it) },
            osPatchLevel = osPatchLevelInt?.let { formatPatchLevel(it) },
            origin = originInt?.let { originToString(it) },
            rootOfTrust = rootOfTrust
        )
    }

    private fun securityLevelToInfo(level: Int): Pair<String, Color> = when (level) {
        0 -> "Software (Emulated)" to Color(0xFFF57C00) // Amber
        1 -> "TEE (Trusted Execution Env)" to Color(0xFF00897B) // Teal
        2 -> "StrongBox (Hardware Chip)" to Color(0xFF2E7D32) // Emerald Green
        else -> "Unknown ($level)" to Color.Gray
    }

    private fun formatOsVersion(v: Long): String {
        val major = v / 10000
        val minor = (v % 10000) / 100
        val patch = v % 100
        return "$major.$minor.$patch"
    }

    private fun formatPatchLevel(p: Long): String {
        val s = p.toString()
        return if (s.length == 6) {
            "${s.substring(0, 4)}-${s.substring(4, 6)}"
        } else s
    }

    private fun originToString(o: Long): String = when (o.toInt()) {
        0 -> "Generated in Hardware"
        1 -> "Derived"
        2 -> "Imported"
        3 -> "Unknown"
        else -> "Origin #$o"
    }

    private data class ParsedAuthList(
        val osVersion: Long? = null,
        val osPatchLevel: Long? = null,
        val origin: Long? = null,
        val rootOfTrust: ParsedRootOfTrust? = null
    )

    private fun parseAuthList(bytes: ByteArray): ParsedAuthList {
        var osVersion: Long? = null
        var osPatchLevel: Long? = null
        var origin: Long? = null
        var rootOfTrust: ParsedRootOfTrust? = null

        val stream = DerInputStream(bytes)
        while (stream.hasMore()) {
            val tagInfo = stream.readTag()
            val len = stream.readLength()
            val valueBytes = stream.readBytes(len)

            when (tagInfo.tagNumber) {
                701 -> {} // creationDateTime
                702 -> {
                    // origin INTEGER
                    origin = runCatching { DerInputStream(valueBytes).readInteger().toLong() }.getOrNull()
                }
                704 -> {
                    // rootOfTrust RootOfTrust (SEQUENCE)
                    rootOfTrust = parseRootOfTrust(valueBytes)
                }
                705 -> {
                    // osVersion INTEGER
                    osVersion = runCatching { DerInputStream(valueBytes).readInteger().toLong() }.getOrNull()
                }
                706 -> {
                    // osPatchLevel INTEGER
                    osPatchLevel = runCatching { DerInputStream(valueBytes).readInteger().toLong() }.getOrNull()
                }
            }
        }

        return ParsedAuthList(
            osVersion = osVersion,
            osPatchLevel = osPatchLevel,
            origin = origin,
            rootOfTrust = rootOfTrust
        )
    }

    private fun parseRootOfTrust(bytes: ByteArray): ParsedRootOfTrust? = runCatching {
        val stream = DerInputStream(bytes)
        val seq = stream.readSequence()
        val seqStream = DerInputStream(seq)

        val bootKey = seqStream.readOctetString()
        val deviceLocked = seqStream.readBoolean()
        val verifiedBootStateInt = seqStream.readEnumerated()
        val verifiedBootHash = if (seqStream.hasMore()) {
            seqStream.readOctetString().joinToString("") { "%02x".format(it) }
        } else null

        val (stateStr, color) = when (verifiedBootStateInt) {
            0 -> "Verified (OEM Locked)" to Color(0xFF2E7D32)
            1 -> "SelfSigned (Custom Rom)" to Color(0xFFF57C00)
            2 -> "Unverified (Unlocked Bootloader)" to Color(0xFFC62828)
            3 -> "Failed (Integrity Compromised)" to Color(0xFFB71C1C)
            else -> "Unknown ($verifiedBootStateInt)" to Color.Gray
        }

        val bootKeyHash = java.security.MessageDigest.getInstance("SHA-256")
            .digest(bootKey)
            .joinToString("") { "%02x".format(it) }

        ParsedRootOfTrust(
            deviceLocked = deviceLocked,
            verifiedBootState = stateStr,
            bootStateColor = color,
            bootKeyHash = bootKeyHash,
            verifiedBootHash = verifiedBootHash
        )
    }.getOrNull()

    private fun unwrapOctetString(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) return bytes
        if (bytes[0].toInt() == 0x04) {
            val stream = DerInputStream(bytes)
            stream.readTag()
            val len = stream.readLength()
            return stream.readBytes(len)
        }
        return bytes
    }

    data class TagInfo(val tagClass: Int, val isConstructed: Boolean, val tagNumber: Int)

    class DerInputStream(private val data: ByteArray) {
        private var pos = 0

        fun hasMore(): Boolean = pos < data.size

        fun readTag(): TagInfo {
            val b = data[pos++].toInt() and 0xFF
            val tagClass = (b and 0xC0) ushr 6
            val isConstructed = (b and 0x20) != 0
            var tagNumber = b and 0x1F

            if (tagNumber == 0x1F) {
                tagNumber = 0
                while (pos < data.size) {
                    val next = data[pos++].toInt() and 0xFF
                    tagNumber = (tagNumber shl 7) or (next and 0x7F)
                    if ((next and 0x80) == 0) break
                }
            }
            return TagInfo(tagClass, isConstructed, tagNumber)
        }

        fun readLength(): Int {
            val b = data[pos++].toInt() and 0xFF
            if ((b and 0x80) == 0) return b
            val numBytes = b and 0x7F
            var len = 0
            for (i in 0 until numBytes) {
                len = (len shl 8) or (data[pos++].toInt() and 0xFF)
            }
            return len
        }

        fun readBytes(length: Int): ByteArray {
            val actual = length.coerceAtMost(data.size - pos)
            val res = data.copyOfRange(pos, pos + actual)
            pos += actual
            return res
        }

        fun readSequence(): ByteArray {
            val tag = readTag()
            val len = readLength()
            return readBytes(len)
        }

        fun readInteger(): BigInteger {
            readTag()
            val len = readLength()
            val bytes = readBytes(len)
            return if (bytes.isEmpty()) BigInteger.ZERO else BigInteger(bytes)
        }

        fun readEnumerated(): Int {
            readTag()
            val len = readLength()
            val bytes = readBytes(len)
            return if (bytes.isEmpty()) 0 else BigInteger(bytes).toInt()
        }

        fun readOctetString(): ByteArray {
            readTag()
            val len = readLength()
            return readBytes(len)
        }

        fun readBoolean(): Boolean {
            readTag()
            val len = readLength()
            val bytes = readBytes(len)
            return bytes.isNotEmpty() && bytes[0] != 0.toByte()
        }
    }
}

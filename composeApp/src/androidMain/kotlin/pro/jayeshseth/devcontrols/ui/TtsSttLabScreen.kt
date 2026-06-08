package pro.jayeshseth.devcontrols.ui

import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

/** TTS / STT — speak typed text and transcribe speech back. STT needs RECORD_AUDIO. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsSttLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var toSpeak by remember { mutableStateOf("Hello from Dev Controls.") }
    var heard by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS }
        tts = engine
        engine.language = Locale.getDefault()
        onDispose { engine.stop(); engine.shutdown() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("TTS / STT") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Text("Text → Speech", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(toSpeak, { toSpeak = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Text to speak") })
            Spacer(Modifier.height(8.dp))
            Button(onClick = { tts?.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "u1") }, enabled = ready) { Text("Speak") }

            Spacer(Modifier.height(20.dp)); HorizontalDivider(); Spacer(Modifier.height(20.dp))

            Text("Speech → Text", style = MaterialTheme.typography.titleSmall)
            PermGate(android.Manifest.permission.RECORD_AUDIO, "Microphone access is needed to transcribe speech.") {
                Column {
                    val available = remember { SpeechRecognizer.isRecognitionAvailable(context) }
                    if (!available) {
                        Text("No speech recognition service available on this device.", color = MaterialTheme.colorScheme.error)
                    } else {
                        Button(onClick = {
                            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                            recognizer.setRecognitionListener(object : RecognitionListener {
                                override fun onResults(results: android.os.Bundle) {
                                    heard = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                                    listening = false; recognizer.destroy()
                                }
                                override fun onError(error: Int) { heard = "error $error"; listening = false; recognizer.destroy() }
                                override fun onReadyForSpeech(p0: android.os.Bundle?) {}
                                override fun onBeginningOfSpeech() {}
                                override fun onRmsChanged(p0: Float) {}
                                override fun onBufferReceived(p0: ByteArray?) {}
                                override fun onEndOfSpeech() {}
                                override fun onPartialResults(p0: android.os.Bundle?) {}
                                override fun onEvent(p0: Int, p1: android.os.Bundle?) {}
                            })
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            listening = true; heard = ""
                            recognizer.startListening(intent)
                        }, enabled = !listening) { Text(if (listening) "Listening…" else "Start listening") }
                        Spacer(Modifier.height(8.dp))
                        Text(heard.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

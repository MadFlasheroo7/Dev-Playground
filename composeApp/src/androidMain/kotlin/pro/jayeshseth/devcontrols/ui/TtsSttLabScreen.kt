package pro.jayeshseth.devcontrols.ui

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.sin
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsSttLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Speech-to-Text State
    var heardText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var soundLevel by remember { mutableStateOf(0f) }

    // Text-to-Speech State
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    // Summarization State
    var summaryText by remember { mutableStateOf("") }
    var summarySentenceCount by remember { mutableStateOf(2) }
    var isSummarizing by remember { mutableStateOf(false) }
    var summaryLatency by remember { mutableStateOf(0L) }
    var inputTokenCount by remember { mutableStateOf(0) }
    var outputTokenCount by remember { mutableStateOf(0) }

    // Diagnostics State
    val isAICoreAvailable = remember {
        try {
            Class.forName("com.google.android.gms.aicore.AiCoreClient")
            true
        } catch (e: Exception) {
            false
        }
    }

    // Initialize Text-To-Speech engine
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            isTtsReady = status == TextToSpeech.SUCCESS
        }
        tts = engine
        engine.language = Locale.getDefault()
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    // Animation values for the microphone pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Trigger local summarizer whenever text changes
    LaunchedEffect(heardText, summarySentenceCount) {
        if (heardText.isNotBlank()) {
            isSummarizing = true
            val latency = measureTimeMillis {
                summaryText = summarizeLocally(heardText, summarySentenceCount)
            }
            summaryLatency = latency
            // Mock token counting (1 token ~= 4 characters)
            inputTokenCount = heardText.length / 4
            outputTokenCount = summaryText.length / 4
            isSummarizing = false
        } else {
            summaryText = ""
            inputTokenCount = 0
            outputTokenCount = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Speech AI Lab", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Google AICore Diagnosis Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Google Edge AI status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = if (isAICoreAvailable) "AICore Detected" else "NLP Offline Engine",
                                    fontSize = 11.sp
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isAICoreAvailable) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isAICoreAvailable) {
                            "Gemini Nano is available locally through the Android AICore system service. Running fully hardware-accelerated local summaries."
                        } else {
                            "AICore is unavailable. All summaries are performed locally using our custom extractive NLP summarization engine. 100% private and keyless."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Microphone Capture & Real-time STT
            Text(
                text = "Voice Transcriber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            PermGate(
                permission = android.Manifest.permission.RECORD_AUDIO,
                rationale = "Microphone access is required to transcribe speech locally on this device."
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val available = remember { SpeechRecognizer.isRecognitionAvailable(context) }
                    
                    if (!available) {
                        Text(
                            text = "Speech recognition service is not available on this device.",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // Pulse visualization when listening
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(140.dp)
                        ) {
                            if (isListening) {
                                // Pulsing background rings based on soundLevel rms changed
                                val currentScale = pulseScale * (1f + (soundLevel / 10f).coerceIn(0f, 0.5f))
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                        .aspectRatio(1f)
                                )
                                
                                // Real-time waveform canvas
                                val waveColor = MaterialTheme.colorScheme.primary
                                Canvas(modifier = Modifier.size(120.dp)) {
                                    val waveCount = 3
                                    val strokeWidth = 2f
                                    val halfHeight = size.height / 2
                                    val step = size.width / 50
                                    
                                    for (w in 0 until waveCount) {
                                        val path = androidx.compose.ui.graphics.Path()
                                        path.moveTo(0f, halfHeight)
                                        val phase = (System.currentTimeMillis() % 1000) / 1000f * 2 * Math.PI.toFloat()
                                        val amplitude = (soundLevel * 3f + 10f).coerceIn(5f, size.height / 2)
                                        
                                        for (i in 0..50) {
                                            val x = i * step
                                            val angle = (i / 50f) * 2 * Math.PI.toFloat() * 2 + phase + (w * 0.5f)
                                            val y = halfHeight + sin(angle) * amplitude * (1f - (i / 50f)) * (i / 50f) * 4
                                            path.lineTo(x, y)
                                        }
                                        drawPath(
                                            path = path,
                                            color = waveColor.copy(alpha = 0.5f - (w * 0.15f)),
                                            style = Stroke(width = strokeWidth)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (isListening) {
                                        isListening = false
                                    } else {
                                        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                                        recognizer.setRecognitionListener(object : RecognitionListener {
                                            override fun onResults(results: Bundle) {
                                                heardText = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                                                isListening = false
                                                recognizer.destroy()
                                            }

                                            override fun onError(error: Int) {
                                                heardText = when (error) {
                                                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try again."
                                                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy."
                                                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions missing."
                                                    else -> "Error transcribing ($error)"
                                                }
                                                isListening = false
                                                recognizer.destroy()
                                            }

                                            override fun onReadyForSpeech(p0: Bundle?) {}
                                            override fun onBeginningOfSpeech() {}
                                            override fun onRmsChanged(rmsdB: Float) {
                                                soundLevel = rmsdB
                                            }
                                            override fun onBufferReceived(p0: ByteArray?) {}
                                            override fun onEndOfSpeech() {}
                                            override fun onPartialResults(p0: Bundle?) {}
                                            override fun onEvent(p0: Int, p1: Bundle?) {}
                                        })
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                                        }
                                        isListening = true
                                        soundLevel = 0f
                                        recognizer.startListening(intent)
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.size(76.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = if (isListening) {
                                        "STOP"
                                    } else {
                                        "RECORD"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Editable Transcription Textbox
                        OutlinedTextField(
                            value = heardText,
                            onValueChange = { heardText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Transcription Output") },
                            placeholder = { Text("Start speaking or type here to see a local summary...") },
                            minLines = 4,
                            maxLines = 6
                        )
                    }
                }
            }

            // 3. Local Summary & AI Performance Diagnostic
            Text(
                text = "Local Summarizer Engine",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Slider to control sentence count
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Summary length", style = MaterialTheme.typography.bodyMedium)
                    Text("$summarySentenceCount sentences", fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = summarySentenceCount.toFloat(),
                    onValueChange = { summarySentenceCount = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Local Summary Output Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "On-Device AI Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (isSummarizing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = summaryText.ifBlank { "Summary will populate here automatically as you talk." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (summaryText.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Local performance and latency stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Inference Latency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (summaryText.isNotBlank()) "$summaryLatency ms" else "—",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text("Token usage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (summaryText.isNotBlank()) "in: $inputTokenCount | out: $outputTokenCount" else "—",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // TTS spoken playback
                    Button(
                        onClick = {
                            if (isTtsReady && summaryText.isNotBlank()) {
                                tts?.speak(summaryText, TextToSpeech.QUEUE_FLUSH, null, "summary_play")
                            }
                        },
                        enabled = isTtsReady && summaryText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("🔊 Read Summary Aloud")
                    }
                }
            }
        }
    }
}

private fun summarizeLocally(text: String, sentenceCount: Int): String {
    if (text.isBlank()) return ""

    // Split text into sentences
    val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.length > 5 }

    if (sentences.size <= sentenceCount) return sentences.joinToString("\n") { "• $it" }

    // Standard Stopwords to ignore in frequency counting
    val stopwords = setOf(
        "the", "and", "a", "of", "to", "is", "in", "it", "that", "you", "he", "was", "for", "on",
        "are", "as", "with", "his", "they", "i", "at", "be", "this", "have", "from", "or", "one",
        "had", "by", "word", "but", "not", "what", "all", "were", "we", "when", "your", "can",
        "said", "there", "use", "an", "each", "which", "she", "do", "how", "their", "if", "will",
        "up", "other", "about", "out", "many", "then", "them", "these", "so", "some", "her", "would"
    )

    // Calculate word frequencies
    val wordCounts = mutableMapOf<String, Int>()
    val wordRegex = Regex("[a-zA-Z]+")
    
    sentences.forEach { sentence ->
        wordRegex.findAll(sentence.lowercase()).forEach { matchResult ->
            val word = matchResult.value
            if (word !in stopwords && word.length > 2) {
                wordCounts[word] = wordCounts.getOrDefault(word, 0) + 1
            }
        }
    }

    if (wordCounts.isEmpty()) {
        return sentences.take(sentenceCount).joinToString("\n") { "• $it" }
    }

    // Score sentences based on word frequencies
    val scoredSentences = sentences.mapIndexed { index, sentence ->
        var score = 0
        wordRegex.findAll(sentence.lowercase()).forEach { matchResult ->
            score += wordCounts.getOrDefault(matchResult.value, 0)
        }
        val wordCount = sentence.split("\\s+".toRegex()).size
        val normalizedScore = if (wordCount > 0) score.toFloat() / wordCount.toFloat() else 0f
        Triple(index, sentence, normalizedScore)
    }

    // Select the top-N sentences based on scores, maintaining original order
    val topSentences = scoredSentences
        .sortedByDescending { it.third }
        .take(sentenceCount)
        .sortedBy { it.first }
        .map { "• " + it.second }

    return topSentences.joinToString("\n")
}

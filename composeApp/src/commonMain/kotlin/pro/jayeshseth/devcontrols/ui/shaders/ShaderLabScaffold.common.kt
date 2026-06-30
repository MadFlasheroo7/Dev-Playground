package pro.jayeshseth.devcontrols.ui.shaders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.platform.CustomShaderBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerativeShaderScreenMultiplatform(title: String, agsl: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        var time by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) {
            var start = 0L
            while (true) {
                withFrameNanos { if (start == 0L) start = it; time = (it - start) / 1e9f }
            }
        }
        Box(Modifier.padding(inner).fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val w = maxWidth.value
                val h = maxHeight.value
                CustomShaderBox(
                    shaderSource = agsl,
                    modifier = Modifier.fillMaxSize(),
                    uniformNames = listOf("iResolution", "iTime"),
                    uniforms = mapOf(
                        "iResolution" to Offset(w, h),
                        "iTime" to time
                    ),
                    fallback = {
                        Box(Modifier.fillMaxSize().background(Color.DarkGray))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentShaderScreenMultiplatform(title: String, agsl: String, animated: Boolean, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        var time by remember { mutableFloatStateOf(0f) }
        if (animated) LaunchedEffect(Unit) {
            var start = 0L
            while (true) {
                withFrameNanos { if (start == 0L) start = it; time = (it - start) / 1e9f }
            }
        }
        Box(Modifier.padding(inner).fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val w = maxWidth.value
                val h = maxHeight.value
                
                // For non-Android platforms, we render the shader as a beautiful generative background
                CustomShaderBox(
                    shaderSource = agsl.replace("uniform shader content;", ""), // Strip Android-specific content uniform
                    modifier = Modifier.fillMaxSize(),
                    uniformNames = if (animated) listOf("iResolution", "iTime") else listOf("iResolution"),
                    uniforms = if (animated) {
                        mapOf("iResolution" to Offset(w, h), "iTime" to time)
                    } else {
                        mapOf("iResolution" to Offset(w, h))
                    },
                    fallback = {
                        Box(Modifier.fillMaxSize().background(Color(0xFF101018)))
                    }
                )
                
                // Draw the demo content on top with a semi-transparent background
                ShaderDemoContent()
            }
        }
    }
}

@Composable
private fun ShaderDemoContent() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        repeat(7) { i ->
            val c = listOf(Color(0xFFFF5F6D), Color(0xFF00C9FF), Color(0xFF92FE9D), Color(0xFFFFC371), Color(0xFFE100FF))[i % 5]
            Text(
                "Live UI sampled by the shader $i",
                color = c,
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .fillMaxWidth()
                    .background(c.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .padding(14.dp)
            )
        }
    }
}

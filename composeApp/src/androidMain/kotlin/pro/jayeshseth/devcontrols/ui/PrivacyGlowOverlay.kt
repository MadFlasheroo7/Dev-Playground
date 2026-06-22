package pro.jayeshseth.devcontrols.ui

import android.app.AppOpsManager
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush

private const val RAINBOW_GLOW_AGSL = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float alphaCamera;
    uniform float alphaMic;
    uniform float alphaLocation;

    half3 hsv2rgb(half3 c) {
        half4 K = half4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
        half3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
        return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
    }

    float sdRoundedBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + r;
        return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
    }

    half4 main(float2 fragCoord) {
        float totalAlpha = max(max(alphaCamera, alphaMic), alphaLocation);
        if (totalAlpha < 0.01) return half4(0.0);

        float2 p = fragCoord - iResolution.xy * 0.5;
        float2 halfSize = iResolution.xy * 0.5;
        float cornerRadius = min(50.0, min(halfSize.x, halfSize.y));
        float d = sdRoundedBox(p, halfSize, cornerRadius);
        
        float dist = -d;
        if (dist < 0.0) return half4(0.0);

        float edgeAlpha = smoothstep(0.0, 4.0, dist);

        float t = iTime * 0.8;
        float2 uv = fragCoord / iResolution.xy;
        float angle = atan(uv.y - 0.5, uv.x - 0.5);
        float hue = fract(angle / 6.28 + t * 0.2);
        
        // Dynamic wavy ripples along the inner glow edge
        float wave = sin(angle * 8.0 - t * 4.0) * 10.0 + cos(angle * 15.0 + t * 2.5) * 4.0;
        float wavyDist = max(0.1, dist + wave);

        float glow = (0.7 * exp(-wavyDist / 22.0) + 0.4 * exp(-wavyDist / 60.0)) * edgeAlpha;
        
        half3 color = hsv2rgb(half3(hue, 0.8, 1.0));
        
        half3 targetColor = half3(0.0);
        float totalWeight = 0.0;
        if (alphaCamera > 0.0) {
            targetColor += half3(1.0, 0.0, 0.0) * alphaCamera;
            totalWeight += alphaCamera;
        }
        if (alphaMic > 0.0) {
            targetColor += half3(1.0, 0.5, 0.0) * alphaMic;
            totalWeight += alphaMic;
        }
        if (alphaLocation > 0.0) {
            targetColor += half3(0.0, 1.0, 1.0) * alphaLocation;
            totalWeight += alphaLocation;
        }
        if (totalWeight > 0.0) {
            targetColor /= totalWeight;
            color = mix(color, targetColor, 0.85);
        }

        glow *= (0.8 + 0.2 * sin(t * 2.5 - dist * 0.06));

        float finalAlpha = glow * totalAlpha;
        return half4(color * finalAlpha, finalAlpha);
    }
"""

@Composable
fun PrivacyGlowOverlay(activeOps: List<String>) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val cameraAlpha = animateFloatAsState(
        if (AppOpsManager.OPSTR_CAMERA in activeOps) 1f else 0f,
        label = "cameraAlpha",
        animationSpec = tween(1000)
    )
    val micAlpha = animateFloatAsState(
        if (AppOpsManager.OPSTR_RECORD_AUDIO in activeOps) 1f else 0f,
        label = "micAlpha",
        animationSpec = tween(1000)
    )
    val locationActive = AppOpsManager.OPSTR_FINE_LOCATION in activeOps || 
                         AppOpsManager.OPSTR_COARSE_LOCATION in activeOps
    val locationAlpha = animateFloatAsState(
        if (locationActive) 1f else 0f,
        label = "locationAlpha",
        animationSpec = tween(1000)
    )

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (true) {
            withFrameNanos {
                time = (System.currentTimeMillis() - start) / 1000f
            }
        }
    }

    val shader = remember { RuntimeShader(RAINBOW_GLOW_AGSL) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iTime", time)
        shader.setFloatUniform("alphaCamera", cameraAlpha.value)
        shader.setFloatUniform("alphaMic", micAlpha.value)
        shader.setFloatUniform("alphaLocation", locationAlpha.value)

        drawRect(brush = ShaderBrush(shader))
    }

}

package pro.jayeshseth.devcontrols.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import pro.jayeshseth.audiostudio.AudioStudioController
import pro.jayeshseth.audiostudio.ui.GarageBandPedalScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioStudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasRecordPermission) {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val controller = remember { AudioStudioController(context) }
    DisposableEffect(Unit) {
        onDispose {
            controller.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Studio & Pedal") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF141418),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (hasRecordPermission) {
            GarageBandPedalScreen(
                controller = controller,
                modifier = Modifier.padding(padding)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Grant Microphone Permission")
                }
            }
        }
    }
}
